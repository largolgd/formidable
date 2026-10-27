/**
 * The rules of the contact routing section, and what the editor builds them from. Duplicated in spirit
 * from the engine's conditional display editor — the samples do not import the engine's editor code —
 * but much smaller: a rule is "when this field holds this value, also send to this contact option".
 */

/** One stored rule. The JSON of one entry of the action's `routes` property. */
export interface RoutingRule {
    /** Identity of the rule inside the editor; not used at runtime. */
    logicId: string;
    /** The field the rule reads, by node identifier: what the editor resolves it with, and what survives a rename. */
    sourceNodeId: string;
    /** The field's node name, the name its value is submitted under. The fallback when the node is no longer there. */
    fieldName: string;
    /** The value the field must hold; for a field with several values (check boxes), one of them. */
    value: string;
    /** The fmdbsample:contactFormOption node whose addresses the message is also sent to. */
    contactId: string;
}

export const EMPTY_RULE: RoutingRule = {logicId: '', sourceNodeId: '', fieldName: '', value: '', contactId: ''};

const text = (value: unknown): string => (typeof value === 'string' ? value : '');

export const parseRule = (raw?: string): RoutingRule => {
    if (!raw) {
        return {...EMPTY_RULE};
    }

    try {
        const parsed = JSON.parse(raw) as Partial<Record<keyof RoutingRule, unknown>>;
        return {
            logicId: text(parsed.logicId),
            sourceNodeId: text(parsed.sourceNodeId),
            fieldName: text(parsed.fieldName),
            value: text(parsed.value),
            contactId: text(parsed.contactId)
        };
    } catch {
        return {...EMPTY_RULE};
    }
};

export const serializeRule = (rule: RoutingRule): string => JSON.stringify(rule);

/** A rule routes only once it names a field, a value and a contact: the runtime ignores the others. */
export const isComplete = (rule: RoutingRule): boolean =>
    rule.sourceNodeId !== '' && rule.value !== '' && rule.contactId !== '';

export interface ChoiceValue {
    value: string;
    label: string;
}

/** A field a rule can read: one that submits a value. A choice field offers its options as the values. */
export interface SourceField {
    id: string;
    name: string;
    label: string;
    choiceValues: ChoiceValue[];
}

export interface ContactOption {
    id: string;
    label: string;
}

export interface FieldNode {
    uuid: string;
    name: string;
    displayName?: string | null;
    isChoiceField?: boolean;
    properties?: {name: string; values?: string[] | null}[] | null;
    defaultProperties?: {name: string; values?: string[] | null}[] | null;
}

export interface ContactNode {
    uuid: string;
    name: string;
    displayName?: string | null;
}

const parseChoices = (rawValues: string[] = []): ChoiceValue[] =>
    rawValues.flatMap(raw => {
        try {
            const parsed = JSON.parse(raw) as {value?: unknown; label?: unknown};
            return typeof parsed.value === 'string' && parsed.value !== ''
                ? [{value: parsed.value, label: typeof parsed.label === 'string' ? parsed.label : parsed.value}]
                : [];
        } catch {
            return [];
        }
    });

/**
 * The options of a choice field. The identity is the site default language's list — the values every
 * language submits — labelled in the current language when it has a label for the value.
 */
export const mergeChoices = (own: ChoiceValue[], defaults: ChoiceValue[]): ChoiceValue[] => {
    if (defaults.length === 0) {
        return own;
    }

    const labels = new Map(own.filter(choice => choice.label.trim() !== '').map(choice => [choice.value, choice.label]));
    return defaults.map(choice => ({
        value: choice.value,
        label: labels.get(choice.value) ?? (choice.label.trim() === '' ? choice.value : choice.label)
    }));
};

export const buildSourceFields = (nodes: FieldNode[] = []): SourceField[] => {
    const fields = nodes.map(node => {
        const own = node.properties?.find(property => property.name === 'options')?.values ?? [];
        const defaults = node.defaultProperties?.find(property => property.name === 'options')?.values ?? [];
        return {
            id: node.uuid,
            name: node.name,
            label: node.displayName ?? node.name,
            choiceValues: node.isChoiceField ? mergeChoices(parseChoices(own), parseChoices(defaults)) : []
        };
    });

    // Two fields may share a title: told apart by a counter, as the conditional display does
    const counts = new Map<string, number>();
    fields.forEach(field => counts.set(field.label, (counts.get(field.label) ?? 0) + 1));
    const seen = new Map<string, number>();
    return fields.map(field => {
        if ((counts.get(field.label) ?? 0) < 2) {
            return field;
        }

        const index = (seen.get(field.label) ?? 0) + 1;
        seen.set(field.label, index);
        return {...field, label: `${field.label}:${index}`};
    });
};

export const buildContactOptions = (nodes: ContactNode[] = []): ContactOption[] =>
    nodes.map(node => ({id: node.uuid, label: node.displayName ?? node.name}));

/** The site key of a content path (/sites/<key>/...), or undefined outside a site. */
export const siteKeyOf = (path?: string): string | undefined => /^\/sites\/([A-Za-z0-9_-]+)(\/|$)/.exec(path ?? '')?.[1];

/** The values a dropdown offers: the choices, plus a stored value they no longer list, shown as it is rather than blank. */
export const withStoredValue = (choices: ChoiceValue[], stored: string): ChoiceValue[] =>
    stored === '' || choices.some(choice => choice.value === stored) ? choices : [...choices, {value: stored, label: stored}];
