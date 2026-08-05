"use client";

import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { Sparkles } from "lucide-react";
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
import KakaoMapPicker, { TradeLocation } from "@/src/components/KakaoMapPicker";

const FALLBACK_IMAGE_URL = "/window.svg";

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
        if (token) {
          const payload = token.split(".")[1];
          if (payload) {
            const decoded = JSON.parse(atob(payload));
            const currentUserId = Number(decoded.userId || decoded.sub || null); // ⚠️ 숫자로 변환

            if (currentUserId !== response.sellerId) {
              setErrorMessage("상품을 수정할 권한이 없습니다.");
              setIsLoading(false);
              return;
            }
          }
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
          <button
            onClick={() => router.back()}
            className="mt-5 inline-block rounded-lg border border-slate-300 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
          >
            뒤로 가기
          </button>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-100 px-4 py-10">
      <main className="mx-auto w-full max-w-4xl rounded-2xl bg-white p-6 shadow-sm md:p-8">
        <div className="mb-6">
          <button
            onClick={() => router.back()}
            className="inline-block rounded-lg border border-slate-300 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
          >
            뒤로 가기
          </button>
          <h1 className="mt-4 text-2xl font-bold text-slate-900">상품 수정</h1>
        </div>

        {errorMessage && (
          <div className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">
            {errorMessage}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label
              htmlFor="title"
              className="mb-2 block text-sm font-medium text-slate-700"
            >
              상품 제목 *
            </label>
            <input
              id="title"
              type="text"
              value={formData.title}
              onChange={(e) => handleInputChange("title", e.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-slate-500"
              placeholder="상품 제목을 입력하세요"
              required
            />
          </div>

          <div>
            <label
              htmlFor="category"
              className="mb-2 block text-sm font-medium text-slate-700"
            >
              카테고리 *
            </label>
            <select
              id="category"
              value={formData.category}
              onChange={(e) => handleInputChange("category", e.target.value)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 outline-none focus:border-slate-500"
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

          <div>
            <label
              htmlFor="price"
              className="mb-2 block text-sm font-medium text-slate-700"
            >
              가격 *
            </label>
            <input
              id="price"
              type="number"
              value={formData.price}
              onChange={(e) =>
                handleInputChange("price", Number(e.target.value))
              }
              className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-slate-500"
              placeholder="0"
              min="0"
              required
            />
            <p className="mt-1 text-xs text-slate-500">원</p>
          </div>

          <div>
            <div className="mb-2 flex items-center justify-between">
              <label
                htmlFor="description"
                className="block text-sm font-medium text-slate-700"
              >
                상품 설명 *
              </label>
              <button
                type="button"
                onClick={handleGenerateDescription}
                disabled={isGeneratingDescription || isSubmitting}
                className="flex items-center gap-1.5 rounded-lg bg-gradient-to-r from-purple-500 to-pink-500 px-3 py-1.5 text-xs font-medium text-white transition-all hover:from-purple-600 hover:to-pink-600 disabled:cursor-not-allowed disabled:opacity-50"
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
              className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-slate-500 disabled:bg-slate-50"
              rows={8}
              placeholder="상품에 대한 자세한 설명을 입력하세요"
              required
            />
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium text-slate-700">
              거래 희망 장소 *
            </label>
            <KakaoMapPicker initialLocation={tradeLocation} onChange={setTradeLocation} />
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium text-slate-700">
              기존 이미지
            </label>
            {remainingImageUrls.length > 0 ? (
              <div className="flex flex-wrap gap-3">
                {remainingImageUrls.map((imageUrl, index) => (
                  <div key={index} className="relative group">
                    <img
                      src={getImageUrl(imageUrl)}
                      alt={`기존 이미지 ${index + 1}`}
                      className="h-24 w-24 rounded-lg object-cover border border-slate-200"
                    />
                    <button
                      type="button"
                      onClick={() => handleRemoveExistingImage(imageUrl)}
                      className="absolute -right-2 -top-2 flex h-6 w-6 items-center justify-center rounded-full bg-red-500 text-white opacity-0 transition-opacity group-hover:opacity-100"
                    >
                      ×
                    </button>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-sm text-slate-500">기존 이미지가 없습니다.</p>
            )}
          </div>

          <div>
            <label
              htmlFor="newImages"
              className="mb-2 block text-sm font-medium text-slate-700"
            >
              새 이미지 추가
            </label>
            <input
              id="newImages"
              type="file"
              accept="image/*"
              multiple
              onChange={handleAddNewImages}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-slate-500"
            />
            {newImages.length > 0 && (
              <div className="mt-3 flex flex-wrap gap-3">
                {newImages.map((file, index) => (
                  <div key={index} className="relative group">
                    <img
                      src={URL.createObjectURL(file)}
                      alt={`새 이미지 ${index + 1}`}
                      className="h-24 w-24 rounded-lg object-cover border border-slate-200"
                    />
                    <button
                      type="button"
                      onClick={() => handleRemoveNewImage(index)}
                      className="absolute -right-2 -top-2 flex h-6 w-6 items-center justify-center rounded-full bg-red-500 text-white opacity-0 transition-opacity group-hover:opacity-100"
                    >
                      ×
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="flex gap-4">
            <button
              type="button"
              onClick={() => router.back()}
              className="flex-1 rounded-lg border border-slate-300 px-4 py-3 text-sm text-slate-700 hover:bg-slate-50"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="flex-1 rounded-lg bg-slate-900 px-4 py-3 text-sm text-white hover:bg-slate-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isSubmitting ? "수정 중..." : "상품 수정하기"}
            </button>
          </div>
        </form>
      </main>
    </div>
  );
}
