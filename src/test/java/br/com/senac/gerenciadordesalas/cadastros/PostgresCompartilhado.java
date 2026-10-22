package br.com.senac.gerenciadordesalas.cadastros;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Um único PostgreSQL para toda a suíte de integração dos cadastros.
 * O container sobe na primeira vez que a classe é usada, ou seja, só quando o Spring
 * cria o contexto, e nunca é reiniciado entre as classes de teste (o Ryuk o remove
 * ao final). Assim o contexto do Spring pode ser reaproveitado sem apontar para um
 * container já parado.
 */
final class PostgresCompartilhado {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("gerenciador_salas_test")
            .withUsername("postgres")
            .withPassword("postgres");

    static {
        POSTGRES.start();
    }

    private PostgresCompartilhado() {
    }
}