package com.example.pos.integration.fiscal.config;

import com.example.pos.compliance.tis.TisFacade;
import com.example.pos.compliance.tis.TraderInvoicingSystem;
import com.example.pos.integration.fiscal.implementation.LocalTraderInvoicingSystem;
import com.example.pos.integration.fiscal.implementation.NoOpTraderInvoicingSystem;
import com.example.pos.integration.fiscal.implementation.RemoteTraderInvoicingSystem;
import com.example.pos.integration.fiscal.client.FiscalClient;
import com.example.pos.integration.fiscal.client.RestFiscalClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class FiscalConfiguration {

    private final FiscalProperties properties;
    private final TisFacade tisFacade;

    public FiscalConfiguration(FiscalProperties properties, TisFacade tisFacade) {
        this.properties = properties;
        this.tisFacade = tisFacade;
    }

    @Bean
    @Primary
    public TraderInvoicingSystem traderInvoicingSystem(FiscalClient fiscalClient) {
        if (!properties.isEnabled() || properties.getMode() == FiscalMode.OFF) {
            return new NoOpTraderInvoicingSystem();
        }

        return switch (properties.getMode()) {
            case REMOTE -> new RemoteTraderInvoicingSystem(fiscalClient, properties);
            default -> new LocalTraderInvoicingSystem(tisFacade);
        };
    }

    @Bean
    public NoOpTraderInvoicingSystem noOpTraderInvoicingSystem() {
        return new NoOpTraderInvoicingSystem();
    }

    @Bean
    public LocalTraderInvoicingSystem localTraderInvoicingSystem() {
        return new LocalTraderInvoicingSystem(tisFacade);
    }

    @Bean
    public FiscalClient fiscalClient() {
        return new RestFiscalClient(properties);
    }
}
