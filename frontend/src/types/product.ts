export type ProductStatus = "SELLING" | "RESERVED" | "SOLD_OUT";

export type ProductCreateRequest = {
  title: string;
  description: string;
  price: number;
  category: string;
};

export type ProductUpdateRequest = {
  title: string;
  description: string;
  price: number;
  category: string;
};

export type ProductListResponse = {
  id: number;
  title: string;
  price: number;
  sellerName: string;
  representativeImageUrl?: string | null;
  status: ProductStatus;
};

export type ProductDetailResponse = {
  id: number;
  title: string;
  description: string;
  price: number;
  category: string;
  sellerName: string;
  sellerId: number;
  sellerTrustScore: number;
  status: ProductStatus;
  imageUrls?: string[] | null;
};
