package br.com.senac.gerenciadordesalas.manutencao;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import br.com.senac.gerenciadordesalas.erro.RecursoNaoEncontradoException;
import br.com.senac.gerenciadordesalas.material.MaterialRepository;
import br.com.senac.gerenciadordesalas.professor.ProfessorRepository;
import br.com.senac.gerenciadordesalas.salas.SalaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class MaintenanceBlockService {

    private final MaintenanceBlockRepository bloqueios;
    private final SalaRepository salas;
    private final ProfessorRepository professores;
    private final MaterialRepository materiais;
    private final Clock relogio;

    @Autowired
    public MaintenanceBlockService(MaintenanceBlockRepository bloqueios, SalaRepository salas,
                                   ProfessorRepository professores, MaterialRepository materiais) {
        this(bloqueios, salas, professores, materiais, Clock.systemUTC());
    }

    MaintenanceBlockService(MaintenanceBlockRepository bloqueios, SalaRepository salas,
                            ProfessorRepository professores, MaterialRepository materiais, Clock relogio) {
        this.bloqueios = bloqueios;
        this.salas = salas;
        this.professores = professores;
        this.materiais = materiais;
        this.relogio = relogio;
    }

    @Transactional
    public BloqueioDto.Saida criar(BloqueioDto.Entrada entrada) {
        verificarRecursoExistente(entrada.recursoTipo(), entrada.recursoId());
        MaintenanceBlock bloqueio = new MaintenanceBlock(
                entrada.recursoTipo(),
                entrada.recursoId(),
                entrada.motivo().trim(),
                entrada.inicio().toInstant(),
                entrada.fim().toInstant(),
                relogio.instant());
        return salvar(bloqueio);
    }

    @Transactional
    public BloqueioDto.Saida atualizar(Long id, BloqueioDto.Entrada entrada) {
        MaintenanceBlock bloqueio = obter(id);
        if (bloqueio.getRecursoTipo() != entrada.recursoTipo()
                || !bloqueio.getRecursoId().equals(entrada.recursoId())) {
            throw new ConflitoDeEstadoException("O recurso de um bloqueio nao pode ser alterado");
        }
        bloqueio.atualizar(
                entrada.motivo().trim(),
                entrada.inicio().toInstant(),
                entrada.fim().toInstant(),
                relogio.instant());
        return salvar(bloqueio);
    }

    @Transactional
    public BloqueioDto.Saida concluir(Long id) {
        MaintenanceBlock bloqueio = obter(id);
        bloqueio.concluir(relogio.instant());
        return salvar(bloqueio);
    }

    @Transactional
    public BloqueioDto.Saida cancelar(Long id) {
        MaintenanceBlock bloqueio = obter(id);
        bloqueio.cancelar(relogio.instant());
        return salvar(bloqueio);
    }

    @Transactional(readOnly = true)
    public BloqueioDto.Saida consultar(Long id) {
        return BloqueioDto.Saida.de(obter(id));
    }

    @Transactional(readOnly = true)
    public List<BloqueioDto.Saida> listar(TipoRecurso recursoTipo, Long recursoId, StatusBloqueio status) {
        return bloqueios.buscar(recursoTipo, recursoId, status).stream()
                .map(BloqueioDto.Saida::de)
                .toList();
    }

    private MaintenanceBlock obter(Long id) {
        return bloqueios.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Bloqueio nao encontrado"));
    }

    private void verificarRecursoExistente(TipoRecurso tipo, Long id) {
        boolean existe = switch (tipo) {
            case SALA -> salas.existsById(id);
            case PROFESSOR -> professores.existsById(id);
            case MATERIAL -> materiais.existsById(id);
        };
        if (!existe) {
            throw new RecursoNaoEncontradoException("Recurso nao encontrado");
        }
    }

    private BloqueioDto.Saida salvar(MaintenanceBlock bloqueio) {
        try {
            return BloqueioDto.Saida.de(bloqueios.saveAndFlush(bloqueio));
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConflitoDeEstadoException("Bloqueio alterado por outra operacao");
        }
    }
}