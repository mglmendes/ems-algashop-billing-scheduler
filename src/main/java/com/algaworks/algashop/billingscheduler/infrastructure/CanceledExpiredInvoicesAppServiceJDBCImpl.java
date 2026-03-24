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
    private final FastpayPaymentAPIClient fastpayPaymentAPIClient;

    private static final Duration EXPIRED_SINCE = Duration.ofDays(1);

    private static final int BATCH_LIMIT = 50;

    private static final String SELECT_EXPIRED_INVOICES_SQL = String.format("""
            select i.id, ps.gateway_code
            from invoice i
            inner join payment_settings ps on i.payment_settings_id = ps.id
            where i.expires_at <= now() - interval '%d days'
            and i.status = ?
            order by i.expires_at asc
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
            List<InvoiceProjection> invoiceProjections = fetchExpiredInvoices();
            log.info("Task - Total invoices fetched: {}", invoiceProjections.size());
            int totalCanceledInvoices = cancelInvoices(invoiceProjections);
            if (invoiceProjections.isEmpty()) {
                log.info("Task - No expired invoices found for cancellation");
                return true;
            }
            log.info("Task - Total canceled invoices: {}", totalCanceledInvoices);
            return true;
        });
    }

    private List<InvoiceProjection> fetchExpiredInvoices() {
        PreparedStatementSetter pss = ps -> {
            ps.setString(1, UNPAID_STATUS);
            ps.setInt(2, BATCH_LIMIT);
        };
        RowMapper<InvoiceProjection> mapper = (resultSet, rowNumber) ->
                new InvoiceProjection(resultSet.getObject("id", UUID.class),
                        resultSet.getString("gateway_code"));
        return jdbcOperations.query(SELECT_EXPIRED_INVOICES_SQL, pss, mapper);
    }

    private int cancelInvoices(List<InvoiceProjection> invoiceProjections) {
        List<InvoiceProjection> canceledInvoices = invoiceProjections.stream().filter(
                invoiceProjection -> {
                    try {
                        fastpayPaymentAPIClient.cancel(invoiceProjection.getPaymentGatewayCode());
                        log.info("Task - Invoice {} has the payment {} cancelled on gateway",
                                invoiceProjection.getId(), invoiceProjection.getPaymentGatewayCode());
                        return true;
                    } catch (Exception e) {
                        log.error("Task - Failed to cancel invoice {} payment {} on the gateway",
                                invoiceProjection.getId(), invoiceProjection.getPaymentGatewayCode(), e);
                        return false;
                    }

                }
        ).toList();
        try {
            jdbcOperations.batchUpdate(UPDATE_INVOICE_STATUS_SQL, canceledInvoices, canceledInvoices.size(),
                    (ps, invoiceProjection) -> {
                        ps.setString(1, CANCELED_STATUS);
                        ps.setString(2, CANCELED_REASON);
                        ps.setObject(3, invoiceProjection.getId());
                    });

            log.info("Task - Invoice canceled");
            return canceledInvoices.size();
        } catch (DataAccessException e) {
            log.error("Task failed to cancel invoices", e);
            return 0;
        }
    }
}
