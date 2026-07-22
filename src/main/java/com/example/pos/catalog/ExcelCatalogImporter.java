package com.example.pos.catalog;

import org.springframework.stereotype.Component;

import java.io.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class ExcelCatalogImporter implements CatalogImporter {

    @Override
    public String format() {
        return "EXCEL";
    }

    @Override
    public List<CatalogItem> parse(InputStream input) {
        return new ArrayList<>();
    }

    @Override
    public boolean supports(String fileNameOrFormat) {
        String lower = fileNameOrFormat.toLowerCase();
        return lower.endsWith(".xlsx") || lower.endsWith(".xls") || lower.startsWith("excel");
    }
}
