package br.com.senac.gerenciadordesalas.material;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import br.com.senac.gerenciadordesalas.erro.RecursoNaoEncontradoException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class MaterialService {

    private final MaterialRepository materiais;

    public MaterialService(MaterialRepository materiais) {
        this.materiais = materiais;
    }

    @Transactional
    public MaterialDto.Saida criar(MaterialDto.Entrada entrada) {
        String patrimonio = normalizarPatrimonio(entrada.patrimonio());
        validarPatrimonioDuplicado(patrimonio, null);
        Material material = new Material(
                entrada.nome().trim(),
                normalizarOpcional(entrada.descricao()),
                entrada.tipo(),
                patrimonio,
                entrada.quantidadeTotal(),
                entrada.localizacao().trim(),
                entrada.restrito());
        return salvar(material);
    }

    @Transactional
    public MaterialDto.Saida atualizar(Long id, MaterialDto.Entrada entrada) {
        Material material = obter(id);
        String patrimonio = normalizarPatrimonio(entrada.patrimonio());
        validarPatrimonioDuplicado(patrimonio, id);
        material.atualizar(
                entrada.nome().trim(),
                normalizarOpcional(entrada.descricao()),
                entrada.tipo(),
                patrimonio,
                entrada.quantidadeTotal(),
                entrada.localizacao().trim(),
                entrada.restrito());
        return salvar(material);
    }

    @Transactional
    public void desativar(Long id) {
        obter(id).desativar();
    }

    @Transactional(readOnly = true)
    public MaterialDto.Saida consultar(Long id) {
        Material material = materiais.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Material nao encontrado"));
        return MaterialDto.Saida.de(material);
    }

    @Transactional(readOnly = true)
    public List<MaterialDto.Saida> listar(MaterialTipo tipo, Boolean restrito) {
        return materiais.buscarAtivos(tipo, restrito).stream()
                .map(MaterialDto.Saida::de)
                .toList();
    }

    private Material obter(Long id) {
        return materiais.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Material nao encontrado"));
    }

    private MaterialDto.Saida salvar(Material material) {
        try {
            return MaterialDto.Saida.de(materiais.saveAndFlush(material));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflitoDeEstadoException("Patrimonio ja cadastrado");
        }
    }

    private void validarPatrimonioDuplicado(String patrimonio, Long idAtual) {
        if (patrimonio == null) {
            return;
        }
        boolean duplicado = idAtual == null
                ? materiais.existsByPatrimonio(patrimonio)
                : materiais.existsByPatrimonioAndIdNot(patrimonio, idAtual);
        if (duplicado) {
            throw new ConflitoDeEstadoException("Patrimonio ja cadastrado");
        }
    }

    private String normalizarPatrimonio(String patrimonio) {
        String normalizado = normalizarOpcional(patrimonio);
        return normalizado == null ? null : normalizado.toUpperCase(Locale.ROOT);
    }

    private String normalizarOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
