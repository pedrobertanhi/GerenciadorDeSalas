package br.com.senac.gerenciadordesalas.auditoria;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Consulta somente leitura do histórico. A autorização da rota é aplicada
 * centralmente pela configuração de segurança.
 */
@RestController
@RequestMapping("/api/auditoria")
@ConditionalOnBean(AuditService.class)
public class AuditController {

    private final AuditService service;

    public AuditController(AuditService service) {
        this.service = service;
    }

    @GetMapping
    public Page<AuditEventDto.Item> consultar(
            @RequestParam(required = false) String entidade,
            @RequestParam(required = false) Long entidadeId,
            @RequestParam(required = false) Long atorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant ate,
            @PageableDefault(size = 20, sort = "occurredAt") Pageable paginacao) {
        return service.consultar(entidade, entidadeId, atorId, de, ate, paginacao);
    }
}
