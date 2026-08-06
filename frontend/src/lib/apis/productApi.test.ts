import { beforeEach, describe, expect, it, vi } from "vitest";

const mockApi = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  patch: vi.fn(),
  delete: vi.fn(),
}));

vi.mock("@/src/lib/api", () => ({
  default: mockApi,
}));

import { deleteProduct, searchProducts, updateProductStatus } from "@/src/lib/apis/productApi";

// 상품 상세 페이지가 axios 클라이언트 대신 fetch("http://localhost:8080/...")를 직접 호출해
// 배포 환경에서 항상 실패하던 버그(리팩토링 완료)의 회귀를 막기 위한 테스트 — 이 API 함수들이
// 항상 공용 axios 인스턴스를 거쳐서 정확한 엔드포인트/바디로 호출되는지 검증한다.
describe("productApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("updateProductStatus는 PATCH /api/products/{id}/status로 status와 buyerId를 보낸다", async () => {
    mockApi.patch.mockResolvedValue({ data: undefined });

    await updateProductStatus(1, "SOLD_OUT", 99);

    expect(mockApi.patch).toHaveBeenCalledWith("/api/products/1/status", {
      status: "SOLD_OUT",
      buyerId: 99,
    });
  });

  it("updateProductStatus는 buyerId가 없으면 null로 보낸다", async () => {
    mockApi.patch.mockResolvedValue({ data: undefined });

    await updateProductStatus(1, "SELLING");

    expect(mockApi.patch).toHaveBeenCalledWith("/api/products/1/status", {
      status: "SELLING",
      buyerId: null,
    });
  });

  it("deleteProduct는 DELETE /api/products/{id}를 호출한다", async () => {
    mockApi.delete.mockResolvedValue({ data: "삭제 완료" });

    await deleteProduct(5);

    expect(mockApi.delete).toHaveBeenCalledWith("/api/products/5");
  });

  it("searchProducts는 GET /api/products/search에 파라미터를 그대로 전달한다", async () => {
    mockApi.get.mockResolvedValue({ data: [] });

    await searchProducts({ keyword: "노트북", category: "디지털/가전" });

    expect(mockApi.get).toHaveBeenCalledWith("/api/products/search", {
      params: { keyword: "노트북", category: "디지털/가전" },
    });
  });
});
