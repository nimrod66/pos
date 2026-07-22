package com.example.pos.catalog;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "supplier_catalogs")
public class Catalog extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String supplier;

    @Column(name = "catalog_version", length = 20)
    private String catalogVersion;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "total_items")
    private int totalItems;

    @Column(length = 20)
    @Builder.Default
    private String status = "IMPORTING";

    @Builder.Default
    @OneToMany(mappedBy = "catalog", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<CatalogItem> items = new ArrayList<>();

    public enum Status {
        IMPORTING, ACTIVE, OUTDATED, ARCHIVED
    }
}
