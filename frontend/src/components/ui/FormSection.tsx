import type { ReactNode } from "react";

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
      <h2 className="text-xs font-bold uppercase tracking-wider text-accent-strong">
        {title}
      </h2>
      {children}
    </section>
  );
}
