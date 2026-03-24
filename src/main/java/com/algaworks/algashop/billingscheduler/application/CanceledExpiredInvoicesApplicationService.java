package com.algaworks.algashop.billingscheduler.application;

import org.springframework.stereotype.Component;

public interface CanceledExpiredInvoicesApplicationService {

    void cancelExpiredInvoices();
}
