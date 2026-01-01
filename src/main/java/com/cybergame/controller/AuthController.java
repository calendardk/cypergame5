package com.cybergame.controller;

import com.cybergame.context.AccountContext;
import com.cybergame.model.entity.*;
import com.cybergame.repository.AccountRepository;

public class AuthController {

    private final AccountRepository accountRepo;
    private final SessionManager sessionManager;
    private final AccountContext accountContext;

    public AuthController(AccountRepository repo,
                          SessionManager sessionManager,
                          AccountContext accountContext) {
        this.accountRepo = repo;
        this.sessionManager = sessionManager;
        this.accountContext = accountContext;
    }

    public Session loginCustomer(String username,
                                 String password,
                                 Computer computer) {

        Account acc = accountRepo.findByUsername(username);
        if (acc == null) return null;
        if (acc.isLocked()) return null;

        // 🔥 CHECK ĐANG ONLINE
        if (accountContext.isOnline(username)) {
            throw new IllegalStateException(
                "Tài khoản đang đăng nhập ở máy khác"
            );
        }

        if (!acc.login(password)) return null;

        Session session = sessionManager.startSession(acc, computer);
        if (session == null) return null;

        // ✅ ĐÁNH DẤU ONLINE
        accountContext.put(acc);

        return session;
    }

    public void logout(Session session) {
        if (session == null) return;

        // 🔥 GỠ ONLINE
        accountContext.remove(
                session.getAccount().getUsername()
        );

        sessionManager.endSession(session);
    }
}
