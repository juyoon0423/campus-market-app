"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";
import { login } from "@/src/lib/apis/userApi";
import { useAuth } from "@/src/context/AuthContext";
import { AxiosError } from "axios";

export default function LoginPage() {
  const router = useRouter();
  const { login: setAuthLogin } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrorMessage("");
    setIsSubmitting(true);

    try {
      const response = await login({ email, password });
      setAuthLogin(response.token);
      router.replace("/");
    } catch (error) {
      if (error instanceof AxiosError) {
        if (error.response?.status === 401) {
          setErrorMessage("이메일 또는 비밀번호가 올바르지 않습니다.");
        } else if (error.response?.status === 400) {
          setErrorMessage("입력값을 확인해주세요.");
        } else {
          setErrorMessage("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
      } else {
        setErrorMessage("로그인에 실패했습니다.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 px-4 py-10">
      <main className="w-full max-w-md rounded-2xl bg-white p-8 shadow-sm">
        <h1 className="text-2xl font-bold text-slate-900">로그인</h1>
        <p className="mt-2 text-sm text-slate-500">
          캠퍼스 마켓 계정으로 로그인하세요.
        </p>

        <form onSubmit={handleSubmit} className="mt-6 space-y-4">
          <div>
            <label htmlFor="email" className="mb-1 block text-sm text-slate-700">
              이메일
            </label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
              disabled={isSubmitting}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500 disabled:bg-slate-50"
              placeholder="you@example.com"
            />
          </div>

          <div>
            <label htmlFor="password" className="mb-1 block text-sm text-slate-700">
              비밀번호
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
              disabled={isSubmitting}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500 disabled:bg-slate-50"
              placeholder="비밀번호를 입력하세요"
            />
          </div>

          {errorMessage ? (
            <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">
              {errorMessage}
            </p>
          ) : null}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-lg bg-slate-900 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:bg-slate-400"
          >
            {isSubmitting ? "로그인 중..." : "로그인"}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-600">
          계정이 없으신가요?{" "}
          <Link href="/signup" className="font-semibold text-slate-900">
            회원가입
          </Link>
        </p>
      </main>
    </div>
  );
}
