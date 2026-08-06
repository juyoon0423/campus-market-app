"use client";

import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { ChevronLeft, Sparkles } from "lucide-react";
import { useAuth } from "@/src/context/AuthContext";
import {
  getProduct,
  updateProduct,
  generateDescription,
} from "@/src/lib/apis/productApi";
import type {
  ProductDetailResponse,
  ProductUpdateRequest,
} from "@/src/types/product";
import { AxiosError } from "axios";
import { decodeUserIdFromToken } from "@/src/hooks/useCurrentUserId";
import KakaoMapPicker, { TradeLocation } from "@/src/components/KakaoMapPicker";
import SiteHeader from "@/src/components/SiteHeader";
import FormSection from "@/src/components/ui/FormSection";
import Button from "@/src/components/ui/Button";

const FALLBACK_IMAGE_URL = "/window.svg";

const fieldClasses =
  "w-full rounded-field border border-transparent bg-surface-alt px-3.5 py-2.5 text-sm text-text outline-none transition-colors placeholder:text-text-faint focus:border-accent focus:bg-surface disabled:text-text-faint";

function getImageUrl(imageUrl?: string) {
  if (!imageUrl) {
    return FALLBACK_IMAGE_URL;
  }

  if (imageUrl.startsWith("http")) {
    return imageUrl;
  }

  const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";
  const imagePath = imageUrl.startsWith("/") ? imageUrl : `/${imageUrl}`;
  return `${apiUrl}${imagePath}`;
}

