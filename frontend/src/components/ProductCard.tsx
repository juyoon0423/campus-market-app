import Link from "next/link";
import type { ReactNode } from "react";
import { Eye, Heart } from "lucide-react";
import type { ProductListResponse } from "@/src/types/product";
import { getImageUrl } from "@/src/lib/imageUrl";
import StatusPill from "@/src/components/ui/StatusPill";

export default function ProductCard({
  product,
  action,
}: {
  product: ProductListResponse;
  /** 카드 하단, 링크 영역 바깥에 렌더링되는 추가 액션(예: 찜 해제 버튼) */
  action?: ReactNode;
}) {
  const imageUrl = getImageUrl(product.representativeImageUrl);

  return (
    <div className="group overflow-hidden rounded-card border border-border bg-surface shadow-soft transition-all duration-200 hover:-translate-y-1 hover:shadow-elevated">
      <Link href={`/products/${product.id}`}>
        <div className="aspect-square w-full overflow-hidden bg-surface-alt">
          {imageUrl ? (
            <img
              src={imageUrl}
              alt={product.title}
              className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
            />
          ) : (
            <div className="flex h-full w-full items-center justify-center text-text-faint">
              <span className="text-sm">이미지 없음</span>
            </div>
          )}
        </div>
        <div className="space-y-2 p-4 pb-2">
          <h2 className="line-clamp-1 text-[0.9375rem] font-bold text-text">
            {product.title}
          </h2>
          <p className="text-xs text-text-faint">{product.sellerName}</p>
          <div className="flex items-center justify-between pt-1">
            <p className="text-lg font-extrabold tabular-nums text-text">
              ₩{product.price.toLocaleString()}
            </p>
            <StatusPill status={product.status} />
          </div>
          <div className="flex items-center gap-3 pt-0.5 text-xs tabular-nums text-text-faint">
            <span className="inline-flex items-center gap-1">
              <Eye className="h-3.5 w-3.5" />
              {product.viewCount}
            </span>
            <span className="inline-flex items-center gap-1">
              <Heart
                className={`h-3.5 w-3.5 ${product.isLiked ? "fill-red text-red" : ""}`}
              />
              {product.likeCount}
            </span>
          </div>
        </div>
      </Link>
      {action ? <div className="px-4 pb-4">{action}</div> : null}
    </div>
  );
}
