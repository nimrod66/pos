package com.example.pos.catalog;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MedsCatalogProvider implements CatalogProvider {

    @Override
    public String getName() {
        return "Mission for Essential Drugs (MEDS)";
    }

    @Override
    public String getSupplier() {
        return "MEDS";
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
