import {describe, expect, it} from 'vitest';
import {groupChoices, groupOf, normalizeStoredProviderRule, parseRule, storedGroup} from './ConditionalLogic.utils';

describe('rule groups', () => {
    it('puts a rule without a group in group 1', () => {
        expect(groupOf(parseRule(JSON.stringify({operator: 'in'})))).toBe('1');
    });

    it('stores nothing for the default group, so existing rules keep their JSON', () => {
        expect(storedGroup(undefined)).toBeUndefined();
        expect(storedGroup('1')).toBeUndefined();
        expect(storedGroup('2')).toBe('2');
    });

    it('round-trips the group of a rule', () => {
        expect(parseRule(JSON.stringify({operator: 'in', group: '3'})).group).toBe('3');
    });

    it('offers the groups in use plus the next free one, up to the cap', () => {
        expect(groupChoices([], '1')).toEqual(['1', '2']);
        expect(groupChoices(['1', '2'], '1')).toEqual(['1', '2', '3']);
        expect(groupChoices(['8'], '8')).toHaveLength(8);
    });

    it('keeps the group when a provider rule is serialized', () => {
        const stored = normalizeStoredProviderRule({
            logicId: 'x', sourceType: 'cookie', cookie: 'consent', operator: 'exists', group: '2'
        });
        expect(stored.group).toBe('2');
    });
});
