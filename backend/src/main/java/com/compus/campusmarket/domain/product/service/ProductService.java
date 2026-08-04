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
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService {

    private static final int LIKE_TOGGLE_MAX_ATTEMPTS = 6;

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductLikeRepository productLikeRepository;
    private final ReviewRepository reviewRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AiService aiService; // ✅ 추가 (AI 서비스 주입)
    private final ProductCacheService productCacheService;
    private final ProductLikeService productLikeService;

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

    // 좋아요는 호출 빈도가 높고 경합 비용이 낮아, 락을 오래 잡는 대신 낙관적 락(@Version) + 짧은 재시도를 택한다.
    // 실제 DB 갱신은 ProductLikeService.applyToggle(REQUIRES_NEW)에서 시도마다 새 트랜잭션으로 수행된다.
    //
    // 처음엔 ObjectOptimisticLockingFailureException만 재시도 대상으로 잡았는데, 인기 상품처럼 같은 row에
    // 요청이 몰리면 버전 충돌이 나기도 전에 UPDATE 문 자체가 InnoDB 데드락(에러 1213)으로 실패하는 사례가
    // 실측에서 확인됐다. ConcurrencyFailureException(낙관적 락 실패와 데드락의 공통 상위 타입)으로 넓혀서 재시도한다.
    public String toggleLike(Long productId, Long userId) {
        for (int attempt = 1; attempt <= LIKE_TOGGLE_MAX_ATTEMPTS; attempt++) {
            try {
                return productLikeService.applyToggle(productId, userId);
            } catch (ConcurrencyFailureException | DataIntegrityViolationException e) {
                log.warn("좋아요 처리 충돌 발생(시도 {}/{}), productId={}, userId={}, 원인={}",
                        attempt, LIKE_TOGGLE_MAX_ATTEMPTS, productId, userId, e.getClass().getSimpleName());
                if (attempt == LIKE_TOGGLE_MAX_ATTEMPTS) {
                    throw new IllegalStateException("동시 요청이 많아 좋아요 처리에 실패했습니다. 잠시 후 다시 시도해주세요.");
                }
                sleepBeforeRetry(attempt);
            }
        }
        throw new IllegalStateException("좋아요 처리에 실패했습니다.");
    }

    // 재시도가 한꺼번에 몰려 다시 충돌하는 걸 줄이기 위한 짧은 지터 백오프
    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep((long) (Math.random() * 20 * attempt));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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