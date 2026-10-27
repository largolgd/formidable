/**
 * What an author sets on a step (fmdb:step) about its navigation buttons. A step that carries none of
 * the properties — content authored before they existed — offers all of them.
 *
 * `next` governs the forward button of the step, whichever it is: Next on every step but the last,
 * Submit on the last — a step only ever has one of the two.
 */
export interface StepButtons {
	previous: boolean;
	next: boolean;
}

/** The buttons actually shown on the step on screen. */
export interface ShownButtons {
	previous: boolean;
	next: boolean;
	submit: boolean;
}

export const ALL_STEP_BUTTONS: StepButtons = {previous: true, next: true};

/** The settings of a step as stored: each one is true unless the author turned it off. */
export const stepButtonsOf = (props: {showPreviousBtn?: boolean; showNextBtn?: boolean}): StepButtons => ({
	previous: props.showPreviousBtn !== false,
	next: props.showNextBtn !== false,
});

/**
 * The buttons shown on the step on screen: the position decides which one can exist (no Previous on the
 * first visible step, Next on every step but the last, Submit on the last), and the author's settings can
 * only take one away. Steps are counted among the visible ones — a step hidden by a rule does not count.
 */
export const visibleStepButtons = (
	position: {isFirstVisibleStep: boolean; isLastStep: boolean},
	settings: StepButtons = ALL_STEP_BUTTONS
): ShownButtons => ({
	previous: !position.isFirstVisibleStep && settings.previous,
	next: !position.isLastStep && settings.next,
	submit: position.isLastStep && settings.next,
});
