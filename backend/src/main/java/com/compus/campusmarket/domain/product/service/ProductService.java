package com.compus.campusmarket.domain.product.service;

import com.compus.campusmarket.domain.ai.service.AiService;
import com.compus.campusmarket.domain.product.dto.ProductCreateRequest;
import com.compus.campusmarket.domain.product.dto.ProductDetailResponse;
import com.compus.campusmarket.domain.product.dto.ProductListResponse;
import com.compus.campusmarket.domain.product.dto.ProductUpdateRequest;
import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.entity.ProductImage;
import com.compus.campusmarket.domain.product.entity.ProductStatus;
import com.compus.campusmarket.domain.product.repository.ProductLikeRepository;
import com.compus.campusmarket.domain.product.repository.ProductRepository;
import com.compus.campusmarket.domain.product.review.repository.ReviewRepository;
import com.compus.campusmarket.domain.chat.entity.ChatRoom;
import com.compus.campusmarket.domain.chat.repository.ChatMessageRepository;
import com.compus.campusmarket.domain.chat.repository.ChatRoomRepository;
import com.compus.campusmarket.domain.user.entity.User;
import com.compus.campusmarket.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductLikeRepository productLikeRepository;
    private final ReviewRepository reviewRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AiService aiService; // ✅ 추가 (AI 서비스 주입)
    private final ProductCacheService productCacheService;
    private final ProductLikeService productLikeService;

    // 목록 첫 페이지 캐시는 키워드·카테고리·상태 조합마다 따로 저장된다. 상품 하나가 바뀌면 어느 조합에
    // 들어 있는지 알 수 없으므로 캐시 전체를 비운다(allEntries).
    @CacheEvict(value = ProductCacheService.PRODUCTS_CACHE, allEntries = true)
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

        // 5. 거래 희망 장소 저장
        product.updateTradeLocation(
                request.getTradeLocationName(), request.getTradeLatitude(), request.getTradeLongitude());

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
        // 조회수 증가 로직 (비로그인 사용자거나, 본인이 아닐 때만 증가)
        // version을 올리지 않는 벌크 쿼리로 먼저 증가시킨 뒤, 증가된 값이 반영된 엔티티를 응답용으로 조회한다.
        productRepository.increaseViewCount(productId, viewerId);

        Product product = productRepository.findByIdWithSeller(productId)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 존재하지 않습니다."));

        // 현재 사용자의 좋아요 여부 확인
        boolean isLiked = false;
        if (viewerId != null) {
            isLiked = productLikeRepository.existsByUser_IdAndProduct_Id(viewerId, productId);
        }

        return new ProductDetailResponse(product, isLiked);
    }

    @CacheEvict(value = ProductCacheService.PRODUCTS_CACHE, allEntries = true)
    @Transactional
    public void updateProduct(Long productId, Long userId, ProductUpdateRequest request, List<String> newImageUrls) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 존재하지 않습니다."));

        // 1. 본인 확인
        product.validateSeller(userId);

        // 2. 텍스트 정보 업데이트
        product.update(request.getTitle(), request.getDescription(), request.getPrice(), request.getCategory());
        product.updateTradeLocation(
                request.getTradeLocationName(), request.getTradeLatitude(), request.getTradeLongitude());

        // 3. 기존 이미지 정리 로직
        if (request.getRemainingImageUrls() != null) {
            product.getImages().removeIf(productImage -> {
                String dbFileName = productImage.getImageUrl(); // 예: UUID_test.jpg

                // 프론트에서 보낸 URL 목록 중, DB 파일명을 포함하는 게 하나도 없다면 삭제
                return !request.getRemainingImageUrls().stream()
                        .anyMatch(remainingUrl -> remainingUrl.contains(dbFileName));
            });
        } else {
            product.getImages().clear();
        }

        // 4. 새로운 이미지 추가 로직
        if (newImageUrls != null && !newImageUrls.isEmpty()) {
            newImageUrls.stream()
                    .map(url -> ProductImage.create(url, product))
                    .forEach(product.getImages()::add);
        }
    }

    @CacheEvict(value = ProductCacheService.PRODUCTS_CACHE, allEntries = true)
    @Transactional
    public void deleteProduct(Long productId, Long userId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 존재하지 않습니다."));

        // 본인 확인
        product.validateSeller(userId);

        // FK로 상품을 참조하는 연관 데이터를 먼저 정리하지 않으면 DataIntegrityViolationException으로 삭제가 실패한다.
        List<Long> chatRoomIds = chatRoomRepository.findAllByProduct_Id(productId).stream()
                .map(ChatRoom::getId)
                .collect(Collectors.toList());
        if (!chatRoomIds.isEmpty()) {
            chatMessageRepository.deleteAllByChatRoom_IdIn(chatRoomIds);
        }
        chatRoomRepository.deleteAllByProduct_Id(productId);
        reviewRepository.deleteAllByProduct_Id(productId);
        productLikeRepository.deleteAllByProduct_Id(productId);

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

    // completeTrade와 같은 이유(동시 상태 변경 요청)로 비관적 락을 재사용한다.
    @CacheEvict(value = ProductCacheService.PRODUCTS_CACHE, allEntries = true)
    @Transactional
    public void updateStatus(Long productId, Long userId, ProductStatus status) {
        Product product = productRepository.findByIdForUpdate(productId)
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

    // 거래 완료는 호출 빈도가 낮고, 실패 시 "다시 눌러주세요"라고 요구하기 어려운 사용자 액션이라
    // 낙관적 재시도 대신 비관적 락으로 동시 완료 처리를 원천 차단한다(findByIdForUpdate).
    @CacheEvict(value = ProductCacheService.PRODUCTS_CACHE, allEntries = true)
    @Transactional
    public void completeTrade(Long productId, Long sellerId, Long buyerId) {
        // 1. 상품 조회 (락 획득 — 동시에 들어온 다른 completeTrade 요청은 이 트랜잭션이 끝날 때까지 대기)
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 존재하지 않습니다."));

        // 2. 판매자 권한 확인 (본인의 상품인지)
        product.validateSeller(sellerId);

        // 3. 구매자 정보 조회
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("구매자 정보가 올바르지 않습니다."));

        // 4. 거래 완료 처리 (상태 변경 및 구매자 세팅) — 이미 SOLD_OUT이면 completeTrade 내부에서 예외 발생
        product.completeTrade(buyer, sellerId);
    }

    // 좋아요는 상품 행에 비관적 락을 먼저 잡고 처리한다(ProductLikeService.applyToggle).
    // 예전에는 "경합이 낮다"는 판단으로 낙관적 락(@Version) + 최대 6회 재시도를 썼지만, product_like INSERT의
    // FK 검사가 상품 행에 공유 락을 걸어 동시 좋아요끼리 데드락이 구조적으로 생겼고(상품 1개 동시 20건 × 50라운드
    // 측정, 3회 중앙값: 데드락 1,971회, 재시도 소진 14건), 재시도는 원인이 아니라 증상을 덮고 있었다(docs/like-deadlock/).
    // 락을 먼저 잡으면 데드락·버전 충돌·유니크 위반이 이 경로에서 생기지 않으므로 재시도 루프를 없앴다.
    // 그래도 예상 밖의 락 실패(예: 락 대기 시간 초과)가 나면 GlobalExceptionHandler가 409로 응답한다.
    public String toggleLike(Long productId, Long userId) {
        return productLikeService.applyToggle(productId, userId);
    }

    @Transactional(readOnly = true)
    public List<ProductListResponse> getLikedProducts(Long userId) {
        return productLikeRepository.findLikedProductsByUserId(userId).stream()
                // 이 리스트에 있는 상품들은 이미 내가 찜한 상품들이므로 isLiked는 무조건 true로 고정
                .map(product -> new ProductListResponse(product, true))
                .collect(Collectors.toList());
    }

}