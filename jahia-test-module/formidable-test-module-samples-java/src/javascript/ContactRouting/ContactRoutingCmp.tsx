import {Dropdown, Input, Loader, Typography} from '@jahia/moonstone';
import React, {useEffect, useMemo, useState} from 'react';
import {useTranslation} from 'react-i18next';
import {parseRule, type RoutingRule, serializeRule, withStoredValue} from './routing';
import {loadSources, type RoutingSources} from './sources';
import './contactRouting.css';

interface EditorContextLike {
    path?: string;
    lang?: string;
    language?: string;
    uilang?: string;
    workspace?: string;
    siteInfo?: {defaultLanguage?: string};
    nodeData?: {path?: string; lang?: string; language?: string; workspace?: string};
}

/** What the Content Editor hands a selector (the part this one reads). */
interface SelectorProps {
    field: {readOnly?: boolean; node?: {path?: string}; nodePath?: string; path?: string};
    id: string;
    value?: string;
    onChange: (value: string) => void;
    editorContext?: EditorContextLike;
    context?: EditorContextLike;
}

const generateId = (): string => Math.random().toString(36).substring(2, 10);

type Load = {state: 'loading'} | {state: 'error'} | {state: 'ready'; sources: RoutingSources};

/**
 * One routing rule: "when this field holds this value, also send to this contact". The editor mounts the
 * component once per stored rule (supportMultiple: false) and offers the add button, as it does for the
 * conditional display rules of the engine.
 */
export const ContactRoutingCmp = ({field, id, value, onChange, editorContext, context}: SelectorProps) => {
    const {t} = useTranslation('formidable-test-module-samples-java');
    const editor = editorContext ?? context;
    const path = field.node?.path ?? field.nodePath ?? field.path ?? editor?.nodeData?.path ?? editor?.path;
    const language = editor?.nodeData?.language ?? editor?.nodeData?.lang ?? editor?.language ?? editor?.lang ?? editor?.uilang ?? 'en';
    const defaultLanguage = editor?.siteInfo?.defaultLanguage ?? language;
    const workspace = editor?.nodeData?.workspace ?? editor?.workspace ?? 'EDIT';

    const rule = useMemo(() => parseRule(value), [value]);
    const [load, setLoad] = useState<Load>({state: path ? 'loading' : 'error'});

    useEffect(() => {
        if (!path) {
            return undefined;
        }

        let cancelled = false;
        loadSources({path, workspace, language, defaultLanguage})
            .then(sources => !cancelled && setLoad({state: 'ready', sources}))
            .catch((error: unknown) => {
                console.error('[ContactRouting] failed to load the form and the contact options', error);
                if (!cancelled) {
                    setLoad({state: 'error'});
                }
            });
        return () => {
            cancelled = true;
        };
    }, [path, workspace, language, defaultLanguage]);

    if (load.state === 'loading') {
        return <Loader size="small"/>;
    }

    if (load.state === 'error') {
        return <Typography variant="body" style={{color: 'var(--color-danger)'}}>{t('contactRouting.loadError')}</Typography>;
    }

    const {fields, contacts} = load.sources;
    if (fields.length === 0 || contacts.length === 0) {
        return (
            <Typography variant="body" style={{color: 'var(--color-gray)'}}>
                {t(fields.length === 0 ? 'contactRouting.noFields' : 'contactRouting.noContacts')}
            </Typography>
        );
    }

    const update = (patch: Partial<RoutingRule>) =>
        onChange(serializeRule({...rule, ...patch, logicId: rule.logicId || generateId()}));

    const selectedField = fields.find(candidate => candidate.id === rule.sourceNodeId);
    const fieldOptions = fields.map(candidate => ({label: candidate.label, value: candidate.id}));
    const contactOptions = contacts.map(candidate => ({label: candidate.label, value: candidate.id}));
    const valueOptions = withStoredValue(selectedField?.choiceValues ?? [], rule.value)
        .map(choice => ({label: choice.label, value: choice.value}));

    return (
        <div className="fmdbRoutingGrid" data-sel-role="contact-routing-rule">
            <div>
                <Dropdown
                    variant="outlined"
                    data={fieldOptions}
                    hasSearch={fieldOptions.length >= 5}
                    value={selectedField?.id}
                    placeholder={t('contactRouting.selectField')}
                    isDisabled={field.readOnly}
                    onChange={(_event: React.MouseEvent, item: {value?: string}) => {
                        const next = fields.find(candidate => candidate.id === item.value);
                        // A new field starts without a value: the old one belonged to another field's options
                        update({sourceNodeId: next?.id ?? '', fieldName: next?.name ?? '', value: ''});
                    }}
                />
            </div>
            <div>
                {valueOptions.length > 0 ? (
                    <Dropdown
                        variant="outlined"
                        data={valueOptions}
                        hasSearch={valueOptions.length >= 5}
                        value={rule.value || undefined}
                        placeholder={t('contactRouting.selectValue')}
                        isDisabled={field.readOnly || !selectedField}
                        onChange={(_event: React.MouseEvent, item: {value?: string}) => update({value: item.value ?? ''})}
                    />
                ) : (
                    <Input
                        id={`${id}-value`}
                        size="big"
                        isReadOnly={field.readOnly}
                        isDisabled={!selectedField}
                        placeholder={t('contactRouting.value')}
                        title={rule.value || undefined}
                        value={rule.value}
                        onChange={event => update({value: event.target.value})}
                    />
                )}
            </div>
            <div>
                <Dropdown
                    variant="outlined"
                    data={contactOptions}
                    hasSearch={contactOptions.length >= 5}
                    value={rule.contactId || undefined}
                    placeholder={t('contactRouting.selectContact')}
                    isDisabled={field.readOnly}
                    onChange={(_event: React.MouseEvent, item: {value?: string}) => update({contactId: item.value ?? ''})}
                />
            </div>
        </div>
    );
};

ContactRoutingCmp.displayName = 'ContactRoutingCmp';
