import api from "@/src/lib/api";
import type {
  ProductCreateRequest,
  ProductDetailResponse,
  ProductListResponse,
  ProductStatus,
  ProductUpdateRequest,
} from "@/src/types/product";

type ProductSearchParams = {
  keyword?: string;
  category?: string;
  status?: ProductStatus;
};

export async function createProduct(
  request: ProductCreateRequest,
  images?: File[],
): Promise<string> {
  const formData = new FormData();
  const jsonBlob = new Blob([JSON.stringify(request)], {
    type: "application/json",
  });

  formData.append("data", jsonBlob);

  images?.forEach((image) => {
    formData.append("images", image);
  });

  const response = await api.post<string>("/api/products", formData);
  return response.data;
}

export async function updateProduct(
  productId: number,
  request: ProductUpdateRequest,
  newImages?: File[],
): Promise<string> {
  const formData = new FormData();
  const jsonBlob = new Blob([JSON.stringify(request)], {
    type: "application/json",
  });

  formData.append("data", jsonBlob);

  newImages?.forEach((image) => {
    formData.append("newImages", image);
  });

  const response = await api.patch<string>(`/api/products/${productId}`, formData);
  return response.data;
}

export async function deleteProduct(productId: number): Promise<string> {
  const response = await api.delete<string>(`/api/products/${productId}`);
  return response.data;
}

export async function getMyProducts(): Promise<ProductListResponse[]> {
  const response = await api.get<ProductListResponse[]>("/api/products/me");
  return response.data;
}

export async function getAllProducts(): Promise<ProductListResponse[]> {
  const response = await api.get<ProductListResponse[]>("/api/products");
  return response.data;
}

export async function getProduct(productId: number): Promise<ProductDetailResponse> {
  const response = await api.get<ProductDetailResponse>(`/api/products/${productId}`);
  return response.data;
}

export async function searchProducts(
  params: ProductSearchParams,
): Promise<ProductListResponse[]> {
  const response = await api.get<ProductListResponse[]>("/api/products/search", {
    params,
  });
  return response.data;
}

export async function toggleLike(productId: number): Promise<void> {
  await api.post(`/api/products/${productId}/likes`);
}

export async function getLikedProducts(): Promise<ProductListResponse[]> {
  const response = await api.get<ProductListResponse[]>("/api/products/me/likes");
  return response.data;
}

export async function generateDescription(title: string, category: string): Promise<string> {
  const response = await api.get<{ description: string }>("/api/ai/generate-description", {
    params: { title, category },
  });
  return response.data.description;
}
