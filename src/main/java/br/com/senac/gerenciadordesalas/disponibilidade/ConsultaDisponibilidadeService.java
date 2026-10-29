package br.com.senac.gerenciadordesalas.disponibilidade;

import br.com.senac.gerenciadordesalas.manutencao.MaintenanceBlockRepository;
import br.com.senac.gerenciadordesalas.manutencao.TipoRecurso;
import br.com.senac.gerenciadordesalas.material.Material;
import br.com.senac.gerenciadordesalas.material.MaterialRepository;
import br.com.senac.gerenciadordesalas.professor.Professor;
import br.com.senac.gerenciadordesalas.professor.ProfessorRepository;
import br.com.senac.gerenciadordesalas.reservas.ReservationRepository;
import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.salas.SalaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Consulta recursos ativos e livres no intervalo semiaberto [inicio, termino). */
@Service
public class ConsultaDisponibilidadeService {
    private final SalaRepository salas;
    private final ProfessorRepository professores;
    private final MaterialRepository materiais;
    private final ReservationRepository reservas;
    private final MaintenanceBlockRepository manutencoes;

    public ConsultaDisponibilidadeService(SalaRepository salas, ProfessorRepository professores,
            MaterialRepository materiais, ReservationRepository reservas,
            MaintenanceBlockRepository manutencoes) {
        this.salas = salas;
        this.professores = professores;
        this.materiais = materiais;
        this.reservas = reservas;
        this.manutencoes = manutencoes;
    }

    @Transactional(readOnly = true)
    public RecursosDisponiveis consultar(IntervaloDisponibilidade intervalo) {
        List<Long> idsSalas = salas.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getAtiva()))
                .filter(s -> !bloqueado(TipoRecurso.SALA, s.getId(), intervalo))
                .filter(s -> reservas.buscarAtivasDaSalaSobrepondo(s.getId(),
                        intervalo.inicio(), intervalo.termino()).isEmpty())
                .map(Sala::getId).toList();
        List<Long> idsProfessores = professores.buscarAtivos(null).stream()
                .filter(p -> !bloqueado(TipoRecurso.PROFESSOR, p.getId(), intervalo))
                .filter(p -> reservas.buscarAtivasDoProfessorSobrepondo(p.getId(),
                        intervalo.inicio(), intervalo.termino()).isEmpty())
                .map(Professor::getId).toList();
        List<Long> idsMateriais = materiais.buscarAtivos(null, null).stream()
                .filter(m -> !bloqueado(TipoRecurso.MATERIAL, m.getId(), intervalo))
                .filter(m -> m.getQuantidadeTotal() > reservas.quantidadeAtivaDoMaterialSobrepondo(
                        m.getId(), intervalo.inicio(), intervalo.termino(), null))
                .map(Material::getId).toList();
        return new RecursosDisponiveis(idsSalas, idsProfessores, idsMateriais);
    }

    private boolean bloqueado(TipoRecurso tipo, Long id, IntervaloDisponibilidade intervalo) {
        return manutencoes.existeBloqueioNoPeriodo(tipo, id, intervalo.inicio(), intervalo.termino());
    }

    public record RecursosDisponiveis(List<Long> salas, List<Long> professores, List<Long> materiais) {}
}
