import type { ReactNode } from "react";

// 폼 구역 제목. 강조색과 대문자 라벨은 쓰지 않고 본문색 제목 + 1px 구분선으로만 나눈다.
export default function FormSection({
  title,
  first = false,
  children,
}: {
  title: string;
  first?: boolean;
  children: ReactNode;
}) {
  return (
    <section className={`space-y-4 ${first ? "" : "border-t border-border pt-8"}`}>
      <h2 className="text-base font-semibold text-text">{title}</h2>
      {children}
    </section>
  );
}
