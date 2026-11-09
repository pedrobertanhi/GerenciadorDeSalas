package br.com.senac.gerenciadordesalas.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repositório somente com operações de leitura e inclusão herdada.
 * A aplicação não declara operações de atualização ou exclusão para auditoria.
 */
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long>,
        JpaSpecificationExecutor<AuditEvent> {
}
