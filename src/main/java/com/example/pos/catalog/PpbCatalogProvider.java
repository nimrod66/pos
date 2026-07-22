package com.example.pos.catalog;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PpbCatalogProvider implements CatalogProvider {

    @Override
    public String getName() {
        return "Pharmacy and Poisons Board (PPB)";
    }

    @Override
    public String getSupplier() {
        return "PPB";
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
