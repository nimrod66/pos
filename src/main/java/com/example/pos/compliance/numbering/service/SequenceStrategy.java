package com.example.pos.compliance.numbering.service;

public interface SequenceStrategy {

    String next(String documentType, String branchCode);
}
