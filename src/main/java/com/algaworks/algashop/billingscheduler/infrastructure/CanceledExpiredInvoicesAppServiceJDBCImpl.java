package com.algaworks.algashop.billingscheduler.infrastructure;

import com.algaworks.algashop.billingscheduler.application.CanceledExpiredInvoicesApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CanceledExpiredInvoicesAppServiceJDBCImpl implements CanceledExpiredInvoicesApplicationService {

    private final JdbcOperations jdbcOperations;
    private final TransactionTemplate transactionTemplate;

    private static final Duration EXPIRED_SINCE = Duration.ofDays(1);

    private static final int BATCH_LIMIT = 5;

    private static final String SELECT_EXPIRED_INVOICES_SQL = String.format("""
            select id from invoice i where i.expires_at <= now() - interval '%d days' and i.status = ?
            limit ?
            for update
            skip locked
            """, EXPIRED_SINCE.toDays());

    private static final String UPDATE_INVOICE_STATUS_SQL = """
            update invoice set status = ?, canceled_at = now(), cancel_reason = ? where id = ?;
            """;

    private static final String UNPAID_STATUS = "UNPAID";
    private static final String CANCELED_STATUS = "CANCELED";
    private static final String CANCELED_REASON = "Invoice expired";


    @Override
    public void cancelExpiredInvoices() {
        transactionTemplate.execute(status -> {
            List<UUID> invoiceIds = fetchExpiredInvoices();
            log.info("Task - Total invoices fetched: {}", invoiceIds.size());
            int totalCanceledInvoices = cancelInvoices(invoiceIds);
            if (invoiceIds.isEmpty()) {
                log.info("Task - No expired invoices found for cancellation");
                return true;
            }
            log.info("Task - Total canceled invoices: {}", totalCanceledInvoices);
            return true;
        });
    }

    private List<UUID> fetchExpiredInvoices() {
        PreparedStatementSetter pss = ps -> {
            ps.setString(1, UNPAID_STATUS);
            ps.setInt(2, BATCH_LIMIT);
        };
        RowMapper<UUID> mapper = (resultSet, rowNumber) -> resultSet.getObject("id", UUID.class);
        return jdbcOperations.query(SELECT_EXPIRED_INVOICES_SQL, pss, mapper);
    }

    private int cancelInvoices(List<UUID> invoiceIds) {
        try {
            jdbcOperations.batchUpdate(UPDATE_INVOICE_STATUS_SQL, invoiceIds, invoiceIds.size(),
                    (ps, id) -> {
                        ps.setString(1, CANCELED_STATUS);
                        ps.setString(2, CANCELED_REASON);
                        ps.setObject(3, id);
                    });

            log.info("Task - Invoice canceled IDS: {}", invoiceIds);
            return invoiceIds.size();
        } catch (DataAccessException e) {
            log.error("Task failed to cancel invoices", e);
            return 0;
        }
    }
}
