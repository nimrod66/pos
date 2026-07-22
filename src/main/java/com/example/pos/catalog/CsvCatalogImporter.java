package com.example.pos.catalog;

import org.springframework.stereotype.Component;

import java.io.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvCatalogImporter implements CatalogImporter {

    @Override
    public String format() {
        return "CSV";
    }

    @Override
    public List<CatalogItem> parse(InputStream input) {
        List<CatalogItem> items = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            String header = reader.readLine();
            if (header == null) return items;

            String[] columns = header.split(",", -1);
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",", -1);
                CatalogItem item = new CatalogItem();
                for (int i = 0; i < Math.min(columns.length, values.length); i++) {
                    String key = columns[i].trim().toLowerCase().replace(" ", "_");
                    String val = values[i].trim();
                    if (val.isEmpty()) continue;

                    switch (key) {
                        case "code", "supplier_code", "item_code" -> item.setSupplierCode(val);
                        case "product_name", "name", "description" -> item.setProductName(val);
                        case "generic_name", "generic" -> item.setGenericName(val);
                        case "dosage_form", "form" -> item.setDosageForm(val);
                        case "strength" -> item.setStrength(val);
                        case "pack_size", "pack" -> item.setPackSize(val);
                        case "unit", "unit_of_measure", "uom" -> item.setUnitOfMeasure(val);
                        case "manufacturer", "manufacturer_name" -> item.setManufacturerName(val);
                        case "manufacturer_country", "country" -> item.setManufacturerCountry(val);
                        case "etims_code", "etims_classification", "kra_code" -> item.setEtimsClassificationCode(val);
                        case "barcode", "ean", "upc" -> item.setBarcode(val);
                        case "price", "unit_price", "cost" -> item.setUnitPrice(new BigDecimal(val));
                        case "atc", "atc_code" -> item.setAtcCode(val);
                    }
                }
                if (item.getSupplierCode() != null && !item.getSupplierCode().isBlank()) {
                    items.add(item);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse CSV catalog", e);
        }
        return items;
    }

    @Override
    public boolean supports(String fileNameOrFormat) {
        String lower = fileNameOrFormat.toLowerCase();
        return lower.endsWith(".csv") || lower.startsWith("csv");
    }
}
