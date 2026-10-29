package br.com.senac.gerenciadordesalas.erro;

public class ConflitoDeEstadoException extends IllegalStateException {
    public ConflitoDeEstadoException(String mensagem) {
        super(mensagem);
    }
}
