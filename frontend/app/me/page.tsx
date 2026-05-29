"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { Eye, Heart } from "lucide-react";
import { useAuth } from "@/src/context/AuthContext";
import { useUser } from "@/src/hooks/useUser";
import { getMyProducts, getLikedProducts, toggleLike } from "@/src/lib/apis/productApi";
import type { ProductListResponse } from "@/src/types/product";

function formatDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }
  return date.toLocaleString();
}

function getImageUrl(representativeImageUrl?: string | null) {
  if (!representativeImageUrl) {
    return "/window.svg";
  }

  if (representativeImageUrl.startsWith("http")) {
    return representativeImageUrl;
  }

  return `http://localhost:8080/images/${representativeImageUrl}`;
}

export default function MyPage() {
  const router = useRouter();
  const { isLoggedIn, isHydrated, logout } = useAuth();
  const { user, isLoading: userLoading, error: userError } = useUser();
  const [activeTab, setActiveTab] = useState<"sales" | "likes">("sales");
  const [products, setProducts] = useState<ProductListResponse[]>([]);
  const [productsLoading, setProductsLoading] = useState(true);
  const [productsError, setProductsError] = useState("");

  // Redirect to login if not authenticated (after hydration)
  useEffect(() => {
    if (isHydrated && !isLoggedIn) {
      router.replace("/login");
      return;
    }

    if (!isLoggedIn) {
      return;
    }

    const fetchProducts = async () => {
      try {
        if (activeTab === "sales") {
          const productsResponse = await getMyProducts();
          setProducts(productsResponse);
        } else {
          const productsResponse = await getLikedProducts();
          setProducts(productsResponse);
        }
      } catch {
        setProductsError(activeTab === "sales" ? "내 상품 정보를 불러오지 못했습니다." : "찜한 상품 정보를 불러오지 못했습니다.");
      } finally {
        setProductsLoading(false);
      }
    };

    fetchProducts();
  }, [isLoggedIn, isHydrated, router, activeTab]);

  const handleLogout = () => {
    logout();
    router.push("/login");
  };

  const handleToggleLike = async (productId: number, currentIsLiked: boolean) => {
    const previousProducts = [...products];

    // Optimistic update
    setProducts(prev => prev.filter(p => p.id !== productId));

    try {
      await toggleLike(productId);
    } catch (error) {
      console.error("Toggle like error:", error);
      setProducts(previousProducts);
      alert("찜 해제에 실패했습니다.");
    }
  };

  const handleTabChange = (tab: "sales" | "likes") => {
    setActiveTab(tab);
    setProductsLoading(true);
    setProductsError("");
  };

  // Show nothing until hydration is complete
  if (!isHydrated) {
    return null;
  }

  if (!isLoggedIn) {
    return null;
  }

  const isLoading = userLoading || productsLoading;
  const errorMessage = userError?.message || productsError;

  return (
    <div className="min-h-screen bg-slate-100 px-4 py-10">
      <main className="mx-auto w-full max-w-5xl space-y-6">
        <header className="flex flex-wrap items-center justify-between gap-3">
          <h1 className="text-3xl font-bold text-slate-900">마이페이지</h1>
          <div className="flex gap-2">
            <Link
              href="/"
              className="rounded-lg border border-slate-300 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
            >
              메인으로
            </Link>
            <Link
              href="/chat"
              className="rounded-lg border border-slate-300 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
            >
              채팅
            </Link>
            <button
              type="button"
              onClick={handleLogout}
              className="rounded-lg border border-red-200 px-4 py-2 text-sm text-red-600 hover:bg-red-50"
            >
              로그아웃
            </button>
          </div>
        </header>

        {isLoading ? (
          <section className="rounded-2xl bg-white p-6 shadow-sm">
            <p className="text-sm text-slate-600">정보를 불러오는 중...</p>
          </section>
        ) : null}

        {!isLoading && errorMessage ? (
          <section className="rounded-2xl bg-white p-6 shadow-sm">
            <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">
              {errorMessage}
            </p>
          </section>
        ) : null}

        {!isLoading && !errorMessage && user ? (
          <>
            <section className="rounded-2xl bg-white p-6 shadow-sm">
              <h2 className="text-lg font-semibold text-slate-900">내 정보</h2>
              <dl className="mt-4 grid gap-3 text-sm text-slate-700 sm:grid-cols-2">
                <div>
                  <dt className="text-slate-500">이름</dt>
                  <dd className="mt-1 font-medium">{user.name}</dd>
                </div>
                <div>
                  <dt className="text-slate-500">학번</dt>
                  <dd className="mt-1 font-medium">{user.studentId}</dd>
                </div>
                <div>
                  <dt className="text-slate-500">학과</dt>
                  <dd className="mt-1 font-medium">{user.department}</dd>
                </div>
                <div>
                  <dt className="text-slate-500">신뢰도</dt>
                  <dd className="mt-1 font-medium">{user.trustScore}</dd>
                </div>
                <div className="sm:col-span-2">
                  <dt className="text-slate-500">가입일</dt>
                  <dd className="mt-1 font-medium">{formatDate(user.createdAt)}</dd>
                </div>
              </dl>
            </section>

            <section className="rounded-2xl bg-white p-6 shadow-sm">
              {/* Tab UI */}
              <div className="flex gap-6 border-b border-slate-200">
                <button
                  type="button"
                  onClick={() => handleTabChange("sales")}
                  className={`pb-3 text-sm font-medium transition-colors ${
                    activeTab === "sales"
                      ? "border-b-2 border-slate-900 text-slate-900"
                      : "text-slate-500 hover:text-slate-700"
                  }`}
                >
                  판매 내역
                </button>
                <button
                  type="button"
                  onClick={() => handleTabChange("likes")}
                  className={`pb-3 text-sm font-medium transition-colors ${
                    activeTab === "likes"
                      ? "border-b-2 border-slate-900 text-slate-900"
                      : "text-slate-500 hover:text-slate-700"
                  }`}
                >
                  관심 목록 (찜)
                </button>
              </div>

              {/* Product List */}
              {productsLoading ? (
                <p className="mt-4 text-sm text-slate-600">데이터를 불러오는 중...</p>
              ) : productsError ? (
                <p className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">
                  {productsError}
                </p>
              ) : products.length === 0 ? (
                <div className="mt-8 flex flex-col items-center justify-center py-12 text-center">
                  <Heart className="mb-3 h-12 w-12 text-slate-300" />
                  <p className="text-sm text-slate-600">
                    {activeTab === "sales"
                      ? "등록한 상품이 없습니다."
                      : "찜한 상품이 없습니다. 마음에 드는 상품에 하트를 눌러보세요!"}
                  </p>
                </div>
              ) : (
                <div className="mt-6 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
                  {products.map((product) => {
                    const imageUrl = getImageUrl(product.representativeImageUrl);

                    return (
                      <div
                        key={product.id}
                        className="overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-sm transition-transform duration-200 hover:-translate-y-1 hover:shadow-lg"
                      >
                        <Link href={`/products/${product.id}`}>
                          <div className="h-48 w-full overflow-hidden rounded-2xl bg-slate-200">
                            <div
                              className="h-full w-full bg-cover bg-center"
                              style={{ backgroundImage: `url(${imageUrl})` }}
                            />
                          </div>
                        </Link>
                        <div className="space-y-2 p-4">
                          <h2 className="line-clamp-1 text-[15px] font-semibold text-slate-900">
                            {product.title}
                          </h2>
                          <p className="text-sm text-slate-500 before:mr-1 before:content-['👤']">
                            {product.sellerName}
                          </p>
                          <div className="flex items-center justify-between pt-1">
                            <p className="text-2xl font-extrabold tracking-tight text-slate-900">
                              ₩ {product.price.toLocaleString()}
                            </p>
                            <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                              product.status === "SELLING"
                                ? "bg-green-100 text-green-800"
                                : product.status === "RESERVED"
                                ? "bg-yellow-100 text-yellow-800"
                                : "bg-gray-100 text-gray-800"
                            }`}>
                              {product.status === "SELLING" && "판매중"}
                              {product.status === "RESERVED" && "예약중"}
                              {product.status === "SOLD_OUT" && "판매완료"}
                            </span>
                          </div>
                          <div className="flex items-center gap-3 pt-1 text-xs text-gray-500">
                            <div className="flex items-center gap-1">
                              <Eye className="h-3.5 w-3.5" />
                              <span>{product.viewCount}</span>
                            </div>
                            <div className="flex items-center gap-1">
                              <Heart className={`h-3.5 w-3.5 ${product.isLiked ? "fill-red-500 text-red-500" : ""}`} />
                              <span>{product.likeCount}</span>
                            </div>
                          </div>
                          {activeTab === "likes" && (
                            <button
                              type="button"
                              onClick={() => handleToggleLike(product.id, product.isLiked)}
                              className="mt-2 w-full rounded-lg border border-slate-200 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
                            >
                              찜 해제
                            </button>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </section>
          </>
        ) : null}
      </main>
    </div>
  );
}
