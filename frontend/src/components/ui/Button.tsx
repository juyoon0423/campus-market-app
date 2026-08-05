import { ButtonHTMLAttributes } from "react";

export type ButtonVariant = "primary" | "secondary" | "ghost" | "danger";
export type ButtonSize = "lg" | "md" | "sm";

const VARIANT_CLASSES: Record<ButtonVariant, string> = {
  primary:
    "bg-accent text-white shadow-soft hover:bg-accent-strong disabled:bg-surface-alt disabled:text-text-faint disabled:shadow-none",
  secondary:
    "bg-surface text-text border border-border-strong hover:bg-surface-alt disabled:text-text-faint disabled:border-border",
  ghost:
    "bg-transparent text-text-muted hover:bg-surface-alt hover:text-text disabled:text-text-faint",
  danger:
    "bg-transparent text-red hover:bg-red-soft disabled:text-text-faint",
};

const SIZE_CLASSES: Record<ButtonSize, string> = {
  lg: "rounded-button px-6 py-3.5 text-[0.9375rem]",
  md: "rounded-button px-4 py-2.5 text-sm",
  sm: "rounded-field px-3 py-2 text-xs",
};

export function buttonClasses(
  variant: ButtonVariant = "primary",
  size: ButtonSize = "md",
  className = "",
) {
  return [
    "inline-flex items-center justify-center gap-1.5 font-bold whitespace-nowrap transition-all",
    "active:scale-[0.97] disabled:cursor-not-allowed disabled:active:scale-100",
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
