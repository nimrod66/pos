package com.example.pos.sync.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.sync.model.Terminal;
import com.example.pos.sync.service.TerminalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sync/terminals")
public class TerminalController {

    private final TerminalService terminalService;

    public TerminalController(TerminalService terminalService) {
        this.terminalService = terminalService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Terminal>> register(@RequestBody Map<String, String> body) {
        String name = body.getOrDefault("name", "Unnamed Terminal");
        String branchId = body.get("branchId");
        Terminal terminal;
        if (body.containsKey("terminalId")) {
            terminal = terminalService.registerByLocalId(body.get("terminalId"), name, branchId);
        } else {
            terminal = terminalService.registerTerminal(body.get("terminalId"), name);
        }
        return ResponseEntity.ok(ApiResponse.ok(terminal));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Terminal>>> listAll() {
        return ResponseEntity.ok(ApiResponse.ok(terminalService.listAll()));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<Terminal>>> listPending() {
        return ResponseEntity.ok(ApiResponse.ok(terminalService.listPending()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Terminal>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(terminalService.findById(id)));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Terminal>> approve(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(terminalService.approveTerminal(id)));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Terminal>> deactivate(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(terminalService.deactivateTerminal(id)));
    }

    @PutMapping("/{id}/regenerate-key")
    public ResponseEntity<ApiResponse<Terminal>> regenerateKey(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(terminalService.regenerateApiKey(id)));
    }

    @GetMapping("/{id}/health")
    public ResponseEntity<ApiResponse<Object>> terminalHealth(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(terminalService.getTerminalHealth(id)));
    }
}
