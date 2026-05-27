import axios, {
  AxiosError,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from "axios";
import { tokenStorage } from "@/lib/auth/storage";
import type { ApiError, RefreshResponse } from "@/lib/auth/types";

const baseURL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8086/api";

/**
 * Key used by AuthProvider to persist the user's active organization across
 * page loads. We read it on every request to inject the X-Genba-Org-Id header
 * the backend uses for org scoping.
 */
const ACTIVE_ORG_KEY = "genba.activeOrg";

function readActiveOrgId(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(ACTIVE_ORG_KEY);
}

export const apiClient = axios.create({
  baseURL,
  timeout: 30_000,
  headers: { "Content-Type": "application/json" },
});

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStorage.getAccess();
  if (token) {
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  const orgId = readActiveOrgId();
  if (orgId && !config.headers.has("X-Genba-Org-Id")) {
    config.headers.set("X-Genba-Org-Id", orgId);
  }
  return config;
});

/**
 * Marker we attach to requests we've already retried after a refresh — keeps
 * a single failed refresh from looping forever on permanently expired tokens.
 */
type RetryableConfig = InternalAxiosRequestConfig & { _retry?: boolean };

let refreshInFlight: Promise<string | null> | null = null;

async function performRefresh(): Promise<string | null> {
  const refresh = tokenStorage.getRefresh();
  if (!refresh) return null;
  try {
    const response = await axios.post<RefreshResponse>(
      `${baseURL}/auth/refresh`,
      { refreshToken: refresh },
      { headers: { "Content-Type": "application/json" } },
    );
    tokenStorage.set(response.data.accessToken);
    return response.data.accessToken;
  } catch {
    tokenStorage.clear();
    return null;
  }
}

apiClient.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error: AxiosError<ApiError>) => {
    const original = error.config as RetryableConfig | undefined;
    if (
      error.response?.status === 401 &&
      original &&
      !original._retry &&
      !original.url?.includes("/auth/refresh") &&
      !original.url?.includes("/auth/login")
    ) {
      original._retry = true;
      refreshInFlight = refreshInFlight ?? performRefresh();
      const newToken = await refreshInFlight;
      refreshInFlight = null;
      if (newToken) {
        original.headers.set("Authorization", `Bearer ${newToken}`);
        return apiClient.request(original);
      }
      if (typeof window !== "undefined" && !window.location.pathname.startsWith("/login")) {
        window.location.assign("/login");
      }
    }
    return Promise.reject(error);
  },
);
