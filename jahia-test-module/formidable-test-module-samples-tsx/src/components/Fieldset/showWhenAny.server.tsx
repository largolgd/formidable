import { Island, jahiaComponent, Render } from "@jahia/javascript-modules-library";
import ShowWhenAny from "./ShowWhenAny.client";

interface FieldsetShowWhenAnyProps {
  "jcr:title"?: string;
  "showWhenAnyOf"?: string[];
}

/**
 * Opt-in view (View chooser). Same markup as Formidable's fieldset, plus an island that shows the
 * fieldset only when one group of rules holds: AND inside a group, OR between groups. Browser-side
 * only — Formidable's server-side enforcement of hidden fields does not know these rules.
 */
jahiaComponent(
  {
    componentType: "view",
    nodeType: "fmdb:fieldset",
    name: "showWhenAny",
    displayName: "Fieldset - Show when any group holds",
  },
  ({ "jcr:title": title, showWhenAnyOf = [] }: FieldsetShowWhenAnyProps, { currentNode }) => {
    const id = `fieldset-${currentNode.getIdentifier()}`;
    const groups = showWhenAnyOf.filter((group) => group.trim() !== "");

    return (
      <fieldset id={id} className="fmdb-fieldset">
        {title && <legend className="fmdb-fieldset-legend">{title}</legend>}

        <Render
          node={currentNode}
          view="hidden.logic"
          parameters={{
            className: "fmdb-fieldset-elements",
            childClassName: "fmdb-form-element",
          }}
        />

        {groups.length > 0 && <Island component={ShowWhenAny} props={{ targetId: id, groups }} />}
      </fieldset>
    );
  },
);
