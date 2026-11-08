package br.com.senac.gerenciadordesalas.aprovacao;

import br.com.senac.gerenciadordesalas.reservas.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Fila de aprovacao do Responsavel (APR-01, #34). A restricao de acesso por perfil
 * fica no SecurityConfig; aqui so se monta a consulta de pendencias.
 */
@Service
public class AprovacaoService {

    private final ReservationRepository reservas;

    public AprovacaoService(ReservationRepository reservas) {
        this.reservas = reservas;
    }

    @Transactional(readOnly = true)
    public List<AprovacaoDto.ItemFila> listarPendentes() {
        return reservas.buscarPendentesDeDecisao().stream()
                .map(AprovacaoDto.ItemFila::de)
                .toList();
    }
}
