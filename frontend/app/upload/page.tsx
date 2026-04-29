"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ChangeEvent, FormEvent, useMemo, useState, useEffect } from "react";
import { createProduct } from "@/src/lib/apis/productApi";
import { useAuth } from "@/src/context/AuthContext";
import { AxiosError } from "axios";

export default function UploadPage() {
  const router = useRouter();
  const { isLoggedIn, isHydrated } = useAuth();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [price, setPrice] = useState("");
  const [category, setCategory] = useState("");
  const [images, setImages] = useState<File[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  // Redirect to login if not authenticated (after hydration)
  useEffect(() => {
    if (isHydrated && !isLoggedIn) {
      router.replace("/login");
    }
  }, [isLoggedIn, isHydrated, router]);

  const imageNames = useMemo(() => images.map((file) => file.name), [images]);

  const handleImageChange = (event: ChangeEvent<HTMLInputElement>) => {
    const nextFiles = event.target.files ? Array.from(event.target.files) : [];
    setImages(nextFiles);
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

    try {
      await createProduct(
        {
          title,
          description,
          price: parsedPrice,
          category,
        },
        images,
      );

      setSuccessMessage("상품이 등록되었습니다.");
      setTitle("");
      setDescription("");
      setPrice("");
      setCategory("");
      setImages([]);
      router.push("/");
    } catch (error) {
      if (error instanceof AxiosError) {
        if (error.response?.status === 401) {
          setErrorMessage("인증이 만료되었습니다. 다시 로그인해주세요.");
          router.replace("/login");
        } else if (error.response?.status === 400) {
          setErrorMessage("입력값을 확인해주세요.");
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
    <div className="min-h-screen bg-slate-100 px-4 py-10">
      <main className="mx-auto w-full max-w-2xl rounded-2xl bg-white p-6 shadow-sm md:p-8">
        <div className="mb-6 flex items-center justify-between">
          <h1 className="text-2xl font-bold text-slate-900">상품 등록</h1>
          <Link
            href="/"
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-700 hover:bg-slate-50"
          >
            목록으로
          </Link>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="title" className="mb-1 block text-sm text-slate-700">
              제목
            </label>
            <input
              id="title"
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              required
              disabled={isSubmitting}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-slate-500 disabled:bg-slate-50"
            />
          </div>

          <div>
            <label
              htmlFor="description"
              className="mb-1 block text-sm text-slate-700"
            >
              설명
            </label>
            <textarea
              id="description"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              required
              disabled={isSubmitting}
              rows={5}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-slate-500 disabled:bg-slate-50"
            />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label htmlFor="price" className="mb-1 block text-sm text-slate-700">
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
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-slate-500 disabled:bg-slate-50"
              />
            </div>

            <div>
              <label
                htmlFor="category"
                className="mb-1 block text-sm text-slate-700"
              >
                카테고리
              </label>
              <input
                id="category"
                value={category}
                onChange={(event) => setCategory(event.target.value)}
                required
                disabled={isSubmitting}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-slate-500 disabled:bg-slate-50"
              />
            </div>
          </div>

          <div>
            <label htmlFor="images" className="mb-1 block text-sm text-slate-700">
              이미지 파일 (여러 장 선택 가능)
            </label>
            <input
              id="images"
              type="file"
              multiple
              accept="image/*"
              onChange={handleImageChange}
              disabled={isSubmitting}
              className="block w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-700 file:mr-3 file:rounded-md file:border-0 file:bg-slate-900 file:px-3 file:py-1.5 file:text-sm file:font-medium file:text-white hover:file:bg-slate-700 disabled:bg-slate-50"
            />
            {imageNames.length > 0 ? (
              <ul className="mt-2 space-y-1 text-xs text-slate-500">
                {imageNames.map((name) => (
                  <li key={name}>{name}</li>
                ))}
              </ul>
            ) : null}
          </div>

          {errorMessage ? (
            <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">
              {errorMessage}
            </p>
          ) : null}

          {successMessage ? (
            <p className="rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
              {successMessage}
            </p>
          ) : null}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-lg bg-slate-900 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:bg-slate-400"
          >
            {isSubmitting ? "등록 중..." : "상품 등록하기"}
          </button>
        </form>
      </main>
    </div>
  );
}
            {imageNames.length > 0 ? (
              <ul className="mt-2 space-y-1 text-xs text-slate-500">
                {imageNames.map((name) => (
                  <li key={name}>{name}</li>
                ))}
              </ul>
            ) : null}
          </div>

          {errorMessage ? (
            <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">
              {errorMessage}
            </p>
          ) : null}

          {successMessage ? (
            <p className="rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
              {successMessage}
            </p>
          ) : null}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-lg bg-slate-900 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:bg-slate-400"
          >
            {isSubmitting ? "등록 중..." : "상품 등록하기"}
          </button>
        </form>
      </main>
    </div>
  );
}
