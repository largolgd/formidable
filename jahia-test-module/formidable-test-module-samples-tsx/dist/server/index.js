import { jsxs, jsx, Fragment } from "react/jsx-runtime";
import { jahiaComponent, Render, AddResources, buildModuleFileUrl, Island } from "@jahia/javascript-modules-library";
import { useRef } from "react";
function r(e) {
  var t, f, n = "";
  if ("string" == typeof e || "number" == typeof e) n += e;
  else if ("object" == typeof e) if (Array.isArray(e)) {
    var o = e.length;
    for (t = 0; t < o; t++) e[t] && (f = r(e[t])) && (n && (n += " "), n += f);
  } else for (f in e) e[f] && (n && (n += " "), n += f);
  return n;
}
function clsx() {
  for (var e, t, f = 0, n = "", o = arguments.length; f < o; f++) (e = arguments[f]) && (t = r(e)) && (n && (n += " "), n += t);
  return n;
}
jahiaComponent(
  {
    componentType: "view",
    nodeType: "fmdb:fieldset",
    name: "customStyle",
    displayName: "Fieldset - Custom CSS class names"
  },
  ({ "jcr:title": title, customCssClassname }, { currentNode }) => {
    return /* @__PURE__ */ jsxs("fieldset", { className: clsx("fmdb-fieldset", customCssClassname), children: [
      title && /* @__PURE__ */ jsx("legend", { className: "fmdb-fieldset-legend", children: title }),
      /* @__PURE__ */ jsx(
        Render,
        {
          node: currentNode,
          view: "hidden.logic",
          parameters: {
            className: "fmdb-fieldset-elements",
            childClassName: "fmdb-form-element"
          }
        }
      )
    ] });
  }
);
const grid = "_grid_1usgs_1";
const item = "_item_1usgs_15";
const classes = {
  grid,
  item
};
jahiaComponent(
  {
    componentType: "view",
    nodeType: "fmdb:fieldset",
    name: "twoColumns",
    displayName: "Fieldset - Two columns"
  },
  ({ "jcr:title": title }, { currentNode }) => {
    return /* @__PURE__ */ jsxs(Fragment, { children: [
      /* @__PURE__ */ jsx(AddResources, { type: "css", resources: buildModuleFileUrl("dist/assets/style.css") }),
      /* @__PURE__ */ jsxs("fieldset", { children: [
        title && /* @__PURE__ */ jsx("legend", { children: title }),
        /* @__PURE__ */ jsx(
          Render,
          {
            node: currentNode,
            view: "hidden.logic",
            parameters: {
              className: classes.grid,
              childClassName: classes.item
            }
          }
        )
      ] })
    ] });
  }
);
const helpTextId = (nodeId) => `help-${nodeId}`;
function HelpText({ id, text, decorative = false }) {
  if (!text) {
    return null;
  }
  return /* @__PURE__ */ jsx(
    "div",
    {
      id: decorative ? void 0 : id,
      className: "fmdb-form-help",
      "aria-hidden": decorative ? "true" : void 0,
      dangerouslySetInnerHTML: { __html: text }
    }
  );
}
const validationDataAttributes = (props) => {
  const attrs = {};
  if ("msgValueMissing" in props) attrs["data-fmdb-msg-value-missing"] = props.msgValueMissing || void 0;
  if ("msgTypeMismatch" in props) attrs["data-fmdb-msg-type-mismatch"] = props.msgTypeMismatch || void 0;
  if ("msgPatternMismatch" in props) attrs["data-fmdb-msg-pattern-mismatch"] = props.msgPatternMismatch || void 0;
  if ("msgTooShort" in props) attrs["data-fmdb-msg-too-short"] = props.msgTooShort || void 0;
  if ("msgTooLong" in props) attrs["data-fmdb-msg-too-long"] = props.msgTooLong || void 0;
  if ("msgRangeUnderflow" in props) attrs["data-fmdb-msg-range-underflow"] = props.msgRangeUnderflow || void 0;
  if ("msgRangeOverflow" in props) attrs["data-fmdb-msg-range-overflow"] = props.msgRangeOverflow || void 0;
  if ("msgStepMismatch" in props) attrs["data-fmdb-msg-step-mismatch"] = props.msgStepMismatch || void 0;
  if ("msgBadInput" in props) attrs["data-fmdb-msg-bad-input"] = props.msgBadInput || void 0;
  return attrs;
};
const MASK_TOKENS = {
  "9": { pattern: /[0-9]/, patternSource: "[0-9]" },
  "A": { pattern: /[a-zA-Z]/, patternSource: "[A-Za-z]", transform: (char) => char.toUpperCase() },
  "a": { pattern: /[a-zA-Z]/, patternSource: "[A-Za-z]", transform: (char) => char.toLowerCase() },
  "X": { pattern: /[a-zA-Z0-9]/, patternSource: "[A-Za-z0-9]", transform: (char) => char.toUpperCase() },
  "x": { pattern: /[a-zA-Z0-9]/, patternSource: "[A-Za-z0-9]", transform: (char) => char.toLowerCase() }
};
const extractRawValue = (value) => value.replace(/[^a-zA-Z0-9]/g, "");
const formatWithMask = (value, mask, { fillTrailingLiterals = true } = {}) => {
  if (!mask) return value;
  let masked = "";
  let maskIndex = 0;
  let consumedToken = false;
  for (const char of extractRawValue(value)) {
    while (maskIndex < mask.length && !MASK_TOKENS[mask[maskIndex]]) {
      masked += mask[maskIndex];
      maskIndex++;
    }
    if (maskIndex >= mask.length) break;
    const token = MASK_TOKENS[mask[maskIndex]];
    if (token.pattern.test(char)) {
      masked += token.transform ? token.transform(char) : char;
      maskIndex++;
      consumedToken = true;
    }
  }
  if (fillTrailingLiterals && consumedToken && maskIndex < mask.length) {
    const remainder = mask.slice(maskIndex);
    if (Array.from(remainder).every((char) => !MASK_TOKENS[char])) {
      masked += remainder;
    }
  }
  return masked;
};
const applyMask = (value, mask) => formatWithMask(value, mask);
const maskToPattern = (mask) => {
  if (!mask) return void 0;
  const pattern = Array.from(mask).map((char) => {
    const token = MASK_TOKENS[char];
    if (token) return token.patternSource;
    return char.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  }).join("");
  return `^${pattern}$`;
};
const maskedCursorPosition = (mask, rawCount) => {
  let consumed = 0;
  let index = 0;
  while (index < mask.length && consumed < rawCount) {
    if (MASK_TOKENS[mask[index]]) consumed++;
    index++;
  }
  return index;
};
const useMask = ({ mask }) => {
  const inputRef = useRef(null);
  const handleInput = (e) => {
    if (!mask) return;
    const input = e.currentTarget;
    const cursorPos = input.selectionStart ?? input.value.length;
    const caretAtEnd = cursorPos >= input.value.length;
    const rawBeforeCursor = extractRawValue(input.value.slice(0, cursorPos)).length;
    const isDeletion = (e.nativeEvent.inputType ?? "").startsWith("delete");
    const maskedValue = formatWithMask(input.value, mask, { fillTrailingLiterals: !isDeletion });
    input.value = maskedValue;
    const newCursorPos = caretAtEnd ? maskedValue.length : Math.min(maskedCursorPosition(mask, rawBeforeCursor), maskedValue.length);
    input.setSelectionRange(newCursorPos, newCursorPos);
  };
  return {
    inputRef,
    handleInput,
    // Utility function to format values programmatically
    formatValue: (value) => mask ? applyMask(value || "", mask) : value
  };
};
const MaskedTextInput = (function(v) {
  if (typeof v === "function" || typeof v === "object" && v)
    Object.defineProperty(v, "__filename", {
      value: "dist/client/components\\Input\\Text\\Text.client.tsx",
      enumerable: false
    });
  return v;
})(function MaskedTextInput2({
  mask,
  defaultValue,
  inputAttributes
}) {
  const { inputRef, handleInput } = useMask({ mask });
  return /* @__PURE__ */ jsx("input", { ...inputAttributes, ref: inputRef, defaultValue, onInput: handleInput });
});
const DEFAULT_LIST = [];
const readPosition = (value) => value === "down" || value === "both" ? value : "up";
jahiaComponent(
  {
    componentType: "view",
    nodeType: "fmdb:inputText",
    name: "default",
    // Above Formidable's own default view (priority 0): the highest priority wins.
    priority: 1
  },
  ({
    "jcr:title": label,
    helpText,
    helpTextPosition,
    placeholder,
    defaultValue,
    list = DEFAULT_LIST,
    minLength,
    maxLength,
    required,
    autocomplete,
    mask,
    pattern,
    readonly,
    autofocus,
    disabled,
    form,
    dirname,
    spellcheck = true,
    size,
    title,
    ...validationMsgs
  }, { currentNode }) => {
    const position = readPosition(helpTextPosition);
    const nodeId = currentNode.getIdentifier();
    const inputId = `input-${nodeId}`;
    const inputName = currentNode.getName();
    const datalistId = list.length > 0 ? `datalist-${nodeId}` : void 0;
    const helpId = helpText ? helpTextId(nodeId) : void 0;
    const helpAbove = position !== "down";
    const helpBelow = position !== "up";
    const inputAttributes = {
      "type": "text",
      "id": inputId,
      "name": inputName,
      "aria-describedby": helpId,
      "className": "fmdb-form-control",
      placeholder,
      "list": datalistId,
      minLength,
      maxLength,
      // An explicit pattern wins; a mask alone stands for one the browser checks without JavaScript
      "pattern": pattern || maskToPattern(mask),
      "data-mask": mask,
      required,
      "autoComplete": autocomplete,
      "readOnly": readonly,
      "autoFocus": autofocus,
      disabled,
      form,
      // Submits the field's text direction as `{name}.dir`, like the built-in view
      "dirName": dirname ? `${inputName}.dir` : void 0,
      "spellCheck": spellcheck,
      size,
      title,
      ...validationDataAttributes(validationMsgs)
    };
    return (
      // The data attribute tells this rendering from Formidable's, whatever the position.
      /* @__PURE__ */ jsxs("div", { className: "fmdb-form-group", "data-fmdbsample-help-position": position, children: [
        label && /* @__PURE__ */ jsxs("label", { htmlFor: inputId, className: "fmdb-form-label", children: [
          label,
          required && /* @__PURE__ */ jsx("span", { className: "fmdb-required-indicator", "aria-hidden": "true", children: "*" })
        ] }),
        helpAbove && /* @__PURE__ */ jsx(HelpText, { id: helpId, text: helpText }),
        mask ? (
          // Hydrated only when a mask is configured; the default value is pre-formatted server-side
          /* @__PURE__ */ jsx(
            Island,
            {
              component: MaskedTextInput,
              props: {
                mask,
                defaultValue: defaultValue ? applyMask(defaultValue, mask) : void 0,
                inputAttributes
              }
            }
          )
        ) : /* @__PURE__ */ jsx("input", { ...inputAttributes, defaultValue }),
        helpBelow && /* @__PURE__ */ jsx(HelpText, { id: helpId, text: helpText, decorative: helpAbove }),
        list.length > 0 && /* @__PURE__ */ jsx("datalist", { id: datalistId, children: list.map((option) => /* @__PURE__ */ jsx("option", { value: option }, option)) })
      ] })
    );
  }
);
//# sourceMappingURL=index.js.map
