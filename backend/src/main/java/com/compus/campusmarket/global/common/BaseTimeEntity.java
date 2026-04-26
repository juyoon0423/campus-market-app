package com.compus.campusmarket.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass                        // DB 테이블로 생성되지 않고, 자식 엔티티에 필드만 상속
@EntityListeners(AuditingEntityListener.class)  // Spring Data JPA Auditing 활성화
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(updatable = false)           // 최초 저장 후 수정 불가
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}