"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useAuth } from "@/src/context/AuthContext";

export default function OAuthCallbackPage() {
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
    <div className="flex min-h-screen items-center justify-center bg-slate-100 px-4 py-10">
      <main className="w-full max-w-md rounded-2xl bg-white p-8 shadow-sm text-center">
        {/* 로딩 스피너 */}
        {status === "loading" && (
          <div className="flex flex-col items-center space-y-4">
            <div className="h-8 w-8 animate-spin rounded-full border-4 border-slate-200 border-t-slate-900"></div>
            <div>
              <h1 className="text-lg font-semibold text-slate-900">카카오 로그인 처리 중</h1>
              <p className="mt-2 text-sm text-slate-500">잠시만 기다려주세요...</p>
            </div>
          </div>
        )}

        {/* 성공 상태 */}
        {status === "success" && (
          <div className="flex flex-col items-center space-y-4">
            <div className="h-12 w-12 rounded-full bg-green-100 flex items-center justify-center">
              <svg
                className="h-6 w-6 text-green-600"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M5 13l4 4L19 7"
                />
              </svg>
            </div>
            <div>
              <h1 className="text-lg font-semibold text-slate-900">로그인 성공!</h1>
              <p className="mt-2 text-sm text-slate-500">{message}</p>
            </div>
          </div>
        )}

        {/* 에러 상태 */}
        {status === "error" && (
          <div className="flex flex-col items-center space-y-4">
            <div className="h-12 w-12 rounded-full bg-red-100 flex items-center justify-center">
              <svg
                className="h-6 w-6 text-red-600"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M6 18L18 6M6 6l12 12"
                />
              </svg>
            </div>
            <div>
              <h1 className="text-lg font-semibold text-slate-900">로그인 실패</h1>
              <p className="mt-2 text-sm text-slate-500">{message}</p>
            </div>
            <button
              onClick={() => router.replace("/login")}
              className="mt-4 rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-700"
            >
              로그인 페이지로 이동
            </button>
          </div>
        )}
      </main>
    </div>
  );
}
