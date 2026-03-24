package com.algaworks.algashop.billingscheduler.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class FastpayPaymentAPIClientConfig {

    @Bean
    public FastpayPaymentAPIClient fastpayPaymentAPIClient(
            RestClient.Builder builder,
            AlgaShopPaymentProperties properties) {
        var fastpay = properties.getFastpay();

        RestClient build = builder.baseUrl(fastpay.getHostname()).requestInterceptor(
                ((request, body, execution) -> {
                    request.getHeaders().add("Token", fastpay.getPrivateToken());
                    return execution.execute(request, body);
                })
        ).build();

        RestClientAdapter adapter = RestClientAdapter.create(build);
        HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory.builderFor(adapter).build();
        return proxyFactory.createClient(FastpayPaymentAPIClient.class);
    }
}