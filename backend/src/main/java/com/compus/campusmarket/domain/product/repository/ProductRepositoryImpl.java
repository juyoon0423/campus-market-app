package com.compus.campusmarket.domain.product.repository;

import com.compus.campusmarket.domain.product.entity.Product;
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
    public List<Product> searchProducts(String keyword, String category) {
        return queryFactory
                .selectFrom(product)
                .leftJoin(product.seller).fetchJoin() // N+1 방지
                .leftJoin(product.images).fetchJoin() // N+1 방지
                .where(
                        containKeyword(keyword),
                        eqCategory(category)
                )
                .orderBy(product.createdAt.desc())
                .fetch();
    }

    // 제목 또는 내용에 키워드 포함 조건
    private BooleanExpression containKeyword(String keyword) {
        return StringUtils.hasText(keyword) ?
                product.title.contains(keyword).or(product.description.contains(keyword)) : null;
    }

    // 카테고리 일치 조건 (엔티티에 category 필드가 있다는 가정 하에)
    private BooleanExpression eqCategory(String category) {
        return StringUtils.hasText(category) ? product.category.eq(category) : null;
    }
}