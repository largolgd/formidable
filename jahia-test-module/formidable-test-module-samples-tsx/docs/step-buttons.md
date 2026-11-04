# Step navigation buttons

Two settings added to a step — show the **Previous** button, show the forward button (**Next**,
or **Submit** on the last step) — in a section of their own in the Content Editor, honoured by a
rendering of the step that takes the place of Formidable's. Nothing changes in Formidable itself.

## What the contributor gets

In the Content Editor of a step, a **Navigation** section holds a **Step buttons** switch; on, it
reveals **Show Previous button** and **Show Next / Submit button**, both on by default. Turning
one off hides that button while the step is on screen. The position still rules: Previous is never
shown on the first step, whatever the setting.

## How it is built

- [`settings/definitions.cnd`](../settings/definitions.cnd) declares `fmdbsamplemix:stepButtons`,
  a mixin that _extends_ `fmdb:step` with the two booleans.
- [`settings/jahia-content-editor-forms/forms/fmdbsamplemix_stepButtons.json`](../settings/jahia-content-editor-forms/forms/fmdbsamplemix_stepButtons.json)
  gives it the **Navigation** section; labels come from the module's
  [resource bundles](../settings/resources/).
- [`src/components/Step/default.server.tsx`](../src/components/Step/default.server.tsx) is a
  default view of `fmdb:step` of higher priority than Formidable's. The buttons belong to the form
  island, outside the step, so the view marks the step with a `data-` attribute and emits a rule
  of CSS hiding `.fmdb-prev-btn`, `.fmdb-next-btn` and the submit button of the form while that
  step is the one on screen.

## Limit

The hiding is visual: the buttons stay in the DOM, and pressing Enter in a field still submits the
form or goes on. A setting that must be enforced belongs in Formidable itself.
