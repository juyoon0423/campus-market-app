package com.compus.campusmarket.domain.item.entity;

public enum ItemStatus {
    SELLING("판매 중"),
    RESERVED("예약 중"),
    SOLD("판매 완료");

    private final String description;

    ItemStatus(String description) {
        this.description = description;
    }
}