export default function ProductEditPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const productId = Number(params.id);
  const isInvalidProductId = Number.isNaN(productId);
  const { isLoggedIn, isHydrated } = useAuth();

  const [product, setProduct] = useState<ProductDetailResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isGeneratingDescription, setIsGeneratingDescription] = useState(false);

  const [formData, setFormData] = useState<ProductUpdateRequest>({
    title: "",
    description: "",
    price: 0,
    category: "",
    tradeLocationName: "",
    tradeLatitude: 0,
    tradeLongitude: 0,
    remainingImageUrls: [],
  });
  const [tradeLocation, setTradeLocation] = useState<TradeLocation | null>(null);

  const [remainingImageUrls, setRemainingImageUrls] = useState<string[]>([]);
  const [newImages, setNewImages] = useState<File[]>([]);

  const categoryOptions = ["학업 관련", "디지털/가전", "생활/자취", "기타"];

  useEffect(() => {
    if (!isHydrated) {
      return;
    }

    if (!isLoggedIn) {
      router.push("/login");
      return;
    }

    if (isInvalidProductId) {
      setErrorMessage("잘못된 상품 경로입니다.");
      setIsLoading(false);
      return;
    }

    const fetchProduct = async () => {
      try {
        const response = await getProduct(productId);

        // 판매자 권한 확인
        const token = localStorage.getItem("accessToken");
        const currentUserId = decodeUserIdFromToken(token);
        if (currentUserId !== null && currentUserId !== response.sellerId) {
          setErrorMessage("상품을 수정할 권한이 없습니다.");
          setIsLoading(false);
          return;
        }

        setProduct(response);
        setFormData({
          title: response.title,
          description: response.description,
          price: response.price,
          category: response.category,
          tradeLocationName: response.tradeLocationName ?? "",
          tradeLatitude: response.tradeLatitude ?? 0,
          tradeLongitude: response.tradeLongitude ?? 0,
          remainingImageUrls: response.imageUrls || [],
        });
        setRemainingImageUrls(response.imageUrls || []);
        if (response.tradeLatitude != null && response.tradeLongitude != null) {
          setTradeLocation({
            locationName: response.tradeLocationName ?? "",
            latitude: response.tradeLatitude,
            longitude: response.tradeLongitude,
          });
        }
      } catch {
        setErrorMessage("상품 정보를 불러오지 못했습니다.");
      } finally {
        setIsLoading(false);
      }
    };

    fetchProduct();
  }, [isHydrated, productId, isInvalidProductId, isLoggedIn, router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (
      !formData.title.trim() ||
      !formData.description.trim() ||
      formData.price <= 0 ||
      !formData.category
    ) {
      setErrorMessage("모든 필드를 올바르게 입력해주세요.");
      return;
    }

    if (!tradeLocation) {
      setErrorMessage("지도에서 거래 희망 장소를 선택해주세요.");
      return;
    }

    setIsSubmitting(true);
    setErrorMessage("");

    try {
      const updateRequest: ProductUpdateRequest = {
        title: formData.title,
        description: formData.description,
        price: formData.price,
        category: formData.category,
        tradeLocationName: tradeLocation.locationName,
        tradeLatitude: tradeLocation.latitude,
        tradeLongitude: tradeLocation.longitude,
        remainingImageUrls: remainingImageUrls,
      };

      await updateProduct(productId, updateRequest, newImages);
      alert("상품이 성공적으로 수정되었습니다.");
      router.push(`/products/${productId}`);
    } catch (error) {
      console.error("Update product error:", error);
      if (error instanceof AxiosError) {
        if (error.response?.status === 400) {
          // Check for inappropriate content error message
          const errorData = error.response?.data as { message?: string };
          if (errorData?.message?.includes("부적절한 내용")) {
            setErrorMessage(errorData.message);
          } else {
            setErrorMessage("입력값을 확인해주세요.");
          }
        } else {
          setErrorMessage("상품 수정에 실패했습니다.");
        }
      } else {
        setErrorMessage("상품 수정에 실패했습니다.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleInputChange = (
    field: keyof ProductUpdateRequest,
    value: string | number,
  ) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleRemoveExistingImage = (imageUrl: string) => {
    setRemainingImageUrls((prev) => prev.filter((url) => url !== imageUrl));
  };

  const handleAddNewImages = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files) return;

    const newImageFiles = Array.from(files);
    setNewImages((prev) => [...prev, ...newImageFiles]);
  };

  const handleRemoveNewImage = (index: number) => {
    setNewImages((prev) => prev.filter((_, i) => i !== index));
  };

  const handleGenerateDescription = async () => {
    if (!formData.title.trim() || !formData.category.trim()) {
      alert("제목과 카테고리를 먼저 입력해주세요.");
      return;
    }

    setIsGeneratingDescription(true);
    setErrorMessage("");

    try {
      const generatedDescription = await generateDescription(
        formData.title,
        formData.category,
      );
      setFormData((prev) => ({
        ...prev,
        description: generatedDescription,
      }));
    } catch (error) {
      console.error("Generate description error:", error);
      setErrorMessage("설명 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setIsGeneratingDescription(false);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen">
        <SiteHeader />
        <main className="mx-auto w-full max-w-3xl px-4 py-10">
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
        <main className="mx-auto w-full max-w-3xl px-4 py-10">
          <div className="rounded-card border border-border bg-surface p-8 shadow-soft">
            <p className="rounded-field bg-red-soft px-4 py-3 text-sm text-red-ink">
              {errorMessage || "상품 정보가 없습니다."}
            </p>
            <Button variant="secondary" size="md" onClick={() => router.back()} className="mt-5">
              뒤로 가기
            </Button>
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen">
      <SiteHeader />
      <main className="mx-auto w-full max-w-3xl px-4 pb-28 pt-6 sm:px-6 md:pb-16 md:pt-10">
        <div className="mb-6">
          <button
            type="button"
            onClick={() => router.back()}
            className="inline-flex items-center gap-1 text-sm font-semibold text-text-muted transition-colors hover:text-text"
          >
            <ChevronLeft className="h-4 w-4" />
            뒤로
          </button>
          <h1 className="mt-3 text-2xl font-extrabold text-text sm:text-[1.75rem]">상품 수정</h1>
        </div>

        <form onSubmit={handleSubmit} className="rounded-card border border-border bg-surface p-6 shadow-soft md:p-8">
          <div className="space-y-8">
            {errorMessage && (
              <div className="rounded-field bg-red-soft px-4 py-3 text-sm text-red-ink">
                {errorMessage}
              </div>
            )}

            <FormSection title="기본 정보" first>
              <div>
                <label htmlFor="title" className="mb-2 block text-sm font-semibold text-text-muted">
                  상품 제목
                </label>
                <input
                  id="title"
                  type="text"
                  value={formData.title}
                  onChange={(e) => handleInputChange("title", e.target.value)}
                  className={fieldClasses}
                  placeholder="상품 제목을 입력하세요"
                  required
                />
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                <div>
                  <label htmlFor="price" className="mb-2 block text-sm font-semibold text-text-muted">
                    가격
                  </label>
                  <input
                    id="price"
                    type="number"
                    value={formData.price}
                    onChange={(e) =>
                      handleInputChange("price", Number(e.target.value))
                    }
                    className={fieldClasses}
                    placeholder="0"
                    min="0"
                    required
                  />
                </div>

                <div>
                  <label htmlFor="category" className="mb-2 block text-sm font-semibold text-text-muted">
                    카테고리
                  </label>
                  <select
                    id="category"
                    value={formData.category}
                    onChange={(e) => handleInputChange("category", e.target.value)}
                    className={fieldClasses}
                    required
                  >
                    <option value="">카테고리를 선택하세요</option>
                    {categoryOptions.map((category) => (
                      <option key={category} value={category}>
                        {category}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
            </FormSection>

            <FormSection title="상품 설명">
              <div className="flex items-center justify-between">
                <label htmlFor="description" className="text-sm font-semibold text-text-muted">
                  자세히 적을수록 잘 팔려요
                </label>
                <button
                  type="button"
                  onClick={handleGenerateDescription}
                  disabled={isGeneratingDescription || isSubmitting}
                  className="flex items-center gap-1.5 rounded-button bg-gradient-to-r from-purple-500 to-pink-500 px-3 py-1.5 text-xs font-semibold text-white transition-all hover:from-purple-600 hover:to-pink-600 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <Sparkles className="h-3.5 w-3.5" />
                  {isGeneratingDescription ? "AI가 작성 중..." : "AI로 설명 쓰기"}
                </button>
              </div>
              <textarea
                id="description"
                value={formData.description}
                onChange={(e) => handleInputChange("description", e.target.value)}
                disabled={isGeneratingDescription || isSubmitting}
                className={fieldClasses}
                rows={8}
                placeholder="상품에 대한 자세한 설명을 입력하세요"
                required
              />
            </FormSection>

            <FormSection title="거래 희망 장소">
              <KakaoMapPicker initialLocation={tradeLocation} onChange={setTradeLocation} />
            </FormSection>

            <FormSection title="사진">
              <div>
                <label className="mb-2 block text-sm font-semibold text-text-muted">
                  기존 이미지
                </label>
                {remainingImageUrls.length > 0 ? (
                  <div className="flex flex-wrap gap-3">
                    {remainingImageUrls.map((imageUrl, index) => (
                      <div key={index} className="group relative">
                        <img
                          src={getImageUrl(imageUrl)}
                          alt={`기존 이미지 ${index + 1}`}
                          className="h-24 w-24 rounded-field border border-border object-cover"
                        />
                        <button
                          type="button"
                          onClick={() => handleRemoveExistingImage(imageUrl)}
                          className="absolute -right-2 -top-2 flex h-6 w-6 items-center justify-center rounded-full bg-red text-white opacity-0 transition-opacity group-hover:opacity-100"
                        >
                          ×
                        </button>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-sm text-text-faint">기존 이미지가 없습니다.</p>
                )}
              </div>

              <div>
                <label htmlFor="newImages" className="mb-2 block text-sm font-semibold text-text-muted">
                  새 이미지 추가
                </label>
                <input
                  id="newImages"
                  type="file"
                  accept="image/*"
                  multiple
                  onChange={handleAddNewImages}
                  className="block w-full rounded-field border border-transparent bg-surface-alt px-3.5 py-2.5 text-sm text-text-muted file:mr-3 file:rounded-field file:border-0 file:bg-accent file:px-3 file:py-1.5 file:text-sm file:font-semibold file:text-white hover:file:bg-accent-strong"
                />
                {newImages.length > 0 && (
                  <div className="mt-3 flex flex-wrap gap-3">
                    {newImages.map((file, index) => (
                      <div key={index} className="group relative">
                        <img
                          src={URL.createObjectURL(file)}
                          alt={`새 이미지 ${index + 1}`}
                          className="h-24 w-24 rounded-field border border-border object-cover"
                        />
                        <button
                          type="button"
                          onClick={() => handleRemoveNewImage(index)}
                          className="absolute -right-2 -top-2 flex h-6 w-6 items-center justify-center rounded-full bg-red text-white opacity-0 transition-opacity group-hover:opacity-100"
                        >
                          ×
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </FormSection>
          </div>

          {/* 제출 버튼 — 모바일에서는 화면 하단에 고정 */}
          <div className="fixed inset-x-0 bottom-0 z-20 flex gap-3 border-t border-border bg-surface/95 p-4 backdrop-blur md:static md:mt-8 md:border-0 md:bg-transparent md:p-0">
            <Button
              type="button"
              variant="secondary"
              size="lg"
              onClick={() => router.back()}
              className="flex-1"
            >
              취소
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="lg"
              disabled={isSubmitting}
              className="flex-1"
            >
              {isSubmitting ? "수정 중..." : "상품 수정하기"}
            </Button>
          </div>
        </form>
      </main>
    </div>
  );
}
