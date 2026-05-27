export type OrgRole = "OWNER" | "ADMIN" | "MEMBER" | "GUEST";

export interface User {
  id: string;
  email: string;
  displayName: string | null;
  preferredLocale: string;
  active: boolean;
  createdAt: string;
}

export interface UserOrganization {
  organizationId: string;
  organizationName: string;
  orgRole: OrgRole;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresInMs: number;
  user: User;
  organizations: UserOrganization[];
}

export interface RefreshResponse {
  accessToken: string;
  accessTokenExpiresInMs: number;
}

export interface CurrentUserResponse {
  user: User;
  organizations: UserOrganization[];
}

export interface ApiError {
  messageKey: string;
  message: string;
  status: number;
  errorCode: string;
  timestamp: string;
  path: string;
  fieldErrors: Record<string, string[]> | null;
}
