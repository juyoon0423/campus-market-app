package com.compus.campusmarket.domain.product.service;

import com.compus.campusmarket.domain.ai.service.AiService;
import com.compus.campusmarket.domain.product.dto.ProductCreateRequest;
import com.compus.campusmarket.domain.product.dto.ProductDetailResponse;
import com.compus.campusmarket.domain.product.dto.ProductListResponse;
import com.compus.campusmarket.domain.product.dto.ProductUpdateRequest;
import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.entity.ProductImage;
import com.compus.campusmarket.domain.product.entity.ProductLike;
import com.compus.campusmarket.domain.product.entity.ProductStatus;
import com.compus.campusmarket.domain.product.repository.ProductLikeRepository;
import com.compus.campusmarket.domain.product.repository.ProductRepository;
import com.compus.campusmarket.domain.user.entity.User;
import com.compus.campusmarket.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductLikeRepository productLikeRepository;
    private final AiService aiService; // ✅ 추가 (AI 서비스 주입)
    private final ProductCacheService productCacheService;

    @Transactional
    public Long createProduct(Long sellerId, ProductCreateRequest request, List<String> imageUrls) {

        // 🚨 1. AI 부적절 매물 필터링 (저장 전에 검사)
        String contentToAnalyze = request.getTitle() + " " + request.getDescription();
        boolean isAppropriate = aiService.isAppropriateProduct(contentToAnalyze);

        if (!isAppropriate) {
            throw new IllegalArgumentException("부적절한 내용(욕설, 불법 등)이 포함되어 상품을 등록할 수 없습니다.");
        }

        // 2. 판매자 정보 조회
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 3. 상품 생성
        Product product = Product.create(
                request.getTitle(),
                request.getDescription(),
                request.getPrice(),
                seller,
                request.getCategory()
        );

        // 4. 이미지 정보 추가
        if (imageUrls != null) {
            imageUrls.stream()
                    .map(url -> ProductImage.create(url, product))
                    .forEach(product.getImages()::add);
        }

        return productRepository.save(product).getId();
    }

    // 전체 상품 조회
    @Transactional(readOnly = true)
    public List<ProductListResponse> getAllProducts(Long viewerId) {
        return productRepository.findActiveProducts().stream()
                .map(product -> {
                    boolean isLiked = (viewerId != null) &&
                            productLikeRepository.existsByUser_IdAndProduct_Id(viewerId, product.getId());
                    return new ProductListResponse(product, isLiked); // ✅ isLiked 전달
                })
                .collect(Collectors.toList());
    }

    @Transactional // 조회수가 '업데이트' 되어야 하므로 readOnly=true를 덮어씁니다.
    public ProductDetailResponse getProductDetail(Long productId, Long viewerId) {
        Product product = productRepository.findByIdWithSeller(productId)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 존재하지 않습니다."));

        // 조회수 증가 로직 (비로그인 사용자거나, 본인이 아닐 때만 증가)
        if (viewerId == null || !product.getSeller().getId().equals(viewerId)) {
            product.increaseViewCount();
        }

        // 현재 사용자의 좋아요 여부 확인
        boolean isLiked = false;
        if (viewerId != null) {
            isLiked = productLikeRepository.existsByUser_IdAndProduct_Id(viewerId, productId);
        }

        return new ProductDetailResponse(product, isLiked);
    }

    @Transactional
    public void updateProduct(Long productId, Long userId, ProductUpdateRequest request, List<String> newImageUrls) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 존재하지 않습니다."));

        // 1. 본인 확인
        product.validateSeller(userId);

        // 2. 텍스트 정보 업데이트
        product.update(request.getTitle(), request.getDescription(), request.getPrice(), request.getCategory());

        // 3. 기존 이미지 정리 로직
        // 프론트에서 넘어온 '남길 이미지(remainingImageUrls)'에 포함되지 않은 이미지는 삭제
        if (request.getRemainingImageUrls() != null) {
            product.getImages().removeIf(productImage ->
                    !request.getRemainingImageUrls().contains(productImage.getImageUrl())
            );
        } else {
            // remainingImageUrls가 null로 넘어오면 모두 삭제로 간주
            product.getImages().clear();
        }

        // 4. 새로운 이미지 추가 로직
        if (newImageUrls != null && !newImageUrls.isEmpty()) {
            newImageUrls.stream()
                    .map(url -> ProductImage.create(url, product))
                    .forEach(product.getImages()::add);
        }
    }

    @Transactional
    public void deleteProduct(Long productId, Long userId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 존재하지 않습니다."));

        // 본인 확인
        product.validateSeller(userId);

        productRepository.delete(product);
    }

    // 검색
    @Transactional(readOnly = true)
    public List<ProductListResponse> search(String keyword, String category, ProductStatus status, Long viewerId, Pageable pageable) {

        // ✅ 내부 호출(this)이 아니라 외부 호출(Proxy 거침)로 변경됨!
        List<ProductListResponse> rawProducts = productCacheService.getCachedProducts(keyword, category, status, pageable);

        if (viewerId == null) {
            return rawProducts;
        }

        return rawProducts.stream()
                .map(productDto -> {
                    boolean isLiked = productLikeRepository.existsByUser_IdAndProduct_Id(viewerId, productDto.getId());
                    productDto.setLiked(isLiked);
                    return productDto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateStatus(Long productId, Long userId, ProductStatus status) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 존재하지 않습니다."));
        product.changeStatus(status, userId);
    }

    // 내 상품 조회
    @Transactional(readOnly = true)
    public List<ProductListResponse> getMyProducts(Long userId) {
        return productRepository.findMyProducts(userId).stream()
                // 내 상품은 좋아요를 누를 수 없으므로 무조건 false 전달
                .map(product -> new ProductListResponse(product, false))
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

    // 2. 좋아요 토글 로직 추가
    @Transactional
    public String toggleLike(Long productId, Long userId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 존재하지 않습니다."));

        // 요구사항: 본인 상품은 좋아요 불가
        if (product.getSeller().getId().equals(userId)) {
            throw new IllegalStateException("자신의 상품에는 좋아요를 누를 수 없습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 이미 좋아요를 눌렀는지 확인
        Optional<ProductLike> existingLike = productLikeRepository.findByUserAndProduct(user, product);

        if (existingLike.isPresent()) {
            // 이미 있으면 -> 좋아요 취소
            productLikeRepository.delete(existingLike.get());
            product.decreaseLikeCount();
            return "좋아요가 취소되었습니다.";
        } else {
            // 없으면 -> 좋아요 추가
            productLikeRepository.save(new ProductLike(user, product));
            product.increaseLikeCount();
            return "좋아요가 추가되었습니다.";
        }
    }

    @Transactional(readOnly = true)
    public List<ProductListResponse> getLikedProducts(Long userId) {
        return productLikeRepository.findLikedProductsByUserId(userId).stream()
                // 이 리스트에 있는 상품들은 이미 내가 찜한 상품들이므로 isLiked는 무조건 true로 고정
                .map(product -> new ProductListResponse(product, true))
                .collect(Collectors.toList());
    }

}