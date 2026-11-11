import {Island, jahiaComponent} from "@jahia/javascript-modules-library";
import {useTranslation} from "react-i18next";
import EyeOffIcon from "~/design/EyeOffIcon";
import HiddenPrefilled from "./HiddenPrefilled.client";

interface InputHiddenPrefilledProps {
	"jcr:title"?: string;
	prefillKey?: string;
	value?: string;
}

jahiaComponent(
	{
		componentType: "view",
		nodeType: "fmdb:inputHiddenPrefilled",
		name: "default"
	},
	(
		{"jcr:title": title, prefillKey, value}: InputHiddenPrefilledProps,
		{currentNode, renderContext}
	) => {
		const {t} = useTranslation("formidable-elements", {keyPrefix: "fmdb_inputHiddenPrefilled"});

		const inputName = currentNode.getName();

		if (renderContext.isEditMode()) {
			// While authoring: a line the contributor can see and click, saying where the value comes from.
			// The bare input stays so the field is part of the form the way it will be live.
			return (
				<div className="fmdb-authoring-hidden-field">
					<EyeOffIcon/>
					<span className="fmdb-authoring-hidden-field-label">{t("label")}</span>
					<span className="fmdb-authoring-hidden-field-title">{title || inputName}</span>
					<span className="fmdb-authoring-hidden-field-value">{prefillKey ? t("from", {key: prefillKey}) : t("noKey")}</span>
					<input type="hidden" name={inputName} value={value || ""}/>
				</div>
			);
		}

		// The page is cached: the island asks for the visitor's value once hydrated. Without a key there is
		// nothing to ask for, so the fallback value is rendered as a plain hidden input.
		if (!prefillKey) {
			return <input type="hidden" name={inputName} value={value || ""}/>;
		}

		const prefillUrl = `${renderContext.getRequest().getContextPath()}/modules/formidable-engine/prefill`;
		return (
			<Island
				component={HiddenPrefilled}
				props={{name: inputName, prefillKey, prefillUrl, fallback: value || ""}}
			/>
		);
	}
);
