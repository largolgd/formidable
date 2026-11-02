import {buildContactOptions, buildSourceFields, type ContactNode, type ContactOption, type FieldNode, siteKeyOf, type SourceField} from './routing';

export interface RoutingSourcesInput {
    path: string;
    workspace: string;
    language: string;
    defaultLanguage: string;
}

export interface RoutingSources {
    fields: SourceField[];
    contacts: ContactOption[];
}

// The fields that submit a value (the engine's fmdbmix:submittableField marker) of the form above the
// action, and the contact options of the site: what a rule is chosen among.
const QUERY = `
    query ContactRoutingSources($path: String!, $workspace: Workspace!, $language: String!, $defaultLanguage: String!, $contactsQuery: String!) {
        jcr(workspace: $workspace) {
            nodeByPath(path: $path) {
                ancestors(fieldFilter: {filters: [{fieldName: "primaryNodeType.name", value: "fmdb:form"}]}) {
                    uuid
                    primaryNodeType { name }
                    descendants(typesFilter: {types: ["fmdbmix:submittableField"], multi: ANY}) {
                        nodes {
                            uuid
                            name
                            displayName(language: $language)
                            isChoiceField: isNodeType(type: {types: ["fmdbmix:choiceField"]})
                            properties(names: ["options"], language: $language) { name values }
                            defaultProperties: properties(names: ["options"], language: $defaultLanguage) { name values }
                        }
                    }
                }
            }
            contacts: nodesByQuery(query: $contactsQuery) {
                nodes { uuid name displayName(language: $language) }
            }
        }
    }
`;

interface Response {
    data?: {
        jcr?: {
            nodeByPath?: {ancestors?: {primaryNodeType?: {name?: string}; descendants?: {nodes?: FieldNode[]}}[]} | null;
            contacts?: {nodes?: ContactNode[]} | null;
        } | null;
    } | null;
    errors?: {message: string}[];
}

const contextPath = (): string =>
    (globalThis as typeof globalThis & {contextJsParameters?: {contextPath?: string}}).contextJsParameters?.contextPath ?? '';

export const fetchSources = async (input: RoutingSourcesInput): Promise<RoutingSources> => {
    const site = siteKeyOf(input.path);
    if (!site) {
        throw new Error(`no site above ${input.path}`);
    }

    const response = await fetch(`${contextPath()}/modules/graphql`, {
        method: 'POST',
        credentials: 'same-origin',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({
            query: QUERY,
            variables: {
                ...input,
                contactsQuery: `select * from [fmdbsample:contactFormOption] where isdescendantnode('/sites/${site}')`
            }
        })
    });
    if (!response.ok) {
        throw new Error(`GraphQL answered ${response.status}`);
    }

    const result = await response.json() as Response;
    if (result.errors?.length) {
        throw new Error(result.errors.map(error => error.message).join('; '));
    }

    const jcr = result.data?.jcr;
    const form = jcr?.nodeByPath?.ancestors?.find(ancestor => ancestor.primaryNodeType?.name === 'fmdb:form');
    if (!form) {
        throw new Error(`no form above ${input.path}`);
    }

    return {
        fields: buildSourceFields(form.descendants?.nodes ?? []),
        contacts: buildContactOptions(jcr?.contacts?.nodes ?? [])
    };
};

// The editor mounts one component per stored rule, each wanting the same form: they share one request.
// Kept for a few seconds only, so that reopening the editor reads the form again.
const SHARE_MS = 3000;
const loads = new Map<string, Promise<RoutingSources>>();

export const loadSources = (input: RoutingSourcesInput): Promise<RoutingSources> => {
    const key = [input.workspace, input.path, input.language, input.defaultLanguage].join('|');
    let load = loads.get(key);
    if (!load) {
        load = fetchSources(input);
        loads.set(key, load);
        const forget = () => setTimeout(() => loads.delete(key), SHARE_MS);
        load.then(forget, () => loads.delete(key));
    }

    return load;
};
