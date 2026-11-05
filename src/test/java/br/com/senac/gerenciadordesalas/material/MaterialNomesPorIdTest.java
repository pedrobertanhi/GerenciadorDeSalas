package br.com.senac.gerenciadordesalas.material;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Nomes de materiais por identificador (tela de manutenção, #15): o histórico continua mostrando
 * o material mesmo depois que ele é inativado.
 */
class MaterialNomesPorIdTest {

    private final MaterialRepository materiais = mock(MaterialRepository.class);
    private final MaterialService service = new MaterialService(materiais);

    private static Material material(Long id, String nome) {
        Material material = mock(Material.class);
        when(material.getId()).thenReturn(id);
        when(material.getNome()).thenReturn(nome);
        return material;
    }

    @Test
    void devolveONomeDeCadaMaterialPeloIdentificador() {
        Material projetor = material(1L, "Projetor");
        Material cabo = material(2L, "Cabo HDMI");
        when(materiais.findAllById(Set.of(1L, 2L))).thenReturn(List.of(projetor, cabo));

        assertThat(service.nomesPorId(Set.of(1L, 2L)))
                .containsExactlyInAnyOrderEntriesOf(Map.of(1L, "Projetor", 2L, "Cabo HDMI"));
    }

    @Test
    void incluiMaterialInativadoPoisNaoFiltraPorSituacao() {
        Material inativo = material(3L, "Cabo Antigo");
        when(inativo.isAtivo()).thenReturn(false);
        when(materiais.findAllById(Set.of(3L))).thenReturn(List.of(inativo));

        assertThat(service.nomesPorId(Set.of(3L))).containsEntry(3L, "Cabo Antigo");
        verify(materiais).findAllById(Set.of(3L));
    }

    @Test
    void identificadorSemCadastroNaoApareceNoResultado() {
        when(materiais.findAllById(Set.of(9L))).thenReturn(List.of());

        assertThat(service.nomesPorId(Set.of(9L))).isEmpty();
    }

    @Test
    void semIdentificadoresNaoConsultaOBanco() {
        assertThat(service.nomesPorId(Set.of())).isEmpty();

        verifyNoInteractions(materiais);
    }
}