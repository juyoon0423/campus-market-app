"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState, useEffect } from "react";
import { Check, RefreshCw } from "lucide-react";
import { signUp, sendVerificationCode, verifyEmailCode } from "@/src/lib/apis/userApi";
import { AxiosError } from "axios";

export default function SignUpPage() {
  const router = useRouter();
  const [form, setForm] = useState({
    email: "",
    name: "",
    studentId: "",
    department: "",
    password: "",
  });
  const [verificationCode, setVerificationCode] = useState("");
  const [isEmailVerified, setIsEmailVerified] = useState(false);
  const [isSendingCode, setIsSendingCode] = useState(false);
  const [isVerifyingCode, setIsVerifyingCode] = useState(false);
  const [timerSeconds, setTimerSeconds] = useState(0);
  const [canResend, setCanResend] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrorMessage("");

    if (!isEmailVerified) {
      setErrorMessage("이메일 인증을 먼저 완료해주세요.");
      return;
    }

    setIsSubmitting(true);

    try {
      await signUp(form);
      router.push("/login");
    } catch (error) {
      if (error instanceof AxiosError) {
        if (error.response?.status === 400) {
          setErrorMessage("입력값을 확인해주세요. 이메일이 이미 등록되어 있을 수 있습니다.");
        } else {
          setErrorMessage("회원가입에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
      } else {
        setErrorMessage("회원가입에 실패했습니다.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSendVerificationCode = async () => {
    if (!form.email.endsWith("@sj.sangji.ac.kr")) {
      alert("상지대학교 이메일(@sj.sangji.ac.kr)만 가입 가능합니다.");
      return;
    }

    setIsSendingCode(true);
    setErrorMessage("");

    try {
      await sendVerificationCode(form.email);
      setTimerSeconds(180); // 3 minutes
      setCanResend(false);
    } catch (error) {
      console.error("Send verification code error:", error);
      setErrorMessage("인증번호 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setIsSendingCode(false);
    }
  };

  const handleVerifyCode = async () => {
    setIsVerifyingCode(true);
    setErrorMessage("");

    try {
      await verifyEmailCode(form.email, verificationCode);
      setIsEmailVerified(true);
      setTimerSeconds(0);
    } catch (error) {
      console.error("Verify code error:", error);
      setErrorMessage("인증 코드가 일치하지 않습니다.");
    } finally {
      setIsVerifyingCode(false);
    }
  };

  useEffect(() => {
    if (timerSeconds > 0) {
      const timer = setInterval(() => {
        setTimerSeconds((prev) => {
          if (prev <= 1) {
            setCanResend(true);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);

      return () => clearInterval(timer);
    }
  }, [timerSeconds]);

  const formatTime = (seconds: number) => {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${String(minutes).padStart(2, "0")}:${String(remainingSeconds).padStart(2, "0")}`;
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 px-4 py-10">
      <main className="w-full max-w-md rounded-2xl bg-white p-8 shadow-sm">
        <h1 className="text-2xl font-bold text-slate-900">회원가입</h1>
        <p className="mt-2 text-sm text-slate-500">
          캠퍼스 마켓 가입 정보를 입력해주세요.
        </p>

        <form onSubmit={handleSubmit} className="mt-6 space-y-4">
          <div>
            <label htmlFor="email" className="mb-1 block text-sm text-slate-700">
              이메일
            </label>
            <div className="flex gap-2">
              <input
                id="email"
                type="email"
                value={form.email}
                onChange={(event) =>
                  setForm((prev) => ({ ...prev, email: event.target.value }))
                }
                required
                disabled={isSubmitting || isEmailVerified || timerSeconds > 0}
                className="flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500 disabled:bg-slate-50"
                placeholder="학교 이메일 (@sj.sangji.ac.kr)"
              />
              <button
                type="button"
                onClick={handleSendVerificationCode}
                disabled={isSendingCode || isEmailVerified || !form.email || timerSeconds > 0}
                className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:bg-slate-400"
              >
                {isSendingCode ? "발송 중..." : canResend ? "재발송" : "인증번호 발송"}
              </button>
            </div>
            {isEmailVerified && (
              <div className="mt-2 flex items-center gap-1 text-sm text-green-600">
                <Check className="h-4 w-4" />
                <span>이메일 인증 완료</span>
              </div>
            )}
          </div>

          {timerSeconds > 0 && !isEmailVerified && (
            <div>
              <label htmlFor="verificationCode" className="mb-1 block text-sm text-slate-700">
                인증번호
              </label>
              <div className="flex gap-2">
                <input
                  id="verificationCode"
                  type="text"
                  value={verificationCode}
                  onChange={(event) => setVerificationCode(event.target.value)}
                  maxLength={6}
                  disabled={isVerifyingCode}
                  className="flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500 disabled:bg-slate-50"
                  placeholder="6자리 인증번호"
                />
                <button
                  type="button"
                  onClick={handleVerifyCode}
                  disabled={isVerifyingCode || verificationCode.length !== 6}
                  className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:bg-slate-400"
                >
                  {isVerifyingCode ? "인증 중..." : "인증하기"}
                </button>
              </div>
              <p className="mt-1 text-xs text-slate-500">
                남은 시간: {formatTime(timerSeconds)}
              </p>
            </div>
          )}

          <div>
            <label htmlFor="name" className="mb-1 block text-sm text-slate-700">
              이름
            </label>
            <input
              id="name"
              value={form.name}
              onChange={(event) =>
                setForm((prev) => ({ ...prev, name: event.target.value }))
              }
              required
              disabled={isSubmitting}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500 disabled:bg-slate-50"
            />
          </div>

          <div>
            <label
              htmlFor="studentId"
              className="mb-1 block text-sm text-slate-700"
            >
              학번
            </label>
            <input
              id="studentId"
              value={form.studentId}
              onChange={(event) =>
                setForm((prev) => ({ ...prev, studentId: event.target.value }))
              }
              required
              disabled={isSubmitting}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500 disabled:bg-slate-50"
            />
          </div>

          <div>
            <label
              htmlFor="department"
              className="mb-1 block text-sm text-slate-700"
            >
              학과
            </label>
            <input
              id="department"
              value={form.department}
              onChange={(event) =>
                setForm((prev) => ({ ...prev, department: event.target.value }))
              }
              required
              disabled={isSubmitting}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500 disabled:bg-slate-50"
            />
          </div>

          <div>
            <label
              htmlFor="password"
              className="mb-1 block text-sm text-slate-700"
            >
              비밀번호
            </label>
            <input
              id="password"
              type="password"
              value={form.password}
              onChange={(event) =>
                setForm((prev) => ({ ...prev, password: event.target.value }))
              }
              required
              disabled={isSubmitting}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-500 disabled:bg-slate-50"
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
            {isSubmitting ? "가입 중..." : "회원가입"}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-600">
          이미 계정이 있나요?{" "}
          <Link href="/login" className="font-semibold text-slate-900">
            로그인
          </Link>
        </p>
      </main>
    </div>
  );
}
