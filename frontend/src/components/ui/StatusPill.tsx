import type { ProductStatus } from "@/src/types/product";

const STATUS_LABEL: Record<ProductStatus, string> = {
  SELLING: "판매중",
  RESERVED: "예약중",
  SOLD_OUT: "판매완료",
};

const STATUS_CLASSES: Record<ProductStatus, string> = {
  SELLING: "bg-green-soft text-green-ink",
  RESERVED: "bg-amber-soft text-amber-ink",
  SOLD_OUT: "bg-surface-alt text-text-faint",
};

export default function StatusPill({
  status,
  className = "",
}: {
  status: ProductStatus;
  className?: string;
}) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-bold ${STATUS_CLASSES[status]} ${className}`}
    >
      {STATUS_LABEL[status]}
    </span>
  );
}
