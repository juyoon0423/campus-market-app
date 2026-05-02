"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useAuth } from "@/src/context/AuthContext";
import { getProduct } from "@/src/lib/apis/productApi";
import { createOrGetChatRoom } from "@/src/lib/apis/chatApi";
import type { ProductDetailResponse } from "@/src/types/product";
import SellerProfile from "@/src/components/SellerProfile";

const FALLBACK_IMAGE_URL = "/window.svg";

function getImageUrl(imageUrls?: string[] | null) {
  const firstImage = imageUrls?.[0];

  if (!firstImage) {
    return FALLBACK_IMAGE_URL;
  }

  if (firstImage.startsWith("http")) {
    return firstImage;
  }

  return `http://localhost:8080/images/${firstImage}`;
}

export default function ProductDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const productId = Number(params.id);
  const isInvalidProductId = Number.isNaN(productId);
  const { isLoggedIn, isHydrated } = useAuth();
  const [product, setProduct] = useState<ProductDetailResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");
  const [isChatLoading, setIsChatLoading] = useState(false);

  useEffect(() => {
    if (isInvalidProductId) {
      return;
    }

    const fetchProduct = async () => {
      try {
        const response = await getProduct(productId);
        setProduct(response);
      } catch {
        setErrorMessage("상품 상세 정보를 불러오지 못했습니다.");
      } finally {
        setIsLoading(false);
      }
    };

    fetchProduct();
  }, [productId, isInvalidProductId]);

  const handleInitiateChat = async () => {
    if (!isLoggedIn || isChatLoading) {
      return;
    }

    setIsChatLoading(true);
    try {
      const room = await createOrGetChatRoom(productId);
      router.push(`/chat?roomId=${room.id}`);
    } catch {
      alert("채팅방을 생성할 수 없습니다.");
      setIsChatLoading(false);
    }
  };

  if (isInvalidProductId) {
    return (
      <div className="min-h-screen bg-slate-100 px-4 py-10">
        <main className="mx-auto w-full max-w-4xl rounded-2xl bg-white p-8 shadow-sm">
          <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">
            잘못된 상품 경로입니다.
          </p>
          <Link
            href="/"
            className="mt-5 inline-block rounded-lg border border-slate-300 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
          >
            목록으로 돌아가기
          </Link>
        </main>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-100 px-4 py-10">
        <main className="mx-auto w-full max-w-4xl rounded-2xl bg-white p-8 shadow-sm">
          <p className="text-sm text-slate-600">상품 정보를 불러오는 중...</p>
        </main>
      </div>
    );
  }

  if (errorMessage || !product) {
    return (
      <div className="min-h-screen bg-slate-100 px-4 py-10">
        <main className="mx-auto w-full max-w-4xl rounded-2xl bg-white p-8 shadow-sm">
          <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">
            {errorMessage || "상품 정보가 없습니다."}
          </p>
          <Link
            href="/"
            className="mt-5 inline-block rounded-lg border border-slate-300 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
          >
            목록으로 돌아가기
          </Link>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-100 px-4 py-10">
      <main className="mx-auto w-full max-w-4xl rounded-2xl bg-white p-6 shadow-sm md:p-8">
        <Link
          href="/"
          className="inline-block rounded-lg border border-slate-300 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
        >
          목록으로 돌아가기
        </Link>
        {isHydrated ? (
          isLoggedIn ? (
            <button
              type="button"
              onClick={handleInitiateChat}
              disabled={isChatLoading}
              className="ml-2 inline-block rounded-lg bg-slate-900 px-4 py-2 text-sm text-white hover:bg-slate-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isChatLoading ? "채팅방 생성 중..." : "채팅 문의하기"}
            </button>
          ) : (
            <Link
              href="/login"
              className="ml-2 inline-block rounded-lg bg-slate-900 px-4 py-2 text-sm text-white hover:bg-slate-700"
            >
              채팅 문의하기
            </Link>
          )
        ) : null}

        <h1 className="mt-5 text-2xl font-bold text-slate-900">{product.title}</h1>
        
        {/* 판매자 프로필 컴포넌트 */}
        <div className="mt-4">
          <SellerProfile sellerId={product?.sellerId || 0} sellerName={product?.sellerName || ""} />
        </div>
        <p className="mt-4 text-2xl font-bold text-slate-900">
          {product.price.toLocaleString()}원
        </p>
        <p className="mt-6 whitespace-pre-wrap text-slate-700">
          {product.description}
        </p>

        <section className="mt-8">
          <div
            className="h-72 rounded-xl bg-slate-200 bg-cover bg-center"
            style={{ backgroundImage: `url(${getImageUrl(product.imageUrls)})` }}
          />
        </section>
      </main>
    </div>
  );
}
