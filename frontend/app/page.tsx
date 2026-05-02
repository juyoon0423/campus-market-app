"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useAuth } from "@/src/context/AuthContext";
import { getAllProducts, searchProducts } from "@/src/lib/apis/productApi";
import type { ProductListResponse, ProductStatus } from "@/src/types/product";

const FALLBACK_IMAGE_URL = "/window.svg";

function getImageUrl(representativeImageUrl?: string | null) {
  if (!representativeImageUrl) {
    return FALLBACK_IMAGE_URL;
  }

  if (representativeImageUrl.startsWith("http")) {
    return representativeImageUrl;
  }

  return `http://localhost:8080/images/${representativeImageUrl}`;
}

export default function HomePage() {
  const router = useRouter();
  const { isLoggedIn, isHydrated, logout } = useAuth();
  const [products, setProducts] = useState<ProductListResponse[]>([]);
  const [keyword, setKeyword] = useState("");
  const [category, setCategory] = useState("");
  const [status, setStatus] = useState<ProductStatus | "">("");
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        const response = await getAllProducts();
        setProducts(response);
      } catch {
        setErrorMessage("상품 목록을 불러오지 못했습니다.");
      } finally {
        setIsLoading(false);
      }
    };

    fetchProducts();
  }, []);

  const handleSearch = async () => {
    setIsLoading(true);
    setErrorMessage("");

    try {
      const result = await searchProducts({
        keyword: keyword || undefined,
        category: category || undefined,
        status: status || undefined,
      });
      setProducts(result);
    } catch {
      setErrorMessage("검색 중 문제가 발생했습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleReset = async () => {
    setKeyword("");
    setCategory("");
    setStatus("");
    setIsLoading(true);
    setErrorMessage("");

    try {
      const result = await getAllProducts();
      setProducts(result);
    } catch {
      setErrorMessage("상품 목록을 불러오지 못했습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogout = () => {
    logout();
    router.push("/login");
  };

  return (
    <div className="min-h-screen bg-gray-50 px-4 py-8 md:py-10">
      <main className="mx-auto w-full max-w-6xl">
        <header className="mb-6 flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
          <div>
            <h1 className="text-3xl font-extrabold tracking-tight text-slate-900">
              캠퍼스 마켓
            </h1>
            <p className="mt-2 text-sm text-slate-500">
              원하는 상품을 검색하고 상세 정보를 확인해보세요.
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            {isHydrated && isLoggedIn ? (
              <>
                <Link
                  href="/upload"
                  className="rounded-xl bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-700"
                >
                  상품 등록
                </Link>
                <Link
                  href="/me"
                  className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
                >
                  마이페이지
                </Link>
                <Link
                  href="/chat"
                  className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
                >
                  채팅
                </Link>
                <button
                  type="button"
                  onClick={handleLogout}
                  className="rounded-xl border border-red-200 bg-white px-4 py-2 text-sm font-semibold text-red-600 hover:bg-red-50"
                >
                  로그아웃
                </button>
              </>
            ) : (
              <>
                <Link
                  href="/login"
                  className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
                >
                  로그인
                </Link>
                <Link
                  href="/signup"
                  className="rounded-xl bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-700"
                >
                  회원가입
                </Link>
              </>
            )}
          </div>
        </header>

        <section className="mb-8 rounded-2xl border border-slate-100 bg-white p-4 shadow-sm md:p-5">
          <div className="space-y-4">
            {/* 검색창 */}
            <div className="relative">
              <input
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="검색어를 입력하세요"
                className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 pl-10 text-sm outline-none focus:border-[#ff8a3d] focus:bg-white"
              />
              <svg
                className="absolute left-3 top-3.5 h-4 w-4 text-slate-400"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                />
              </svg>
            </div>
            
            {/* 필터 */}
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
              <div className="flex-1">
                <label className="mb-1 block text-xs font-medium text-slate-600">카테고리</label>
                <select
                  value={category}
                  onChange={(event) => setCategory(event.target.value)}
                  className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm outline-none focus:border-[#ff8a3d] focus:bg-white"
                >
                  <option value="">전체 카테고리</option>
                  <option value="학업 관련">학업 관련</option>
                  <option value="디지털/가전">디지털/가전</option>
                  <option value="생활/자취">생활/자취</option>
                  <option value="기타">기타</option>
                </select>
              </div>
              
              <div className="flex-1">
                <label className="mb-1 block text-xs font-medium text-slate-600">상태</label>
                <select
                  value={status}
                  onChange={(event) => setStatus(event.target.value as ProductStatus | "")}
                  className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm outline-none focus:border-[#ff8a3d] focus:bg-white"
                >
                  <option value="">전체 상태</option>
                  <option value="SELLING">판매중</option>
                  <option value="RESERVED">예약중</option>
                  <option value="SOLD_OUT">판매완료</option>
                </select>
              </div>
              
              <div className="flex gap-2 sm:mt-6">
                <button
                  type="button"
                  onClick={handleSearch}
                  className="flex-1 rounded-xl bg-[#ff8a3d] px-4 py-2.5 text-sm font-semibold text-white hover:bg-[#ff7a25] sm:flex-initial"
                >
                  검색
                </button>
                <button
                  type="button"
                  onClick={handleReset}
                  className="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50"
                >
                  초기화
                </button>
              </div>
            </div>
          </div>
        </section>

        {errorMessage ? (
          <p className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">
            {errorMessage}
          </p>
        ) : null}

        {isLoading ? (
          <p className="text-sm text-slate-600">상품을 불러오는 중...</p>
        ) : (
          <section className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {products.map((product) => {
              const imageUrl = getImageUrl(product.representativeImageUrl);

              return (
                <Link
                  key={product.id}
                  href={`/products/${product.id}`}
                  className="overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-sm transition-transform duration-200 hover:-translate-y-1 hover:shadow-lg"
                >
                  <div className="h-48 w-full overflow-hidden rounded-2xl bg-slate-200">
                    <div
                      className="h-full w-full bg-cover bg-center"
                      style={{ backgroundImage: `url(${imageUrl})` }}
                    />
                  </div>
                  <div className="space-y-2 p-4">
                    <h2 className="line-clamp-1 text-[15px] font-semibold text-slate-900">
                      {product.title}
                    </h2>
                    <p className="text-sm text-slate-500 before:mr-1 before:content-['👤']">
                      {product.sellerName}
                    </p>
                    <p className="pt-1 text-2xl font-extrabold tracking-tight text-slate-900">
                      ₩ {product.price.toLocaleString()}
                    </p>
                  </div>
                </Link>
              );
            })}
          </section>
        )}

        {!isLoading && products.length === 0 ? (
          <p className="mt-6 text-sm text-slate-600">검색 결과가 없습니다.</p>
        ) : null}
      </main>
    </div>
  );
}
