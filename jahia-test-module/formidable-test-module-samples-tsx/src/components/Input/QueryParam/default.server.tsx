import { Island, jahiaComponent } from "@jahia/javascript-modules-library";
import QueryParamInput from "./QueryParam.client";

interface InputQueryParamProps {
  "jcr:title"?: string;
  "queryParameter"?: string;
  "defaultValue"?: string;
}

/**
 * A third-party leaf field (docs/extension/how-to-extend-views-and-elements-from-third-party-module.md,
 * Case 3): the control is named `currentNode.getName()`, as for every field, so the submission,
 * the field actions and the conditional logic treat it like a built-in one. It has no label and no
 * help block — it is not shown. While authoring, a line the contributor can see and click says what
 * the field reads: without it the field's Page Builder box has no height.
 */
jahiaComponent(
  {
    componentType: "view",
    nodeType: "fmdbsampletsx:inputQueryParam",
    name: "default",
  },
  (
    { "jcr:title": title, queryParameter, defaultValue = "" }: InputQueryParamProps,
    { currentNode, renderContext },
  ) => {
    const name = currentNode.getName();

    if (renderContext.isEditMode()) {
      return (
        <div className="fmdb-authoring-hidden-field">
          <span className="fmdb-authoring-hidden-field-title">{title || name}</span>
          <span className="fmdb-authoring-hidden-field-value">
            ?{queryParameter ?? "…"}
            {defaultValue ? ` (${defaultValue})` : ""}
          </span>
          <input type="hidden" name={name} value={defaultValue} readOnly />
        </div>
      );
    }

    // Without a parameter name there is nothing to read: the field submits its default.
    if (!queryParameter) {
      return <input type="hidden" name={name} value={defaultValue} />;
    }

    return (
      <Island component={QueryParamInput} props={{ name, defaultValue, queryParameter }} />
    );
  },
);
