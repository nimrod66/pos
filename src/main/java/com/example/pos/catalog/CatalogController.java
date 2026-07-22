package com.example.pos.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listCatalogs(
            @RequestParam(required = false) String supplier) {
        List<Catalog> catalogs = supplier != null
                ? catalogService.listActiveBySupplier(supplier)
                : catalogService.listCatalogs();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "count", catalogs.size(),
                "data", catalogs
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCatalog(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("success", true, "data", catalogService.getCatalog(id)));
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<Map<String, Object>> getItems(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean unmatchedOnly) {
        List<CatalogItem> items = unmatchedOnly
                ? catalogService.getUnmatchedItems(id)
                : catalogService.getCatalogItems(id);
        return ResponseEntity.ok(Map.of("success", true, "count", items.size(), "data", items));
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(@RequestParam String code) {
        return ResponseEntity.ok(Map.of("success", true, "data", catalogService.searchItems(code)));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createEmpty(@RequestBody Map<String, String> body) {
        Catalog catalog = catalogService.createEmpty(
                body.get("name"),
                body.get("supplier"),
                body.getOrDefault("version", "1.0")
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("success", true, "data", catalog));
    }

    @PostMapping("/import/provider")
    public ResponseEntity<Map<String, Object>> importFromProvider(@RequestBody Map<String, String> body) {
        Catalog catalog = catalogService.importFromProvider(
                body.get("provider"),
                body.get("catalogName"),
                body.get("sourceUrl")
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("success", true, "totalItems", catalog.getTotalItems(), "data", catalog));
    }

    @PostMapping("/{id}/import/file")
    public ResponseEntity<Map<String, Object>> importFromFile(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        Catalog catalog = catalogService.importFromFile(id, file);
        return ResponseEntity.ok(Map.of("success", true, "totalItems", catalog.getTotalItems()));
    }

    @PostMapping("/items/{itemId}/match")
    public ResponseEntity<Map<String, Object>> matchItem(@PathVariable Long itemId) {
        ProductMatcher.MatchResult result = catalogService.matchItem(itemId);
        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }

    @PostMapping("/{id}/match-all")
    public ResponseEntity<Map<String, Object>> matchAll(@PathVariable Long id) {
        int matched = catalogService.matchAllUnmatched(id);
        return ResponseEntity.ok(Map.of("success", true, "matched", matched));
    }

    @GetMapping("/providers")
    public ResponseEntity<Map<String, Object>> listProviders() {
        return ResponseEntity.ok(Map.of("success", true, "data", catalogService.listProviders()));
    }

    @GetMapping("/importers")
    public ResponseEntity<Map<String, Object>> listImporters() {
        return ResponseEntity.ok(Map.of("success", true, "data", catalogService.listImporters()));
    }
}
