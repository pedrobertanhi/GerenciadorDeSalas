package br.com.senac.gerenciadordesalas.erro;

public class ConflitoDeEstadoException extends RuntimeException {
    public ConflitoDeEstadoException(String mensagem) {
        super(mensagem);
    }
}