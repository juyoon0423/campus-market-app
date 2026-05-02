package com.compus.campusmarket.domain.product.repository;

import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.entity.ProductStatus;
import com.compus.campusmarket.domain.product.entity.QProduct;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.compus.campusmarket.domain.product.entity.QProduct.product;

@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Product> searchProducts(String keyword, String category, ProductStatus status) {
        return queryFactory
                .selectFrom(product)
                .distinct() // OneToMany 페치 조인 시 중복 데이터 제거
                .leftJoin(product.seller).fetchJoin()
                .leftJoin(product.images).fetchJoin()
                .where(
                        containKeyword(keyword),
                        eqCategory(category),
                        eqStatus(status)
                )
                .orderBy(product.createdAt.desc())
                .fetch();
    }

    // 제목 또는 내용에 키워드 포함 조건
    private BooleanExpression containKeyword(String keyword) {
        return StringUtils.hasText(keyword) ?
                product.title.contains(keyword).or(product.description.contains(keyword)) : null;
    }

    private BooleanExpression eqStatus(ProductStatus status) {
        // status가 null이거나 유효하지 않은 값이 들어오는 경우를 대비
        return status != null ? product.status.eq(status) : null;
    }

    // 카테고리 일치 조건 (엔티티에 category 필드가 있다는 가정 하에)
    private BooleanExpression eqCategory(String category) {
        return StringUtils.hasText(category) ? product.category.eq(category) : null;
    }

    @Override
    public List<Product> findMyProducts(Long userId) {
        return queryFactory
                .selectFrom(product)
                .leftJoin(product.seller).fetchJoin() // 판매자 정보
                .leftJoin(product.images).fetchJoin() // 이미지 정보
                .where(product.seller.id.eq(userId))  // 내 ID와 일치하는 것만
                .orderBy(product.createdAt.desc())     // 최신순
                .fetch();
    }
}