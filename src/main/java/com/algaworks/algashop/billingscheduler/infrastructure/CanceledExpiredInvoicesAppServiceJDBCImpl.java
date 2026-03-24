package com.algaworks.algashop.billingscheduler.infrastructure;

import com.algaworks.algashop.billingscheduler.application.CanceledExpiredInvoicesApplicationService;
import org.springframework.stereotype.Service;

@Service
public class CanceledExpiredInvoicesAppServiceJDBCImpl implements CanceledExpiredInvoicesApplicationService {

    @Override
    public void cancelExpiredInvoices() {

    }
}
