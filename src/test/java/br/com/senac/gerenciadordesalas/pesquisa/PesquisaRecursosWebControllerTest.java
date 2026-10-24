package br.com.senac.gerenciadordesalas.pesquisa;

import br.com.senac.gerenciadordesalas.erro.CorrelationIdFilter;
import br.com.senac.gerenciadordesalas.material.MaterialDto;
import br.com.senac.gerenciadordesalas.material.MaterialService;
import br.com.senac.gerenciadordesalas.material.MaterialTipo;
import br.com.senac.gerenciadordesalas.professor.ProfessorDto;
import br.com.senac.gerenciadordesalas.professor.ProfessorService;
import br.com.senac.gerenciadordesalas.salas.SalaFiltroDTO;
import br.com.senac.gerenciadordesalas.salas.SalaResponseDTO;
import br.com.senac.gerenciadordesalas.salas.SalaService;
import br.com.senac.gerenciadordesalas.seguranca.RespostasDeSeguranca;
import br.com.senac.gerenciadordesalas.seguranca.SecurityConfig;
import br.com.senac.gerenciadordesalas.seguranca.UsuarioDetailsService;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Tela de pesquisa combinada de recursos (RES-05, #19): filtros individuais,
 * combinados, vazios, inválidos, sem resultado, paginação e acesso por perfil.
 */
@WebMvcTest(PesquisaRecursosWebController.class)
@Import({SecurityConfig.class, RespostasDeSeguranca.class, UsuarioDetailsService.class, CorrelationIdFilter.class})
@ActiveProfiles("test")
class PesquisaRecursosWebControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean SalaService salas;
    @MockitoBean MaterialService materiais;
    @MockitoBean ProfessorService professores;
    @MockitoBean UserRepository userRepository;

    private static SalaResponseDTO sala(long id, String nome, int capacidade, String localizacao) {
        return new SalaResponseDTO(id, nome, capacidade, localizacao, true);
    }

    private static MaterialDto.Saida material(long id, String nome, MaterialTipo tipo, String localizacao) {
        return new MaterialDto.Saida(id, nome, null, tipo, null, 2, localizacao, false, true);
    }

    private static ProfessorDto.Saida professor(long id, String nome, String... competencias) {
        return new ProfessorDto.Saida(id, nome, "professor" + id + "@exemplo.test", true, List.of(competencias));
    }

    @Test
    void semAutenticacao_naoAcessaATela() throws Exception {
        mvc.perform(get("/recursos/pesquisa"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(salas, materiais, professores);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SOLICITANTE", "RESPONSAVEL", "ADMINISTRADOR"})
    void todosOsPerfisAcessamAPesquisa(String perfil) throws Exception {
        when(salas.pesquisar(any(), any())).thenReturn(Page.empty(PageRequest.of(0, 10)));
        mvc.perform(get("/recursos/pesquisa").with(user("usuario").roles(perfil)))
                .andExpect(status().isOk())
                .andExpect(view().name("recursos/pesquisa"));
    }

    @Test
    void salas_filtrosCombinadosChegamAoServicoNormalizados() throws Exception {
        when(salas.pesquisar(any(), any())).thenReturn(new PageImpl<>(
                List.of(sala(1, "Sala 101", 40, "Bloco A - 1o andar")), PageRequest.of(0, 10), 1));

        mvc.perform(get("/recursos/pesquisa").with(user("solicitante").roles("SOLICITANTE"))
                        .param("recurso", "SALA")
                        .param("capacidadeMinima", "30")
                        .param("localizacao", "  Bloco A  "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sala 101")))
                .andExpect(content().string(containsString("Capacidade: 40 pessoas")));

        verify(salas).pesquisar(eq(new SalaFiltroDTO(30, "Bloco A")),
                eq(PageRequest.of(0, 10, Sort.by("nome"))));
    }

    @Test
    void filtrosVazios_equivalemAAusenciaDeFiltroESalaEOPadrao() throws Exception {
        when(salas.pesquisar(any(), any())).thenReturn(Page.empty(PageRequest.of(0, 10)));

        mvc.perform(get("/recursos/pesquisa").with(user("solicitante").roles("SOLICITANTE"))
                        .param("capacidadeMinima", "")
                        .param("localizacao", "   "))
                .andExpect(status().isOk());

        verify(salas).pesquisar(eq(new SalaFiltroDTO(null, null)), any());
        verifyNoInteractions(materiais, professores);
    }

    @Test
    void materiais_tipoELocalizacaoCombinados() throws Exception {
        when(materiais.listar(MaterialTipo.EQUIPAMENTO, null)).thenReturn(List.of(
                material(1, "Projetor", MaterialTipo.EQUIPAMENTO, "Bloco A - Almoxarifado"),
                material(2, "Notebook", MaterialTipo.EQUIPAMENTO, "Bloco B - Laboratorio")));

        mvc.perform(get("/recursos/pesquisa").with(user("solicitante").roles("SOLICITANTE"))
                        .param("recurso", "material")
                        .param("tipo", "equipamento")
                        .param("localizacao", "bloco a"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("resultado", hasProperty("totalElements", is(1L))))
                .andExpect(content().string(containsString("Projetor")))
                .andExpect(content().string(not(containsString("Notebook"))));

        verify(materiais).listar(MaterialTipo.EQUIPAMENTO, null);
        verifyNoInteractions(salas, professores);
    }

    @Test
    void professores_filtroPorCompetencia() throws Exception {
        when(professores.listar("java")).thenReturn(List.of(
                professor(2, "Bruno Lima", "java", "sql"),
                professor(1, "Ana Souza", "java")));

        mvc.perform(get("/recursos/pesquisa").with(user("solicitante").roles("SOLICITANTE"))
                        .param("recurso", "PROFESSOR")
                        .param("competencia", " java "))
                .andExpect(status().isOk())
                .andExpect(model().attribute("resultado", hasProperty("totalElements", is(2L))))
                .andExpect(content().string(containsString("Ana Souza")))
                .andExpect(content().string(containsString("java, sql")));

        verify(professores).listar("java");
    }

    @Test
    void semResultado_exibeMensagemCom200() throws Exception {
        when(salas.pesquisar(any(), any())).thenReturn(Page.empty(PageRequest.of(0, 10)));

        mvc.perform(get("/recursos/pesquisa").with(user("solicitante").roles("SOLICITANTE"))
                        .param("capacidadeMinima", "500"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("resultado", hasProperty("totalElements", is(0L))))
                .andExpect(content().string(containsString("Nenhum recurso encontrado.")));
    }

    @Test
    void paginacao_exibeSomenteOsItensDaPaginaPedida() throws Exception {
        List<ProfessorDto.Saida> doze = IntStream.rangeClosed(1, 12)
                .mapToObj(i -> professor(i, "Professor %02d".formatted(i), "java"))
                .toList();
        when(professores.listar(null)).thenReturn(doze);

        mvc.perform(get("/recursos/pesquisa").with(user("solicitante").roles("SOLICITANTE"))
                        .param("recurso", "PROFESSOR")
                        .param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("resultado", hasProperty("number", is(1))))
                .andExpect(model().attribute("resultado", hasProperty("totalElements", is(12L))))
                .andExpect(content().string(containsString("Professor 11")))
                .andExpect(content().string(containsString("Professor 12")))
                .andExpect(content().string(not(containsString("Professor 10"))));
    }

    @ParameterizedTest
    @CsvSource({
            "recurso, PREDIO",
            "capacidadeMinima, abc",
            "capacidadeMinima, 0",
            "tipo, COMPUTADOR",
            "pagina, -1"
    })
    void filtroInvalido_retorna400SemPesquisar(String parametro, String valor) throws Exception {
        mvc.perform(get("/recursos/pesquisa").with(user("solicitante").roles("SOLICITANTE"))
                        .param(parametro, valor))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("recursos/pesquisa"))
                .andExpect(model().attribute("erros", hasSize(1)));

        verifyNoInteractions(salas, materiais, professores);
    }
}
