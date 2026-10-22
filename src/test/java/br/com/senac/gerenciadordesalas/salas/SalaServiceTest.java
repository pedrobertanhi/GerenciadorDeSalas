package br.com.senac.gerenciadordesalas.salas;

import jakarta.persistence.EntityExistsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SalaServiceTest {

    @Mock
    private SalaRepository repository;

    @InjectMocks
    private SalaService service;

    @Test
    void deveCriarSalaQuandoNomeNaoExistir() {
        Sala sala = new Sala("Sala Nova", 20, "Térreo");
        when(repository.existsByNome("Sala Nova")).thenReturn(false);
        when(repository.save(any(Sala.class))).thenReturn(sala);

        Sala salaCriada = service.criar(sala);

        assertNotNull(salaCriada);
        verify(repository, times(1)).save(sala);
    }

    @Test
    void deveLancarExcecaoAoCriarSalaComNomeDuplicado() {
        Sala sala = new Sala("Sala Duplicada", 20, "Térreo");
        when(repository.existsByNome("Sala Duplicada")).thenReturn(true);

        // Atualizado para esperar a nova exceção de conflito
        assertThrows(EntityExistsException.class, () -> service.criar(sala));
        verify(repository, never()).save(any());
    }
}