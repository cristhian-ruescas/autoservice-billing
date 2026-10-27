package com.autoservice.billing.persistence;

import com.autoservice.billing.domain.PagamentoStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pagamento")
public class PagamentoEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36)
    private UUID id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "ordem_servico_id", nullable = false, unique = true, length = 36)
    private UUID ordemServicoId;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "orcamento_id", nullable = false, length = 36)
    private UUID orcamentoId;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "correlation_id", nullable = false, length = 36)
    private UUID correlationId;

    @Column(name = "mp_order_id", length = 120)
    private String mpOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PagamentoStatus status;

    @Column(name = "status_mp", length = 40)
    private String statusMp;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "qr_data", length = 2000)
    private String qrData;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;
}
