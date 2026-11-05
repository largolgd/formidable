# Show a fieldset when any group of rules holds

OR between groups of display rules, with nothing changed in Formidable. Formidable's own
conditional display ANDs the rules of a field; here a fieldset carries **groups**: the rules
of a group are ANDed, and the fieldset shows as soon as one group holds —
`(country=FR & age=adult) or vip=yes` is two groups.

## What the contributor gets

On a fieldset, a **Show when any group holds** switch (in the **Style** section) reveals
**Groups**, one value per group written `field=value`, rules of a group joined by `&`.
Picking **Fieldset - Show when any group holds** in the **View** chooser applies them.

## How it is built

- [`settings/definitions.cnd`](../settings/definitions.cnd): the mixin
  `fmdbsamplemix:showWhenAny`, extending `fmdb:fieldset` with the multi-valued `showWhenAnyOf`.
- [`src/components/Fieldset/showWhenAny.server.tsx`](../src/components/Fieldset/showWhenAny.server.tsx):
  the opt-in view, Formidable's fieldset markup plus an island.
- [`src/components/Fieldset/ShowWhenAny.client.tsx`](../src/components/Fieldset/ShowWhenAny.client.tsx):
  the island listens to the form and hides and disables the fieldset unless a group holds.

## Limits to know

- **Browser only.** The server knows nothing of these groups: a value submitted from a hidden
  fieldset is not refused, and a required field inside it is not exempted by the server. A
  disabled fieldset submits nothing, which covers ordinary browsers.
- Rules compare a field's current value (checked boxes and radios count when checked) by
  equality, and refer to fields by name.
- Without JavaScript the fieldset stays visible.
