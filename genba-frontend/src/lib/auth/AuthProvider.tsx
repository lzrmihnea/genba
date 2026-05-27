"use client";

import { createContext, useCallback, useEffect, useMemo, useState } from "react";
import { authApi } from "@/lib/auth/api";
import { tokenStorage } from "@/lib/auth/storage";
import type { User, UserOrganization } from "@/lib/auth/types";

export type AuthStatus = "loading" | "authenticated" | "unauthenticated";

export interface AuthContextValue {
  status: AuthStatus;
  user: User | null;
  organizations: UserOrganization[];
  activeOrg: UserOrganization | null;
  login: (email: string, password: string, locale?: string) => Promise<void>;
  logout: () => Promise<void>;
  setActiveOrg: (orgId: string) => void;
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const ACTIVE_ORG_KEY = "genba.activeOrg";

export function AuthProvider({ children }: { children: React.ReactNode }): React.ReactElement {
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [user, setUser] = useState<User | null>(null);
  const [organizations, setOrganizations] = useState<UserOrganization[]>([]);
  const [activeOrgId, setActiveOrgIdState] = useState<string | null>(null);

  useEffect(() => {
    const token = tokenStorage.getAccess();
    if (!token) {
      setStatus("unauthenticated");
      return;
    }
    authApi
      .me()
      .then(({ user, organizations }) => {
        setUser(user);
        setOrganizations(organizations);
        const stored = typeof window !== "undefined" ? window.localStorage.getItem(ACTIVE_ORG_KEY) : null;
        const preferred = stored && organizations.some((o) => o.organizationId === stored)
          ? stored
          : organizations[0]?.organizationId ?? null;
        setActiveOrgIdState(preferred);
        setStatus("authenticated");
      })
      .catch(() => {
        tokenStorage.clear();
        setStatus("unauthenticated");
      });
  }, []);

  const login = useCallback(async (email: string, password: string, locale?: string) => {
    const result = await authApi.login(email, password, locale);
    tokenStorage.set(result.accessToken, result.refreshToken);
    setUser(result.user);
    setOrganizations(result.organizations);
    const firstOrg = result.organizations[0]?.organizationId ?? null;
    setActiveOrgIdState(firstOrg);
    if (typeof window !== "undefined" && firstOrg) {
      window.localStorage.setItem(ACTIVE_ORG_KEY, firstOrg);
    }
    setStatus("authenticated");
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // server-side logout is a no-op in L0; tolerate failures and continue clearing local state
    }
    tokenStorage.clear();
    if (typeof window !== "undefined") {
      window.localStorage.removeItem(ACTIVE_ORG_KEY);
    }
    setUser(null);
    setOrganizations([]);
    setActiveOrgIdState(null);
    setStatus("unauthenticated");
  }, []);

  const setActiveOrg = useCallback(
    (orgId: string) => {
      if (!organizations.some((o) => o.organizationId === orgId)) return;
      setActiveOrgIdState(orgId);
      if (typeof window !== "undefined") {
        window.localStorage.setItem(ACTIVE_ORG_KEY, orgId);
      }
    },
    [organizations],
  );

  const activeOrg = useMemo(
    () => organizations.find((o) => o.organizationId === activeOrgId) ?? null,
    [organizations, activeOrgId],
  );

  const value = useMemo<AuthContextValue>(
    () => ({ status, user, organizations, activeOrg, login, logout, setActiveOrg }),
    [status, user, organizations, activeOrg, login, logout, setActiveOrg],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
