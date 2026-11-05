import { useEffect } from "react";

interface ShowWhenAnyProps {
  /** The id of the fieldset to show or hide. */
  targetId: string;
  /** Each group is "field=value&field2=value2": ANDed inside, ORed between groups. */
  groups: string[];
}

const parseGroup = (group: string) =>
  group
    .split("&")
    .map((rule) => rule.split("="))
    .filter(([name]) => name?.trim())
    .map(([name, ...value]) => ({ name: name.trim(), value: value.join("=").trim() }));

/** The values currently held by the controls named `name`: checked boxes/radios, or the value. */
const valuesOf = (form: HTMLFormElement, name: string): string[] =>
  Array.from(form.elements)
    .filter(
      (el): el is HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement =>
        (el as HTMLInputElement).name === name,
    )
    .flatMap((el) => {
      if (el instanceof HTMLInputElement && (el.type === "checkbox" || el.type === "radio")) {
        return el.checked ? [el.value] : [];
      }
      if (el instanceof HTMLSelectElement && el.multiple) {
        return Array.from(el.selectedOptions).map((option) => option.value);
      }
      return [el.value];
    });

/**
 * Renders nothing: toggles the fieldset it points to. The fieldset is hidden and disabled (so its
 * controls are not submitted) unless one group holds. Without JavaScript it simply stays visible.
 */
export default function ShowWhenAny({ targetId, groups }: ShowWhenAnyProps) {
  useEffect(() => {
    const target = document.getElementById(targetId) as HTMLFieldSetElement | null;
    const form = target?.closest("form");
    if (!target || !form) return;

    const parsed = groups.map(parseGroup);
    const evaluate = () => {
      const visible = parsed.some((rules) =>
        rules.every(({ name, value }) => valuesOf(form, name).includes(value)),
      );
      target.hidden = !visible;
      target.disabled = !visible;
    };

    evaluate();
    form.addEventListener("input", evaluate);
    form.addEventListener("change", evaluate);
    return () => {
      form.removeEventListener("input", evaluate);
      form.removeEventListener("change", evaluate);
    };
  }, [targetId, groups]);

  return null;
}
