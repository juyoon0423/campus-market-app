"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { LogOut, MessageCircle, UserRound } from "lucide-react";
import { useAuth } from "@/src/context/AuthContext";
import { buttonClasses } from "@/src/components/ui/Button";

// 데스크톱은 텍스트 링크, 375px 같은 좁은 화면은 아이콘 버튼(접근 가능한 이름 포함)으로 한 줄을 유지한다.
// 이전에는 `hidden sm:inline-flex`와 buttonClasses의 `inline-flex`가 겹쳐 hidden이 무시되었고,
// 375px에서 버튼 4개가 모두 보여 문서 폭이 397px로 넘쳤다.
const textLink =
  "rounded-button px-2.5 py-2 text-sm text-text-muted transition-colors hover:text-text";
const iconLink =
  "inline-flex h-9 w-9 items-center justify-center rounded-button text-text-muted transition-colors hover:bg-surface-alt hover:text-text";

export default function SiteHeader() {
  const router = useRouter();
  const pathname = usePathname();
  const { isLoggedIn, isHydrated, logout } = useAuth();

  const handleLogout = () => {
    logout();
    router.push("/login");
  };

  const current = (href: string) => (pathname === href ? "page" : undefined);

  return (
    <header className="sticky top-0 z-30 border-b border-border bg-surface/95 backdrop-blur">
      <div className="mx-auto flex h-14 w-full max-w-6xl items-center justify-between gap-2 px-4 sm:px-6">
        <Link
          href="/"
          className="shrink-0 whitespace-nowrap text-base font-bold sm:text-xl tracking-tight text-text"
        >
          Campus Market
        </Link>

        <nav aria-label="주요 메뉴" className="flex min-w-0 items-center gap-1">
          {isHydrated && isLoggedIn ? (
            <>
              <Link href="/chat" aria-current={current("/chat")} className={`hidden sm:block ${textLink} aria-[current=page]:text-text`}>
                채팅
              </Link>
              <Link href="/me" aria-current={current("/me")} className={`hidden sm:block ${textLink} aria-[current=page]:text-text`}>
                마이페이지
              </Link>
              <button type="button" onClick={handleLogout} className={`hidden sm:block ${textLink}`}>
                로그아웃
              </button>

              <Link href="/chat" aria-label="채팅" aria-current={current("/chat")} className={`sm:hidden ${iconLink}`}>
                <MessageCircle className="h-5 w-5" aria-hidden />
              </Link>
              <Link href="/me" aria-label="마이페이지" aria-current={current("/me")} className={`sm:hidden ${iconLink}`}>
                <UserRound className="h-5 w-5" aria-hidden />
              </Link>
              <button type="button" onClick={handleLogout} aria-label="로그아웃" className={`sm:hidden ${iconLink}`}>
                <LogOut className="h-5 w-5" aria-hidden />
              </button>

              <Link href="/upload" className={`ml-1 ${buttonClasses("primary", "sm")}`}>
                상품 등록
              </Link>
            </>
          ) : (
            <>
              <Link href="/login" className={textLink}>
                로그인
              </Link>
              <Link href="/signup" className={`ml-1 ${buttonClasses("primary", "sm")}`}>
                회원가입
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
