package br.com.senac.gerenciadordesalas.reservas;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import br.com.senac.gerenciadordesalas.erro.RecursoNaoEncontradoException;
import br.com.senac.gerenciadordesalas.material.Material;
import br.com.senac.gerenciadordesalas.manutencao.MaintenanceBlockService;
import br.com.senac.gerenciadordesalas.manutencao.TipoRecurso;
import br.com.senac.gerenciadordesalas.material.MaterialRepository;
import br.com.senac.gerenciadordesalas.professor.Professor;
import br.com.senac.gerenciadordesalas.professor.ProfessorRepository;
import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.salas.SalaRepository;
import br.com.senac.gerenciadordesalas.usuario.Role;
import br.com.senac.gerenciadordesalas.usuario.User;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Comparator;

/**
 * Alteracao e cancelamento preservando o historico da reserva (RSV-03, #25).
 * Deteccao de sobreposicao e disponibilidade por periodo ficam pendentes das
 * dependencias RSV-06, RSV-07 e RSV-08.
 */
@Service
public class ReservationManagementService {

    private final ReservationRepository reservas;
    private final UserRepository usuarios;
    private final SalaRepository salas;
    private final ProfessorRepository professores;
    private final MaterialRepository materiais;
    private final ReservationTimePolicy politicaTemporal;
    private final MaintenanceBlockService manutencoes;

    public ReservationManagementService(ReservationRepository reservas, UserRepository usuarios,
                                        SalaRepository salas, ProfessorRepository professores,
                                        MaterialRepository materiais, ReservationTimePolicy politicaTemporal,
                                        MaintenanceBlockService manutencoes) {
        this.reservas = reservas;
        this.usuarios = usuarios;
        this.salas = salas;
        this.professores = professores;
        this.materiais = materiais;
        this.politicaTemporal = politicaTemporal;
        this.manutencoes = manutencoes;
    }

    @Transactional
    public ReservationDto.Saida alterar(Long reservaId, ReservationDto.Alteracao entrada, String emailAtor) {
        Reservation reserva = buscarAutorizada(reservaId, emailAtor);
        politicaTemporal.validarParaReserva(entrada.inicioInstant(), entrada.terminoInstant());
        Sala sala = buscarSalaAtiva(entrada.salaId());
        Professor professor = buscarProfessorAtivo(entrada.professorId());
        travarRecursos(sala.getId(), professor, entrada.materiaisOuVazio());
        validarManutencao(TipoRecurso.SALA, sala.getId(), entrada.inicioInstant(), entrada.terminoInstant());
        if (professor != null) {
            validarManutencao(TipoRecurso.PROFESSOR, professor.getId(), entrada.inicioInstant(), entrada.terminoInstant());
        }
        validarConflitos(sala, professor, entrada.inicioInstant(), entrada.terminoInstant(), reserva.getId());

        reserva.alterar(sala, professor, entrada.inicioInstant(), entrada.terminoInstant(), entrada.finalidade());
        substituirMateriais(reserva, entrada.materiaisOuVazio(),
                entrada.inicioInstant(), entrada.terminoInstant());
        return ReservationDto.Saida.de(reservas.save(reserva));
    }

    /** Histórico visível ao dono e aos perfis operacionais, sem expor reserva de outro solicitante. */
    @Transactional(readOnly = true)
    public List<ReservationDto.Saida> listarPara(String emailAtor) {
        User ator = obterAtor(emailAtor);
        List<Reservation> resultado = ator.getProfile() == Role.SOLICITANTE
                ? reservas.findBySolicitanteIdOrderByInicioDesc(ator.getId())
                : reservas.findAllByOrderByInicioDesc();
        return resultado.stream().map(ReservationDto.Saida::de).toList();
    }

    @Transactional(readOnly = true)
    public ReservationDto.Saida consultarPara(Long reservaId, String emailAtor) {
        return ReservationDto.Saida.de(buscarAutorizada(reservaId, emailAtor));
    }

    @Transactional
    public ReservationDto.Saida cancelar(Long reservaId, String emailAtor) {
        Reservation reserva = buscarAutorizada(reservaId, emailAtor);
        reserva.cancelar();
        return ReservationDto.Saida.de(reservas.save(reserva));
    }

    private Reservation buscarAutorizada(Long reservaId, String emailAtor) {
        User ator = obterAtor(emailAtor);

        Reservation reserva = reservas.findById(reservaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva nao encontrada"));

        boolean eDono = reserva.getSolicitante().getId().equals(ator.getId());
        boolean perfilOperacional = ator.getProfile() == Role.RESPONSAVEL
                || ator.getProfile() == Role.ADMINISTRADOR;
        if (!eDono && !perfilOperacional) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Somente o dono ou perfil autorizado pode alterar a reserva");
        }
        return reserva;
    }

