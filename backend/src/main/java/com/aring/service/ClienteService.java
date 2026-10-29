package com.aring.service;

import com.aring.dto.ClienteRequest;
import com.aring.dto.ClienteUpdateRequest;
import com.aring.exception.ApiException;
import com.aring.model.Cliente;
import com.aring.repository.ClienteRepository;
import com.aring.repository.TareaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
public class ClienteService {

    /** Máximo de la imagen ya decodificada: 5 MB. */
    private static final int MAX_IMAGEN_BYTES = 5 * 1024 * 1024;

    private final ClienteRepository clienteRepository;
    private final TareaRepository tareaRepository;

    public ClienteService(ClienteRepository clienteRepository, TareaRepository tareaRepository) {
        this.clienteRepository = clienteRepository;
        this.tareaRepository = tareaRepository;
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar() {
        return clienteRepository.findAllByOrderByFechaCreacionDesc();
    }

    @Transactional
    public Cliente crear(ClienteRequest request) {
        Cliente cliente = new Cliente();
        aplicarDatos(cliente, request.getDireccion(), request.getNombreRecinto());
        aplicarImagen(cliente, request.getImagenBase64());
        cliente.setFechaCreacion(Instant.now());
        return clienteRepository.save(cliente);
    }

    @Transactional
    public Cliente actualizar(Long id, ClienteUpdateRequest request) {
        Cliente cliente = buscarOError(id);
        aplicarDatos(cliente, request.getDireccion(), request.getNombreRecinto());
        if (request.getImagenBase64() != null && !request.getImagenBase64().isBlank()) {
            aplicarImagen(cliente, request.getImagenBase64());
        }
        return clienteRepository.save(cliente);
    }

    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = buscarOError(id);
        if (tareaRepository.existsByClienteId(id)) {
            throw new ApiException("No se puede eliminar: el cliente tiene tareas asociadas.", HttpStatus.CONFLICT);
        }
        clienteRepository.delete(cliente);
    }

    private void aplicarDatos(Cliente cliente, String direccion, String nombreRecinto) {
        cliente.setDireccion(direccion.trim());
        cliente.setNombreRecinto(nombreRecinto.trim());
    }

    private void aplicarImagen(Cliente cliente, String imagenBase64) {
        if (imagenBase64 == null || imagenBase64.isBlank()) {
            return;
        }
        String base64 = limpiarDataUri(imagenBase64);
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new ApiException("La imagen adjuntada no es válida.", HttpStatus.BAD_REQUEST);
        }
        if (bytes.length > MAX_IMAGEN_BYTES) {
            throw new ApiException("La imagen supera el tamaño máximo permitido (5 MB).", HttpStatus.BAD_REQUEST);
        }
        String tipo = detectarTipoImagen(bytes);
        if (tipo == null) {
            throw new ApiException("El archivo seleccionado no es una imagen válida (solo JPG, JPEG o PNG).", HttpStatus.BAD_REQUEST);
        }
        cliente.setImagenBase64(base64);
        cliente.setImagenTipo(tipo);
    }

    private String limpiarDataUri(String imagen) {
        String valor = imagen.trim();
        String prefijo = "data:image/";
        if (valor.regionMatches(true, 0, prefijo, 0, prefijo.length())) {
            int separador = valor.indexOf(";base64,");
            if (separador >= 0) {
                return valor.substring(separador + ";base64,".length());
            }
        }
        return valor;
    }

    private String detectarTipoImagen(byte[] bytes) {
        if (bytes.length >= 3
                && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) {
            return "jpeg";
        }
        if (bytes.length >= 4
                && bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return "png";
        }
        return null;
    }

    private Cliente buscarOError(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ApiException("El cliente no existe.", HttpStatus.NOT_FOUND));
    }
}
