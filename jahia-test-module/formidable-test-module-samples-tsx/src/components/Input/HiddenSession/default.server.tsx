import { Island, jahiaComponent } from "@jahia/javascript-modules-library";
import HiddenSession from "./HiddenSession.client";

interface InputHiddenSessionProps {
  sessionKey?: string;
  /** What the field submits when the session holds nothing under the key. */
  value?: string;
}

/**
 * A hidden field filled from the visitor's HTTP session. The name is the node's name, as every Formidable
 * field's is (the submission keys the value by it); the page is cached, so the value is asked for by the
 * browser once hydrated, never rendered here.
 */
jahiaComponent(
  {
    componentType: "view",
    nodeType: "fmdbsample:inputHiddenSession",
    name: "default",
  },
  ({ sessionKey, value }: InputHiddenSessionProps, { currentNode, renderContext }) => {
    const name = currentNode.getName();

    // While authoring, and without a key to ask for, the bare input is all there is to render.
    if (!sessionKey || renderContext.isEditMode()) {
      return <input type="hidden" name={name} value={value ?? ""} />;
    }

    const endpoint = `${renderContext.getRequest().getContextPath()}/modules/formidable-samples/session-value`;
    return (
      <Island
        component={HiddenSession}
        props={{ name, sessionKey, endpoint, fallback: value ?? "" }}
      />
    );
  },
);
