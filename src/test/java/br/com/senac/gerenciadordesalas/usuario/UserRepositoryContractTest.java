package br.com.senac.gerenciadordesalas.usuario;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryContractTest {

    @Test
    void repositorioNaoExpoeExclusaoFisicaDeUsuarios() {
        assertThat(UserRepository.class.getMethods())
                .extracting(Method::getName)
                .noneMatch(nome -> nome.startsWith("delete") || nome.startsWith("remove"));
    }

    @Test
    void repositorioMantemAsOperacoesNecessarias() {
        assertThat(UserRepository.class.getMethods())
                .extracting(Method::getName)
                .contains("save", "saveAndFlush", "findById", "findAll",
                        "findByEmailIgnoreCase", "existsByEmailIgnoreCase");
    }
}