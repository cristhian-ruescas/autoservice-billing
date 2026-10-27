package com.autoservice.billing.orcamento;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class OrcamentoPdf {

    private OrcamentoPdf() {
    }

    public static byte[] gerar(final String titulo, final List<String> linhas) {
        final String texto = sanitizar(titulo + " | " + String.join(" | ", linhas));
        final String stream = "BT /F1 11 Tf 40 780 Td (" + texto + ") Tj ET\n";
        final StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        final int o1 = pdf.length();
        pdf.append("1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n");
        final int o2 = pdf.length();
        pdf.append("2 0 obj<</Type/Pages/Count 1/Kids[3 0 R]>>endobj\n");
        final int o3 = pdf.length();
        pdf.append("3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 595 842]/Contents 4 0 R/Resources<</Font<</F1 5 0 R>>>>>>endobj\n");
        final int o4 = pdf.length();
        pdf.append("4 0 obj<</Length ").append(stream.length()).append(">>stream\n");
        pdf.append(stream);
        pdf.append("endstream\nendobj\n");
        final int o5 = pdf.length();
        pdf.append("5 0 obj<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>endobj\n");
        final int xref = pdf.length();
        pdf.append("xref\n0 6\n");
        pdf.append("0000000000 65535 f \n");
        pdf.append(ref(o1)).append(ref(o2)).append(ref(o3)).append(ref(o4)).append(ref(o5));
        pdf.append("trailer<</Size 6/Root 1 0 R>>\nstartxref\n").append(xref).append("\n%%EOF");
        return pdf.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private static String ref(final int offset) {
        return "%010d 00000 n \n".formatted(offset);
    }

    static String sanitizar(final String texto) {
        final StringBuilder saida = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            final char caractere = texto.charAt(i);
            if (caractere == '\\' || caractere == '(' || caractere == ')') {
                saida.append('\\').append(caractere);
            } else if (caractere >= 32 && caractere < 127) {
                saida.append(caractere);
            } else {
                saida.append('?');
            }
        }
        return saida.toString();
    }
}
