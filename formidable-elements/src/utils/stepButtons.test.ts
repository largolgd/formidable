import {describe, expect, it} from "vitest";
import {stepButtonsOf, visibleStepButtons} from "./stepButtons";

describe("stepButtonsOf", () => {
	it("offers every button to a step that carries no setting", () => {
		expect(stepButtonsOf({})).toEqual({previous: true, next: true});
	});

	it("turns a button off only when the author did", () => {
		expect(stepButtonsOf({showPreviousBtn: false, showNextBtn: true})).toEqual({previous: false, next: true});
		expect(stepButtonsOf({showNextBtn: false})).toEqual({previous: true, next: false});
	});
});

describe("visibleStepButtons", () => {
	const all = {previous: true, next: true};

	it("follows the position: Next in the middle, Submit at the end, Previous after the first", () => {
		expect(visibleStepButtons({isFirstVisibleStep: true, isLastStep: false}, all)).toEqual({previous: false, next: true, submit: false});
		expect(visibleStepButtons({isFirstVisibleStep: false, isLastStep: false}, all)).toEqual({previous: true, next: true, submit: false});
		expect(visibleStepButtons({isFirstVisibleStep: false, isLastStep: true}, all)).toEqual({previous: true, next: false, submit: true});
	});

	it("gives a single step only Submit", () => {
		expect(visibleStepButtons({isFirstVisibleStep: true, isLastStep: true}, all)).toEqual({previous: false, next: false, submit: true});
	});

	it("applies the forward setting to Next in the middle and to Submit at the end, one at a time", () => {
		const off = {previous: true, next: false};
		expect(visibleStepButtons({isFirstVisibleStep: false, isLastStep: false}, off)).toEqual({previous: true, next: false, submit: false});
		expect(visibleStepButtons({isFirstVisibleStep: false, isLastStep: true}, off)).toEqual({previous: true, next: false, submit: false});
	});

	it("never shows Next and Submit together", () => {
		for (const isLastStep of [false, true]) {
			const shown = visibleStepButtons({isFirstVisibleStep: false, isLastStep}, {previous: true, next: true});
			expect(shown.next && shown.submit).toBe(false);
		}
	});

	it("lets the author take Previous away", () => {
		expect(visibleStepButtons({isFirstVisibleStep: false, isLastStep: false}, {previous: false, next: true}).previous).toBe(false);
	});

	it("keeps the historical buttons without settings", () => {
		expect(visibleStepButtons({isFirstVisibleStep: false, isLastStep: false})).toEqual({previous: true, next: true, submit: false});
	});
});
