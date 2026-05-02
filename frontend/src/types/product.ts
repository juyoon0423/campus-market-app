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
};

export type ProductDetailResponse = {
  id: number;
  title: string;
  description: string;
  price: number;
  sellerName: string;
  sellerId: number;
  sellerTrustScore: number;
  imageUrls?: string[] | null;
};
