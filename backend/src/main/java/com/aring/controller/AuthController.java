package com.aring.controller;

import com.aring.dto.AuthResponse;
import com.aring.dto.LoginRequest;
import com.aring.dto.TecnicoDto;
import com.aring.exception.ApiException;
import com.aring.model.Admin;
import com.aring.model.SuperUsuario;
import com.aring.model.Tecnico;
import com.aring.repository.AdminRepository;
import com.aring.repository.SuperUsuarioRepository;
import com.aring.repository.TecnicoRepository;
import com.aring.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AdminRepository adminRepository;
    private final SuperUsuarioRepository superUsuarioRepository;
    private final TecnicoRepository tecnicoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(AdminRepository adminRepository,
                          SuperUsuarioRepository superUsuarioRepository,
                          TecnicoRepository tecnicoRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.adminRepository = adminRepository;
        this.superUsuarioRepository = superUsuarioRepository;
        this.tecnicoRepository = tecnicoRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        Admin admin = adminRepository.findByUsername(request.getUsername()).orElse(null);
        if (admin != null) {
            if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
                throw new ApiException("Usuario o contraseña incorrectos.", HttpStatus.UNAUTHORIZED);
            }
            String token = jwtUtil.generateToken(admin);
            return new AuthResponse(token, admin.getNombre(), admin.getApellido(),
                    TecnicoDto.iniciales(admin.getNombre(), admin.getApellido()),
                    admin.getUsername(), "ROLE_ADMIN", null);
        }

        SuperUsuario superUsuario = superUsuarioRepository.findByUsername(request.getUsername()).orElse(null);
        if (superUsuario != null) {
            if (!passwordEncoder.matches(request.getPassword(), superUsuario.getPassword())) {
                throw new ApiException("Usuario o contraseña incorrectos.", HttpStatus.UNAUTHORIZED);
            }
            String token = jwtUtil.generateToken(superUsuario);
            return new AuthResponse(token, superUsuario.getNombre(), superUsuario.getApellido(),
                    TecnicoDto.iniciales(superUsuario.getNombre(), superUsuario.getApellido()),
                    superUsuario.getUsername(), "ROLE_SUPER", null);
        }

        Tecnico tecnico = tecnicoRepository.findByRut(request.getUsername()).orElse(null);
        if (tecnico != null) {
            if (!passwordEncoder.matches(request.getPassword(), tecnico.getPassword())) {
                throw new ApiException("Usuario o contraseña incorrectos.", HttpStatus.UNAUTHORIZED);
            }
            String token = jwtUtil.generateToken(tecnico);
            return new AuthResponse(token, tecnico.getNombre(), tecnico.getApellido(),
                    TecnicoDto.iniciales(tecnico.getNombre(), tecnico.getApellido()),
                    tecnico.getRut(), "ROLE_TECNICO", tecnico.getId());
        }

        throw new ApiException("Usuario o contraseña incorrectos.", HttpStatus.UNAUTHORIZED);
    }
}
