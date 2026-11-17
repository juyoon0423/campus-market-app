import type { ProductStatus } from "@/src/types/product";

const STATUS_LABEL: Record<ProductStatus, string> = {
  SELLING: "판매중",
  RESERVED: "예약중",
  SOLD_OUT: "판매완료",
};

// 상세·마이페이지·채팅에서 쓰는 글자 배지. 색과 함께 항상 글자로 상태를 알린다.
const STATUS_CLASSES: Record<ProductStatus, string> = {
  SELLING: "bg-green-soft text-green-ink",
  RESERVED: "bg-amber-soft text-amber-ink",
  SOLD_OUT: "bg-surface-alt text-text-muted",
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
      className={`inline-flex items-center rounded-[4px] px-2 py-0.5 text-[13px] font-semibold ${STATUS_CLASSES[status]} ${className}`}
    >
      {STATUS_LABEL[status]}
    </span>
  );
}
