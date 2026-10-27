package com.autoservice.billing.orcamento;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrcamentoPdfTest {

    @Test
    void geraPdfComTextoEscapado() {
        final byte[] pdf = OrcamentoPdf.gerar("Orcamento (oficina)", List.of("Troca de oleo", "R$ 10"));
        final String texto = new String(pdf, StandardCharsets.US_ASCII);

        assertThat(texto).startsWith("%PDF-1.4");
        assertThat(texto).contains("Orcamento \\(oficina\\)");
        assertThat(texto).contains("Troca de oleo");
        assertThat(OrcamentoPdf.sanitizar("ação\\")).isEqualTo("a??o\\\\");
    }
}
