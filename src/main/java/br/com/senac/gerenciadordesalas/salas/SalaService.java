package br.com.senac.gerenciadordesalas.salas;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SalaService {

    private final SalaRepository repository;

    public SalaService(SalaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Sala criar(Sala sala) {
        if (repository.existsByNome(sala.getNome())) {
            throw new EntityExistsException("Já existe uma sala cadastrada com este nome.");
        }
        return repository.save(sala);
    }

    public List<Sala> listarTodas() {
        return repository.findAll();
    }

    public Sala buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sala não encontrada."));
    }

    public Page<SalaResponseDTO> pesquisar(SalaFiltroDTO filtro, Pageable pageable) {
        Specification<Sala> spec = SalaSpecification.comFiltros(filtro);
        Page<Sala> salas = repository.findAll(spec, pageable);
        return salas.map(SalaResponseDTO::new);
    }

    @Transactional
    public Sala atualizar(Long id, Sala dadosAtualizados) {
        Sala salaExistente = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sala não encontrada."));

        if (repository.existsByNomeAndIdNot(dadosAtualizados.getNome(), id)) {
            throw new EntityExistsException("Já existe outra sala cadastrada com este nome.");
        }

        if (!salaExistente.getAtiva()) {
            throw new IllegalStateException("Não é possível atualizar uma sala que já foi inativada.");
        }

        salaExistente.setNome(dadosAtualizados.getNome());
        salaExistente.setCapacidade(dadosAtualizados.getCapacidade());
        salaExistente.setLocalizacao(dadosAtualizados.getLocalizacao());
        
        return repository.save(salaExistente);
    }

    @Transactional
    public void inativar(Long id) {
        Sala sala = buscarPorId(id);
        sala.setAtiva(false);
        repository.save(sala);
    }
}