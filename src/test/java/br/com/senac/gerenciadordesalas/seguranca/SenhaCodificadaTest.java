package br.com.senac.gerenciadordesalas.seguranca;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class SenhaCodificadaTest {

    private static final String SENHA = "SenhaSintetica#2026";
    private static final String SENHA_DA_DEMONSTRACAO = "Demo#Salas2026";

    private final PasswordEncoder codificador = new SecurityConfig().passwordEncoder();

    @Test
    void senhaCodificadaNaoEhTextoPuroEEhBcrypt() {
        String hash = codificador.encode(SENHA);

        assertThat(hash)
                .isNotEqualTo(SENHA)
                .doesNotContain(SENHA)
                .startsWith("$2")
                .hasSize(60);
        assertThat(codificador.matches(SENHA, hash)).isTrue();
        assertThat(codificador.matches("SenhaErrada#0000", hash)).isFalse();
    }

    @Test
    void mesmaSenhaGeraHashesDiferentesPorCausaDoSal() {
        assertThat(codificador.encode(SENHA)).isNotEqualTo(codificador.encode(SENHA));
    }

    @Test
    void hashDosUsuariosDeDemonstracaoCorrespondeASenhaDocumentada() throws IOException {
        String sql = new String(
                new ClassPathResource("db/demo/R__seed_demo_users.sql").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);

        Matcher hashes = Pattern.compile("'(\\$2a\\$10\\$[^']+)'").matcher(sql);

        int encontrados = 0;
        while (hashes.find()) {
            encontrados++;
            assertThat(codificador.matches(SENHA_DA_DEMONSTRACAO, hashes.group(1))).isTrue();
        }
        assertThat(encontrados).isGreaterThan(0);
    }
}