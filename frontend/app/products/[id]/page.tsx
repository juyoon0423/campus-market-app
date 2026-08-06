"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { Eye, Heart, MoreVertical, ChevronDown, ChevronLeft } from "lucide-react";
import { AxiosError } from "axios";
import { useAuth } from "@/src/context/AuthContext";
import { deleteProduct, getProduct, toggleLike, updateProductStatus } from "@/src/lib/apis/productApi";
import { createOrGetChatRoom, getProductChatRooms } from "@/src/lib/apis/chatApi";
import type { ProductDetailResponse, ProductStatus } from "@/src/types/product";
import type { ChatRoomResponse } from "@/src/types/chat";
import KakaoMapView from "@/src/components/KakaoMapView";
import SiteHeader from "@/src/components/SiteHeader";
import StatusPill from "@/src/components/ui/StatusPill";
import Button, { buttonClasses } from "@/src/components/ui/Button";

function resolveImageUrl(imageUrl: string) {
  if (imageUrl.startsWith("http")) {
    return imageUrl;
  }

  const apiUrl = process.env.NEXT_PUBLIC_API_URL;
  const imagePath = imageUrl.startsWith("/") ? imageUrl : `/${imageUrl}`;
  return `${apiUrl}${imagePath}`;
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
  const [chatRooms, setChatRooms] = useState<ChatRoomResponse[]>([]);
  const [isLoadingChatRooms, setIsLoadingChatRooms] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState("");
  const [isLiking, setIsLiking] = useState(false);
  const [activeImageIndex, setActiveImageIndex] = useState(0);
  const hasFetchedProduct = useRef(false);

  // 현재 사용자가 판매자인지 확인
  const getUserIdFromToken = (token: string | null): number | null => {
    if (!token) return null;
    try {
      const payload = token.split('.')[1];
      if (!payload) return null;
      const decoded = JSON.parse(atob(payload));
      const userId = decoded.userId || decoded.sub || null;
      return userId ? Number(userId) : null;
    } catch {
      return null;
    }
  };

  const token = typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null;
  const currentUserId = getUserIdFromToken(token);
  const isSeller = !!(currentUserId && product?.sellerId === currentUserId);

  useEffect(() => {
    if (isInvalidProductId) {
      return;
    }

    // Prevent duplicate API calls (React Strict Mode)
    if (hasFetchedProduct.current) {
      return;
    }

    const fetchProduct = async () => {
      try {
        const response = await getProduct(productId);
        setProduct(response);
        setActiveImageIndex(0);
      } catch {
        setErrorMessage("상품 상세 정보를 불러오지 못했습니다.");
      } finally {
        setIsLoading(false);
      }
    };

    fetchProduct();
    hasFetchedProduct.current = true;
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

    setIsStatusUpdating(true);
    setStatusError("");

    try {
      await updateProductStatus(product.id, newStatus, buyerId);

      // 상품 정보 새로고침
      const updatedProduct = await getProduct(product.id);
      setProduct(updatedProduct);

      // SOLD_OUT 모달 닫기
      if (newStatus === "SOLD_OUT") {
        setIsSoldOutModalOpen(false);
        setBuyerId("");
        setStatusError("");
      }
    } catch (error) {
      if (error instanceof AxiosError && error.response?.status === 403) {
        setStatusError("상품 상태를 변경할 권한이 없습니다.");
      } else if (error instanceof AxiosError && error.response?.status === 400) {
        setStatusError("요청 형식이 올바르지 않습니다.");
      } else {
        setStatusError("상태 변경에 실패했습니다.");
      }
    } finally {
      setIsStatusUpdating(false);
      setIsDropdownOpen(false);
    }
  };

  const handleEditProduct = () => {
    if (!product) return;
    router.push(`/products/${product.id}/edit`);
  };

  const loadChatRooms = async () => {
    if (!product) return;

    setIsLoadingChatRooms(true);
    try {
      const rooms = await getProductChatRooms(productId);
      setChatRooms(rooms);
    } catch (error) {
      console.error("Failed to load chat rooms:", error);
    } finally {
      setIsLoadingChatRooms(false);
    }
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
      await deleteProduct(product.id);
      alert("상품이 성공적으로 삭제되었습니다.");
      router.push("/");
    } catch (error) {
      if (error instanceof AxiosError && error.response?.status === 403) {
        setDeleteError("상품을 삭제할 권한이 없습니다.");
      } else {
        setDeleteError("상품 삭제에 실패했습니다.");
      }
    } finally {
      setIsDeleting(false);
      setIsDropdownOpen(false);
    }
  };

  const handleToggleLike = async () => {
    if (!product || !product.id) return;
    if (!isLoggedIn) {
      alert("로그인이 필요합니다.");
      return;
    }

    // Optimistic update: 즉시 UI 업데이트
    const previousIsLiked = product.isLiked;
    const previousLikeCount = product.likeCount;

    setProduct(prev => prev ? {
      ...prev,
      isLiked: !prev.isLiked,
      likeCount: prev.isLiked ? prev.likeCount - 1 : prev.likeCount + 1,
    } : null);

    setIsLiking(true);

    try {
      await toggleLike(product.id);
    } catch (error) {
      console.error("Toggle like error:", error);

      // Rollback: 실패 시 원래 상태로 복원
      setProduct(prev => prev ? {
        ...prev,
        isLiked: previousIsLiked,
        likeCount: previousLikeCount,
      } : null);

      // 자신의 상품인 경우 에러 메시지 표시
      if (error instanceof Error && error.message.includes("자신의 상품")) {
        alert("자신의 상품은 찜할 수 없습니다.");
      } else {
        alert("찜하기에 실패했습니다.");
      }
    } finally {
      setIsLiking(false);
    }
  };

  if (isInvalidProductId) {
    return (
      <div className="min-h-screen">
        <SiteHeader />
        <main className="mx-auto w-full max-w-4xl px-4 py-10">
          <div className="rounded-card border border-border bg-surface p-8 shadow-soft">
            <p className="rounded-field bg-red-soft px-4 py-3 text-sm text-red-ink">
              잘못된 상품 경로입니다.
            </p>
            <Link href="/" className={`mt-5 inline-flex ${buttonClasses("secondary", "md")}`}>
              목록으로 돌아가기
            </Link>
          </div>
        </main>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="min-h-screen">
        <SiteHeader />
        <main className="mx-auto w-full max-w-4xl px-4 py-10">
          <div className="rounded-card border border-border bg-surface p-8 shadow-soft">
            <p className="text-sm text-text-muted">상품 정보를 불러오는 중...</p>
          </div>
        </main>
      </div>
    );
  }

  if (errorMessage || !product) {
    return (
      <div className="min-h-screen">
        <SiteHeader />
        <main className="mx-auto w-full max-w-4xl px-4 py-10">
          <div className="rounded-card border border-border bg-surface p-8 shadow-soft">
            <p className="rounded-field bg-red-soft px-4 py-3 text-sm text-red-ink">
              {errorMessage || "상품 정보가 없습니다."}
            </p>
            <Link href="/" className={`mt-5 inline-flex ${buttonClasses("secondary", "md")}`}>
              목록으로 돌아가기
            </Link>
          </div>
        </main>
      </div>
    );
  }

  const resolvedImageUrls = (product.imageUrls ?? []).map(resolveImageUrl);

  return (
    <div className="min-h-screen">
      <SiteHeader />
      <main className="mx-auto w-full max-w-5xl px-4 pb-28 pt-6 sm:px-6 md:pb-10 md:pt-8">
        <div className="mb-5 flex items-center justify-between">
          <Link
            href="/"
            className="inline-flex items-center gap-1 text-sm font-semibold text-text-muted transition-colors hover:text-text"
          >
            <ChevronLeft className="h-4 w-4" />
            목록으로
          </Link>

          {/* 판매자만 보이는 점 세개 드롭다운 메뉴 */}
          {isHydrated && isLoggedIn && isSeller && (
            <div className="relative">
              <button
                onClick={() => setIsDropdownOpen(!isDropdownOpen)}
                className="inline-flex h-9 w-9 items-center justify-center rounded-field text-text-muted transition-colors hover:bg-surface-alt hover:text-text"
                disabled={isStatusUpdating || isDeleting}
              >
                <MoreVertical className="h-5 w-5" />
              </button>

              {isDropdownOpen && (
                <div className="absolute right-0 z-10 mt-2 w-52 rounded-button border border-border bg-surface py-1 shadow-elevated">
                  {/* 판매완료 상품이 아닌 경우에만 수정 및 상태 변경 표시 */}
                  {product?.status !== "SOLD_OUT" && (
                    <>
                      <button
                        onClick={handleEditProduct}
                        className="w-full px-4 py-2 text-left text-sm text-text transition-colors hover:bg-surface-alt"
                      >
                        상품 내용 수정하기
                      </button>
                      <div className="relative">
                        <button
                          onClick={() => setIsStatusDropdownOpen(!isStatusDropdownOpen)}
                          disabled={isStatusUpdating}
                          className="flex w-full items-center justify-between px-4 py-2 text-left text-sm text-text transition-colors hover:bg-surface-alt disabled:cursor-not-allowed disabled:opacity-50"
                        >
                          <span>{isStatusUpdating ? "상태 변경 중..." : "상품 상태 변경하기"}</span>
                          <ChevronDown className="h-4 w-4 text-text-faint" />
                        </button>

                        {isStatusDropdownOpen && (
                          <div className="absolute left-0 z-20 mt-1 w-full rounded-button border border-border bg-surface py-1 shadow-elevated">
                            <button
                              onClick={() => {
                                handleStatusChange("SELLING");
                                setIsStatusDropdownOpen(false);
                              }}
                              disabled={isStatusUpdating || product?.status === "SELLING"}
                              className={`w-full px-4 py-2 text-left text-sm transition-colors ${
                                product?.status === "SELLING"
                                  ? "cursor-not-allowed bg-surface-alt text-text-faint"
                                  : "text-text hover:bg-surface-alt"
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
                              className={`w-full px-4 py-2 text-left text-sm transition-colors ${
                                product?.status === "RESERVED"
                                  ? "cursor-not-allowed bg-surface-alt text-text-faint"
                                  : "text-text hover:bg-surface-alt"
                              }`}
                            >
                              예약중 {product?.status === "RESERVED" && "(현재)"}
                            </button>
                            <button
                              onClick={() => {
                                setIsSoldOutModalOpen(true);
                                setIsStatusDropdownOpen(false);
                                loadChatRooms();
                              }}
                              disabled={isStatusUpdating}
                              className="w-full px-4 py-2 text-left text-sm text-text transition-colors hover:bg-surface-alt"
                            >
                              판매완료
                            </button>
                          </div>
                        )}
                      </div>
                    </>
                  )}

                  {/* 상품 삭제는 항상 표시 */}
                  <button
                    onClick={handleDeleteProduct}
                    disabled={isDeleting}
                    className="w-full px-4 py-2 text-left text-sm text-red transition-colors hover:bg-red-soft disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {isDeleting ? "삭제 중..." : "상품 삭제"}
                  </button>
                </div>
              )}
            </div>
          )}
        </div>

        {/* 2-column layout for desktop */}
        <div className="grid gap-8 md:grid-cols-2 md:gap-10">
          {/* Left column: Image gallery */}
          <div className="space-y-3">
            <div className="aspect-square w-full overflow-hidden rounded-card bg-surface-alt">
              {resolvedImageUrls.length > 0 ? (
                <img
                  src={resolvedImageUrls[activeImageIndex]}
                  alt={product.title}
                  className="h-full w-full object-contain"
                />
              ) : (
                <div className="flex h-full w-full items-center justify-center text-text-faint">
                  <span className="text-sm">이미지 없음</span>
                </div>
              )}
            </div>

            {resolvedImageUrls.length > 1 ? (
              <div className="flex gap-2 overflow-x-auto">
                {resolvedImageUrls.map((url, index) => (
                  <button
                    key={url + index}
                    type="button"
                    onClick={() => setActiveImageIndex(index)}
                    className={`h-16 w-16 shrink-0 overflow-hidden rounded-field border-2 transition-colors ${
                      index === activeImageIndex ? "border-accent" : "border-transparent"
                    }`}
                  >
                    <img src={url} alt="" className="h-full w-full object-cover" />
                  </button>
                ))}
              </div>
            ) : null}
          </div>

          {/* Right column: Product info */}
          <div className="space-y-7">
            <div>
              <h1 className="text-2xl font-extrabold leading-snug text-text">{product.title}</h1>

              {/* 상태 변경 및 삭제 에러 메시지 */}
              {(statusError || deleteError) && (
                <div className="mt-3 rounded-field bg-red-soft px-4 py-2 text-sm text-red-ink">
                  {statusError || deleteError}
                </div>
              )}
            </div>

            <div className="border-b border-border pb-7">
              {/* 가격과 상태 표시 */}
              <div className="flex items-center justify-between">
                <p className="text-3xl font-extrabold tabular-nums text-text">
                  {product.price.toLocaleString()}원
                </p>
                <div className="flex items-center gap-3">
                  <StatusPill status={product.status} />
                  <button
                    type="button"
                    onClick={handleToggleLike}
                    disabled={isLiking || isSeller}
                    className="flex h-10 w-10 items-center justify-center rounded-full border border-border bg-surface transition-colors hover:bg-surface-alt disabled:cursor-not-allowed disabled:opacity-50"
                    title={product.isLiked ? "찜 취소" : "찜하기"}
                  >
                    <Heart
                      className={`h-5 w-5 ${product.isLiked ? "fill-red text-red" : "text-text-faint"}`}
                    />
                  </button>
                </div>
              </div>

              {/* 조회수와 좋아요 수 표시 */}
              <div className="mt-3 flex items-center gap-4 text-sm tabular-nums text-text-faint">
                <div className="flex items-center gap-1.5">
                  <Eye className="h-4 w-4" />
                  <span>조회 {product.viewCount}</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <Heart className="h-4 w-4" />
                  <span>찜 {product.likeCount}</span>
                </div>
              </div>
            </div>

            {/* 판매자 정보 */}
            <div className="flex items-center gap-3 border-b border-border pb-7">
              <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-accent to-accent-strong text-base font-bold text-white">
                {product.sellerName.charAt(0)}
              </div>
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-text">{product.sellerName}</p>
                <p className="text-xs text-text-faint">신뢰도 {product.sellerTrustScore.toFixed(1)}</p>
              </div>
            </div>

            <div className="border-b border-border pb-7">
              <h2 className="mb-3 text-lg font-bold text-text">상품 설명</h2>
              <p className="whitespace-pre-wrap text-sm leading-relaxed text-text-muted">
                {product.description}
              </p>
            </div>

            {product.tradeLatitude != null && product.tradeLongitude != null ? (
              <div className="border-b border-border pb-7 md:border-b-0 md:pb-0">
                <h2 className="mb-3 text-lg font-bold text-text">거래 희망 장소</h2>
                <KakaoMapView
                  latitude={product.tradeLatitude}
                  longitude={product.tradeLongitude}
                  locationName={product.tradeLocationName}
                />
              </div>
            ) : null}

            {/* 채팅 문의하기 버튼 (판매자가 아닌 경우) — 모바일에서는 화면 하단에 고정 */}
            {isHydrated && (
              isLoggedIn ? (
                !isSeller && (
                  <div className="fixed inset-x-0 bottom-0 z-20 border-t border-border bg-surface/95 p-4 backdrop-blur md:static md:border-0 md:bg-transparent md:p-0">
                    <Button
                      variant="primary"
                      size="lg"
                      onClick={handleInitiateChat}
                      disabled={isChatLoading}
                      className="mx-auto w-full max-w-5xl md:mx-0"
                    >
                      {isChatLoading ? "채팅방 생성 중..." : "채팅 문의하기"}
                    </Button>
                  </div>
                )
              ) : (
                <div className="fixed inset-x-0 bottom-0 z-20 border-t border-border bg-surface/95 p-4 backdrop-blur md:static md:border-0 md:bg-transparent md:p-0">
                  <Link
                    href="/login"
                    className={`mx-auto block max-w-5xl text-center md:mx-0 ${buttonClasses("primary", "lg")}`}
                  >
                    채팅 문의하기
                  </Link>
                </div>
              )
            )}
          </div>
        </div>
      </main>
      {/* SOLD_OUT 모달 */}
      {isSoldOutModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4">
          <div className="w-full max-w-md rounded-card bg-surface p-6 shadow-floating">
            <h3 className="mb-4 text-lg font-bold text-text">판매완료 처리</h3>
            <p className="mb-4 text-sm text-text-muted">채팅을 나눈 구매자를 선택해주세요.</p>

            {isLoadingChatRooms ? (
              <div className="py-4 text-center">
                <div className="text-sm text-text-muted">채팅 목록을 불러오는 중...</div>
              </div>
            ) : chatRooms.length === 0 ? (
              <div className="py-4 text-center">
                <div className="text-sm text-text-muted">채팅을 나눈 구매자가 없습니다.</div>
              </div>
            ) : (
              <div className="max-h-48 space-y-2 overflow-y-auto">
                {chatRooms.map((room) => (
                  <button
                    key={room.id}
                    onClick={() => {
                      setBuyerId(room.buyerId.toString());
                      handleStatusChange("SOLD_OUT", room.buyerId);
                    }}
                    disabled={isStatusUpdating}
                    className="w-full rounded-field border border-border px-3 py-2 text-left transition-colors hover:bg-surface-alt disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    <div className="font-medium text-text">{room.opponentName}</div>
                    <div className="text-xs text-text-faint">{room.lastMessage || "메시지 없음"}</div>
                  </button>
                ))}
              </div>
            )}

            {statusError && (
              <div className="mt-3 rounded-field bg-red-soft px-3 py-2 text-sm text-red-ink">
                {statusError}
              </div>
            )}

            <div className="mt-6 flex gap-3">
              <Button
                variant="secondary"
                size="md"
                onClick={() => {
                  setIsSoldOutModalOpen(false);
                  setBuyerId("");
                  setStatusError("");
                  setChatRooms([]);
                }}
                disabled={isStatusUpdating}
                className="flex-1"
              >
                취소
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
