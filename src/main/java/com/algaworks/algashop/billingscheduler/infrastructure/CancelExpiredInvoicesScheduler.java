package com.algaworks.algashop.billingscheduler.infrastructure;

import com.algaworks.algashop.billingscheduler.application.CanceledExpiredInvoicesApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CancelExpiredInvoicesScheduler {

    private final CanceledExpiredInvoicesApplicationService applicationService;

    @Scheduled(fixedRate = 5000)
    public void runTask() {
        log.info("Task started - Canceling expired invoices.");
        applicationService.cancelExpiredInvoices();
        log.info("Task ended - Expired invoices.");
    }
}
