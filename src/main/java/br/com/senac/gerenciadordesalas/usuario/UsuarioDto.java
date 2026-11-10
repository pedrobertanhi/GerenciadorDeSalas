package br.com.senac.gerenciadordesalas.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Contratos da administração de usuários (ENT-02). A senha só existe na entrada e nunca volta em
 * nenhuma resposta: o banco guarda apenas o hash.
 */
public final class UsuarioDto {

    private UsuarioDto() {
    }

    @Schema(name = "UsuarioEntrada", description = "Cadastro de usuário pelo administrador; a senha é guardada somente como hash")
    public record Entrada(
            @Schema(description = "Nome completo", example = "Maria Responsável")
            @NotBlank @Size(max = 150) String nome,
            @Schema(description = "E-mail único, usado no login", example = "maria@exemplo.test", format = "email")
            @NotBlank @Email @Size(max = 254) String email,
            @Schema(description = "Senha inicial: de 10 a 72 caracteres ASCII, com ao menos uma letra e um número",
                    accessMode = Schema.AccessMode.WRITE_ONLY)
            @NotBlank
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9])[\\x20-\\x7E]{10,72}$",
                    message = "A senha deve ter de 10 a 72 caracteres ASCII, com ao menos uma letra e um número.")
            String senha,
            @Schema(description = "Perfil de acesso") @NotNull Role perfil) {
    }

    @Schema(name = "UsuarioAtualizacao", description = "Dados editáveis: nome e perfil. O e-mail e a senha não mudam por aqui")
    public record Atualizacao(
            @Schema(description = "Nome completo", example = "Maria Responsável")
            @NotBlank @Size(max = 150) String nome,
            @Schema(description = "Perfil de acesso") @NotNull Role perfil) {
    }

    @Schema(name = "UsuarioSaida", description = "Usuário sem nenhum dado de credencial")
    public record Saida(Long id, String nome, String email, Role perfil, boolean ativo,
                        Instant criadoEm, Instant atualizadoEm) {

        public static Saida de(User usuario) {
            return new Saida(usuario.getId(), usuario.getName(), usuario.getEmail(), usuario.getProfile(),
                    usuario.isActive(), usuario.getCreatedAt(), usuario.getUpdatedAt());
        }
    }
}
