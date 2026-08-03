"use client";

import { getMyProfile } from "@/src/lib/apis/userApi";
import type { UserProfileResponse } from "@/src/types/user";
import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/src/context/AuthContext";

type UseUserState = {
  user: UserProfileResponse | null;
  isLoading: boolean;
  error: Error | null;
};

export function useUser() {
  const { isLoggedIn, isHydrated, logout } = useAuth();
  const [state, setState] = useState<UseUserState>({
    user: null,
    isLoading: false,
    error: null,
  });

  const fetchUser = useCallback(async () => {
    if (!isLoggedIn) {
      setState({ user: null, isLoading: false, error: null });
      return;
    }

    setState((prev) => ({ ...prev, isLoading: true, error: null }));

    try {
      const profile = await getMyProfile();
      setState({ user: profile, isLoading: false, error: null });
    } catch (err) {
      const error = err instanceof Error ? err : new Error("Failed to fetch user profile");
      
      // If 401, logout and clear state
      if (err instanceof Error && err.message.includes("401")) {
        logout();
      }
      
      setState({ user: null, isLoading: false, error });
    }
  }, [isLoggedIn, logout]);

  // Fetch user when logged in (only after hydration)
  useEffect(() => {
    if (isHydrated && isLoggedIn) {
      fetchUser();
    }
  }, [isHydrated, isLoggedIn, fetchUser]);

  const refetch = useCallback(() => {
    if (isHydrated && isLoggedIn) {
      fetchUser();
    }
  }, [isHydrated, isLoggedIn, fetchUser]);

  return {
    ...state,
    refetch,
  };
}
