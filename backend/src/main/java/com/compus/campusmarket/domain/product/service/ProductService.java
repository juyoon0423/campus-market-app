package com.compus.campusmarket.domain.product.service;

import com.compus.campusmarket.domain.product.dto.ProductCreateRequest;
import com.compus.campusmarket.domain.product.dto.ProductDetailResponse;
import com.compus.campusmarket.domain.product.dto.ProductListResponse;
import com.compus.campusmarket.domain.product.dto.ProductUpdateRequest;
import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.entity.ProductImage;
import com.compus.campusmarket.domain.product.entity.ProductStatus;
import com.compus.campusmarket.domain.product.repository.ProductRepository;
import com.compus.campusmarket.domain.user.entity.User;
import com.compus.campusmarket.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long createProduct(Long sellerId, ProductCreateRequest request, List<String> imageUrls) {
        // 1. 판매자 정보 조회
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 2. 상품 생성
        Product product = Product.create(
                request.getTitle(),
                request.getDescription(),
                request.getPrice(),
                seller,
                request.getCategory()
        );

        // 3. 이미지 정보 추가
        if (imageUrls != null) {
            imageUrls.stream()
                    .map(url -> ProductImage.create(url, product))
                    .forEach(product.getImages()::add);
        }

        return productRepository.save(product).getId();
    }

    @Transactional(readOnly = true)
    public List<ProductListResponse> getAllProducts() {
        return productRepository.findAllWithSellerAndImages().stream()
                .map(ProductListResponse::new)
                .collect(Collectors.toList());
    }
    public ProductDetailResponse getProductDetail(Long productId) {
        Product product = productRepository.findByIdWithSeller(productId)  // 수정!
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 존재하지 않습니다."));
        return new ProductDetailResponse(product);
    }

    @Transactional
    public void updateProduct(Long productId, Long userId, ProductUpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 존재하지 않습니다."));

        // 본인 확인
        product.validateSeller(userId);

        // 수정 반영
        product.update(request.getTitle(), request.getDescription(), request.getPrice(), request.getCategory());
    }

    @Transactional
    public void deleteProduct(Long productId, Long userId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 존재하지 않습니다."));

        // 본인 확인
        product.validateSeller(userId);

        productRepository.delete(product);
    }

    public List<ProductListResponse> search(String keyword, String category, ProductStatus status) {
        return productRepository.searchProducts(keyword, category, status).stream()
                .map(ProductListResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateStatus(Long productId, Long userId, ProductStatus status) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 존재하지 않습니다."));
        product.changeStatus(status, userId);
    }

    @Transactional(readOnly = true)
    public List<ProductListResponse> getMyProducts(Long userId) {
        return productRepository.findMyProducts(userId).stream()
                .map(ProductListResponse::new)
                .collect(Collectors.toList());
    }

    // ProductService.java 내부에 추가

    @Transactional
    public void completeTrade(Long productId, Long sellerId, Long buyerId) {
        // 1. 상품 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 존재하지 않습니다."));

        // 2. 판매자 권한 확인 (본인의 상품인지)
        product.validateSeller(sellerId);

        // 3. 구매자 정보 조회
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("구매자 정보가 올바르지 않습니다."));

        // 4. 거래 완료 처리 (상태 변경 및 구매자 세팅)
        product.completeTrade(buyer, sellerId);
    }

}