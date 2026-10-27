package com.autoservice.billing.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PagamentoRepository extends JpaRepository<PagamentoEntity, UUID> {

    Optional<PagamentoEntity> findByOrdemServicoId(UUID ordemServicoId);

    Optional<PagamentoEntity> findByMpOrderId(String mpOrderId);
}
