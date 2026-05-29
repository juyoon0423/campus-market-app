package com.compus.campusmarket.domain.product.controller;

import com.compus.campusmarket.domain.product.dto.*;
import com.compus.campusmarket.domain.product.entity.ProductStatus;
import com.compus.campusmarket.domain.product.service.ProductService;
import com.compus.campusmarket.global.config.auth.CustomUserDetails;
import com.compus.campusmarket.global.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final FileUploadUtil fileUploadUtil;

    // 상품 등록
    @PostMapping
    public ResponseEntity<String> createProduct(
            @RequestPart("data") ProductCreateRequest requestDto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws IOException {

        // CustomUserDetails에서 안전하게 ID 추출
        Long sellerId = userDetails.getUserId();

        List<String> imageUrls = new ArrayList<>();
        if (images != null) {
            for (MultipartFile file : images) {
                String savedFileName = fileUploadUtil.saveFile(file);
                imageUrls.add(savedFileName);
            }
        }

        productService.createProduct(sellerId, requestDto, imageUrls);
        return ResponseEntity.ok("상품 등록 완료");
    }

    @PatchMapping("/{productId}")
    public ResponseEntity<String> updateProduct(
            @PathVariable Long productId,
            @RequestPart("data") ProductUpdateRequest updateRequest,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws IOException {

        // 1. 새 이미지 파일 저장 (있는 경우)
        List<String> newImageUrls = new ArrayList<>();
        if (newImages != null && !newImages.isEmpty()) {
            for (MultipartFile file : newImages) {
                String savedFileName = fileUploadUtil.saveFile(file);
                newImageUrls.add(savedFileName);
            }
        }

        // 2. 서비스 단으로 수정 요청 넘기기
        productService.updateProduct(productId, userDetails.getUserId(), updateRequest, newImageUrls);
        return ResponseEntity.ok("상품 수정 완료");
    }

    // 상품 삭제
    @DeleteMapping("/{productId}")
    public ResponseEntity<String> deleteProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        productService.deleteProduct(productId, userDetails.getUserId());
        return ResponseEntity.ok("상품 삭제 완료");
    }

    // 내 상품 조회 (본인 상품이므로 좋아요는 무조건 false지만, 생성자 구색을 맞추기 위해)
    @GetMapping("/me")
    public ResponseEntity<List<ProductListResponse>> getMyProducts(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(productService.getMyProducts(userDetails.getUserId()));
    }

    // 내가 찜한 상품 조회 (관심 목록)
    @GetMapping("/me/likes")
    public ResponseEntity<List<ProductListResponse>> getLikedProducts(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(productService.getLikedProducts(userDetails.getUserId()));
    }

    // 전체 조회
    @GetMapping
    public ResponseEntity<List<ProductListResponse>> getAllProducts(
            @AuthenticationPrincipal CustomUserDetails userDetails) { // ✅ 추가
        Long viewerId = (userDetails != null) ? userDetails.getUserId() : null;
        return ResponseEntity.ok(productService.getAllProducts(viewerId));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponse> getProduct(
            @PathVariable("productId") Long productId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long viewerId = (userDetails != null) ? userDetails.getUserId() : null;
        return ResponseEntity.ok(productService.getProductDetail(productId, viewerId));
    }

    // 검색
    @GetMapping("/search")
    public ResponseEntity<List<ProductListResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ProductStatus status,
            @AuthenticationPrincipal CustomUserDetails userDetails) { // ✅ 추가
        Long viewerId = (userDetails != null) ? userDetails.getUserId() : null;
        return ResponseEntity.ok(productService.search(keyword, category, status, viewerId));
    }

    @PatchMapping("/{productId}/status")
    public ResponseEntity<String> updateStatus(
            @PathVariable Long productId,
            @RequestBody StatusUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (request.getStatus() == ProductStatus.SOLD_OUT) {
            // 판매 완료 시에는 구매자 ID가 필요함
            productService.completeTrade(productId, userDetails.getUserId(), request.getBuyerId());
        } else {
            productService.updateStatus(productId, userDetails.getUserId(), request.getStatus());
        }
        return ResponseEntity.ok("상태 변경 완료");
    }

    @PostMapping("/{productId}/likes")
    public ResponseEntity<String> toggleLike(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // SecurityConfig의 anyRequest().authenticated()에 의해 비로그인 유저는 이 API 접근 불가
        String result = productService.toggleLike(productId, userDetails.getUserId());
        return ResponseEntity.ok(result);
    }
}