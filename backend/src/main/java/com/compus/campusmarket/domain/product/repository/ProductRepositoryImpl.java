package com.compus.campusmarket.domain.product.repository;

import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.entity.ProductStatus;
import com.compus.campusmarket.domain.product.entity.QProduct;
import org.springframework.data.domain.Pageable;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.data.domain.Pageable;
import java.util.List;

import static com.compus.campusmarket.domain.product.entity.QProduct.product;

@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    // 파라미터에 Pageable 추가
    @Override
    public List<Product> searchProducts(String keyword, String category, ProductStatus status, Pageable pageable) {
        // seller는 ManyToOne이라 fetch join해도 row가 늘어나지 않아 offset/limit 페이징과 함께
        // 써도 안전하다(=N+1 없이 판매자 이름을 즉시 로드). images는 OneToMany라 여기서
        // fetch join하면 컬렉션과 페이징을 동시에 쓸 때 Hibernate가 메모리에서 페이징을 적용해
        // 페이지 크기만큼만 가져오는 게 깨진다 — 대신 application.yml의
        // default_batch_fetch_size(=100)가 지연 로딩된 images를 IN절로 일괄 조회해준다.
        return queryFactory
                .selectFrom(product)
                .leftJoin(product.seller).fetchJoin()
                .where(
                        containKeyword(keyword),
                        eqCategory(category),
                        eqStatus(status)
                )
                .orderBy(product.createdAt.desc())
                .offset(pageable.getOffset()) // 페이징 시작점
                .limit(pageable.getPageSize()) // 페이징 갯수 (20개)
                .fetch();
    }

    // ✅ 활성 상품 조회 메서드 추가 (판매중 + 예약중)
    @Override
    public List<Product> findActiveProducts() {
        return queryFactory
                .selectFrom(product)
                .distinct()
                .leftJoin(product.seller).fetchJoin()
                .leftJoin(product.images).fetchJoin()
                .where(product.status.in(ProductStatus.SELLING, ProductStatus.RESERVED)) // ✅ 활성 상태만
                .orderBy(product.createdAt.desc())
                .fetch();
    }

    // 제목 또는 내용에 키워드 포함 조건
    private BooleanExpression containKeyword(String keyword) {
        return StringUtils.hasText(keyword) ?
                product.title.contains(keyword).or(product.description.contains(keyword)) : null;
    }

    private BooleanExpression eqStatus(ProductStatus status) {
        // status 파라미터가 없으면(전체 상태) 판매중/예약중만 노출하고, SOLD_OUT은 명시적으로
        // status=SOLD_OUT을 요청했을 때만 보여준다. 홈 화면이 이 메서드를 기본 목록 조회로
        // 쓰기 때문에 findActiveProducts()와 동일한 기본 필터를 적용해야 한다.
        return status != null
                ? product.status.eq(status)
                : product.status.in(ProductStatus.SELLING, ProductStatus.RESERVED);
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