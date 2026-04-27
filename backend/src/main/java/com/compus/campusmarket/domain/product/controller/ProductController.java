package com.compus.campusmarket.domain.product.controller;

import com.compus.campusmarket.domain.product.dto.ProductCreateRequest;
import com.compus.campusmarket.domain.product.dto.ProductDetailResponse;
import com.compus.campusmarket.domain.product.dto.ProductListResponse;
import com.compus.campusmarket.domain.product.dto.ProductUpdateRequest;
import com.compus.campusmarket.domain.product.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<String> createProduct(
            @RequestPart("data") ProductCreateRequest requestDto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            HttpServletRequest request) {

        // 1. 세션에서 로그인 유저 ID 추출 (인터셉터에서 검증됨)
        HttpSession session = request.getSession(false);
        Long sellerId = (Long) session.getAttribute("loginUser");

        // 2. 이미지 저장 로직 (현재는 파일명만 추출, 실제 구현은 util/FileUploadUtil 참고)
        List<String> imageUrls = new ArrayList<>();
        if (images != null) {
            for (MultipartFile file : images) {
                // 임시로 파일명만 저장 (나중에 FileUploadUtil로 실제 경로 반환)
                imageUrls.add("uploads/" + file.getOriginalFilename());
            }
        }

        // 3. 상품 등록
        productService.createProduct(sellerId, requestDto, imageUrls);

        return ResponseEntity.ok("상품이 성공적으로 등록되었습니다.");
    }
    @GetMapping
    public ResponseEntity<List<ProductListResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponse> getProductDetail(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProductDetail(productId));
    }

    @PatchMapping("/{productId}")
    public ResponseEntity<String> updateProduct(
            @PathVariable Long productId,
            @RequestBody ProductUpdateRequest updateRequest,
            HttpServletRequest request) {

        HttpSession session = request.getSession(false);
        Long userId = (Long) session.getAttribute("loginUser");

        productService.updateProduct(productId, userId, updateRequest);
        return ResponseEntity.ok("상품 수정이 완료되었습니다.");
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<String> deleteProduct(
            @PathVariable Long productId,
            HttpServletRequest request) {

        HttpSession session = request.getSession(false);
        Long userId = (Long) session.getAttribute("loginUser");

        productService.deleteProduct(productId, userId);
        return ResponseEntity.ok("상품이 성공적으로 삭제되었습니다.");
    }
}