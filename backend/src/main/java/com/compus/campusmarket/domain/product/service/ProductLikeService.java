package com.compus.campusmarket.domain.product.service;

import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.entity.ProductLike;
import com.compus.campusmarket.domain.product.repository.ProductLikeRepository;
import com.compus.campusmarket.domain.product.repository.ProductRepository;
import com.compus.campusmarket.domain.user.entity.User;
import com.compus.campusmarket.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// toggleLike의 재시도 루프가 시도마다 완전히 새 트랜잭션/영속성 컨텍스트를 쓰도록
// 별도 빈으로 분리(ProductCacheService와 동일하게 self-invocation으로는 프록시가 걸리지 않기 때문).
@Service
@RequiredArgsConstructor
public class ProductLikeService {

    private final ProductRepository productRepository;
    private final ProductLikeRepository productLikeRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String applyToggle(Long productId, Long userId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 존재하지 않습니다."));

        String result = toggle(product, userId);

        // Product.version 충돌이나 ProductLike unique 제약 위반을 이 트랜잭션 안에서 확정시켜
        // 호출부의 재시도 루프가 잡을 수 있게 함(그렇지 않으면 커밋 시점에야 터져 메서드 밖에서 발생).
        productLikeRepository.flush();
        productRepository.flush();

        return result;
    }

    // 좋아요는 낙관적 락(@Version) + 재시도로 최종 확정했지만(applyToggle),
    // "왜 비관적 락 대신 이걸 골랐는지"를 벤치마크로 뒷받침하기 위한 비교 대상 구현.
    // 프로덕션 API 경로에서는 호출하지 않고 ProductConcurrencyTest의 벤치마크에서만 사용한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String applyTogglePessimistic(Long productId, Long userId) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 존재하지 않습니다."));
        return toggle(product, userId);
    }

    private String toggle(Product product, Long userId) {
        if (product.getSeller().getId().equals(userId)) {
            throw new IllegalStateException("자신의 상품에는 좋아요를 누를 수 없습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Optional<ProductLike> existingLike = productLikeRepository.findByUserAndProduct(user, product);

        if (existingLike.isPresent()) {
            productLikeRepository.delete(existingLike.get());
            product.decreaseLikeCount();
            return "좋아요가 취소되었습니다.";
        } else {
            productLikeRepository.save(new ProductLike(user, product));
            product.increaseLikeCount();
            return "좋아요가 추가되었습니다.";
        }
    }
}
