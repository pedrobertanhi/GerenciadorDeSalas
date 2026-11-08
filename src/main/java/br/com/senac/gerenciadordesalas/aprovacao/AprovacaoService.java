package br.com.senac.gerenciadordesalas.aprovacao;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import br.com.senac.gerenciadordesalas.erro.RecursoNaoEncontradoException;
import br.com.senac.gerenciadordesalas.manutencao.MaintenanceBlockService;
import br.com.senac.gerenciadordesalas.manutencao.TipoRecurso;
import br.com.senac.gerenciadordesalas.material.Material;
import br.com.senac.gerenciadordesalas.professor.Professor;
import br.com.senac.gerenciadordesalas.reservas.Reservation;
import br.com.senac.gerenciadordesalas.reservas.ReservationDto;
import br.com.senac.gerenciadordesalas.reservas.ReservationMaterial;
import br.com.senac.gerenciadordesalas.reservas.ReservationRepository;
import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.usuario.Role;
import br.com.senac.gerenciadordesalas.usuario.User;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Fila e decisoes do Responsavel (APR-01 e APR-02, #34).
 *
 * <p>Antes de decidir, a reserva e travada e depois os seus recursos, na mesma ordem
 * global usada na alteracao (reserva, sala, professor, materiais). Assim duas decisoes
 * simultaneas acontecem uma de cada vez e a segunda enxerga a primeira. A aprovacao
 * revalida recursos ativos, manutencao e sobreposicao: um conflito surgido depois da
 * solicitacao impede a aprovacao.
 */
@Service
public class AprovacaoService {

    private final ReservationRepository reservas;
    private final UserRepository usuarios;
    private final MaintenanceBlockService manutencoes;
    private final ApplicationEventPublisher eventos;
    private final Clock relogio;

    public AprovacaoService(ReservationRepository reservas, UserRepository usuarios,
                            MaintenanceBlockService manutencoes, ApplicationEventPublisher eventos,
                            Clock relogio) {
        this.reservas = reservas;
        this.usuarios = usuarios;
        this.manutencoes = manutencoes;
        this.eventos = eventos;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<AprovacaoDto.ItemFila> listarPendentes() {
        return reservas.buscarPendentesDeDecisao().stream()
                .map(AprovacaoDto.ItemFila::de)
                .toList();
    }

    @Transactional
    public ReservationDto.Saida aprovar(Long reservaId, String emailResponsavel) {
        User responsavel = obterResponsavel(emailResponsavel);
        Reservation reserva = carregarTravada(reservaId);
        reserva.exigirAprovacaoRecursoPendente();
        revalidar(reserva);

        Instant agora = relogio.instant();
        reserva.aprovarRecurso(responsavel, agora);
        Reservation salva = salvar(reserva);
        eventos.publishEvent(new ReservaDecididaEvent(salva.getId(),
                ReservaDecididaEvent.TipoDecisao.APROVACAO_RECURSO,
                ReservaDecididaEvent.Resultado.APROVADA,
                responsavel.getId(), agora, null));
        return ReservationDto.Saida.de(salva);
    }

    @Transactional
    public ReservationDto.Saida rejeitar(Long reservaId, String motivo, String emailResponsavel) {
        User responsavel = obterResponsavel(emailResponsavel);
        Reservation reserva = carregarTravada(reservaId);

        Instant agora = relogio.instant();
        reserva.rejeitarRecurso(responsavel, agora, motivo);
        Reservation salva = salvar(reserva);
        eventos.publishEvent(new ReservaDecididaEvent(salva.getId(),
                ReservaDecididaEvent.TipoDecisao.APROVACAO_RECURSO,
                ReservaDecididaEvent.Resultado.REJEITADA,
                responsavel.getId(), agora, salva.getRecursoMotivoRejeicao()));
        return ReservationDto.Saida.de(salva);
    }

    private User obterResponsavel(String email) {
        User usuario = usuarios.findByEmailIgnoreCase(email)
                .filter(User::isActive)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario autenticado nao encontrado"));
        if (usuario.getProfile() != Role.RESPONSAVEL) {
            throw new AccessDeniedException("Somente o Responsavel decide reservas");
        }
        return usuario;
    }

    private Reservation carregarTravada(Long reservaId) {
        reservas.travarReserva(reservaId);
        Reservation reserva = reservas.findById(reservaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva nao encontrada"));
        reservas.travarSala(reserva.getSala().getId());
        if (reserva.getProfessor() != null) {
            reservas.travarProfessor(reserva.getProfessor().getId());
        }
        List<Long> materialIds = reserva.getMateriais().stream()
                .map(item -> item.getMaterial().getId())
                .distinct()
                .sorted()
                .toList();
        if (!materialIds.isEmpty()) {
            reservas.travarMateriais(materialIds);
        }
        return reserva;
    }

    private void revalidar(Reservation reserva) {
        Instant inicio = reserva.getInicio();
        Instant termino = reserva.getTermino();

        Sala sala = reserva.getSala();
        if (!Boolean.TRUE.equals(sala.getAtiva())) {
            throw new ConflitoDeEstadoException("A sala foi inativada; a reserva nao pode ser aprovada");
        }
        validarManutencao(TipoRecurso.SALA, sala.getId(), inicio, termino);
        if (reservas.buscarAtivasDaSalaSobrepondo(sala.getId(), inicio, termino).stream()
                .anyMatch(outra -> !Objects.equals(outra.getId(), reserva.getId()))) {
            throw new ConflitoDeEstadoException("Sala ja possui outra reserva no periodo informado");
        }

        Professor professor = reserva.getProfessor();
        if (professor != null) {
            if (!professor.isAtivo()) {
                throw new ConflitoDeEstadoException("O professor foi inativado; a reserva nao pode ser aprovada");
            }
            validarManutencao(TipoRecurso.PROFESSOR, professor.getId(), inicio, termino);
            if (reservas.buscarAtivasDoProfessorSobrepondo(professor.getId(), inicio, termino).stream()
                    .anyMatch(outra -> !Objects.equals(outra.getId(), reserva.getId()))) {
                throw new ConflitoDeEstadoException("Professor ja possui outra reserva no periodo informado");
            }
        }

        for (ReservationMaterial item : reserva.getMateriais()) {
            Material material = item.getMaterial();
            if (!material.isAtivo()) {
                throw new ConflitoDeEstadoException(
                        "O material " + material.getNome() + " foi inativado; a reserva nao pode ser aprovada");
            }
            validarManutencao(TipoRecurso.MATERIAL, material.getId(), inicio, termino);
            long outrasReservas = reservas.quantidadeAtivaDoMaterialSobrepondo(
                    material.getId(), inicio, termino, reserva.getId());
            if (outrasReservas + item.getQuantidade() > material.getQuantidadeTotal()) {
                throw new ConflitoDeEstadoException(
                        "Material " + material.getNome() + " nao possui quantidade disponivel no periodo informado");
            }
        }
    }

    private void validarManutencao(TipoRecurso tipo, Long recursoId, Instant inicio, Instant termino) {
        if (manutencoes.recursoEmManutencao(tipo, recursoId, inicio, termino)) {
            throw new ConflitoDeEstadoException("Recurso em manutencao no periodo informado");
        }
    }

    private Reservation salvar(Reservation reserva) {
        try {
            return reservas.saveAndFlush(reserva);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConflitoDeEstadoException("Reserva alterada por outra operacao");
        }
    }
}
