package br.com.senac.gerenciadordesalas.usuario;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import br.com.senac.gerenciadordesalas.erro.RecursoNaoEncontradoException;
import br.com.senac.gerenciadordesalas.auditoria.AuditService;
import br.com.senac.gerenciadordesalas.auditoria.TipoAcaoAuditoria;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Administração de usuários (ENT-02). Nunca apaga usuário: a inativação preserva o histórico.
 */
@Service
public class UsuarioAdminService {

    private final UserRepository usuarios;
    private final PasswordEncoder codificador;

    private final AuditService auditoria;

    public UsuarioAdminService(UserRepository usuarios, PasswordEncoder codificador, AuditService auditoria) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.auditoria = auditoria;
    }

    @Transactional
    public UsuarioDto.Saida criar(UsuarioDto.Entrada entrada, String ator) {
        String email = User.normalizarEmail(entrada.email());
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw new ConflitoDeEstadoException("E-mail ja cadastrado");
        }
        User usuario = new User(entrada.nome().trim(), email, codificador.encode(entrada.senha()), entrada.perfil());
        try {
            usuario = usuarios.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflitoDeEstadoException("E-mail ja cadastrado");
        }
        registrar(usuario, TipoAcaoAuditoria.CRIACAO, ator, null, usuario.getProfile().name());
        return UsuarioDto.Saida.de(usuario);
    }

    @Transactional(readOnly = true)
    public List<UsuarioDto.Saida> listar(Role perfil, Boolean ativo) {
        return usuarios.findAll().stream()
                .filter(u -> perfil == null || u.getProfile() == perfil)
                .filter(u -> ativo == null || u.isActive() == ativo)
                .sorted(Comparator.comparing((User u) -> u.getName().toLowerCase(Locale.ROOT))
                        .thenComparing(User::getEmail))
                .map(UsuarioDto.Saida::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<UsuarioDto.Saida> paginar(Pageable pagina) {
        return usuarios.findAllBy(pagina).map(UsuarioDto.Saida::de);
    }

    @Transactional(readOnly = true)
    public UsuarioDto.Saida consultar(Long id) {
        return UsuarioDto.Saida.de(obter(id));
    }

    @Transactional
    public UsuarioDto.Saida atualizar(Long id, UsuarioDto.Atualizacao atualizacao, String ator) {
        List<User> administradores = usuarios.bloquearAtivosDoPerfil(Role.ADMINISTRADOR);
        User usuario = usuarios.bloquearPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));
        Role perfilAnterior = usuario.getProfile();
        boolean deixaDeSerAdministrador = usuario.isActive()
                && usuario.getProfile() == Role.ADMINISTRADOR
                && atualizacao.perfil() != Role.ADMINISTRADOR;
        if (deixaDeSerAdministrador) {
            garantirOutroAdministradorAtivo(administradores, "Nao e possivel remover o perfil do ultimo administrador ativo");
        }
        usuario.setName(atualizacao.nome().trim());
        usuario.setProfile(atualizacao.perfil());
        usuario = usuarios.saveAndFlush(usuario);
        registrar(usuario, TipoAcaoAuditoria.ALTERACAO, ator, perfilAnterior.name(), usuario.getProfile().name());
        return UsuarioDto.Saida.de(usuario);
    }

    @Transactional
    public UsuarioDto.Saida inativar(Long id, String ator) {
        List<User> administradores = usuarios.bloquearAtivosDoPerfil(Role.ADMINISTRADOR);
        User usuario = usuarios.bloquearPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));
        if (!usuario.isActive()) {
            throw new ConflitoDeEstadoException("Usuario ja esta inativo");
        }
        if (usuario.getProfile() == Role.ADMINISTRADOR) {
            garantirOutroAdministradorAtivo(administradores, "Nao e possivel inativar o ultimo administrador ativo");
        }
        usuario.deactivate();
        usuario = usuarios.saveAndFlush(usuario);
        registrar(usuario, TipoAcaoAuditoria.INATIVACAO, ator, "ATIVO", "INATIVO");
        return UsuarioDto.Saida.de(usuario);
    }

    @Transactional
    public UsuarioDto.Saida ativar(Long id, String ator) {
        usuarios.bloquearAtivosDoPerfil(Role.ADMINISTRADOR);
        User usuario = usuarios.bloquearPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));
        if (usuario.isActive()) {
            throw new ConflitoDeEstadoException("Usuario ja esta ativo");
        }
        usuario.activate();
        usuario = usuarios.saveAndFlush(usuario);
        registrar(usuario, TipoAcaoAuditoria.ALTERACAO, ator, "INATIVO", "ATIVO");
        return UsuarioDto.Saida.de(usuario);
    }

    /**
     * Bloqueia as linhas dos administradores ativos (SELECT ... FOR UPDATE) para que duas decisões
     * simultâneas não deixem o sistema sem nenhum administrador. Quem chama já é um administrador ativo.
     */
    private void garantirOutroAdministradorAtivo(List<User> administradores, String mensagem) {
        if (administradores.size() <= 1) {
            throw new ConflitoDeEstadoException(mensagem);
        }
    }

    private User obter(Long id) {
        return usuarios.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));
    }

    private void registrar(User usuario, TipoAcaoAuditoria acao, String emailAtor, String anterior, String novo) {
        User ator = usuarios.findByEmailIgnoreCase(emailAtor).orElse(null);
        auditoria.registrar("USUARIO", usuario.getId(), acao, ator, anterior, novo, Map.of());
    }
}
