package com.example.springlab.item;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
public class Item {

    // 【方針決定】id は Spring Boot / JPA の都合を優先し Long の IDENTITY を維持する。
    // （元の NestJS/Prisma は UUID だが、DB の id 型移行を避けるため UUID 化しない）
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 入力上限は 40 文字（元 DTO の @MaxLength(40) 準拠）。DB 列長は Prisma に合わせ 255。
    @NotBlank
    @Size(max = 40)
    @Column(length = 255, nullable = false)
    private String name;

    // 元は @Min(1)。0 以下の価格は不可。
    @NotNull
    @Min(1)
    private Integer price;

    // 元は任意項目（NULL 可 / 最大 1000 文字）。DB 型は TEXT。
    @Size(max = 1000)
    @Column(columnDefinition = "text")
    private String description;

    // 元 enum ItemStatus。既定は ON_SALE。VARCHAR に文字列で保存する。
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Item() {
    }

    public Item(String name, Integer price, String description) {
        this.name = name;
        this.price = price;
        this.description = description;
        this.status = ItemStatus.ON_SALE;
    }

    /**
     * ステータスを SOLD_OUT に変更する（元 NestJS の updateStatus 相当）。
     */
    public void markSoldOut() {
        this.status = ItemStatus.SOLD_OUT;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
