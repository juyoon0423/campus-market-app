export type ProductStatus = "SELLING" | "RESERVED" | "SOLD_OUT";

export type ProductCreateRequest = {
  title: string;
  description: string;
  price: number;
  category: string;
  tradeLocationName: string;
  tradeLatitude: number;
  tradeLongitude: number;
};

export type ProductUpdateRequest = {
  title: string;
  description: string;
  price: number;
  category: string;
  tradeLocationName: string;
  tradeLatitude: number;
  tradeLongitude: number;
  remainingImageUrls?: string[];
};

export type ProductListResponse = {
  id: number;
  title: string;
  price: number;
  sellerName: string;
  representativeImageUrl?: string | null;
  status: ProductStatus;
  viewCount: number;
  likeCount: number;
  isLiked: boolean;
  /** 거래 희망 장소 이름. 기능 도입 전 상품은 null */
  tradeLocationName?: string | null;
  /** 등록 시각(서버 시간대 기준 ISO 문자열, 시간대 표기 없음) */
  createdAt?: string | null;
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
  viewCount: number;
  likeCount: number;
  tradeLocationName?: string | null;
  tradeLatitude?: number | null;
  tradeLongitude?: number | null;
  isLiked: boolean;
};
