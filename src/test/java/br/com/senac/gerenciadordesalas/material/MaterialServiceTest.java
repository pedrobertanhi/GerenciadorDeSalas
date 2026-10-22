package br.com.senac.gerenciadordesalas.material;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import br.com.senac.gerenciadordesalas.erro.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MaterialServiceTest {

    @Mock
    MaterialRepository materiais;

    private MaterialService service;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        service = new MaterialService(materiais);
    }

    @Test
    void cadastraNormalizandoDados() {
        when(materiais.saveAndFlush(any(Material.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MaterialDto.Saida saida = service.criar(entrada("  Projetor  ", " pat-01 ", 3));

        assertThat(saida.nome()).isEqualTo("Projetor");
        assertThat(saida.patrimonio()).isEqualTo("PAT-01");
        assertThat(saida.quantidadeTotal()).isEqualTo(3);
        assertThat(saida.ativo()).isTrue();
    }

    @Test
    void patrimonioVazioViraNulo() {
        when(materiais.saveAndFlush(any(Material.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MaterialDto.Saida saida = service.criar(entrada("Cabo HDMI", "   ", 10));

        assertThat(saida.patrimonio()).isNull();
        verify(materiais, never()).existsByPatrimonio(any());
    }

    @Test
    void rejeitaPatrimonioDuplicadoSemPersistir() {
        when(materiais.existsByPatrimonio("PAT-01")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(entrada("Projetor", "pat-01", 1)))
                .isInstanceOf(ConflitoDeEstadoException.class);

        verify(materiais, never()).saveAndFlush(any());
    }

    @Test
    void entidadeRejeitaQuantidadeNegativa() {
        assertThatThrownBy(() -> new Material(
                "Projetor", null, MaterialTipo.EQUIPAMENTO, "PAT-01", -1, "Almoxarifado", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativa");
    }

    @Test
    void desativacaoPreservaRegistro() {
        Material material = materialAtivo();
        when(materiais.findById(1L)).thenReturn(Optional.of(material));
        when(materiais.saveAndFlush(material)).thenReturn(material);

        service.desativar(1L);

        assertThat(material.isAtivo()).isFalse();
        verify(materiais, never()).delete(any());
    }

    @Test
    void materialInativoNaoPodeSerConsultadoPelaApi() {
        when(materiais.findByIdAndAtivoTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consultar(1L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void listaComFiltrosDeTipoERestricao() {
        when(materiais.buscarAtivos(MaterialTipo.EQUIPAMENTO, true)).thenReturn(List.of(materialAtivo()));

        List<MaterialDto.Saida> resultado = service.listar(MaterialTipo.EQUIPAMENTO, true);

        assertThat(resultado).hasSize(1);
        verify(materiais).buscarAtivos(MaterialTipo.EQUIPAMENTO, true);
    }

    @Test
    void conflitoConcorrenteNaoSobrescreveEstado() {
        Material material = materialAtivo();
        when(materiais.findById(1L)).thenReturn(Optional.of(material));
        when(materiais.saveAndFlush(material))
                .thenThrow(new ObjectOptimisticLockingFailureException(Material.class, 1L));

        assertThatThrownBy(() -> service.atualizar(1L, entrada("Projetor novo", "PAT-01", 4)))
                .isInstanceOf(ConflitoDeEstadoException.class)
                .hasMessageContaining("outra operacao");
    }

    private MaterialDto.Entrada entrada(String nome, String patrimonio, int quantidade) {
        return new MaterialDto.Entrada(
                nome,
                "  Projetor compartilhado  ",
                MaterialTipo.EQUIPAMENTO,
                patrimonio,
                quantidade,
                "  Almoxarifado A  ",
                true);
    }

    private Material materialAtivo() {
        return new Material(
                "Projetor",
                "Projetor compartilhado",
                MaterialTipo.EQUIPAMENTO,
                "PAT-01",
                3,
                "Almoxarifado A",
                true);
    }
}
