package br.com.senac.gerenciadordesalas.auditoria;

import br.com.senac.gerenciadordesalas.usuario.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
