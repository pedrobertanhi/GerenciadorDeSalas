package br.com.senac.gerenciadordesalas.seguranca;

import br.com.senac.gerenciadordesalas.usuario.User;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Verifica propriedade de recursos sem confiar em identificadores enviados pelo cliente.
 * Deve ser usada em regras de método para operações limitadas ao dono do recurso.
 */
@Component("autorizacaoDePropriedade")
public class AutorizacaoDePropriedade {

    public boolean ehDono(Authentication autenticacao, String emailDoDono) {
        if (autenticacao == null || !autenticacao.isAuthenticated() || emailDoDono == null) {
            return false;
        }

        String emailAutenticado = User.normalizarEmail(autenticacao.getName());
        String emailNormalizadoDoDono = User.normalizarEmail(emailDoDono);
        return emailAutenticado != null && emailAutenticado.equals(emailNormalizadoDoDono);
    }
}
