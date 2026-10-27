import {describe, expect, it} from 'vitest';
import {
    buildContactOptions,
    buildSourceFields,
    isComplete,
    mergeChoices,
    parseRule,
    serializeRule,
    siteKeyOf,
    withStoredValue
} from './routing';

describe('rule', () => {
    it('round-trips through its JSON', () => {
        const rule = {logicId: 'a1', sourceNodeId: 'uuid-1', fieldName: 'topic', value: 'sales', contactId: 'uuid-2'};
        expect(parseRule(serializeRule(rule))).toEqual(rule);
    });

    it('reads a missing or broken entry as an empty rule', () => {
        const empty = {logicId: '', sourceNodeId: '', fieldName: '', value: '', contactId: ''};
        expect(parseRule(undefined)).toEqual(empty);
        expect(parseRule('{not json')).toEqual(empty);
        expect(parseRule(JSON.stringify({value: 3, contactId: null}))).toEqual(empty);
    });

    it('routes only once a field, a value and a contact are chosen', () => {
        const rule = {logicId: 'a', sourceNodeId: 'f', fieldName: 'topic', value: 'sales', contactId: 'c'};
        expect(isComplete(rule)).toBe(true);
        expect(isComplete({...rule, value: ''})).toBe(false);
        expect(isComplete({...rule, contactId: ''})).toBe(false);
        expect(isComplete({...rule, sourceNodeId: ''})).toBe(false);
    });
});

describe('choices', () => {
    const option = (value: string, label: string) => JSON.stringify({value, label});

    it('takes the identity of the default language and the labels of the current one', () => {
        const merged = mergeChoices(
            [{value: 'a', label: 'Alpha'}, {value: 'b', label: ''}],
            [{value: 'a', label: 'Default A'}, {value: 'b', label: 'Default B'}, {value: 'c', label: ''}]
        );
        expect(merged).toEqual([
            {value: 'a', label: 'Alpha'},
            {value: 'b', label: 'Default B'},
            {value: 'c', label: 'c'}
        ]);
    });

    it('falls back on its own list when the default language has none', () => {
        expect(mergeChoices([{value: 'x', label: 'X'}], [])).toEqual([{value: 'x', label: 'X'}]);
    });

    it('lists the options of a choice field only', () => {
        const fields = buildSourceFields([
            {uuid: '1', name: 'topic', displayName: 'Topic', isChoiceField: true, properties: [{name: 'options', values: [option('s', 'Sales')]}]},
            {uuid: '2', name: 'who', displayName: 'Who'}
        ]);
        expect(fields.map(field => field.choiceValues.length)).toEqual([1, 0]);
        expect(fields[0].choiceValues[0]).toEqual({value: 's', label: 'Sales'});
    });

    it('tells two fields with one title apart', () => {
        const fields = buildSourceFields([{uuid: '1', name: 'a', displayName: 'Name'}, {uuid: '2', name: 'b', displayName: 'Name'}]);
        expect(fields.map(field => field.label)).toEqual(['Name:1', 'Name:2']);
    });

    it('keeps a stored value the options no longer list, shown as it is', () => {
        expect(withStoredValue([{value: 'a', label: 'A'}], 'gone')).toEqual([{value: 'a', label: 'A'}, {value: 'gone', label: 'gone'}]);
        expect(withStoredValue([{value: 'a', label: 'A'}], 'a')).toEqual([{value: 'a', label: 'A'}]);
        expect(withStoredValue([], '')).toEqual([]);
    });
});

describe('contacts and site', () => {
    it('labels a contact by its title, else its name', () => {
        expect(buildContactOptions([{uuid: '1', name: 'sales', displayName: 'Sales team'}, {uuid: '2', name: 'boss'}])).toEqual([
            {id: '1', label: 'Sales team'},
            {id: '2', label: 'boss'}
        ]);
    });

    it('reads the site key of a content path', () => {
        expect(siteKeyOf('/sites/luxe/home/forms/main/form')).toBe('luxe');
        expect(siteKeyOf('/sites/my-site_2')).toBe('my-site_2');
        expect(siteKeyOf('/modules/x')).toBeUndefined();
        expect(siteKeyOf(undefined)).toBeUndefined();
        // the key goes into a query: nothing that could close its quote gets through
        expect(siteKeyOf("/sites/a'b/home")).toBeUndefined();
    });
});
