import { ButtonHTMLAttributes } from "react";

export type ButtonVariant = "primary" | "secondary" | "ghost" | "danger";
export type ButtonSize = "lg" | "md" | "sm";

// 비활성 주요 버튼은 회색 테두리로 바꾸지 않고 강조색을 흐리게만 한다(꺼진 버튼처럼 보이지 않게).
const VARIANT_CLASSES: Record<ButtonVariant, string> = {
  primary:
    "bg-accent text-white hover:bg-accent-strong disabled:opacity-60 disabled:hover:bg-accent",
  secondary:
    "bg-surface text-text border border-border-strong hover:bg-surface-alt disabled:text-text-faint",
  ghost:
    "bg-transparent text-text-muted hover:bg-surface-alt hover:text-text disabled:text-text-faint",
  danger:
    "bg-transparent text-red hover:bg-red-soft disabled:text-text-faint",
};

// 가장 작은 글자는 13px
const SIZE_CLASSES: Record<ButtonSize, string> = {
  lg: "rounded-button px-5 py-3 text-base",
  md: "rounded-button px-4 py-2.5 text-sm",
  sm: "rounded-button px-3 py-2 text-[13px]",
};

export function buttonClasses(
  variant: ButtonVariant = "primary",
  size: ButtonSize = "md",
  className = "",
) {
  return [
    "inline-flex items-center justify-center gap-1.5 font-semibold whitespace-nowrap transition-colors",
    "disabled:cursor-not-allowed",
    VARIANT_CLASSES[variant],
    SIZE_CLASSES[size],
    className,
  ]
    .filter(Boolean)
    .join(" ");
}

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  size?: ButtonSize;
};

export default function Button({
  variant = "primary",
  size = "md",
  className = "",
  type = "button",
  ...props
}: ButtonProps) {
  return (
    <button
      type={type}
      className={buttonClasses(variant, size, className)}
      {...props}
    />
  );
}
