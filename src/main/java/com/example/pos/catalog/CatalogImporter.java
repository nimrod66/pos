package com.example.pos.catalog;

import java.io.InputStream;
import java.util.List;

public interface CatalogImporter {
    String format();
    List<CatalogItem> parse(InputStream input);
    boolean supports(String fileNameOrFormat);
}
