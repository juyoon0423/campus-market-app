package com.compus.campusmarket.domain.product.service;

import com.compus.campusmarket.domain.product.dto.ProductCreateRequest;
import com.compus.campusmarket.domain.product.dto.ProductDetailResponse;
import com.compus.campusmarket.domain.product.dto.ProductListResponse;
import com.compus.campusmarket.domain.product.dto.ProductUpdateRequest;
import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.entity.ProductImage;
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
                seller
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
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long productId) {
        Product product = productRepository.findById(productId)
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
        product.update(request.getTitle(), request.getDescription(), request.getPrice());
    }

    @Transactional
    public void deleteProduct(Long productId, Long userId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 존재하지 않습니다."));

        // 본인 확인
        product.validateSeller(userId);

        productRepository.delete(product);
    }
}