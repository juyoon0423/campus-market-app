package com.compus.campusmarket.domain.product.service;

import com.compus.campusmarket.domain.product.dto.ProductListResponse;
import com.compus.campusmarket.domain.product.entity.ProductStatus;
import com.compus.campusmarket.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductCacheService {

    private final ProductRepository productRepository; // 레포지토리 주입

    // ✅ 외부 클래스로 분리되었기 때문에 이제 @Cacheable이 완벽하게 작동합니다!
    // 캐시 이름에 버전을 붙인다. ProductListResponse에 필드를 추가하면 버전을 올려서,
    // 배포 직후 예전 형식의 캐시(필드가 null로 읽힘)를 읽지 않게 한다. 예전 키는 TTL(10분) 뒤 사라진다.
    // v2: tradeLocationName, createdAt 추가 (2026-11-17)
    @Cacheable(
            value = "products-v2",
            key = "{#keyword, #category, #status, #pageable.pageNumber, #pageable.pageSize}",
            condition = "#pageable.pageNumber == 0"
    )
    @Transactional(readOnly = true)
    public List<ProductListResponse> getCachedProducts(String keyword, String category, ProductStatus status, Pageable pageable) {
        return productRepository.searchProducts(keyword, category, status, pageable).stream()
                .map(product -> new ProductListResponse(product, false))
                .collect(Collectors.toList());
    }
}