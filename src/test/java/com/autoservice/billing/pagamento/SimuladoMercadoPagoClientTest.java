package com.autoservice.billing.pagamento;

import com.autoservice.billing.domain.ItemSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SimuladoMercadoPagoClientTest {

    @Test
    void aprovaNaHoraECancelaSemChamadaExterna() {
        final SimuladoMercadoPagoClient client = new SimuladoMercadoPagoClient();
        final UUID ordemId = UUID.randomUUID();

        final ResultadoPagamentoMp criado = client.criarOrdem(new CriarOrdemMp(
                ordemId, "OS", new BigDecimal("15.00"), List.of(new ItemSnapshot("peca", new BigDecimal("15")))
        ));

        assertThat(criado.aprovado()).isTrue();
        assertThat(criado.id()).contains(ordemId.toString());
        assertThat(client.consultar(criado.id()).aprovado()).isTrue();
        client.cancelarOuEstornar(criado.id(), new BigDecimal("15.00"), "approved");
    }
}
