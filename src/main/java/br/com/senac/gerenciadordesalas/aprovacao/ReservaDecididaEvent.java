package br.com.senac.gerenciadordesalas.aprovacao;

import java.time.Instant;

/**
 * Evento publicado depois de cada decisao do Responsavel (APR-02 e APR-03, #34).
 * A auditoria (AUD-01, #39) e a notificacao (NOT-01, #41) podem escutar este evento
 * sem acoplar o fluxo de aprovacao aos seus modulos.
 */
public record ReservaDecididaEvent(
        Long reservaId,
        TipoDecisao tipo,
        Resultado resultado,
        Long responsavelId,
        Instant decididaEm,
        String motivo) {

    public enum TipoDecisao {
        APROVACAO_RECURSO,
        VALIDACAO_DOCENTE
    }

    public enum Resultado {
        APROVADA,
        REJEITADA
    }
}
