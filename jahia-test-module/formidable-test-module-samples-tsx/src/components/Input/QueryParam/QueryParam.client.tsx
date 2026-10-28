import { useEffect, useState } from "react";

interface QueryParamInputProps {
  name: string;
  defaultValue: string;
  queryParameter: string;
}

/**
 * The hidden input of fmdbsampletsx:inputQueryParam. The GET parameter is read in the browser, not
 * during the server render: the page may be served from a cache shared by every query string, so
 * the markup carries the contributor's default and the parameter replaces it once the island is
 * hydrated. An absent or empty parameter leaves the default.
 */
export default function QueryParamInput({
  name,
  defaultValue,
  queryParameter,
}: QueryParamInputProps) {
  const [value, setValue] = useState(defaultValue);

  useEffect(() => {
    const fromUrl = new URLSearchParams(window.location.search).get(queryParameter);
    if (fromUrl) {
      setValue(fromUrl);
    }
  }, [queryParameter]);

  return <input type="hidden" name={name} value={value} />;
}
