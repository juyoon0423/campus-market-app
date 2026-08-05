"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";
import { login } from "@/src/lib/apis/userApi";
import { useAuth } from "@/src/context/AuthContext";
import { AxiosError } from "axios";
import AuthLayout from "@/src/components/AuthLayout";
import Button from "@/src/components/ui/Button";

const fieldClasses =
  "w-full rounded-field border border-transparent bg-surface-alt px-3.5 py-2.5 text-sm text-text outline-none transition-colors placeholder:text-text-faint focus:border-accent focus:bg-surface disabled:text-text-faint";

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

  // 🗺️ 카카오 OAuth2 로그인
  const handleKakaoLogin = () => {
    const kakaoAuthUrl = `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/oauth2/authorization/kakao`;
    window.location.href = kakaoAuthUrl;
  };

  return (
    <AuthLayout title="로그인" description="캠퍼스 마켓 계정으로 로그인하세요.">
      <form onSubmit={handleSubmit} className="mt-6 space-y-4">
        <div>
          <label htmlFor="email" className="mb-1.5 block text-sm font-semibold text-text-muted">
            이메일
          </label>
          <input
            id="email"
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
            disabled={isSubmitting}
            className={fieldClasses}
            placeholder="you@example.com"
          />
        </div>

        <div>
          <label htmlFor="password" className="mb-1.5 block text-sm font-semibold text-text-muted">
            비밀번호
          </label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
            disabled={isSubmitting}
            className={fieldClasses}
            placeholder="비밀번호를 입력하세요"
          />
        </div>

        {errorMessage ? (
          <p className="rounded-field bg-red-soft px-3 py-2 text-sm text-red-ink">
            {errorMessage}
          </p>
        ) : null}

        <Button type="submit" variant="primary" size="lg" disabled={isSubmitting} className="w-full">
          {isSubmitting ? "로그인 중..." : "로그인"}
        </Button>
      </form>

      {/* 🗺️ 카카오 로그인 버튼 */}
      <div className="mt-3">
        <button
          onClick={handleKakaoLogin}
          className="w-full rounded-button bg-[#FEE500] px-4 py-3.5 text-[0.9375rem] font-bold text-black transition hover:bg-[#E5D400] active:bg-[#D4BC00]"
        >
          카카오로 시작하기
        </button>
      </div>

      <p className="mt-6 text-center text-sm text-text-muted">
        계정이 없으신가요?{" "}
        <Link href="/signup" className="font-bold text-text hover:text-accent-strong">
          회원가입
        </Link>
      </p>
    </AuthLayout>
  );
}
