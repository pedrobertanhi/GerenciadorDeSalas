package br.com.senac.gerenciadordesalas.auditoria;

/**
 * Catálogo padronizado de ações que podem ser consultadas no histórico.
 */
public enum TipoAcaoAuditoria {
    CRIACAO,
    ALTERACAO,
    INATIVACAO,
    TRANSICAO_DE_STATUS,
    APROVACAO_RECURSO,
    VALIDACAO_DOCENTE,
    CANCELAMENTO,
    RETIRADA_MATERIAL,
    DEVOLUCAO_MATERIAL,
    MANUTENCAO_CRIADA,
    MANUTENCAO_ALTERADA
}
