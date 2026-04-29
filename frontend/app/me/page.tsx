"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useAuth } from "@/src/context/AuthContext";
import { useUser } from "@/src/hooks/useUser";
import { getMyProducts } from "@/src/lib/apis/productApi";
import type { ProductListResponse } from "@/src/types/product";

function formatDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }
  return date.toLocaleString();
}

export default function MyPage() {
  const router = useRouter();
  const { isLoggedIn, isHydrated, logout } = useAuth();
  const { user, isLoading: userLoading, error: userError } = useUser();
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

    const fetchMyProducts = async () => {
      try {
        const productsResponse = await getMyProducts();
        setProducts(productsResponse);
      } catch {
        setProductsError("내 상품 정보를 불러오지 못했습니다.");
      } finally {
        setProductsLoading(false);
      }
    };

    fetchMyProducts();
  }, [isLoggedIn, isHydrated, router]);

  const handleLogout = () => {
    logout();
    router.push("/login");
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
              <h2 className="text-lg font-semibold text-slate-900">내 상품</h2>
              {products.length === 0 ? (
                <p className="mt-4 text-sm text-slate-600">등록한 상품이 없습니다.</p>
              ) : (
                <ul className="mt-4 space-y-3">
                  {products.map((product) => (
                    <li
                      key={product.id}
                      className="rounded-xl border border-slate-200 px-4 py-3"
                    >
                      <Link
                        href={`/products/${product.id}`}
                        className="flex items-center justify-between gap-2"
                      >
                        <span className="font-medium text-slate-900">{product.title}</span>
                        <span className="text-sm font-semibold text-slate-700">
                          {product.price.toLocaleString()}원
                        </span>
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </>
        ) : null}
      </main>
    </div>
  );
}
