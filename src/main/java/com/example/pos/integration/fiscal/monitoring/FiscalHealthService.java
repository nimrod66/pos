package com.example.pos.integration.fiscal.monitoring;

import com.example.pos.integration.fiscal.client.FiscalClient;
import com.example.pos.integration.fiscal.config.FiscalMode;
import com.example.pos.integration.fiscal.config.FiscalProperties;
import com.example.pos.integration.fiscal.dto.v1.FiscalHealthResponse;
import org.springframework.stereotype.Service;

@Service
public class FiscalHealthService {

    private final FiscalProperties properties;
    private final FiscalClient fiscalClient;

    public FiscalHealthService(FiscalProperties properties, FiscalClient fiscalClient) {
        this.properties = properties;
        this.fiscalClient = fiscalClient;
    }

    public FiscalHealthResponse check() {
        FiscalMode mode = properties.isEnabled() ? properties.getMode() : FiscalMode.OFF;

        if (mode == FiscalMode.OFF || !properties.isEnabled()) {
            return new FiscalHealthResponse("DISABLED", "OFF", null, 0L);
        }

        if (mode == FiscalMode.LOCAL) {
            return new FiscalHealthResponse("CONNECTED", "LOCAL", null, 0L);
        }

        return fiscalClient.health();
    }
}
