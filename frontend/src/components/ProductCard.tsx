import Image from "next/image";
import Link from "next/link";
import type { ReactNode } from "react";
import { Heart } from "lucide-react";
import type { ProductListResponse, ProductStatus } from "@/src/types/product";
import { getImageUrl } from "@/src/lib/imageUrl";
import { formatPrice, formatRelativeTime } from "@/src/components/format";

// 콘셉트 "물건 사진에 가격표 한 장": 상자(테두리·그림자·배경) 없이 사진과 정보 3줄만 둔다.
// 사진 위 표식은 판매 상태 하나. 기본값인 판매중은 표시하지 않고 예약중·판매완료만 글자 태그로 붙인다.
const STATUS_TAG: Partial<Record<ProductStatus, { label: string; className: string }>> = {
  RESERVED: { label: "예약중", className: "bg-amber text-white" },
  SOLD_OUT: { label: "판매완료", className: "bg-text-muted text-white" },
};

export default function ProductCard({
  product,
  action,
  eager = false,
  highPriority = false,
}: {
  product: ProductListResponse;
  /** 카드 하단, 링크 영역 바깥에 렌더링되는 추가 액션(예: 찜 해제 버튼) */
  action?: ReactNode;
  /** 첫 줄 카드: 지연 로딩하지 않는다 */
  eager?: boolean;
  /** 첫 두 장: 이미지 요청 우선순위를 높인다 */
  highPriority?: boolean;
}) {
  const imageUrl = getImageUrl(product.representativeImageUrl);
  const tag = STATUS_TAG[product.status];
  const isSoldOut = product.status === "SOLD_OUT";
  const relative = formatRelativeTime(product.createdAt);

  return (
    <article className="group">
      <Link
        href={`/products/${product.id}`}
        className="block rounded-field focus-visible:outline-offset-4"
      >
        <div className="relative aspect-square w-full overflow-hidden rounded-field bg-surface-alt">
          {imageUrl ? (
            <Image
              src={imageUrl}
              alt={product.title}
              fill
              sizes="(max-width: 640px) 50vw, (max-width: 1024px) 33vw, 270px"
              loading={eager ? "eager" : "lazy"}
              fetchPriority={highPriority ? "high" : undefined}
              className={`object-cover ${isSoldOut ? "opacity-60 grayscale" : ""}`}
            />
          ) : (
            <div className="flex h-full w-full items-center justify-center">
              <span className="text-[13px] text-text-muted">사진 없음</span>
            </div>
          )}
          {/* 누를 수 있다는 표시: hover 때 사진을 살짝 어둡게 (움직임 없음) */}
          <span
            aria-hidden
            className="pointer-events-none absolute inset-0 bg-text/0 transition-colors group-hover:bg-text/[0.06]"
          />
          {tag ? (
            <span
              className={`absolute left-2 top-2 rounded-[4px] px-1.5 py-0.5 text-[13px] font-semibold ${tag.className}`}
            >
              {tag.label}
            </span>
          ) : null}
        </div>

        <h2 className="mt-3 line-clamp-1 text-base font-semibold text-text underline-offset-2 group-hover:underline">
          {product.title}
        </h2>
        <p className="mt-0.5 text-base font-bold text-text">{formatPrice(product.price)}</p>
        <p className="mt-1 flex items-center gap-2 text-[13px] text-text-faint">
          {product.tradeLocationName ? (
            <span className="min-w-0 truncate">{product.tradeLocationName}</span>
          ) : null}
          {relative ? (
            <time dateTime={product.createdAt ?? undefined} className="shrink-0">
              {relative}
            </time>
          ) : null}
          <span className="inline-flex shrink-0 items-center gap-0.5" aria-label={`찜 ${product.likeCount}`}>
            <Heart
              aria-hidden
              className={`h-3.5 w-3.5 ${product.isLiked ? "fill-current text-text" : ""}`}
            />
            {product.likeCount}
          </span>
        </p>
      </Link>
      {action ? <div className="mt-2">{action}</div> : null}
    </article>
  );
}
