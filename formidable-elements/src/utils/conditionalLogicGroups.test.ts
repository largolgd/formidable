import {describe, expect, it} from "vitest";
import {
	type ConditionalLogicRule,
	DEFAULT_RULE_GROUP,
	isVisibleByGroups,
	parseConditionalLogicRule,
	ruleGroupOf
} from "./conditionalLogic";

const rule = (sourceFieldName: string, group?: string): ConditionalLogicRule => ({
	sourceFieldName,
	operator: "in",
	values: ["yes"],
	group
});

/** (a AND b) OR c, with the given fields "on". */
const aAndBOrC = (on: string[]) =>
	isVisibleByGroups([rule("a", "1"), rule("b", "1"), rule("c", "2")], r => on.includes(r.sourceFieldName ?? ""));

describe("rule groups", () => {
	it("ands the rules of a group and ors the groups", () => {
		expect(aAndBOrC(["a", "b"])).toBe(true);
		expect(aAndBOrC(["c"])).toBe(true);
		expect(aAndBOrC(["a", "b", "c"])).toBe(true);
		expect(aAndBOrC(["a"])).toBe(false);
		expect(aAndBOrC([])).toBe(false);
	});

	it("keeps rules without a group as one anded group", () => {
		const rules = [rule("a"), rule("b")];
		expect(isVisibleByGroups(rules, r => r.sourceFieldName === "a")).toBe(false);
		expect(isVisibleByGroups(rules, () => true)).toBe(true);
	});

	it("treats a blank group as the default group, like the server", () => {
		expect(ruleGroupOf(rule("a"))).toBe(DEFAULT_RULE_GROUP);
		expect(ruleGroupOf(rule("a", "  "))).toBe(DEFAULT_RULE_GROUP);
		expect(ruleGroupOf(rule("a", "2"))).toBe("2");
	});

	it("carries the group through the parser", () => {
		const parsed = parseConditionalLogicRule(
			JSON.stringify({sourceFieldName: "a", operator: "in", values: ["x"], group: "2"})
		);
		expect(parsed?.group).toBe("2");
	});

	it("shows a field with no rule at all, like the server", () => {
		expect(isVisibleByGroups([], () => false)).toBe(true);
	});
});
