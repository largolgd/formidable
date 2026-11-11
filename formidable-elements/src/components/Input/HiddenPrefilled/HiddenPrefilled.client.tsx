import {useEffect, useState} from 'react';

interface HiddenPrefilledProps {
	name: string;
	/** The session key the redirecting code stored the value under. */
	prefillKey: string;
	/** The engine endpoint that hands the value over. */
	prefillUrl: string;
	/** What the field carries when the session holds nothing for the key. */
	fallback: string;
}

/**
 * Hidden input carrying a value the server kept in the visitor's session. The page comes from the fragment
 * cache, so the value cannot be in the markup: it is asked for at hydration, from an endpoint that is never
 * cached and answers from the session of whoever asks. The read is one-shot server-side.
 *
 * The input is rendered at once with the fallback, then updated — a submission that leaves before the answer
 * arrives carries the fallback, which is what the field is without a session value.
 */
export default function HiddenPrefilled({name, prefillKey, prefillUrl, fallback}: HiddenPrefilledProps) {
	const [value, setValue] = useState(fallback);

	useEffect(() => {
		const controller = new AbortController();
		const url = `${prefillUrl}?key=${encodeURIComponent(prefillKey)}`;
		fetch(url, {credentials: 'same-origin', cache: 'no-store', signal: controller.signal})
			.then(async response => {
				if (response.status !== 200) {
					return;
				}

				const body = await response.json() as {value?: unknown};
				if (typeof body.value === 'string') {
					setValue(body.value);
				}
			})
			.catch(() => {
				// no value is not a failure for the form: the field keeps its fallback
			});
		return () => controller.abort();
	}, [prefillKey, prefillUrl]);

	return <input type='hidden' name={name} value={value}/>;
}
