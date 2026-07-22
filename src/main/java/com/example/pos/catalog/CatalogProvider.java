package com.example.pos.catalog;

import java.util.List;

public interface CatalogProvider {
    String getName();
    String getSupplier();
    List<CatalogItem> fetchItems(String sourceUrl);
    boolean isAvailable();
}
