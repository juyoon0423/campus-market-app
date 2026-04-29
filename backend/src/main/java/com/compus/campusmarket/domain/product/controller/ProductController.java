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

    // 상품 수정
    @PatchMapping("/{productId}")
    public ResponseEntity<String> updateProduct(
            @PathVariable Long productId,
            @RequestBody ProductUpdateRequest updateRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        productService.updateProduct(productId, userDetails.getUserId(), updateRequest);
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

    // 내 상품 조회
    @GetMapping("/me")
    public ResponseEntity<List<ProductListResponse>> getMyProducts(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(productService.getMyProducts(userDetails.getUserId()));
    }

    // 전체 조회 (인증 불필요 - SecurityConfig에서 permitAll 설정됨)
    @GetMapping
    public ResponseEntity<List<ProductListResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponse> getProduct(@PathVariable("productId") Long productId) {
        return ResponseEntity.ok(productService.getProductDetail(productId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductListResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ProductStatus status) {
        return ResponseEntity.ok(productService.search(keyword, category, status));
    }
}