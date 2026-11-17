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

// 카테고리: 1px 밑줄 탭. 선택된 탭만 강조색 2px 밑줄 + 본문색 글자
function tabClasses(active: boolean) {
  return `relative shrink-0 whitespace-nowrap px-1 pb-3 pt-1 text-base transition-colors ${
    active
      ? "font-semibold text-text after:absolute after:inset-x-0 after:-bottom-px after:h-0.5 after:bg-accent"
      : "text-text-muted hover:text-text"
  }`;
}

// 상태: 결과 개수 줄 오른쪽의 작은 텍스트 토글
function toggleClasses(active: boolean) {
  return `shrink-0 rounded-button px-2 py-1.5 text-sm transition-colors ${
    active ? "font-semibold text-text" : "text-text-muted hover:text-text"
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

      <main className="mx-auto w-full max-w-6xl px-4 pb-20 pt-8 sm:px-6 md:pt-10">
        <h1 className="text-2xl font-bold tracking-tight text-text">우리 학교 중고거래</h1>

        <form
          role="search"
          className="relative mt-4 max-w-xl"
          onSubmit={(event) => {
            event.preventDefault();
            runSearch();
          }}
        >
          <label htmlFor="home-search" className="sr-only">
            상품 검색
          </label>
          <Search
            aria-hidden
            className="pointer-events-none absolute left-3.5 top-1/2 h-4.5 w-4.5 -translate-y-1/2 text-text-faint"
          />
          <input
            id="home-search"
            type="search"
            enterKeyHint="search"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="어떤 물건을 찾으세요?"
            className="w-full rounded-field border border-border-strong bg-surface py-3 pl-11 pr-4 text-base text-text outline-none transition-colors placeholder:text-text-faint focus:border-accent focus-visible:outline-none "
          />
        </form>

        <div
          role="group"
          aria-label="카테고리"
          className="-mx-4 mt-6 flex gap-5 overflow-x-auto border-b border-border px-4 sm:mx-0 sm:px-0"
        >
          {CATEGORY_FILTERS.map((filter) => (
            <button
              key={filter.label}
              type="button"
              aria-pressed={category === filter.value}
              onClick={() => handleCategorySelect(filter.value)}
              className={tabClasses(category === filter.value)}
            >
              {filter.label}
            </button>
          ))}
        </div>

        <div className="mb-5 mt-3 flex flex-wrap items-center justify-between gap-x-3 gap-y-1">
          <div className="flex items-center gap-2">
            <p className="text-sm text-text-muted" aria-live="polite">
              {isLoading ? "불러오는 중" : errorMessage ? "" : `${products.length}개 상품`}
            </p>
            {hasActiveFilters ? (
              <button
                type="button"
                onClick={handleReset}
                aria-label="검색어와 필터 초기화"
                className="rounded-button px-1.5 py-1 text-[13px] text-text-muted underline underline-offset-2 transition-colors hover:text-text"
              >
                초기화
              </button>
            ) : null}
          </div>
          <div role="group" aria-label="판매 상태" className="-mr-2 flex items-center">
            {STATUS_FILTERS.map((filter) => (
              <button
                key={filter.label}
                type="button"
                aria-pressed={status === filter.value}
                onClick={() => handleStatusSelect(filter.value)}
                className={toggleClasses(status === filter.value)}
              >
                {filter.label}
              </button>
            ))}
          </div>
        </div>

        {errorMessage ? (
          <p role="alert" className="mb-6 rounded-field bg-red-soft px-4 py-3 text-sm text-red-ink">
            {errorMessage}
          </p>
        ) : null}

        {isLoading ? (
          <div className="grid grid-cols-2 gap-x-3 gap-y-7 sm:grid-cols-3 sm:gap-x-4 sm:gap-y-10 lg:grid-cols-4">
            {Array.from({ length: 8 }).map((_, index) => (
              <div key={index} className="animate-pulse" aria-hidden>
                <div className="aspect-square w-full rounded-field bg-surface-alt" />
                <div className="mt-3 h-4 w-3/4 rounded-[4px] bg-surface-alt" />
                <div className="mt-2 h-4 w-1/3 rounded-[4px] bg-surface-alt" />
                <div className="mt-2 h-3 w-1/2 rounded-[4px] bg-surface-alt" />
              </div>
            ))}
          </div>
        ) : products.length === 0 && !errorMessage ? (
          <div className="border-t border-border py-20 text-center">
            <p className="text-base font-semibold text-text">조건에 맞는 상품이 없어요</p>
            <p className="mt-1 text-sm text-text-muted">검색어를 바꾸거나 필터를 풀어 보세요.</p>
            {hasActiveFilters ? (
              <button
                type="button"
                onClick={handleReset}
                className="mt-4 inline-flex items-center gap-1 rounded-button border border-border-strong px-3 py-2 text-sm font-semibold text-text hover:bg-surface-alt"
              >
                <X aria-hidden className="h-4 w-4" />
                필터 초기화
              </button>
            ) : null}
          </div>
        ) : (
          <section
            aria-label="상품 목록"
            className="grid grid-cols-2 gap-x-3 gap-y-7 sm:grid-cols-3 sm:gap-x-4 sm:gap-y-10 lg:grid-cols-4"
          >
            {products.map((product, index) => (
              <ProductCard
                key={product.id}
                product={product}
                eager={index < 4}
                highPriority={index < 2}
              />
            ))}
          </section>
        )}
      </main>
    </div>
  );
}
