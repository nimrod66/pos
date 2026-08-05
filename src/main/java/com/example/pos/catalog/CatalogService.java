package com.example.pos.catalog;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CatalogService {

    private final CatalogRepository catalogRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final ProductMatcher productMatcher;
    private final List<CatalogProvider> providers;
    private final List<CatalogImporter> importers;

    public CatalogService(CatalogRepository catalogRepository,
                          CatalogItemRepository catalogItemRepository,
                          ProductMatcher productMatcher,
                          List<CatalogProvider> providers,
                          List<CatalogImporter> importers) {
        this.catalogRepository = catalogRepository;
        this.catalogItemRepository = catalogItemRepository;
        this.productMatcher = productMatcher;
        this.providers = providers;
        this.importers = importers;
    }

    @Transactional(readOnly = true)
    public List<Catalog> listCatalogs() {
        return catalogRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Catalog> listActiveBySupplier(String supplier) {
        return catalogRepository.findActiveBySupplier(supplier);
    }

    @Transactional(readOnly = true)
    public Catalog getCatalog(UUID id) {
        return catalogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catalog not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<CatalogItem> getCatalogItems(UUID catalogId) {
        return catalogItemRepository.findByCatalogId(catalogId);
    }

    @Transactional(readOnly = true)
    public List<CatalogItem> getUnmatchedItems(UUID catalogId) {
        return catalogItemRepository.findByMatchedMedicineIdIsNullAndCatalogId(catalogId);
    }

    @Transactional(readOnly = true)
    public List<CatalogItem> searchItems(String supplierCode) {
        var item = catalogItemRepository.findBySupplierCodeActive(supplierCode);
        return item.map(List::of).orElseGet(List::of);
    }

    public Catalog importFromProvider(String providerName, String catalogName, String sourceUrl) {
        CatalogProvider provider = providers.stream()
                .filter(p -> p.getSupplier().equalsIgnoreCase(providerName)
                        || p.getName().toLowerCase().contains(providerName.toLowerCase()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No provider found for: " + providerName));

        List<CatalogItem> items = provider.fetchItems(sourceUrl);
        if (items.isEmpty()) {
            throw new RuntimeException("No items fetched from provider: " + providerName);
        }

        Catalog catalog = Catalog.builder()
                .name(catalogName)
                .supplier(provider.getSupplier())
                .catalogVersion("1.0")
                .sourceUrl(sourceUrl)
                .totalItems(items.size())
                .status("ACTIVE")
                .build();
        catalog = catalogRepository.save(catalog);

        for (CatalogItem item : items) {
            item.setCatalog(catalog);
        }
        catalogItemRepository.saveAll(items);
        catalog.setItems(items);
        return catalog;
    }

    public Catalog importFromFile(UUID catalogId, MultipartFile file) {
        Catalog catalog = getCatalog(catalogId);

        CatalogImporter importer = importers.stream()
                .filter(i -> i.supports(file.getOriginalFilename()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No importer for file: " + file.getOriginalFilename()));

        List<CatalogItem> items;
        try {
            items = importer.parse(file.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file", e);
        }

        if (items.isEmpty()) {
            throw new RuntimeException("No items parsed from file");
        }

        for (CatalogItem item : items) {
            item.setCatalog(catalog);
        }
        catalogItemRepository.saveAll(items);
        catalog.setTotalItems(catalog.getTotalItems() + items.size());
        catalogRepository.save(catalog);
        return catalog;
    }

    public ProductMatcher.MatchResult matchItem(UUID itemId) {
        CatalogItem item = catalogItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Catalog item not found: " + itemId));

        ProductMatcher.MatchResult result = productMatcher.match(item);

        if (result.matched()) {
            item.setMatchedMedicineId(result.medicineId());
            item.setMatchConfidence(result.confidence());
            catalogItemRepository.save(item);
        }

        return result;
    }

    public int matchAllUnmatched(UUID catalogId) {
        List<CatalogItem> unmatched = catalogItemRepository.findByMatchedMedicineIdIsNullAndCatalogId(catalogId);
        int matched = 0;
        for (CatalogItem item : unmatched) {
            ProductMatcher.MatchResult result = productMatcher.match(item);
            if (result.matched()) {
                item.setMatchedMedicineId(result.medicineId());
                item.setMatchConfidence(result.confidence());
                catalogItemRepository.save(item);
                matched++;
            }
        }
        return matched;
    }

    @Transactional(readOnly = true)
    public List<String> listProviders() {
        return providers.stream()
                .filter(CatalogProvider::isAvailable)
                .map(p -> p.getSupplier() + " - " + p.getName())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> listImporters() {
        return importers.stream().map(CatalogImporter::format).toList();
    }

    public Catalog createEmpty(String name, String supplier, String version) {
        Catalog catalog = Catalog.builder()
                .name(name)
                .supplier(supplier)
                .catalogVersion(version)
                .totalItems(0)
                .status("ACTIVE")
                .build();
        return catalogRepository.save(catalog);
    }
}
