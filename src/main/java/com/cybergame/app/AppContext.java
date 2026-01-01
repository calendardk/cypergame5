package com.cybergame.app;

import com.cybergame.context.AccountContext;
import com.cybergame.controller.SessionManager;
import com.cybergame.repository.sql.*;

public class AppContext {

    public static final AccountContext accountContext =
            AccountContext.getInstance();

    public static final AccountRepositorySQL accountRepo =
            new AccountRepositorySQL();

    public static final SessionRepositorySQL sessionRepo =
            new SessionRepositorySQL();

    public static final InvoiceRepositorySQL invoiceRepo =
            new InvoiceRepositorySQL();

    public static final ComputerRepositorySQL computerRepo =
            new ComputerRepositorySQL();

    public static final SessionManager sessionManager =
            new SessionManager(
                    sessionRepo,
                    invoiceRepo,
                    accountRepo,
                    accountContext
            );
    public static final EmployeeRepositorySQL employeeRepo =
        new EmployeeRepositorySQL();

    private AppContext() {}
}
