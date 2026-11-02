import { jahiaComponent } from "@jahia/javascript-modules-library";

interface InputRequestParamProps {
  "jcr:title"?: string;
  "requestParameter"?: string;
  "defaultValue"?: string;
}

/**
 * A third-party leaf field (docs/extension/how-to-extend-views-and-elements-from-third-party-module.md,
 * Case 3): the control is named `currentNode.getName()`, as for every field, so the submission,
 * the field actions and the conditional logic treat it like a built-in one.
 *
 * The value is read on the server, from the request that asked for the page: a GET parameter of the
 * URL or a field of a POST to the page — `getParameter` knows both. An absent or empty parameter
 * leaves the contributor's default. The field has no label and no help block, it is not shown; while
 * authoring, a line the contributor can see and click says what it reads (without it the field's
 * Page Builder box has no height).
 */
jahiaComponent(
  {
    componentType: "view",
    nodeType: "fmdbsampletsx:inputRequestParam",
    name: "default",
  },
  (
    { "jcr:title": title, requestParameter, defaultValue = "" }: InputRequestParamProps,
    { currentNode, renderContext },
  ) => {
    const name = currentNode.getName();

    if (renderContext.isEditMode()) {
      return (
        <div className="fmdb-authoring-hidden-field">
          <span className="fmdb-authoring-hidden-field-title">{title || name}</span>
          <span className="fmdb-authoring-hidden-field-value">
            ?{requestParameter ?? "…"}
            {defaultValue ? ` (${defaultValue})` : ""}
          </span>
          <input type="hidden" name={name} value={defaultValue} readOnly />
        </div>
      );
    }

    const fromRequest = requestParameter
      ? renderContext.getRequest().getParameter(requestParameter)
      : null;

    return <input type="hidden" name={name} value={fromRequest || defaultValue} />;
  },
);
