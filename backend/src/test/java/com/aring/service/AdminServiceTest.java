package com.aring.service;

import com.aring.dto.AdminRequest;
import com.aring.exception.ApiException;
import com.aring.model.Admin;
import com.aring.repository.AdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AdminRepository adminRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminService adminService;

    private Admin admin;

    @BeforeEach
    void setUp() {
        admin = new Admin();
        admin.setId(1L);
        admin.setNombre("Luis");
        admin.setApellido("Rojas");
        admin.setUsername("12345678");
        admin.setPassword("hashed");
    }

    @Test
    void crearConUsernameDuplicadoLanzaError() {
        AdminRequest request = new AdminRequest();
        request.setNombre("Ana");
        request.setApellido("Soto");
        request.setUsername("12345678");
        request.setPassword("clave");
        when(adminRepository.existsByUsername("12345678")).thenReturn(true);

        assertThrows(ApiException.class, () -> adminService.crear(request));
    }

    @Test
    void crearOkEncriptaPassword() {
        AdminRequest request = new AdminRequest();
        request.setNombre("Ana");
        request.setApellido("Soto");
        request.setUsername(" 11111111 ");
        request.setPassword("clave");
        when(adminRepository.existsByUsername("11111111")).thenReturn(false);
        when(passwordEncoder.encode("clave")).thenReturn("encriptada");
        when(adminRepository.save(any(Admin.class))).thenAnswer(inv -> inv.getArgument(0));

        Admin creado = adminService.crear(request);

        assertEquals("11111111", creado.getUsername(), "El username debe guardarse sin espacios");
        assertEquals("encriptada", creado.getPassword());
    }

    @Test
    void cambiarPasswordOk() {
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(passwordEncoder.encode("nueva")).thenReturn("nueva-encriptada");

        adminService.cambiarPassword(1L, "nueva");

        assertEquals("nueva-encriptada", admin.getPassword());
        verify(adminRepository).save(admin);
    }

    @Test
    void cambiarPasswordAdminInexistenteLanzaError() {
        when(adminRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> adminService.cambiarPassword(99L, "nueva"));
    }

    @Test
    void eliminarOk() {
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));

        adminService.eliminar(1L);

        verify(adminRepository).delete(admin);
    }

    @Test
    void eliminarAdminInexistenteLanzaError() {
        when(adminRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> adminService.eliminar(99L));
    }
}
