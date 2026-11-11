import { useEffect, useState } from "react";

interface HiddenSessionProps {
  name: string;
  /** The session key the redirecting code stored the value under. */
  sessionKey: string;
  /** The endpoint of the java samples module that hands the value over. */
  endpoint: string;
  /** What the field submits when the session holds nothing for the key. */
  fallback: string;
}

/**
 * Hidden input carrying a value the server kept in the visitor's session. Rendered at once with the fallback,
 * then updated when the endpoint answers — a submission that leaves before the answer arrives carries the
 * fallback. The endpoint is never cached and reads the session of whoever asks; the read is one-shot.
 */
export default function HiddenSession({ name, sessionKey, endpoint, fallback }: HiddenSessionProps) {
  const [value, setValue] = useState(fallback);

  useEffect(() => {
    const controller = new AbortController();
    fetch(`${endpoint}?key=${encodeURIComponent(sessionKey)}`, {
      credentials: "same-origin",
      cache: "no-store",
      signal: controller.signal,
    })
      .then(async (response) => {
        if (response.status !== 200) return;
        const body = (await response.json()) as { value?: unknown };
        if (typeof body.value === "string") setValue(body.value);
      })
      .catch(() => {
        // no value is not a failure for the form: the field keeps its fallback
      });
    return () => controller.abort();
  }, [sessionKey, endpoint]);

  return <input type="hidden" name={name} value={value} />;
}
