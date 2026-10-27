package com.autoservice.billing.orcamento;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrcamentoEmailSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrcamentoEmailSender.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String destino;

    public OrcamentoEmailSender(
            @Autowired(required = false) final JavaMailSender mailSender,
            @Value("${autoservice.mail.enabled:false}") final boolean enabled,
            @Value("${autoservice.mail.to:cliente@autoservice.local}") final String destino
    ) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.destino = destino;
    }

    public boolean enviar(final UUID ordemServicoId, final byte[] pdf) {
        if (!this.enabled || this.mailSender == null) {
            LOGGER.info("Orcamento da OS {} registrado sem SMTP", ordemServicoId);
            return false;
        }
        try {
            final MimeMessage message = this.mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(this.destino);
            helper.setSubject("Orcamento da ordem " + ordemServicoId);
            helper.setText("Segue o orcamento da ordem de servico.");
            helper.addAttachment("orcamento-" + ordemServicoId + ".pdf", () -> new java.io.ByteArrayInputStream(pdf));
            this.mailSender.send(message);
            return true;
        } catch (MailException | jakarta.mail.MessagingException exception) {
            LOGGER.error("Falha ao enviar orcamento da OS {}", ordemServicoId, exception);
            return false;
        }
    }
}
