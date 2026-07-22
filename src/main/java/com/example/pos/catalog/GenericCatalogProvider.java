package com.example.pos.catalog;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GenericCatalogProvider implements CatalogProvider {

    @Override
    public String getName() {
        return "Generic/Wholesaler Catalogue";
    }

    @Override
    public String getSupplier() {
        return "CUSTOM";
    }

    @Override
    public List<CatalogItem> fetchItems(String sourceUrl) {
        return new ArrayList<>();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
