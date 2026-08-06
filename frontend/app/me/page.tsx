"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { Heart } from "lucide-react";
import { useAuth } from "@/src/context/AuthContext";
import { useUser } from "@/src/hooks/useUser";
import { getMyProducts, getLikedProducts, toggleLike } from "@/src/lib/apis/productApi";
import type { ProductListResponse } from "@/src/types/product";
import SiteHeader from "@/src/components/SiteHeader";
import ProductCard from "@/src/components/ProductCard";

function formatDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }
  return date.toLocaleString();
}

export default function MyPage() {
  const router = useRouter();
  const { isLoggedIn, isHydrated } = useAuth();
  const { user, isLoading: userLoading, error: userError } = useUser();
  const [activeTab, setActiveTab] = useState<"sales" | "likes">("sales");
  const [products, setProducts] = useState<ProductListResponse[]>([]);
  const [productsLoading, setProductsLoading] = useState(true);
  const [productsError, setProductsError] = useState("");
  const [actionError, setActionError] = useState("");

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

  const handleToggleLike = async (productId: number) => {
    const previousProducts = [...products];
    setActionError("");

    // Optimistic update
    setProducts(prev => prev.filter(p => p.id !== productId));

    try {
      await toggleLike(productId);
    } catch {
      setProducts(previousProducts);
      setActionError("찜 해제에 실패했습니다.");
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
    <div className="min-h-screen">
      <SiteHeader />
      <main className="mx-auto w-full max-w-6xl px-4 py-8 sm:px-6 md:py-12">
        <h1 className="mb-8 text-2xl font-extrabold text-text sm:text-[1.75rem]">마이페이지</h1>

        {isLoading ? (
          <p className="text-sm text-text-muted">정보를 불러오는 중...</p>
        ) : null}

        {!isLoading && errorMessage ? (
          <p className="rounded-field bg-red-soft px-4 py-3 text-sm text-red-ink">
            {errorMessage}
          </p>
        ) : null}

        {!isLoading && !errorMessage && user ? (
          <div className="space-y-8">
            <section className="flex flex-col gap-4 rounded-card border border-border bg-surface p-6 shadow-soft sm:flex-row sm:items-center">
              <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-accent to-accent-strong text-2xl font-bold text-white">
                {user.name.charAt(0)}
              </div>
              <div className="flex-1">
                <h2 className="text-xl font-extrabold text-text">{user.name}</h2>
                <p className="text-sm text-text-faint">{user.department} · {user.studentId}</p>
                <p className="mt-1 text-xs text-text-faint">가입일 {formatDate(user.createdAt)}</p>
              </div>
              <div className="rounded-field bg-surface-alt px-4 py-3 text-center sm:text-right">
                <p className="text-xs font-semibold text-text-muted">신뢰도</p>
                <p className="text-lg font-extrabold tabular-nums text-text">{user.trustScore}</p>
              </div>
            </section>

            <section>
              {/* Tab UI */}
              <div className="flex gap-6 border-b border-border">
                <button
                  type="button"
                  onClick={() => handleTabChange("sales")}
                  className={`relative pb-3 text-sm font-bold transition-colors ${
                    activeTab === "sales" ? "text-text" : "text-text-faint hover:text-text-muted"
                  }`}
                >
                  판매 내역
                  {activeTab === "sales" && (
                    <span className="absolute bottom-0 left-0 right-0 h-0.5 rounded-full bg-accent" />
                  )}
                </button>
                <button
                  type="button"
                  onClick={() => handleTabChange("likes")}
                  className={`relative pb-3 text-sm font-bold transition-colors ${
                    activeTab === "likes" ? "text-text" : "text-text-faint hover:text-text-muted"
                  }`}
                >
                  관심 목록 (찜)
                  {activeTab === "likes" && (
                    <span className="absolute bottom-0 left-0 right-0 h-0.5 rounded-full bg-accent" />
                  )}
                </button>
              </div>

              {actionError ? (
                <p className="mt-4 rounded-field bg-red-soft px-4 py-3 text-sm text-red-ink">
                  {actionError}
                </p>
              ) : null}

              {/* Product List */}
              {productsLoading ? (
                <p className="mt-6 text-sm text-text-muted">데이터를 불러오는 중...</p>
              ) : productsError ? (
                <p className="mt-6 rounded-field bg-red-soft px-4 py-3 text-sm text-red-ink">
                  {productsError}
                </p>
              ) : products.length === 0 ? (
                <div className="mt-6 flex flex-col items-center justify-center rounded-card border border-dashed border-border-strong py-20 text-center">
                  <Heart className="mb-3 h-10 w-10 text-text-faint" />
                  <p className="text-sm text-text-muted">
                    {activeTab === "sales"
                      ? "등록한 상품이 없어요."
                      : "찜한 상품이 없어요. 마음에 드는 상품에 하트를 눌러보세요!"}
                  </p>
                </div>
              ) : (
                <div className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-3 sm:gap-6">
                  {products.map((product) => (
                    <ProductCard
                      key={product.id}
                      product={product}
                      action={
                        activeTab === "likes" ? (
                          <button
                            type="button"
                            onClick={() => handleToggleLike(product.id)}
                            className="w-full rounded-field border border-border-strong px-4 py-2 text-sm font-semibold text-text-muted transition-colors hover:bg-surface-alt hover:text-text"
                          >
                            찜 해제
                          </button>
                        ) : undefined
                      }
                    />
                  ))}
                </div>
              )}
            </section>
          </div>
        ) : null}
      </main>
    </div>
  );
}
