package br.com.senac.gerenciadordesalas.auditoria;

import br.com.senac.gerenciadordesalas.usuario.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.springframework.util.StringUtils.hasText;

/**
 * Ponto único de gravação de auditoria. Por participar da transação chamadora,
 * qualquer falha na persistência faz a operação de negócio ser revertida.
 */
@Service
public class AuditService {

    private static final Set<String> CAMPOS_SENSIVEIS = Set.of(
            "senha", "password", "senhaHash", "passwordHash", "token", "accessToken", "refreshToken");

    private final AuditEventRepository eventos;
    private final Clock clock;

    public AuditService(AuditEventRepository eventos, Clock clock) {
        this.eventos = eventos;
        this.clock = clock;
    }

    @Transactional
    public AuditEvent registrar(String entidade, Long entidadeId, TipoAcaoAuditoria acao,
                                User ator, String estadoAnterior, String estadoNovo,
                                Map<String, Object> detalhes) {
        return eventos.save(new AuditEvent(entidade, entidadeId, acao, ator, Instant.now(clock),
                estadoAnterior, estadoNovo, mascararDetalhes(detalhes)));
    }

    @Transactional(readOnly = true)
    public Page<AuditEventDto.Item> consultar(String entidade, Long entidadeId, Long atorId,
                                              Instant de, Instant ate, Pageable paginacao) {
        if (de != null && ate != null && de.isAfter(ate)) {
            throw new IllegalArgumentException("O início do período deve ser anterior ao fim.");
        }
        Specification<AuditEvent> filtros = Specification.unrestricted();
        if (hasText(entidade)) {
            filtros = filtros.and((root, query, builder) ->
                    builder.equal(root.get("entity"), entidade.trim()));
        }
        if (entidadeId != null) {
            filtros = filtros.and((root, query, builder) ->
                    builder.equal(root.get("entityId"), entidadeId));
        }
        if (atorId != null) {
            filtros = filtros.and((root, query, builder) ->
                    builder.equal(root.get("actor").get("id"), atorId));
        }
        if (de != null) {
            filtros = filtros.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("occurredAt"), de));
        }
        if (ate != null) {
            filtros = filtros.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.get("occurredAt"), ate));
        }
        return eventos.findAll(filtros, paginacao).map(AuditEventDto.Item::de);
    }

    private Map<String, Object> mascararDetalhes(Map<String, Object> detalhes) {
        Map<String, Object> seguros = new LinkedHashMap<>();
        if (detalhes == null) {
            return seguros;
        }
        detalhes.forEach((chave, valor) -> seguros.put(chave,
                CAMPOS_SENSIVEIS.contains(chave) ? "[MASCARADO]" : valor));
        return seguros;
    }
}
