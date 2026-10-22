package br.com.senac.gerenciadordesalas.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Gerenciador de Salas", version = "v1",
    description = "Autenticação por formulário em /login (email e senha) e sessão no servidor. "
        + "O navegador envia o cookie HttpOnly JSESSIONID após login. Não utiliza Bearer/JWT. "
        + "Operações de escrita exigem o token CSRF da sessão no cabeçalho X-CSRF-TOKEN; "
        + "o token é disponibilizado nos formulários web. Use Accept: application/json para erros JSON; "
        + "requisições HTML sem sessão são redirecionadas ao login. A documentação exige autenticação."))
@SecurityScheme(name = "sessao", type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.COOKIE, paramName = "JSESSIONID",
    description = "Sessão criada pelo login via formulário. No navegador, autentique-se em /login; o cookie é enviado automaticamente.")
public class OpenApiConfig {}
