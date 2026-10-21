package br.com.senac.gerenciadordesalas.salas;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
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
            // Lança EntityExistsException para que o Spring devolva 409 Conflict em vez de 500
            throw new EntityExistsException("Já existe uma sala cadastrada com este nome.");
        }
        return repository.save(sala);
    }

    public List<Sala> listarTodas() {
        return repository.findAll();
    }

    public Sala buscarPorId(Long id) {
        return repository.findById(id)
                // Lança EntityNotFoundException para que o Spring devolva 404 Not Found
                .orElseThrow(() -> new EntityNotFoundException("Sala não encontrada."));
    }

    @Transactional
    public Sala atualizar(Long id, Sala dadosAtualizados) {
        // Recarrega a sala do banco para garantir que temos a versão mais recente
        Sala salaExistente = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sala não encontrada."));

        if (repository.existsByNomeAndIdNot(dadosAtualizados.getNome(), id)) {
            throw new EntityExistsException("Já existe outra sala cadastrada com este nome.");
        }

        // Bloqueia a atualização se a sala tiver sido inativada concorrentemente
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