"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/src/context/AuthContext";
import { buttonClasses } from "@/src/components/ui/Button";

export default function SiteHeader() {
  const router = useRouter();
  const { isLoggedIn, isHydrated, logout } = useAuth();

  const handleLogout = () => {
    logout();
    router.push("/login");
  };

  return (
    <header className="sticky top-0 z-30 border-b border-border bg-surface/90 backdrop-blur supports-[backdrop-filter]:bg-surface/75">
      <div className="mx-auto flex w-full max-w-6xl items-center justify-between gap-3 px-4 py-3 sm:px-6">
        <Link
          href="/"
          className="flex items-center gap-2 text-lg font-extrabold tracking-tight text-text"
        >
          <span className="h-2.5 w-2.5 rounded-full bg-accent" aria-hidden />
          Campus Market
        </Link>

        <nav className="flex items-center gap-2">
          {isHydrated && isLoggedIn ? (
            <>
              <Link href="/upload" className={buttonClasses("primary", "sm")}>
                상품 등록
              </Link>
              <Link
                href="/me"
                className={`hidden sm:inline-flex ${buttonClasses("secondary", "sm")}`}
              >
                마이페이지
              </Link>
              <Link
                href="/chat"
                className={`hidden sm:inline-flex ${buttonClasses("secondary", "sm")}`}
              >
                채팅
              </Link>
              <button
                type="button"
                onClick={handleLogout}
                className={buttonClasses("danger", "sm")}
              >
                로그아웃
              </button>
            </>
          ) : (
            <>
              <Link href="/login" className={buttonClasses("secondary", "sm")}>
                로그인
              </Link>
              <Link href="/signup" className={buttonClasses("primary", "sm")}>
                회원가입
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
