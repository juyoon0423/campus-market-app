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
      {/* 브랜드 패널 (장식, 데스크톱 전용) */}
      <div className="hidden w-[42%] shrink-0 flex-col justify-between bg-accent p-12 text-white lg:flex">
        <Link href="/" className="text-xl font-bold">
          Campus Market
        </Link>

        <div>
          <p className="text-2xl font-bold leading-snug">
            학교 앞 중고거래,
            <br />
            캠퍼스 마켓에서.
          </p>
          <ul className="mt-8 space-y-3 text-sm font-semibold text-white/90">
            {HIGHLIGHTS.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </div>

        <p className="text-[13px] text-white/85">© Campus Market</p>
      </div>

      {/* 폼 패널: 페이지의 주요 콘텐츠 랜드마크 */}
      <main className="flex flex-1 items-center justify-center px-4 py-10 sm:px-6">
        <div className="w-full max-w-sm">
          <Link
            href="/"
            className="mb-8 block text-base font-bold text-text lg:hidden"
          >
            Campus Market
          </Link>
          <h1 className="text-2xl font-bold text-text">{title}</h1>
          <p className="mt-2 text-sm text-text-muted">{description}</p>
          {children}
        </div>
      </main>
    </div>
  );
}
