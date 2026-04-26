package com.compus.campusmarket.domain.item.entity;
import com.compus.campusmarket.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "item_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String imagePath;            // 저장된 파일 경로 or URL

    @Column(nullable = false)
    private int displayOrder;            // 사진 순서 (대표 사진 = 0)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    public static ItemImage create(String imagePath, int displayOrder) {
        ItemImage image = new ItemImage();
        image.imagePath = imagePath;
        image.displayOrder = displayOrder;
        return image;
    }

    // Item.addImage()에서만 호출 (패키지 내부용)
    void assignItem(Item item) {
        this.item = item;
    }
}