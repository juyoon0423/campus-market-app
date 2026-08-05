"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ChangeEvent, FormEvent, useMemo, useState, useEffect } from "react";
import { ChevronLeft, Sparkles } from "lucide-react";
import { createProduct, generateDescription } from "@/src/lib/apis/productApi";
import { useAuth } from "@/src/context/AuthContext";
import { AxiosError } from "axios";
import KakaoMapPicker, { TradeLocation } from "@/src/components/KakaoMapPicker";
import SiteHeader from "@/src/components/SiteHeader";
import FormSection from "@/src/components/ui/FormSection";
import Button from "@/src/components/ui/Button";

const fieldClasses =
  "w-full rounded-field border border-transparent bg-surface-alt px-3.5 py-2.5 text-sm text-text outline-none transition-colors placeholder:text-text-faint focus:border-accent focus:bg-surface disabled:text-text-faint";

export default function UploadPage() {
  const router = useRouter();
  const { isLoggedIn, isHydrated } = useAuth();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [price, setPrice] = useState("");
  const [category, setCategory] = useState("");
  const [images, setImages] = useState<File[]>([]);
  const [tradeLocation, setTradeLocation] = useState<TradeLocation | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isGeneratingDescription, setIsGeneratingDescription] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  // Redirect to login if not authenticated (after hydration)
  useEffect(() => {
    if (isHydrated && !isLoggedIn) {
      router.replace("/login");
    }
  }, [isLoggedIn, isHydrated, router]);

  const imagePreviews = useMemo(() => images.map((file) => URL.createObjectURL(file)), [images]);

  const handleImageChange = (event: ChangeEvent<HTMLInputElement>) => {
    const nextFiles = event.target.files ? Array.from(event.target.files) : [];
    setImages(nextFiles);
  };

  const handleGenerateDescription = async () => {
    if (!title.trim() || !category.trim()) {
      alert("제목과 카테고리를 먼저 입력해주세요.");
      return;
    }

    setIsGeneratingDescription(true);
    setErrorMessage("");

    try {
      const generatedDescription = await generateDescription(title, category);
      setDescription(generatedDescription);
    } catch (error) {
      console.error("Generate description error:", error);
      setErrorMessage("설명 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setIsGeneratingDescription(false);
    }
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setIsSubmitting(true);
    setErrorMessage("");
    setSuccessMessage("");

    const parsedPrice = Number(price);
    if (Number.isNaN(parsedPrice) || parsedPrice < 0) {
      setErrorMessage("가격은 0 이상의 숫자로 입력해주세요.");
      setIsSubmitting(false);
      return;
    }

    if (!tradeLocation) {
      setErrorMessage("지도에서 거래 희망 장소를 선택해주세요.");
      setIsSubmitting(false);
      return;
    }

    try {
      await createProduct(
        {
          title,
          description,
          price: parsedPrice,
          category,
          tradeLocationName: tradeLocation.locationName,
          tradeLatitude: tradeLocation.latitude,
          tradeLongitude: tradeLocation.longitude,
        },
        images,
      );

      setSuccessMessage("상품이 등록되었습니다.");
      setTitle("");
      setDescription("");
      setPrice("");
      setCategory("");
      setImages([]);
      setTradeLocation(null);
      router.push("/");
    } catch (error) {
      if (error instanceof AxiosError) {
        if (error.response?.status === 401) {
          setErrorMessage("인증이 만료되었습니다. 다시 로그인해주세요.");
          router.replace("/login");
        } else if (error.response?.status === 400) {
          // Check for inappropriate content error message
          const errorData = error.response?.data as { message?: string };
          if (errorData?.message?.includes("부적절한 내용")) {
            setErrorMessage(errorData.message);
          } else {
            setErrorMessage("입력값을 확인해주세요.");
          }
        } else {
          setErrorMessage("상품 등록에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
      } else {
        setErrorMessage("상품 등록에 실패했습니다.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  // Show nothing until hydration is complete
  if (!isHydrated) {
    return null;
  }

  if (!isLoggedIn) {
    return null;
  }

  return (
    <div className="min-h-screen">
      <SiteHeader />
      <main className="mx-auto w-full max-w-3xl px-4 pb-28 pt-6 sm:px-6 md:pb-16 md:pt-10">
        <div className="mb-6">
          <Link
            href="/"
            className="inline-flex items-center gap-1 text-sm font-semibold text-text-muted transition-colors hover:text-text"
          >
            <ChevronLeft className="h-4 w-4" />
            목록으로
          </Link>
          <h1 className="mt-3 text-2xl font-extrabold text-text sm:text-[1.75rem]">
            상품 등록
          </h1>
          <p className="mt-1.5 text-sm text-text-muted">
            사진과 정보를 채워서 우리 학교 친구들에게 알려주세요.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="rounded-card border border-border bg-surface p-6 shadow-soft md:p-8">
          <div className="space-y-8">
            <FormSection title="기본 정보" first>
              <div>
                <label htmlFor="title" className="mb-2 block text-sm font-semibold text-text-muted">
                  제목
                </label>
                <input
                  id="title"
                  value={title}
                  onChange={(event) => setTitle(event.target.value)}
                  required
                  disabled={isSubmitting}
                  placeholder="예) 파이썬 전공서적 팝니다"
                  className={fieldClasses}
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
                    min={0}
                    value={price}
                    onChange={(event) => setPrice(event.target.value)}
                    required
                    disabled={isSubmitting}
                    placeholder="0"
                    className={fieldClasses}
                  />
                </div>

                <div>
                  <label htmlFor="category" className="mb-2 block text-sm font-semibold text-text-muted">
                    카테고리
                  </label>
                  <select
                    id="category"
                    value={category}
                    onChange={(event) => setCategory(event.target.value)}
                    required
                    disabled={isSubmitting}
                    className={fieldClasses}
                  >
                    <option value="">카테고리를 선택하세요</option>
                    <option value="학업 관련">학업 관련</option>
                    <option value="디지털/가전">디지털/가전</option>
                    <option value="생활/자취">생활/자취</option>
                    <option value="기타">기타</option>
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
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                required
                disabled={isSubmitting || isGeneratingDescription}
                rows={5}
                placeholder="상품 상태, 구매 시기, 거래 방식 등을 적어주세요"
                className={fieldClasses}
              />
            </FormSection>

            <FormSection title="거래 희망 장소">
              <KakaoMapPicker onChange={setTradeLocation} />
            </FormSection>

            <FormSection title="사진">
              <div>
                <input
                  id="images"
                  type="file"
                  multiple
                  accept="image/*"
                  onChange={handleImageChange}
                  disabled={isSubmitting}
                  className="block w-full rounded-field border border-transparent bg-surface-alt px-3.5 py-2.5 text-sm text-text-muted file:mr-3 file:rounded-field file:border-0 file:bg-accent file:px-3 file:py-1.5 file:text-sm file:font-semibold file:text-white hover:file:bg-accent-strong"
                />
                <p className="mt-1.5 text-xs text-text-faint">여러 장 선택 가능해요</p>
              </div>
              {imagePreviews.length > 0 ? (
                <div className="grid grid-cols-3 gap-3 sm:grid-cols-4">
                  {imagePreviews.map((preview, index) => (
                    <div key={index} className="aspect-square w-full overflow-hidden rounded-field bg-surface-alt">
                      <img
                        src={preview}
                        alt={`Preview ${index + 1}`}
                        className="h-full w-full object-cover"
                      />
                    </div>
                  ))}
                </div>
              ) : null}
            </FormSection>

            {errorMessage ? (
              <p className="rounded-field bg-red-soft px-3 py-2 text-sm text-red-ink">
                {errorMessage}
              </p>
            ) : null}

            {successMessage ? (
              <p className="rounded-field bg-green-soft px-3 py-2 text-sm text-green-ink">
                {successMessage}
              </p>
            ) : null}
          </div>

          {/* 제출 버튼 — 모바일에서는 화면 하단에 고정 */}
          <div className="fixed inset-x-0 bottom-0 z-20 border-t border-border bg-surface/95 p-4 backdrop-blur md:static md:mt-8 md:border-0 md:bg-transparent md:p-0">
            <Button
              type="submit"
              variant="primary"
              size="lg"
              disabled={isSubmitting}
              className="mx-auto w-full max-w-3xl md:mx-0"
            >
              {isSubmitting ? "등록 중..." : "상품 등록하기"}
            </Button>
          </div>
        </form>
      </main>
    </div>
  );
}
