package com.autoservice.billing.api;

import com.autoservice.billing.service.BillingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/orcamentos")
public class OrcamentoController {

    private final BillingService billingService;

    public OrcamentoController(final BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/{ordemServicoId}")
    public OrcamentoResponse consultar(@PathVariable final UUID ordemServicoId) {
        return this.billingService.consultarOrcamento(ordemServicoId);
    }

    @PostMapping("/{ordemServicoId}/aprovacao")
    public PagamentoResponse aprovar(@PathVariable final UUID ordemServicoId) {
        return this.billingService.aprovar(ordemServicoId);
    }

    @PostMapping("/{ordemServicoId}/reprovacao")
    public OrcamentoResponse reprovar(@PathVariable final UUID ordemServicoId) {
        return this.billingService.reprovar(ordemServicoId);
    }
}