    private User obterAtor(String emailAtor) {
        return usuarios.findByEmailIgnoreCase(emailAtor)
                .filter(User::isActive)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario autenticado nao encontrado"));
    }

    private Sala buscarSalaAtiva(Long salaId) {
        return salas.findById(salaId)
                .filter(sala -> Boolean.TRUE.equals(sala.getAtiva()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sala nao encontrada ou inativa"));
    }

    private Professor buscarProfessorAtivo(Long professorId) {
        if (professorId == null) {
            return null;
        }
        return professores.findById(professorId)
                .filter(Professor::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Professor nao encontrado ou inativo"));
    }

    private void substituirMateriais(Reservation reserva, List<ReservationDto.MaterialPedido> pedidos,
                                     java.time.Instant inicio, java.time.Instant termino) {
        List<ReservationMaterial> atuais = new ArrayList<>(reserva.getMateriais());
        atuais.forEach(reserva::removerMaterial);

        Set<Long> vistos = new HashSet<>();
        for (ReservationDto.MaterialPedido pedido : pedidos) {
            if (!vistos.add(pedido.materialId())) {
                throw new ConflitoDeEstadoException(
                        "Material " + pedido.materialId() + " aparece mais de uma vez na reserva");
            }
            Material material = materiais.findById(pedido.materialId())
                    .filter(Material::isAtivo)
                    .orElseThrow(() -> new RecursoNaoEncontradoException(
                            "Material " + pedido.materialId() + " nao encontrado ou inativo"));
            validarManutencao(TipoRecurso.MATERIAL, material.getId(), inicio, termino);
            if (pedido.quantidade() > material.getQuantidadeTotal()) {
                throw new ConflitoDeEstadoException(
                        "Quantidade pedida de " + material.getNome() + " excede o saldo disponivel");
            }
            validarDisponibilidadeMaterial(material, pedido.quantidade(), inicio, termino, reserva.getId());
            reserva.adicionarMaterial(material, pedido.quantidade());
        }
    }

    /**
     * Exclusao mutua (RSV-10, #27): trava sala, professor e materiais no banco antes
     * de validar conflitos. Uma reserva concorrente do mesmo recurso espera a atual
     * terminar e so entao valida, entao nao existe janela entre validacao e gravacao.
     * A ordem fixa (sala, professor, materiais por id) evita deadlock.
     */
    private void travarRecursos(Long salaId, Professor professor,
                                List<ReservationDto.MaterialPedido> pedidos) {
        reservas.travarSala(salaId);
        if (professor != null) {
            reservas.travarProfessor(professor.getId());
        }
        List<Long> materialIds = pedidos.stream()
                .map(ReservationDto.MaterialPedido::materialId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        if (!materialIds.isEmpty()) {
            reservas.travarMateriais(materialIds);
        }
    }

    private void validarManutencao(TipoRecurso tipo, Long recursoId, java.time.Instant inicio,
                                   java.time.Instant termino) {
        if (manutencoes.recursoEmManutencao(tipo, recursoId, inicio, termino)) {
            throw new ConflitoDeEstadoException("Recurso em manutencao no periodo informado");
        }
    }

    private void validarConflitos(Sala sala, Professor professor, java.time.Instant inicio,
                                  java.time.Instant termino, Long reservaIgnorada) {
        if (reservas.buscarAtivasDaSalaSobrepondo(sala.getId(), inicio, termino).stream()
                .anyMatch(reserva -> !java.util.Objects.equals(reserva.getId(), reservaIgnorada))) {
            throw new ConflitoDeEstadoException("Sala ja possui reserva no periodo informado");
        }
        if (professor != null && reservas.buscarAtivasDoProfessorSobrepondo(
                professor.getId(), inicio, termino).stream()
                .anyMatch(reserva -> !java.util.Objects.equals(reserva.getId(), reservaIgnorada))) {
            throw new ConflitoDeEstadoException("Professor ja possui reserva no periodo informado");
        }
    }

    private void validarDisponibilidadeMaterial(Material material, int quantidadeSolicitada,
                                                java.time.Instant inicio, java.time.Instant termino,
                                                Long reservaIgnorada) {
        long quantidadeReservada = reservas.quantidadeAtivaDoMaterialSobrepondo(
                material.getId(), inicio, termino, reservaIgnorada);
        if (quantidadeReservada + quantidadeSolicitada > material.getQuantidadeTotal()) {
            throw new ConflitoDeEstadoException(
                    "Material " + material.getNome() + " nao possui quantidade disponivel no periodo informado");
        }
    }
}
