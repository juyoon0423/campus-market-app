// domain/category/entity/Category.java
package com.compus.campusmarket.domain.category.entity;

import com.campusmarket.global.common.BaseTimeEntity;
import com.compus.campusmarket.domain.item.entity.Item;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    @OneToMany(mappedBy = "category")
    private List<Item> items = new ArrayList<>();

    public static Category create(String name) {
        Category category = new Category();
        category.name = name;
        return category;
    }
}