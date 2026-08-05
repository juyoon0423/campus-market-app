"use client";

import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { CheckCircle2, XCircle } from "lucide-react";
import { useAuth } from "@/src/context/AuthContext";
import Button from "@/src/components/ui/Button";

export default function OAuthCallbackPage() {
  return (
    <Suspense fallback={null}>
      <OAuthCallbackContent />
    </Suspense>
  );
}

function OAuthCallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { login: setAuthLogin } = useAuth();
  const [status, setStatus] = useState<"loading" | "success" | "error">("loading");
  const [message, setMessage] = useState("카카오 로그인 처리 중입니다...");

  useEffect(() => {
    const handleOAuthCallback = async () => {
      try {
        // URL 쿼리 파라미터에서 토큰 추출
        const token = searchParams.get("token");

        if (!token) {
          setStatus("error");
          setMessage("로그인에 실패했습니다. 토큰이 없습니다.");

          // 2초 후 로그인 페이지로 리다이렉트
          setTimeout(() => {
            router.replace("/login");
          }, 2000);
          return;
        }

        // 토큰을 localStorage에 저장
        localStorage.setItem("accessToken", token);

        // 전역 인증 상태 업데이트
        setAuthLogin(token);

        setStatus("success");
        setMessage("로그인 성공! 메인 페이지로 이동합니다...");

        // 1초 후 메인 페이지로 리다이렉트
        setTimeout(() => {
          router.replace("/");
        }, 1000);

      } catch (error) {
        console.error("OAuth 콜백 처리 중 에러:", error);
        setStatus("error");
        setMessage("로그인 처리 중 오류가 발생했습니다.");

        // 2초 후 로그인 페이지로 리다이렉트
        setTimeout(() => {
          router.replace("/login");
        }, 2000);
      }
    };

    handleOAuthCallback();
  }, [searchParams, setAuthLogin, router]);

  return (
    <div className="flex min-h-screen items-center justify-center px-4 py-10">
      <main className="w-full max-w-sm rounded-card border border-border bg-surface p-8 text-center shadow-soft">
        {status === "loading" && (
          <div className="flex flex-col items-center gap-4">
            <div className="h-8 w-8 animate-spin rounded-full border-4 border-surface-alt border-t-accent" />
            <div>
              <h1 className="text-lg font-bold text-text">카카오 로그인 처리 중</h1>
              <p className="mt-2 text-sm text-text-muted">잠시만 기다려주세요...</p>
            </div>
          </div>
        )}

        {status === "success" && (
          <div className="flex flex-col items-center gap-4">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-green-soft">
              <CheckCircle2 className="h-6 w-6 text-green-ink" />
            </div>
            <div>
              <h1 className="text-lg font-bold text-text">로그인 성공!</h1>
              <p className="mt-2 text-sm text-text-muted">{message}</p>
            </div>
          </div>
        )}

        {status === "error" && (
          <div className="flex flex-col items-center gap-4">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-red-soft">
              <XCircle className="h-6 w-6 text-red-ink" />
            </div>
            <div>
              <h1 className="text-lg font-bold text-text">로그인 실패</h1>
              <p className="mt-2 text-sm text-text-muted">{message}</p>
            </div>
            <Button variant="primary" size="md" onClick={() => router.replace("/login")} className="mt-2">
              로그인 페이지로 이동
            </Button>
          </div>
        )}
      </main>
    </div>
  );
}
