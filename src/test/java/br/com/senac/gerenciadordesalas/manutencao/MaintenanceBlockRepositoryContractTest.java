package br.com.senac.gerenciadordesalas.manutencao;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class MaintenanceBlockRepositoryContractTest {

    @Test
    void repositorioNaoExpoeExclusaoDeBloqueios() {
        assertThat(MaintenanceBlockRepository.class.getMethods())
                .extracting(Method::getName)
                .noneMatch(nome -> nome.startsWith("delete") || nome.startsWith("remove"));
    }

    @Test
    void repositorioMantemAsOperacoesNecessarias() {
        assertThat(MaintenanceBlockRepository.class.getMethods())
                .extracting(Method::getName)
                .contains("saveAndFlush", "findById", "buscar");
    }
}