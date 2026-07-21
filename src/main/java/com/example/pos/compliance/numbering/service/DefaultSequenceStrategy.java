package com.example.pos.compliance.numbering.service;

import com.example.pos.compliance.numbering.model.DocumentSequence;
import com.example.pos.compliance.numbering.repository.DocumentSequenceRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class DefaultSequenceStrategy implements SequenceStrategy {

    private final DocumentSequenceRepository sequenceRepo;

    public DefaultSequenceStrategy(DocumentSequenceRepository sequenceRepo) {
        this.sequenceRepo = sequenceRepo;
    }

    @Override
    public String next(String documentType, String branchCode) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        DocumentSequence seq = sequenceRepo
                .findByDocumentTypeAndBranchCodeAndSequenceDate(documentType, branchCode, today)
                .orElseGet(() -> {
                    DocumentSequence newSeq = new DocumentSequence();
                    newSeq.setDocumentType(documentType);
                    newSeq.setBranchCode(branchCode);
                    newSeq.setSequenceDate(today);
                    newSeq.setLastSequence(0L);
                    return newSeq;
                });
        seq.setLastSequence(seq.getLastSequence() + 1);
        sequenceRepo.save(seq);
        return String.format("%s-%s-%06d", branchCode, today, seq.getLastSequence());
    }
}
