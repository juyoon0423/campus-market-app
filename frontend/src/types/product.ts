export type ProductStatus = string;

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
  title: string;
  description: string;
  price: number;
  sellerName: string;
  sellerTrustScore: number;
  imageUrls?: string[] | null;
};
