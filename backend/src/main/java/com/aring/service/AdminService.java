package com.aring.service;

import com.aring.dto.AdminRequest;
import com.aring.exception.ApiException;
import com.aring.model.Admin;
import com.aring.repository.AdminRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Admin> listar() {
        return adminRepository.findAll();
    }

    @Transactional
    public Admin crear(AdminRequest request) {
        String username = request.getUsername().trim();
        if (adminRepository.existsByUsername(username)) {
            throw new ApiException("Ya existe un administrador con el usuario ingresado.", HttpStatus.CONFLICT);
        }
        Admin admin = new Admin();
        admin.setNombre(request.getNombre().trim());
        admin.setApellido(request.getApellido().trim());
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        return adminRepository.save(admin);
    }

    @Transactional
    public void cambiarPassword(Long id, String password) {
        Admin admin = buscarOError(id);
        admin.setPassword(passwordEncoder.encode(password));
        adminRepository.save(admin);
    }

    @Transactional
    public void eliminar(Long id) {
        Admin admin = buscarOError(id);
        adminRepository.delete(admin);
    }

    private Admin buscarOError(Long id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new ApiException("El administrador no existe.", HttpStatus.NOT_FOUND));
    }
}
