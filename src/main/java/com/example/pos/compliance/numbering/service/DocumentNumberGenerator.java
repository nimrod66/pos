package com.example.pos.compliance.numbering.service;

import java.math.BigDecimal;

public interface DocumentNumberGenerator {

    String generate(String documentType, String branchCode);

    BigDecimal getCurrentSequence(String documentType, String branchCode);
}
