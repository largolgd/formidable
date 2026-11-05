---
name: formidable-extend-externally
description: Guardrail for ANY Formidable development request (new field, view, action, field action, enricher, integration, behaviour change). The core modules formidable-elements, formidable-engine, formidable-jexperience-engine, formidable-forms-import and formidable-extended-inputs must NOT be modified; features go in a separate extension module (jahia-test-module/formidable-test-module-samples-java / -samples-tsx, or another third-party module). Use before planning or editing anything in this repo.
---

# Formidable — extend, never modify the core

## The rule

These modules are **frozen for feature work**. Do not create, edit, rename or delete files in them:

- `formidable-elements/`
- `formidable-engine/`
- `formidable-jexperience-engine/`
- `formidable-forms-import/`
- `formidable-extended-inputs/`

(Also `packages/formidable/` — it is the published contract; consume it, don't change it.)

Everything new goes in an **extension module**, the way a third party would write it:

| Need | Where | Reference |
|---|---|---|
| Server-side code (FormAction, FieldAction, SubmissionResponseEnricher, ChoiceOptionsResolver, servlets, config) | `jahia-test-module/formidable-test-module-samples-java/` (or a new sibling Java/OSGi module) | `docs/extension/how-to-create-form-action.md`, `how-to-create-field-action.md`, `how-to-enrich-the-submission-response.md` |
| Views, custom fields/containers, CND definitions, editor overrides | `jahia-test-module/formidable-test-module-samples-tsx/` (or a new sibling JS module) | `docs/extension/how-to-extend-views-and-elements-from-third-party-module.md` |
| Integration with an external system (e.g. NNM sync) | its own module under `jahia-test-module/` (see `formidable-nnm-sync`) | `docs/architecture/` |

Extension modules depend on the core only through its public surface: the `api/` package of `formidable-engine` (`FormAction`, `FieldAction`, `FmdbNodeType`, `FmdbMixin`, `FmdbProperty`, `FmdbNodeName`, …), the CND types, the published `@jahia/formidable-library`, the documented class hooks and CSS variables.

## Procedure

1. **Read the docs first**: `docs/extension/README.md`, then the how-to matching the request, plus `docs/architecture/cnd-module-ownership.md`.
2. **Pick the extension module** from the table above and say which one in one line.
3. **Check the extension point exists.** Search the docs and `api/` for a hook that covers the request.
4. **If no hook covers it, STOP.** Do not patch the core "just this once". Tell the user: what is missing, which core file would need to change, and propose the smallest public extension point. Wait for their decision.
5. Implement only inside the extension module; reuse the samples as templates (copy the pattern, follow their READMEs).
6. Before finishing, run `git status` / `git diff --stat` and confirm **no path under the five frozen modules (or `packages/formidable/`) is touched**. If one is, revert it and redo the work in the extension module.

## Not covered by the freeze

Tests (`tests/` Cypress), `docs/`, and the extension modules themselves may change. Build artefacts (`dist/`) are generated — never hand-edit them.

## Bug fixes in the core

A request that is explicitly a bug fix *of the core module* still needs the user's explicit go-ahead before touching it; ask first.
