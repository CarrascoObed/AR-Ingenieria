package com.aring.service;

import com.aring.dto.ClienteRequest;
import com.aring.dto.ClienteUpdateRequest;
import com.aring.exception.ApiException;
import com.aring.model.Cliente;
import com.aring.repository.ClienteRepository;
import com.aring.repository.TareaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private TareaRepository tareaRepository;

    @InjectMocks
    private ClienteService clienteService;

    private Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setDireccion("Av. Providencia 1234. Providencia.");
        cliente.setNombreRecinto("Bodega Norte");
    }

    private static final byte[] PNG_BYTES = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3,
    };

    @Test
    void crearSinImagenOk() {
        ClienteRequest request = new ClienteRequest();
        request.setDireccion("  Av. Providencia 1234. Providencia.  ");
        request.setNombreRecinto(" Bodega Norte ");

        clienteService.crear(request);

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(captor.capture());
        assertNull(captor.getValue().getImagenBase64());
        assertNull(captor.getValue().getImagenTipo());
        assertEquals("Bodega Norte", captor.getValue().getNombreRecinto());
    }

    @Test
    void crearConImagenValida() {
        ClienteRequest request = new ClienteRequest();
        request.setDireccion("Av. Providencia 1234");
        request.setNombreRecinto("Bodega Norte");
        request.setImagenBase64("data:image/png;base64,"
                + Base64.getEncoder().encodeToString(PNG_BYTES));

        clienteService.crear(request);

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(captor.capture());
        assertEquals("png", captor.getValue().getImagenTipo());
        assertArrayEquals(PNG_BYTES,
                Base64.getDecoder().decode(captor.getValue().getImagenBase64()));
    }

    @Test
    void crearConArchivoNoImagenError() {
        ClienteRequest request = new ClienteRequest();
        request.setDireccion("Av. Providencia 1234");
        request.setNombreRecinto("Bodega Norte");
        request.setImagenBase64(Base64.getEncoder().encodeToString(new byte[]{1, 2, 3, 4}));

        ApiException excepcion = assertThrows(ApiException.class, () -> clienteService.crear(request));

        assertEquals("El archivo seleccionado no es una imagen válida (solo JPG, JPEG o PNG).",
                excepcion.getMessage());
        verify(clienteRepository, never()).save(cliente);
    }

    @Test
    void actualizarConservaImagenCuandoNoVienenDatos() {
        cliente.setImagenBase64("abc123");
        cliente.setImagenTipo("jpeg");
        ClienteUpdateRequest request = new ClienteUpdateRequest();
        request.setDireccion("Av. Nueva 567");
        request.setNombreRecinto("Recinto Renovado");
        request.setImagenBase64(null);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        clienteService.actualizar(1L, request);

        assertEquals("Av. Nueva 567", cliente.getDireccion());
        assertEquals("Recinto Renovado", cliente.getNombreRecinto());
        assertEquals("abc123", cliente.getImagenBase64());
        assertEquals("jpeg", cliente.getImagenTipo());
    }

    @Test
    void actualizarReemplazaImagen() {
        ClienteUpdateRequest request = new ClienteUpdateRequest();
        request.setDireccion("Av. Nueva 567");
        request.setNombreRecinto("Recinto Renovado");
        request.setImagenBase64(Base64.getEncoder().encodeToString(PNG_BYTES));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        clienteService.actualizar(1L, request);

        assertEquals("png", cliente.getImagenTipo());
        assertArrayEquals(PNG_BYTES, Base64.getDecoder().decode(cliente.getImagenBase64()));
    }

    @Test
    void eliminarConTareasAsociadasError() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(tareaRepository.existsByClienteId(1L)).thenReturn(true);

        ApiException excepcion = assertThrows(ApiException.class, () -> clienteService.eliminar(1L));

        assertEquals("No se puede eliminar: el cliente tiene tareas asociadas.", excepcion.getMessage());
        verify(clienteRepository, never()).delete(cliente);
    }

    @Test
    void eliminarSinTareasOk() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(tareaRepository.existsByClienteId(1L)).thenReturn(false);

        clienteService.eliminar(1L);

        verify(clienteRepository).delete(cliente);
    }

    @Test
    void eliminarInexistenteError() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException excepcion = assertThrows(ApiException.class, () -> clienteService.eliminar(99L));

        assertEquals("El cliente no existe.", excepcion.getMessage());
    }

    @Test
    void actualizarInexistenteError() {
        ClienteUpdateRequest request = new ClienteUpdateRequest();
        request.setDireccion("Av. Nueva 567");
        request.setNombreRecinto("Recinto Renovado");
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException excepcion = assertThrows(ApiException.class,
                () -> clienteService.actualizar(99L, request));

        assertEquals("El cliente no existe.", excepcion.getMessage());
    }
}
