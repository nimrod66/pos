package com.example.pos.integration.fiscal.client;

import com.example.pos.integration.fiscal.dto.v1.FiscalSaleRequest;
import com.example.pos.integration.fiscal.dto.v1.FiscalSaleResponse;
import com.example.pos.integration.fiscal.dto.v1.FiscalHealthResponse;

public interface FiscalClient {

    FiscalSaleResponse sendInvoice(FiscalSaleRequest request);

    FiscalHealthResponse health();
}
