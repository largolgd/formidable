import { jahiaComponent, Render } from "@jahia/javascript-modules-library";

interface StepProps {
  "jcr:title"?: string;
  intro?: string;
  /** From fmdbsamplemix:stepButtons, absent until the node is saved with the mixin. */
  showPreviousBtn?: boolean;
  showNextBtn?: boolean;
}

/**
 * The buttons are rendered by Formidable's form island, outside the step, so this view cannot
 * leave them out: it marks the step and hides the buttons with a rule of CSS while the step is
 * the one on screen. The island shows a step by clearing its inline `display` and hides the
 * others with `display: none` (written with or without a space, by the server or the browser).
 * The rule is a visual one: the buttons stay in the DOM, and Enter in a field still submits.
 */
const ON_SCREEN = ':not([style*="display:none"]):not([style*="display: none"])';
const STYLE =
  `.fmdb-form:has(.fmdb-step[data-sample-no-prev]${ON_SCREEN}) .fmdb-prev-btn,` +
  `.fmdb-form:has(.fmdb-step[data-sample-no-next]${ON_SCREEN}) .fmdb-next-btn,` +
  `.fmdb-form:has(.fmdb-step[data-sample-no-next]${ON_SCREEN}) .fmdb-form-actions button[type="submit"]` +
  `{display:none}`;

jahiaComponent(
  {
    componentType: "view",
    nodeType: "fmdb:step",
    name: "default",
    displayName: "Full",
    // Above Formidable's own default view (priority 0): the highest priority wins.
    priority: 1,
  },
  (
    { "jcr:title": title, intro, showPreviousBtn, showNextBtn }: StepProps,
    { currentNode, currentResource },
  ) => {
    const initiallyHidden =
      currentResource.getModuleParams().get("initiallyHidden")?.toString() === "true";
    const showLogicHidden =
      currentResource.getModuleParams().get("showLogicHidden")?.toString() === "true";
    const noPrev = showPreviousBtn === false;
    const noNext = showNextBtn === false;
    return (
      <div
        data-fmdb-step
        className="fmdb-step"
        style={initiallyHidden ? { display: "none" } : undefined}
        data-sample-no-prev={noPrev ? "" : undefined}
        data-sample-no-next={noNext ? "" : undefined}
      >
        {(noPrev || noNext) && <style dangerouslySetInnerHTML={{ __html: STYLE }} />}
        {title && <h2 className="fmdb-step-title">{title}</h2>}
        {intro && <div className="fmdb-step-intro" dangerouslySetInnerHTML={{ __html: intro }} />}

        <Render
          node={currentNode}
          view="hidden.logic"
          readOnly
          parameters={{
            childClassName: "fmdb-form-element",
            ...(showLogicHidden ? { showLogicHidden: "true" } : {}),
          }}
        />
      </div>
    );
  },
);
