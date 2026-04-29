"use client";

import { setOnUnauthorized } from "@/src/lib/api";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";

type AuthContextValue = {
  token: string | null;
  isLoggedIn: boolean;
  isHydrated: boolean;
  login: (nextToken: string) => void;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

type AuthProviderProps = {
  children: React.ReactNode;
};

export function AuthProvider({ children }: AuthProviderProps) {
  const [token, setToken] = useState<string | null>(null);
  const [isHydrated, setIsHydrated] = useState(false);

  // Initialize token from localStorage after hydration
  useEffect(() => {
    const savedToken = localStorage.getItem("token");
    setToken(savedToken);
    setIsHydrated(true);
  }, []);

  const login = useCallback((nextToken: string) => {
    localStorage.setItem("token", nextToken);
    setToken(nextToken);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem("token");
    setToken(null);
  }, []);

  // Set up the unauthorized callback
  useEffect(() => {
    setOnUnauthorized(logout);
  }, [logout]);

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      isLoggedIn: Boolean(token),
      isHydrated,
      login,
      logout,
    }),
    [token, isHydrated, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }

  return context;
}
