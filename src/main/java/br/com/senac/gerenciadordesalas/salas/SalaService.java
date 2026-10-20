package br.com.senac.gerenciadordesalas.salas;

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
            throw new IllegalArgumentException("Já existe uma sala cadastrada com este nome.");
        }
        return repository.save(sala);
    }

    public List<Sala> listarTodas() {
        return repository.findAll();
    }

    public Sala buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sala não encontrada."));
    }

    @Transactional
    public Sala atualizar(Long id, Sala dadosAtualizados) {
        Sala salaExistente = buscarPorId(id);

        if (repository.existsByNomeAndIdNot(dadosAtualizados.getNome(), id)) {
            throw new IllegalArgumentException("Já existe outra sala cadastrada com este nome.");
        }

        salaExistente.setNome(dadosAtualizados.getNome());
        salaExistente.setCapacidade(dadosAtualizados.getCapacidade());
        salaExistente.setLocalizacao(dadosAtualizados.getLocalizacao());
        // A inativação é tratada em um método separado para preservar histórico
        
        return repository.save(salaExistente);
    }

    @Transactional
    public void inativar(Long id) {
        Sala sala = buscarPorId(id);
        sala.setAtiva(false); // Inativa em vez de deletar, preservando o histórico
        repository.save(sala);
    }
}