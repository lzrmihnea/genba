import { cookies, headers } from "next/headers";
import { getRequestConfig } from "next-intl/server";

const SUPPORTED_LOCALES = ["en", "ro"] as const;
type SupportedLocale = (typeof SUPPORTED_LOCALES)[number];
const DEFAULT_LOCALE: SupportedLocale = "en";
const COOKIE_NAME = "genba-locale";

function isSupported(value: string | undefined): value is SupportedLocale {
  return value !== undefined && (SUPPORTED_LOCALES as readonly string[]).includes(value);
}

async function resolveLocale(): Promise<SupportedLocale> {
  // 1. Explicit cookie set by the language switcher takes priority.
  const cookieStore = await cookies();
  const fromCookie = cookieStore.get(COOKIE_NAME)?.value;
  if (isSupported(fromCookie)) {
    return fromCookie;
  }

  // 2. Fall back to Accept-Language header from the browser.
  const headerStore = await headers();
  const accept = headerStore.get("accept-language") ?? "";
  for (const fragment of accept.split(",")) {
    const tag = fragment.trim().split(";")[0]?.toLowerCase() ?? "";
    const primary = tag.split("-")[0];
    if (isSupported(primary)) {
      return primary;
    }
  }

  return DEFAULT_LOCALE;
}

export default getRequestConfig(async () => {
  const locale = await resolveLocale();
  const messages = (await import(`../../messages/${locale}.json`)).default;
  return { locale, messages };
});
