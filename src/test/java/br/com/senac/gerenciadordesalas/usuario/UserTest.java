package br.com.senac.gerenciadordesalas.usuario;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private static Validator validator;

    @BeforeAll
    static void criarValidador() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private User usuarioValido() {
        return new User("Ana Sintetica", "ana@exemplo.test", "$2a$10$hashsinteticodeteste", Role.SOLICITANTE);
    }

    @Test
    void usuarioValidoNaoTemViolacoes() {
        assertThat(validator.validate(usuarioValido())).isEmpty();
    }

    @Test
    void emailEhNormalizadoParaMinusculoSemEspacos() {
        User usuario = new User("Ana Sintetica", "  Ana@Exemplo.TEST ", "$2a$10$hashsinteticodeteste", Role.SOLICITANTE);

        assertThat(usuario.getEmail()).isEqualTo("ana@exemplo.test");
    }

    @Test
    void perfilNuloEhRecusado() {
        User usuario = usuarioValido();
        usuario.setProfile(null);

        Set<ConstraintViolation<User>> violacoes = validator.validate(usuario);

        assertThat(violacoes)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("profile");
    }

    @Test
    void emailInvalidoEhRecusado() {
        User usuario = usuarioValido();
        usuario.setEmail("isto-nao-e-um-email");

        Set<ConstraintViolation<User>> violacoes = validator.validate(usuario);

        assertThat(violacoes)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("email");
    }

    @Test
    void nomeEmBrancoEhRecusado() {
        User usuario = usuarioValido();
        usuario.setName("   ");

        Set<ConstraintViolation<User>> violacoes = validator.validate(usuario);

        assertThat(violacoes)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("name");
    }

    @Test
    void novoUsuarioNasceAtivoEPodeSerInativadoEReativado() {
        User usuario = usuarioValido();
        assertThat(usuario.isActive()).isTrue();

        usuario.deactivate();
        assertThat(usuario.isActive()).isFalse();

        usuario.activate();
        assertThat(usuario.isActive()).isTrue();
    }
}