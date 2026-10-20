package br.com.senac.gerenciadordesalas;

import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration"
})
@ActiveProfiles("test")
class GerenciadordesalasApplicationTests {

    // Sem banco neste teste, o repositório não é criado; o mock mantém o contexto de pé.
    @MockitoBean
    UserRepository userRepository;

    @Test
    void contextLoads() {
    }

}