/**
 * Token storage. localStorage is good enough for L0 (the user is the only
 * principal on their own machine). L1 should swap to httpOnly cookies issued
 * by the backend to remove XSS attack surface.
 */

const ACCESS_KEY = "genba.access";
const REFRESH_KEY = "genba.refresh";

function isBrowser(): boolean {
  return typeof window !== "undefined";
}

export const tokenStorage = {
  getAccess(): string | null {
    if (!isBrowser()) return null;
    return window.localStorage.getItem(ACCESS_KEY);
  },

  getRefresh(): string | null {
    if (!isBrowser()) return null;
    return window.localStorage.getItem(REFRESH_KEY);
  },

  set(accessToken: string, refreshToken?: string): void {
    if (!isBrowser()) return;
    window.localStorage.setItem(ACCESS_KEY, accessToken);
    if (refreshToken) {
      window.localStorage.setItem(REFRESH_KEY, refreshToken);
    }
  },

  clear(): void {
    if (!isBrowser()) return;
    window.localStorage.removeItem(ACCESS_KEY);
    window.localStorage.removeItem(REFRESH_KEY);
  },
};
