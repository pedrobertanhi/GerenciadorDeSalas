package br.com.senac.gerenciadordesalas.reservas;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import br.com.senac.gerenciadordesalas.erro.RecursoNaoEncontradoException;
import br.com.senac.gerenciadordesalas.material.Material;
import br.com.senac.gerenciadordesalas.material.MaterialRepository;
import br.com.senac.gerenciadordesalas.professor.Professor;
import br.com.senac.gerenciadordesalas.professor.ProfessorRepository;
import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.salas.SalaRepository;
import br.com.senac.gerenciadordesalas.usuario.User;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Casos de uso de reserva (RSV-02, #24): criacao com validacao de recursos
 * ativos, dono vindo do usuario autenticado e materiais com quantidade coerente
 * com o saldo total. Reserva recem-criada entra em {@link StatusReserva#CRIADA}
 * para seguir o fluxo de aprovacao (RSV-04 e APR-01).
 */
@Service
public class ReservationService {

    private final ReservationRepository reservas;
    private final UserRepository usuarios;
    private final SalaRepository salas;
    private final ProfessorRepository professores;
    private final MaterialRepository materiais;
    private final ReservationTimePolicy politicaTemporal;

    public ReservationService(ReservationRepository reservas, UserRepository usuarios,
                              SalaRepository salas, ProfessorRepository professores,
                              MaterialRepository materiais, ReservationTimePolicy politicaTemporal) {
        this.reservas = reservas;
        this.usuarios = usuarios;
        this.salas = salas;
        this.professores = professores;
        this.materiais = materiais;
        this.politicaTemporal = politicaTemporal;
    }

    @Transactional
    public ReservationDto.Saida criar(ReservationDto.Entrada entrada, String emailSolicitante) {
        politicaTemporal.validarParaReserva(entrada.inicioInstant(), entrada.terminoInstant());
        User solicitante = usuarios.findByEmailIgnoreCase(emailSolicitante)
                .filter(User::isActive)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitante nao encontrado"));

        Sala sala = salas.findById(entrada.salaId())
                .filter(s -> Boolean.TRUE.equals(s.getAtiva()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sala nao encontrada ou inativa"));

        Professor professor = resolverProfessor(entrada.professorId());
        validarConflitos(sala, professor, entrada.inicioInstant(), entrada.terminoInstant(), null);

        Reservation reserva = new Reservation(
                solicitante, sala, professor,
                entrada.inicioInstant(), entrada.terminoInstant(),
                entrada.finalidade());

        Set<Long> materiaisVistos = new HashSet<>();
        for (ReservationDto.MaterialPedido pedido : entrada.materiaisOuVazio()) {
            if (!materiaisVistos.add(pedido.materialId())) {
                throw new ConflitoDeEstadoException(
                        "Material " + pedido.materialId() + " aparece mais de uma vez na reserva");
            }
            Material material = materiais.findById(pedido.materialId())
                    .filter(Material::isAtivo)
                    .orElseThrow(() -> new RecursoNaoEncontradoException(
                            "Material " + pedido.materialId() + " nao encontrado ou inativo"));
            if (pedido.quantidade() > material.getQuantidadeTotal()) {
                throw new ConflitoDeEstadoException(
                        "Quantidade pedida de " + material.getNome()
                                + " excede o saldo disponivel");
            }
            validarDisponibilidadeMaterial(material, pedido.quantidade(),
                    entrada.inicioInstant(), entrada.terminoInstant(), null);
            reserva.adicionarMaterial(material, pedido.quantidade());
        }

        Reservation salva = reservas.saveAndFlush(reserva);
        return ReservationDto.Saida.de(salva);
    }

    private Professor resolverProfessor(Long professorId) {
        if (professorId == null) {
            return null;
        }
        return professores.findById(professorId)
                .filter(Professor::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Professor nao encontrado ou inativo"));
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
