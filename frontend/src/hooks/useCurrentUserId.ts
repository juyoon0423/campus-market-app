"use client";

import { useMemo } from "react";
import { useAuth } from "@/src/context/AuthContext";

export function findNumericUserId(value: unknown): number | null {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }

  if (typeof value === "string") {
    const direct = Number(value);
    if (!Number.isNaN(direct)) {
      return direct;
    }
  }

  if (Array.isArray(value)) {
    for (const item of value) {
      const found = findNumericUserId(item);
      if (found !== null) {
        return found;
      }
    }
    return null;
  }

  if (typeof value === "object" && value !== null) {
    const record = value as Record<string, unknown>;
    const priorityKeys = ["userId", "id", "user_id", "memberId", "sub"];

    for (const key of priorityKeys) {
      if (key in record) {
        const found = findNumericUserId(record[key]);
        if (found !== null) {
          return found;
        }
      }
    }

    for (const nested of Object.values(record)) {
      const found = findNumericUserId(nested);
      if (found !== null) {
        return found;
      }
    }
  }

  return null;
}

export function decodeUserIdFromToken(token: string | null): number | null {
  if (!token) {
    return null;
  }

  try {
    const payloadBase64 = token.split(".")[1];
    if (!payloadBase64) {
      return null;
    }

    const base64 = payloadBase64.replace(/-/g, "+").replace(/_/g, "/");
    const paddedBase64 = base64 + "=".repeat((4 - (base64.length % 4)) % 4);
    const payloadJson = JSON.parse(atob(paddedBase64));
    return findNumericUserId(payloadJson);
  } catch {
    return null;
  }
}

/** 로그인 토큰(JWT)의 payload에서 현재 사용자 ID를 추출한다. UI 표시/조건부 렌더링용이며,
 * 실제 권한 검증은 서버가 한다. */
export function useCurrentUserId(): number | null {
  const { token } = useAuth();
  return useMemo(() => decodeUserIdFromToken(token), [token]);
}
