package br.com.senac.gerenciadordesalas.erro;

/**
 * Conflito de regra de negocio que tambem preserva o contrato historico das
 * transicoes de estado, que usavam {@link IllegalStateException}.
 */
public class ConflitoDeEstadoException extends IllegalStateException {
    public ConflitoDeEstadoException(String mensagem) {
        super(mensagem);
    }
}
