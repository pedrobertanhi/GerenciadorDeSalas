package br.com.senac.gerenciadordesalas.reservas;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import br.com.senac.gerenciadordesalas.erro.RecursoNaoEncontradoException;
import br.com.senac.gerenciadordesalas.material.Material;
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
import java.util.Set;

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

    public ReservationManagementService(ReservationRepository reservas, UserRepository usuarios,
                                        SalaRepository salas, ProfessorRepository professores,
                                        MaterialRepository materiais) {
        this.reservas = reservas;
        this.usuarios = usuarios;
        this.salas = salas;
        this.professores = professores;
        this.materiais = materiais;
    }

    @Transactional
    public ReservationDto.Saida alterar(Long reservaId, ReservationDto.Alteracao entrada, String emailAtor) {
        Reservation reserva = buscarAutorizada(reservaId, emailAtor);
        Sala sala = buscarSalaAtiva(entrada.salaId());
        Professor professor = buscarProfessorAtivo(entrada.professorId());

        reserva.alterar(sala, professor, entrada.inicioInstant(), entrada.terminoInstant(), entrada.finalidade());
        substituirMateriais(reserva, entrada.materiaisOuVazio());
        return ReservationDto.Saida.de(reservas.save(reserva));
    }

    @Transactional
    public ReservationDto.Saida cancelar(Long reservaId, String emailAtor) {
        Reservation reserva = buscarAutorizada(reservaId, emailAtor);
        reserva.cancelar();
        return ReservationDto.Saida.de(reservas.save(reserva));
    }

    private Reservation buscarAutorizada(Long reservaId, String emailAtor) {
        User ator = usuarios.findByEmailIgnoreCase(emailAtor)
                .filter(User::isActive)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario autenticado nao encontrado"));

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

    private void substituirMateriais(Reservation reserva, List<ReservationDto.MaterialPedido> pedidos) {
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
            if (pedido.quantidade() > material.getQuantidadeTotal()) {
                throw new ConflitoDeEstadoException(
                        "Quantidade pedida de " + material.getNome() + " excede o saldo disponivel");
            }
            reserva.adicionarMaterial(material, pedido.quantidade());
        }
    }
}
