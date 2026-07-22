package com.example.pos.terminal.barcode;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class BarcodeParser {

    private final BarcodeSymbology symbology;

    public BarcodeParser(BarcodeSymbology symbology) {
        this.symbology = symbology;
    }

    public ParseResult parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return ParseResult.unknown("Input is blank");
        }

        String clean = raw.trim();
        List<BarcodeType> candidates = new ArrayList<>();

        for (BarcodeType type : BarcodeType.values()) {
            BarcodeSymbology.Rules rules = symbology.forType(type);
            if (rules == null) continue;

            if (rules.pattern() != null && clean.matches(rules.pattern())) {
                if (rules.fixedLength() > 0 && clean.length() == rules.fixedLength()) {
                    candidates.add(type);
                } else if (rules.fixedLength() <= 0) {
                    candidates.add(type);
                }
            }
        }

        if (candidates.isEmpty()) {
            return ParseResult.unknown("No matching symbology for: " + raw);
        }

        candidates.sort(Comparator.comparingInt(type -> {
            BarcodeSymbology.Rules r = symbology.forType(type);
            return r != null && r.fixedLength() > 0 ? 0 : 1;
        }));

        return ParseResult.detected(clean, candidates.get(0), candidates.size() == 1 ? null : candidates);
    }

    public BarcodeType detectType(String raw) {
        return parse(raw).type();
    }

    public boolean isLikelyBarcode(String raw) {
        return parse(raw).detected();
    }

    public record ParseResult(String value, BarcodeType type, boolean detected, String error, List<BarcodeType> alternatives) {
        public static ParseResult detected(String value, BarcodeType type, List<BarcodeType> alternatives) {
            return new ParseResult(value, type, true, null, alternatives);
        }

        public static ParseResult unknown(String error) {
            return new ParseResult(null, null, false, error, null);
        }
    }
}
