"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState, useEffect } from "react";
import { Check } from "lucide-react";
import { signUp, sendVerificationCode, verifyEmailCode } from "@/src/lib/apis/userApi";
import { AxiosError } from "axios";
import AuthLayout from "@/src/components/AuthLayout";
import Button from "@/src/components/ui/Button";

const fieldClasses =
  "w-full rounded-field border border-transparent bg-surface-alt px-3.5 py-2.5 text-sm text-text outline-none transition-colors placeholder:text-text-faint focus:border-accent focus:bg-surface disabled:text-text-faint";

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
    <AuthLayout title="회원가입" description="캠퍼스 마켓 가입 정보를 입력해주세요.">
      <form onSubmit={handleSubmit} className="mt-6 space-y-4">
        <div>
          <label htmlFor="email" className="mb-1.5 block text-sm font-semibold text-text-muted">
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
              className={`flex-1 ${fieldClasses}`}
              placeholder="학교 이메일 (@sj.sangji.ac.kr)"
            />
            <Button
              type="button"
              variant="secondary"
              size="md"
              onClick={handleSendVerificationCode}
              disabled={isSendingCode || isEmailVerified || !form.email || timerSeconds > 0}
              className="shrink-0"
            >
              {isSendingCode ? "발송 중..." : canResend ? "재발송" : "인증번호 발송"}
            </Button>
          </div>
          {isEmailVerified && (
            <div className="mt-2 flex items-center gap-1 text-sm font-semibold text-green-ink">
              <Check className="h-4 w-4" />
              <span>이메일 인증 완료</span>
            </div>
          )}
        </div>

        {timerSeconds > 0 && !isEmailVerified && (
          <div>
            <label htmlFor="verificationCode" className="mb-1.5 block text-sm font-semibold text-text-muted">
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
                className={`flex-1 ${fieldClasses}`}
                placeholder="6자리 인증번호"
              />
              <Button
                type="button"
                variant="secondary"
                size="md"
                onClick={handleVerifyCode}
                disabled={isVerifyingCode || verificationCode.length !== 6}
                className="shrink-0"
              >
                {isVerifyingCode ? "인증 중..." : "인증하기"}
              </Button>
            </div>
            <p className="mt-1.5 text-xs text-text-faint">
              남은 시간: {formatTime(timerSeconds)}
            </p>
          </div>
        )}

        <div>
          <label htmlFor="name" className="mb-1.5 block text-sm font-semibold text-text-muted">
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
            className={fieldClasses}
          />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label htmlFor="studentId" className="mb-1.5 block text-sm font-semibold text-text-muted">
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
              className={fieldClasses}
            />
          </div>

          <div>
            <label htmlFor="department" className="mb-1.5 block text-sm font-semibold text-text-muted">
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
              className={fieldClasses}
            />
          </div>
        </div>

        <div>
          <label htmlFor="password" className="mb-1.5 block text-sm font-semibold text-text-muted">
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
            className={fieldClasses}
          />
        </div>

        {errorMessage ? (
          <p className="rounded-field bg-red-soft px-3 py-2 text-sm text-red-ink">
            {errorMessage}
          </p>
        ) : null}

        <Button type="submit" variant="primary" size="lg" disabled={isSubmitting} className="w-full">
          {isSubmitting ? "가입 중..." : "회원가입"}
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-text-muted">
        이미 계정이 있나요?{" "}
        <Link href="/login" className="font-bold text-text hover:text-accent-strong">
          로그인
        </Link>
      </p>
    </AuthLayout>
  );
}
