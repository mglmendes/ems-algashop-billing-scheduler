package com.algaworks.algashop.billingscheduler.infrastructure;

import com.algaworks.algashop.billingscheduler.application.CanceledExpiredInvoicesApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CancelExpiredInvoicesRunner implements ApplicationRunner {
    private final CanceledExpiredInvoicesApplicationService applicationService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Task started - Canceling expired invoices.");
        applicationService.cancelExpiredInvoices();
        log.info("Task ended - Expired invoices.");
    }
}
