package com.autoservice.billing.orcamento;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrcamentoEmailSenderTest {

    @Test
    void semSmtpNaoEnvia() {
        final OrcamentoEmailSender desligado = new OrcamentoEmailSender(null, false, "a@b.c");
        final OrcamentoEmailSender semSender = new OrcamentoEmailSender(null, true, "a@b.c");

        assertThat(desligado.enviar(UUID.randomUUID(), new byte[]{1})).isFalse();
        assertThat(semSender.enviar(UUID.randomUUID(), new byte[]{1})).isFalse();
    }

    @Test
    void enviaAnexoEAbsorveFalha() {
        final JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        final OrcamentoEmailSender email = new OrcamentoEmailSender(sender, true, "cliente@autoservice.local");
        final UUID ordemId = UUID.randomUUID();

        assertThat(email.enviar(ordemId, "%PDF".getBytes())).isTrue();
        verify(sender).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));

        doThrow(new MailSendException("smtp")).when(sender).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
        assertThat(email.enviar(ordemId, "%PDF".getBytes())).isFalse();
    }
}
