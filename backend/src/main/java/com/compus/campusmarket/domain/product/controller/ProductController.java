package com.compus.campusmarket.domain.product.controller;

import com.compus.campusmarket.domain.product.dto.ProductCreateRequest;
import com.compus.campusmarket.domain.product.dto.ProductDetailResponse;
import com.compus.campusmarket.domain.product.dto.ProductListResponse;
import com.compus.campusmarket.domain.product.dto.ProductUpdateRequest;
import com.compus.campusmarket.domain.product.entity.ProductStatus;
import com.compus.campusmarket.domain.product.service.ProductService;
import com.compus.campusmarket.global.util.FileUploadUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    public ResponseEntity<String> createProduct(
            @RequestPart("data") ProductCreateRequest requestDto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            HttpServletRequest request) throws IOException {

        HttpSession session = request.getSession(false);
        Long sellerId = (Long) session.getAttribute("loginUser");

        List<String> imageUrls = new ArrayList<>();
        if (images != null) {
            for (MultipartFile file : images) {
                // 실제 파일 저장 후 생성된 파일명 반환받음
                String savedFileName = fileUploadUtil.saveFile(file);
                imageUrls.add(savedFileName);
            }
        }

        productService.createProduct(sellerId, requestDto, imageUrls);
        return ResponseEntity.ok("상품 등록 및 이미지 업로드 완료");
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

    @GetMapping("/search")
    public ResponseEntity<List<ProductListResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ProductStatus status) {
        return ResponseEntity.ok(productService.search(keyword, category, status));
    }

    @PatchMapping("/{productId}/status")
    public ResponseEntity<String> updateStatus(
            @PathVariable Long productId,
            @RequestParam ProductStatus status,
            HttpServletRequest request) {

        HttpSession session = request.getSession(false);
        Long userId = (Long) session.getAttribute("loginUser");

        productService.updateStatus(productId, userId, status);
        return ResponseEntity.ok("상품 상태가 " + status.getDescription() + "(으)로 변경되었습니다.");
    }


    @GetMapping("/me")
    public ResponseEntity<List<ProductListResponse>> getMyProducts(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        // 인터셉터에서 이미 체크하지만, 안전하게 다시 확인
        if (session == null || session.getAttribute("loginUser") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = (Long) session.getAttribute("loginUser");
        List<ProductListResponse> myProducts = productService.getMyProducts(userId);

        return ResponseEntity.ok(myProducts);
    }
}