package com.example.pos.user.loginhistory.service;

import com.example.pos.user.loginhistory.model.LoginHistory;
import com.example.pos.user.loginhistory.repository.LoginHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class LoginHistoryService {

    private final LoginHistoryRepository repo;

    public LoginHistoryService(LoginHistoryRepository repo) {
        this.repo = repo;
    }

    public List<LoginHistory> getByUser(UUID userId) {
        return repo.findByUserIdOrderByLoginTimeDesc(userId);
    }
}
