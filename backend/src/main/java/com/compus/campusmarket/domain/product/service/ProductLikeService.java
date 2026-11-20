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

// 좋아요 토글의 실제 DB 작업. ProductService는 클래스 전체가 readOnly 트랜잭션이라, 쓰기 트랜잭션을
// 새로 열기 위해 별도 빈(REQUIRES_NEW)으로 둔다(self-invocation으로는 프록시가 걸리지 않기 때문).
@Service
@RequiredArgsConstructor
public class ProductLikeService {

    private final ProductRepository productRepository;
    private final ProductLikeRepository productLikeRepository;
    private final UserRepository userRepository;

    // 상품 행의 배타 락(X)을 가장 먼저 잡는다(SELECT ... FOR UPDATE).
    // 예전에는 락 없이 읽고 낙관적 락(@Version)으로 검사했는데, product_like INSERT의 FK 검사가 상품 행에
    // 공유 락(S)을 걸기 때문에 동시 좋아요 두 건이 서로 S를 쥔 채 like_count UPDATE용 X를 기다리는
    // 데드락이 구조적으로 생겼다(docs/like-deadlock/). X를 먼저 잡으면 같은 상품의 좋아요가 한 줄로
    // 처리되고, 이후 FK 검사의 S는 이미 쥔 X로 충족돼 기다리지 않는다.
    // 락을 잡은 뒤에 좋아요 존재 여부를 읽으므로(REPEATABLE READ의 읽기 스냅샷은 첫 일반 조회 때 만들어짐)
    // 같은 사용자의 연속 클릭도 앞 요청의 결과를 보고 판단한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String applyToggle(Long productId, Long userId) {
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
