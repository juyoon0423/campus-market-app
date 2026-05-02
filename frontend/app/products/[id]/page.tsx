"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useAuth } from "@/src/context/AuthContext";
import { getProduct } from "@/src/lib/apis/productApi";
import { createOrGetChatRoom } from "@/src/lib/apis/chatApi";
import type { ProductDetailResponse, ProductStatus } from "@/src/types/product";
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
  const [isStatusUpdating, setIsStatusUpdating] = useState(false);
  const [statusError, setStatusError] = useState("");
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [isStatusDropdownOpen, setIsStatusDropdownOpen] = useState(false);
  const [isSoldOutModalOpen, setIsSoldOutModalOpen] = useState(false);
  const [buyerId, setBuyerId] = useState("");
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState("");

  // 현재 사용자가 판매자인지 확인
  const getUserIdFromToken = (token: string | null): number | null => {
    if (!token) return null;
    try {
      const payload = token.split('.')[1];
      if (!payload) return null;
      const decoded = JSON.parse(atob(payload));
      console.log("JWT 디코딩 결과:", decoded);  // 디버깅용
      const userId = decoded.userId || decoded.sub || null;
      return userId ? Number(userId) : null;  // ⚠️ 숫자로 변환
    } catch (error) {
      console.error('JWT decode error:', error);
      return null;
    }
  };

  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
  const currentUserId = getUserIdFromToken(token);
  const isSeller = currentUserId && product?.sellerId === currentUserId;

  // 판매자 인식 디버깅 로그
  console.log("=== 판매자 인식 디버깅 ===");
  console.log("1. 기본 정보:", {
    isLoggedIn,
    isHydrated,
    token: token ? "있음" : "없음",
    currentUserId,
    productId
  });
  console.log("2. 상품 정보:", {
    productExists: !!product,
    productTitle: product?.title,
    sellerId: product?.sellerId,
    sellerName: product?.sellerName
  });
  console.log("3. 판매자 확인:", {
    isSeller,
    comparison: `currentUserId(${currentUserId}) === sellerId(${product?.sellerId})`,
    shouldShowDropdown: isHydrated && isLoggedIn && isSeller
  });

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

  const handleStatusChange = async (newStatus: ProductStatus, buyerId?: number) => {
    if (!product || !product.id) {
      setStatusError("상품 정보가 없습니다.");
      return;
    }

    console.log("Status Change - Starting:", {
      productId: product.id,
      currentStatus: product.status,
      newStatus,
      currentUserId
    });

    setIsStatusUpdating(true);
    setStatusError("");

    try {
      const token = localStorage.getItem('token');
      if (!token) {
        setStatusError("로그인이 필요합니다.");
        return;
      }

      const response = await fetch(`http://localhost:8080/api/products/${product.id}/status`, {
        method: 'PATCH',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          status: newStatus,
          buyerId: buyerId || null  // SOLD_OUT일 때만 buyerId 전달
        }),
      });

      console.log("Status Change - Response:", response.status, response.statusText);
      
      if (!response.ok) {
        const errorText = await response.text();
        console.error("Status Change - Error:", errorText);
        
        if (response.status === 403) {
          setStatusError("상품 상태를 변경할 권한이 없습니다.");
        } else if (response.status === 400) {
          setStatusError("요청 형식이 올바르지 않습니다.");
        } else {
          setStatusError(`상태 변경에 실패했습니다 (${response.status}).`);
        }
        return;
      }

      // 상품 정보 새로고침
      const updatedProduct = await getProduct(product.id);
      setProduct(updatedProduct);
      console.log("Status Change - Product updated successfully");
      
      // SOLD_OUT 모달 닫기
      if (newStatus === "SOLD_OUT") {
        setIsSoldOutModalOpen(false);
        setBuyerId("");
        setStatusError("");
      }
    } catch (error) {
      console.error("Status Change - Exception:", error);
      setStatusError("상태 변경에 실패했습니다.");
    } finally {
      setIsStatusUpdating(false);
      setIsDropdownOpen(false);
    }
  };

  const handleEditProduct = () => {
    if (!product) return;
    router.push(`/products/${product.id}/edit`);
  };

  const handleDeleteProduct = async () => {
    if (!product || !product.id) {
      setDeleteError("상품 정보가 없습니다.");
      return;
    }

    const confirmed = window.confirm("정말로 이 상품을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.");
    if (!confirmed) return;

    setIsDeleting(true);
    setDeleteError("");

    try {
      const token = localStorage.getItem('token');
      if (!token) {
        setDeleteError("로그인이 필요합니다.");
        return;
      }

      const response = await fetch(`http://localhost:8080/api/products/${product.id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        if (response.status === 403) {
          setDeleteError("상품을 삭제할 권한이 없습니다.");
        } else {
          setDeleteError("상품 삭제에 실패했습니다.");
        }
        return;
      }

      alert("상품이 성공적으로 삭제되었습니다.");
      router.push("/");
    } catch (error) {
      console.error("Delete Product - Exception:", error);
      setDeleteError("상품 삭제에 실패했습니다.");
    } finally {
      setIsDeleting(false);
      setIsDropdownOpen(false);
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
        <div className="flex items-center justify-between">
          <Link
            href="/"
            className="inline-block rounded-lg border border-slate-300 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
          >
            목록으로 돌아가기
          </Link>
          
          {/* 판매자만 보이는 점 세개 드롭다운 메뉴 */}
          {isHydrated && isLoggedIn && isSeller && (
            <div className="relative">
              <button
                onClick={() => setIsDropdownOpen(!isDropdownOpen)}
                className="inline-flex items-center justify-center w-8 h-8 rounded-lg hover:bg-slate-100 transition-colors"
                disabled={isStatusUpdating || isDeleting}
              >
                <svg className="w-5 h-5 text-slate-600" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M10 6a2 2 0 110-4 2 2 0 010 4zM10 12a2 2 0 110-4 2 2 0 010 4zM10 18a2 2 0 110-4 2 2 0 010 4z" />
                </svg>
              </button>
              
              {isDropdownOpen && (
                <div className="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg border border-slate-200 py-1 z-10">
                  <button
                    onClick={handleEditProduct}
                    className="w-full text-left px-4 py-2 text-sm text-slate-700 hover:bg-slate-50 transition-colors"
                  >
                    상품 내용 수정하기
                  </button>
                  <div className="relative">
                    <button
                      onClick={() => setIsStatusDropdownOpen(!isStatusDropdownOpen)}
                      disabled={isStatusUpdating}
                      className="w-full text-left px-4 py-2 text-sm text-slate-700 hover:bg-slate-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-between"
                    >
                      <span>{isStatusUpdating ? "상태 변경 중..." : "상품 상태 변경하기"}</span>
                      <svg className="w-4 h-4 text-slate-400" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd" />
                      </svg>
                    </button>
                    
                    {isStatusDropdownOpen && (
                      <div className="absolute left-0 mt-1 w-full bg-white rounded-lg shadow-lg border border-slate-200 py-1 z-20">
                        <button
                          onClick={() => {
                            handleStatusChange("SELLING");
                            setIsStatusDropdownOpen(false);
                          }}
                          disabled={isStatusUpdating || product?.status === "SELLING"}
                          className={`w-full text-left px-4 py-2 text-sm transition-colors ${
                            product?.status === "SELLING" 
                              ? "text-slate-400 bg-slate-50 cursor-not-allowed" 
                              : "text-slate-700 hover:bg-slate-50"
                          }`}
                        >
                          판매중 {product?.status === "SELLING" && "(현재)"}
                        </button>
                        <button
                          onClick={() => {
                            handleStatusChange("RESERVED");
                            setIsStatusDropdownOpen(false);
                          }}
                          disabled={isStatusUpdating || product?.status === "RESERVED"}
                          className={`w-full text-left px-4 py-2 text-sm transition-colors ${
                            product?.status === "RESERVED" 
                              ? "text-slate-400 bg-slate-50 cursor-not-allowed" 
                              : "text-slate-700 hover:bg-slate-50"
                          }`}
                        >
                          예약중 {product?.status === "RESERVED" && "(현재)"}
                        </button>
                        <button
                          onClick={() => {
                            setIsSoldOutModalOpen(true);
                            setIsStatusDropdownOpen(false);
                          }}
                          disabled={isStatusUpdating || product?.status === "SOLD_OUT"}
                          className={`w-full text-left px-4 py-2 text-sm transition-colors ${
                            product?.status === "SOLD_OUT" 
                              ? "text-slate-400 bg-slate-50 cursor-not-allowed" 
                              : "text-slate-700 hover:bg-slate-50"
                          }`}
                        >
                          판매완료 {product?.status === "SOLD_OUT" && "(현재)"}
                        </button>
                      </div>
                    )}
                  </div>
                  <button
                    onClick={handleDeleteProduct}
                    disabled={isDeleting}
                    className="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {isDeleting ? "삭제 중..." : "상품 삭제"}
                  </button>
                </div>
              )}
            </div>
          )}
        </div>

        <h1 className="mt-5 text-2xl font-bold text-slate-900">{product.title}</h1>
        
        {/* 상태 변경 및 삭제 에러 메시지 */}
        {(statusError || deleteError) && (
          <div className="mt-3 rounded-lg bg-red-50 px-4 py-2 text-sm text-red-600">
            {statusError || deleteError}
          </div>
        )}
        
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

        {/* 채팅 문의하기 버튼 (판매자가 아닌 경우) */}
        {isHydrated && (
          isLoggedIn ? (
            !isSeller && (
              <button
                type="button"
                onClick={handleInitiateChat}
                disabled={isChatLoading}
                className="mt-8 w-full rounded-lg bg-slate-900 px-4 py-3 text-sm text-white hover:bg-slate-700 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isChatLoading ? "채팅방 생성 중..." : "채팅 문의하기"}
              </button>
            )
          ) : (
            <Link
              href="/login"
              className="mt-8 inline-block w-full rounded-lg bg-slate-900 px-4 py-3 text-sm text-center text-white hover:bg-slate-700"
            >
              채팅 문의하기
            </Link>
          )
        )}

        <section className="mt-8">
          <div
            className="h-72 rounded-xl bg-slate-200 bg-cover bg-center"
            style={{ backgroundImage: `url(${getImageUrl(product.imageUrls)})` }}
          />
        </section>
      </main>
      {/* SOLD_OUT 모달 */}
      {isSoldOutModalOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <h3 className="text-lg font-semibold text-slate-900 mb-4">판매완료 처리</h3>
            <p className="text-sm text-slate-600 mb-4">구매자의 ID를 입력해주세요.</p>
            
            <input
              type="number"
              value={buyerId}
              onChange={(e) => setBuyerId(e.target.value)}
              placeholder="구매자 ID"
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              disabled={isStatusUpdating}
            />
            
            {statusError && (
              <div className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">
                {statusError}
              </div>
            )}
            
            <div className="flex gap-3 mt-6">
              <button
                onClick={() => {
                  setIsSoldOutModalOpen(false);
                  setBuyerId("");
                  setStatusError("");
                }}
                disabled={isStatusUpdating}
                className="flex-1 px-4 py-2 border border-slate-300 text-slate-700 rounded-lg hover:bg-slate-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                취소
              </button>
              <button
                onClick={() => {
                  if (!buyerId.trim()) {
                    setStatusError("구매자 ID를 입력해주세요.");
                    return;
                  }
                  handleStatusChange("SOLD_OUT", Number(buyerId));
                }}
                disabled={isStatusUpdating}
                className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isStatusUpdating ? "처리 중..." : "판매완료"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
