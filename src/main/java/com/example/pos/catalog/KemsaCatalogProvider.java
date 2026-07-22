package com.example.pos.catalog;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class KemsaCatalogProvider implements CatalogProvider {

    @Override
    public String getName() {
        return "KEMSA Master Catalogue";
    }

    @Override
    public String getSupplier() {
        return "KEMSA";
    }

    @Override
    public List<CatalogItem> fetchItems(String sourceUrl) {
        return new ArrayList<>();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    private CatalogItem parseKemsaRow(Map<String, String> row) {
        return CatalogItem.builder()
                .supplierCode(row.get("code"))
                .productName(row.get("product_name"))
                .genericName(row.get("generic_name"))
                .dosageForm(row.get("dosage_form"))
                .strength(row.get("strength"))
                .packSize(row.get("pack_size"))
                .unitOfMeasure(row.get("unit"))
                .manufacturerName(row.get("manufacturer"))
                .manufacturerCountry(row.get("country"))
                .barcode(row.get("barcode"))
                .unitPrice(row.containsKey("price") ? new BigDecimal(row.get("price")) : null)
                .atcCode(row.get("atc_code"))
                .matchConfidence("NONE")
                .build();
    }
}
