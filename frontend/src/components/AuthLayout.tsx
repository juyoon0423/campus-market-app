import Link from "next/link";
import type { ReactNode } from "react";

const HIGHLIGHTS = [
  "사진 한 장으로 상품 등록",
  "채팅으로 바로 거래 시작",
  "우리 학교 인증 회원만 이용",
];

export default function AuthLayout({
  title,
  description,
  children,
}: {
  title: string;
  description: string;
  children: ReactNode;
}) {
  return (
    <div className="flex min-h-screen">
      {/* 브랜드 패널 */}
      <div className="relative hidden w-[42%] shrink-0 flex-col justify-between overflow-hidden bg-accent p-12 text-white lg:flex">
        <div className="pointer-events-none absolute -right-24 -top-24 h-72 w-72 rounded-full bg-white/10" />
        <div className="pointer-events-none absolute -bottom-32 -left-16 h-80 w-80 rounded-full bg-white/10" />

        <Link href="/" className="relative z-10 flex items-center gap-2 text-lg font-extrabold">
          <span className="h-2.5 w-2.5 rounded-full bg-white" />
          Campus Market
        </Link>

        <div className="relative z-10">
          <p className="text-3xl font-extrabold leading-snug text-balance">
            학교 앞 중고거래,
            <br />
            캠퍼스 마켓에서.
          </p>
          <ul className="mt-8 space-y-3 text-sm font-medium text-white/90">
            {HIGHLIGHTS.map((item) => (
              <li key={item} className="flex items-center gap-2">
                <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-white" />
                {item}
              </li>
            ))}
          </ul>
        </div>

        <p className="relative z-10 text-xs text-white/70">© Campus Market</p>
      </div>

      {/* 폼 패널 */}
      <div className="flex flex-1 items-center justify-center px-4 py-10 sm:px-6">
        <div className="w-full max-w-sm">
          <Link
            href="/"
            className="mb-8 flex items-center gap-2 text-base font-extrabold text-text lg:hidden"
          >
            <span className="h-2 w-2 rounded-full bg-accent" />
            Campus Market
          </Link>
          <h1 className="text-2xl font-extrabold text-text">{title}</h1>
          <p className="mt-2 text-sm text-text-muted">{description}</p>
          {children}
        </div>
      </div>
    </div>
  );
}
