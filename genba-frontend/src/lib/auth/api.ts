import { apiClient } from "@/lib/api/client";
import type {
  CurrentUserResponse,
  LoginResponse,
  RefreshResponse,
} from "@/lib/auth/types";

export const authApi = {
  async login(email: string, password: string, locale?: string): Promise<LoginResponse> {
    const response = await apiClient.post<LoginResponse>(
      "/auth/login",
      { email, password },
      locale ? { headers: { "Accept-Language": locale } } : undefined,
    );
    return response.data;
  },

  async me(): Promise<CurrentUserResponse> {
    const response = await apiClient.get<CurrentUserResponse>("/auth/me");
    return response.data;
  },

  async refresh(refreshToken: string): Promise<RefreshResponse> {
    const response = await apiClient.post<RefreshResponse>(
      "/auth/refresh",
      { refreshToken },
    );
    return response.data;
  },

  async logout(): Promise<void> {
    await apiClient.post("/auth/logout");
  },
};
