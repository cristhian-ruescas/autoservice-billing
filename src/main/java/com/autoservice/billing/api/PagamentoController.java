package com.autoservice.billing.api;

import com.autoservice.billing.service.BillingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/pagamentos")
public class PagamentoController {

    private final BillingService billingService;

    public PagamentoController(final BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/{ordemServicoId}")
    public PagamentoResponse consultar(@PathVariable final UUID ordemServicoId) {
        return this.billingService.consultarPagamento(ordemServicoId);
    }

    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.OK)
    public void webhook(@RequestBody final Map<String, Object> body) {
        this.billingService.webhook(body);
    }
}
