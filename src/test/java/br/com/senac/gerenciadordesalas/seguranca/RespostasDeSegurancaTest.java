package br.com.senac.gerenciadordesalas.seguranca;

import br.com.senac.gerenciadordesalas.erro.CorrelationIdFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class RespostasDeSegurancaTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final RespostasDeSeguranca respostas = new RespostasDeSeguranca(mapper);

    @Test
    void naoAutenticado_devolve401PadronizadoComCorrelationIdDaRequisicao() throws Exception {
        MockHttpServletRequest requisicao = new MockHttpServletRequest();
        requisicao.setAttribute(CorrelationIdFilter.HEADER_CORRELATION_ID, "correlacao-de-teste");
        MockHttpServletResponse resposta = new MockHttpServletResponse();

        respostas.commence(requisicao, resposta, new BadCredentialsException("detalhe interno secreto"));

        JsonNode corpo = mapper.readTree(resposta.getContentAsString());
        assertThat(resposta.getStatus()).isEqualTo(401);
        assertThat(corpo.get("codigo").asText()).isEqualTo("NAO_AUTENTICADO");
        assertThat(corpo.get("correlationId").asText()).isEqualTo("correlacao-de-teste");
        assertThat(resposta.getContentAsString()).doesNotContain("detalhe interno secreto");
    }

    @Test
    void acessoNegado_devolve403PadronizadoSemDetalheInterno() throws Exception {
        MockHttpServletRequest requisicao = new MockHttpServletRequest();
        requisicao.setAttribute(CorrelationIdFilter.HEADER_CORRELATION_ID, "correlacao-de-teste");
        MockHttpServletResponse resposta = new MockHttpServletResponse();

        respostas.handle(requisicao, resposta, new AccessDeniedException("detalhe interno secreto"));

        JsonNode corpo = mapper.readTree(resposta.getContentAsString());
        assertThat(resposta.getStatus()).isEqualTo(403);
        assertThat(corpo.get("codigo").asText()).isEqualTo("ACESSO_NEGADO");
        assertThat(corpo.get("correlationId").asText()).isEqualTo("correlacao-de-teste");
        assertThat(resposta.getContentAsString()).doesNotContain("detalhe interno secreto");
    }

    @Test
    void semCorrelationIdNaRequisicao_geraUmEPublicaNoHeader() throws Exception {
        MockHttpServletRequest requisicao = new MockHttpServletRequest();
        MockHttpServletResponse resposta = new MockHttpServletResponse();

        respostas.commence(requisicao, resposta, new BadCredentialsException("x"));

        JsonNode corpo = mapper.readTree(resposta.getContentAsString());
        String idNoCorpo = corpo.get("correlationId").asText();
        assertThat(idNoCorpo).isNotBlank();
        assertThat(resposta.getHeader(CorrelationIdFilter.HEADER_CORRELATION_ID)).isEqualTo(idNoCorpo);
    }
}