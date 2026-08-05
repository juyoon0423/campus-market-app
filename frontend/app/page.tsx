"use client";

import { useEffect, useState } from "react";
import { Search, X } from "lucide-react";
import { searchProducts } from "@/src/lib/apis/productApi";
import type { ProductListResponse, ProductStatus } from "@/src/types/product";
import SiteHeader from "@/src/components/SiteHeader";
import ProductCard from "@/src/components/ProductCard";

const CATEGORY_FILTERS = [
  { value: "", label: "전체" },
  { value: "학업 관련", label: "학업 관련" },
  { value: "디지털/가전", label: "디지털/가전" },
  { value: "생활/자취", label: "생활/자취" },
  { value: "기타", label: "기타" },
];

const STATUS_FILTERS: { value: ProductStatus | ""; label: string }[] = [
  { value: "", label: "전체 상태" },
  { value: "SELLING", label: "판매중" },
  { value: "RESERVED", label: "예약중" },
  { value: "SOLD_OUT", label: "판매완료" },
];

function chipClasses(active: boolean) {
  return `inline-flex shrink-0 items-center rounded-full px-4 py-2 text-sm font-semibold transition-colors ${
    active
      ? "bg-text text-bg"
      : "border border-border bg-surface text-text-muted hover:bg-surface-alt hover:text-text"
  }`;
}

export default function HomePage() {
  const [products, setProducts] = useState<ProductListResponse[]>([]);
  const [keyword, setKeyword] = useState("");
  const [category, setCategory] = useState("");
  const [status, setStatus] = useState<ProductStatus | "">("");
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");

  const runSearch = async (overrides?: {
    keyword?: string;
    category?: string;
    status?: ProductStatus | "";
  }) => {
    const nextKeyword = overrides?.keyword ?? keyword;
    const nextCategory = overrides?.category ?? category;
    const nextStatus = overrides?.status ?? status;

    setIsLoading(true);
    setErrorMessage("");

    try {
      const result = await searchProducts({
        keyword: nextKeyword || undefined,
        category: nextCategory || undefined,
        status: nextStatus || undefined,
      });
      setProducts(result);
    } catch {
      setErrorMessage("상품 목록을 불러오지 못했습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    runSearch();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleCategorySelect = (value: string) => {
    setCategory(value);
    runSearch({ category: value });
  };

  const handleStatusSelect = (value: ProductStatus | "") => {
    setStatus(value);
    runSearch({ status: value });
  };

  const handleReset = () => {
    setKeyword("");
    setCategory("");
    setStatus("");
    runSearch({ keyword: "", category: "", status: "" });
  };

  const hasActiveFilters = Boolean(keyword || category || status);

  return (
    <div className="min-h-screen">
      <SiteHeader />

      <main className="mx-auto w-full max-w-6xl px-4 pb-16 pt-8 sm:px-6 md:pt-12">
        {/* 인트로 + 검색 */}
        <div className="mb-8 md:mb-10">
          <h1 className="text-2xl font-extrabold tracking-tight text-text sm:text-[1.75rem]">
            우리 학교 중고거래
          </h1>

          <div className="relative mt-5 max-w-xl">
            <input
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  runSearch();
                }
              }}
              placeholder="어떤 물건을 찾으세요?"
              className="w-full rounded-full border border-transparent bg-surface py-3.5 pl-12 pr-4 text-sm text-text shadow-soft outline-none transition-colors placeholder:text-text-faint focus:border-accent"
            />
            <Search className="pointer-events-none absolute left-4.5 top-1/2 h-4.5 w-4.5 -translate-y-1/2 text-text-faint" />
          </div>
        </div>

        {/* 필터 칩 */}
        <div className="mb-8 space-y-3 md:mb-10">
          <div className="-mx-4 flex gap-2 overflow-x-auto px-4 sm:mx-0 sm:flex-wrap sm:px-0">
            {CATEGORY_FILTERS.map((filter) => (
              <button
                key={filter.label}
                type="button"
                onClick={() => handleCategorySelect(filter.value)}
                className={chipClasses(category === filter.value)}
              >
                {filter.label}
              </button>
            ))}
          </div>
          <div className="flex items-center justify-between gap-3">
            <div className="-mx-4 flex gap-2 overflow-x-auto px-4 sm:mx-0 sm:flex-wrap sm:px-0">
              {STATUS_FILTERS.map((filter) => (
                <button
                  key={filter.label}
                  type="button"
                  onClick={() => handleStatusSelect(filter.value)}
                  className={`${chipClasses(status === filter.value)} text-xs`}
                >
                  {filter.label}
                </button>
              ))}
            </div>
            {hasActiveFilters ? (
              <button
                type="button"
                onClick={handleReset}
                className="inline-flex shrink-0 items-center gap-1 text-xs font-semibold text-text-faint transition-colors hover:text-text"
              >
                <X className="h-3.5 w-3.5" />
                필터 초기화
              </button>
            ) : null}
          </div>
        </div>

        {errorMessage ? (
          <p className="mb-6 rounded-field bg-red-soft px-4 py-3 text-sm text-red-ink">
            {errorMessage}
          </p>
        ) : null}

        {/* 결과 카운트 */}
        {!isLoading && !errorMessage ? (
          <p className="mb-4 text-sm text-text-faint">
            {products.length > 0
              ? `${products.length}개의 상품이 있어요`
              : null}
          </p>
        ) : null}

        {isLoading ? (
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 sm:gap-6 lg:grid-cols-4">
            {Array.from({ length: 8 }).map((_, index) => (
              <div key={index} className="animate-pulse">
                <div className="aspect-square w-full rounded-card bg-surface-alt" />
                <div className="mt-3 h-4 w-3/4 rounded-full bg-surface-alt" />
                <div className="mt-2 h-4 w-1/2 rounded-full bg-surface-alt" />
              </div>
            ))}
          </div>
        ) : products.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-card border border-dashed border-border-strong py-20 text-center">
            <p className="text-sm font-semibold text-text">검색 결과가 없어요</p>
            <p className="mt-1 text-sm text-text-faint">
              다른 키워드나 필터로 다시 찾아볼까요?
            </p>
          </div>
        ) : (
          <section className="grid grid-cols-2 gap-4 sm:grid-cols-3 sm:gap-6 lg:grid-cols-4">
            {products.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </section>
        )}
      </main>
    </div>
  );
}
