package com.autoservice.billing.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrcamentoRepository extends JpaRepository<OrcamentoEntity, UUID> {

    Optional<OrcamentoEntity> findByOrdemServicoId(UUID ordemServicoId);
}
