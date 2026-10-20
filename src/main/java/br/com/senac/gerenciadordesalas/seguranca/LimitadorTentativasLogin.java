package br.com.senac.gerenciadordesalas.seguranca;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Limita falhas de autenticação por conta e por origem.
 *
 * <p>O estado fica somente em memória, expira em quinze minutos e tem tamanho
 * máximo para não permitir crescimento ilimitado. Nenhum e-mail ou endereço de
 * origem é armazenado em texto puro.</p>
 */
@Component
public class LimitadorTentativasLogin {

    static final int LIMITE_DE_FALHAS = 5;
    static final Duration JANELA = Duration.ofMinutes(15);
    static final Duration BLOQUEIO_INICIAL = Duration.ofSeconds(30);
    static final Duration BLOQUEIO_MAXIMO = Duration.ofMinutes(15);
    private static final int MAXIMO_DE_CHAVES = 10_000;

    private final Clock relogio;
    private final Map<String, Estado> estados = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Estado> maisAntigo) {
            return size() > MAXIMO_DE_CHAVES;
        }
    };

    public LimitadorTentativasLogin() {
        this(Clock.systemUTC());
    }

    LimitadorTentativasLogin(Clock relogio) {
        this.relogio = relogio;
    }

    public synchronized Duration tempoRestante(String email, String origem) {
        Instant agora = relogio.instant();
        Duration conta = tempoRestante(chaveConta(email), agora);
        Duration endereco = tempoRestante(chaveOrigem(origem), agora);
        return conta.compareTo(endereco) >= 0 ? conta : endereco;
    }

    public synchronized void registrarFalha(String email, String origem) {
        Instant agora = relogio.instant();
        registrarFalha(chaveConta(email), agora);
        registrarFalha(chaveOrigem(origem), agora);
    }

    public synchronized void registrarSucesso(String email, String origem) {
        estados.remove(chaveConta(email));
        estados.remove(chaveOrigem(origem));
    }

    public String identificadorSeguroDaOrigem(String origem) {
        return resumo("origem:" + valorSeguro(origem)).substring(0, 12);
    }

    synchronized void limpar() {
        estados.clear();
    }

    private Duration tempoRestante(String chave, Instant agora) {
        Estado estado = estados.get(chave);
        if (estado == null) {
            return Duration.ZERO;
        }
        if (!agora.isBefore(estado.inicioDaJanela().plus(JANELA))) {
            estados.remove(chave);
            return Duration.ZERO;
        }
        if (estado.bloqueadoAte() == null || !agora.isBefore(estado.bloqueadoAte())) {
            return Duration.ZERO;
        }
        return Duration.between(agora, estado.bloqueadoAte());
    }

    private void registrarFalha(String chave, Instant agora) {
        Estado anterior = estados.get(chave);
        if (anterior == null || !agora.isBefore(anterior.inicioDaJanela().plus(JANELA))) {
            estados.put(chave, new Estado(agora, 1, null));
            return;
        }

        int falhas = anterior.falhas() + 1;
        Instant bloqueadoAte = anterior.bloqueadoAte();
        if (falhas >= LIMITE_DE_FALHAS) {
            int expoente = Math.min(falhas - LIMITE_DE_FALHAS, 10);
            long multiplicador = 1L << expoente;
            Duration bloqueio = BLOQUEIO_INICIAL.multipliedBy(multiplicador);
            if (bloqueio.compareTo(BLOQUEIO_MAXIMO) > 0) {
                bloqueio = BLOQUEIO_MAXIMO;
            }
            bloqueadoAte = agora.plus(bloqueio);
        }
        estados.put(chave, new Estado(anterior.inicioDaJanela(), falhas, bloqueadoAte));
    }

    private String chaveConta(String email) {
        String normalizado = valorSeguro(email).trim().toLowerCase(Locale.ROOT);
        return "conta:" + resumo(normalizado);
    }

    private String chaveOrigem(String origem) {
        return "origem:" + resumo(valorSeguro(origem));
    }

    private String valorSeguro(String valor) {
        return valor == null || valor.isBlank() ? "<ausente>" : valor;
    }

    private String resumo(String valor) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException excecao) {
            throw new IllegalStateException("SHA-256 indisponivel", excecao);
        }
    }

    private record Estado(Instant inicioDaJanela, int falhas, Instant bloqueadoAte) {
    }
}
