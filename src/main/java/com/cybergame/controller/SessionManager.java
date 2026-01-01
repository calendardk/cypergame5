package com.cybergame.controller;

import com.cybergame.context.AccountContext;
import com.cybergame.model.entity.*;
import com.cybergame.model.enums.*;
import com.cybergame.repository.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class SessionManager {

    private final SessionRepository sessionRepo;
    private final InvoiceRepository invoiceRepo;
    private final AccountRepository accountRepo;
    private final AccountContext accountContext;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    private int nextSessionId = 1;
    private int nextInvoiceId;

    public SessionManager(SessionRepository sessionRepo,
                          InvoiceRepository invoiceRepo,
                          AccountRepository accountRepo,
                          AccountContext accountContext) {

        this.sessionRepo = sessionRepo;
        this.invoiceRepo = invoiceRepo;
        this.accountRepo = accountRepo;
        this.accountContext = accountContext;

        this.nextInvoiceId = invoiceRepo.findAll()
                .stream()
                .mapToInt(Invoice::getInvoiceId)
                .max()
                .orElse(0) + 1;

        startTimer();
    }

    /* ================= TIMER ================= */

    private void startTimer() {
        scheduler.scheduleAtFixedRate(
                this::tickUpdate,
                1, 1, TimeUnit.SECONDS
        );
    }

    private void chargeTimeCostPerSecond(Session session) {

        Account acc = session.getAccount();
        Computer pc = session.getComputer();

        double costPerSecond =
                pc.getPricePerHour() / 3600.0
                * acc.getTimeDiscountRate();

        if (!acc.canPay(costPerSecond)) {
            forceLogout(session);
            return;
        }

        acc.deduct(costPerSecond);
    }

    private void tickUpdate() {
        for (Session s : sessionRepo.findRunningSessions()) {
            chargeTimeCostPerSecond(s);
        }
    }

    /* ================= SESSION ================= */

    public Session startSession(Account acc, Computer comp) {
        if (acc.isLocked()) return null;
        if (comp.getStatus() != ComputerStatus.AVAILABLE) return null;

        Session session = new Session(nextSessionId++, acc, comp);
        comp.markInUse();
        sessionRepo.save(session);
        return session;
    }

    public void endSession(Session session) {

        if (session.getStatus() == SessionStatus.CLOSED) return;

        session.end();

        sessionRepo.delete(session);

        Account acc = session.getAccount();
        accountRepo.save(acc);

        session.getComputer().markAvailable();

        Invoice invoice = new Invoice(nextInvoiceId++, session);
        invoiceRepo.save(invoice);
    }

    public void forceLogout(Session session) {
        if (session == null) return;

        // 🔥 GỠ ONLINE KHI BỊ FORCE LOGOUT
        accountContext.remove(
                session.getAccount().getUsername()
        );

        endSession(session);
    }

    public List<OrderItem> getPendingOrders() {

        List<OrderItem> result = new ArrayList<>();

        for (Session s : sessionRepo.findRunningSessions()) {
            for (OrderItem item : s.getOrderItems()) {
                if (item.getStatus() == OrderStatus.PENDING) {
                    result.add(item);
                }
            }
        }
        return result;
    }
}
