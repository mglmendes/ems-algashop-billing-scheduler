package com.algaworks.algashop.billingscheduler.infrastructure;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class InvoiceProjection {

    private UUID id;
    private String paymentGatewayCode;

}
