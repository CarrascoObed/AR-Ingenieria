package com.aring.config;

import com.aring.model.PantallaConfig;
import com.aring.model.SecuenciaTarea;
import com.aring.model.SuperUsuario;
import com.aring.model.TipoTarea;
import com.aring.repository.PantallaConfigRepository;
import com.aring.repository.SecuenciaTareaRepository;
import com.aring.repository.SuperUsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final SuperUsuarioRepository superUsuarioRepository;
    private final PantallaConfigRepository pantallaConfigRepository;
    private final SecuenciaTareaRepository secuenciaTareaRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(SuperUsuarioRepository superUsuarioRepository,
                      PantallaConfigRepository pantallaConfigRepository,
                      SecuenciaTareaRepository secuenciaTareaRepository,
                      PasswordEncoder passwordEncoder) {
        this.superUsuarioRepository = superUsuarioRepository;
        this.pantallaConfigRepository = pantallaConfigRepository;
        this.secuenciaTareaRepository = secuenciaTareaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedSuperUsuario();
        seedPantallas();
        asegurarSecuencia();
    }

    private void seedSuperUsuario() {
        if (!superUsuarioRepository.existsByUsername("29094728")) {
            SuperUsuario superUsuario = new SuperUsuario();
            superUsuario.setNombre("Super");
            superUsuario.setApellido("Usuario");
            superUsuario.setUsername("29094728");
            superUsuario.setPassword(passwordEncoder.encode("18330707"));
            superUsuarioRepository.save(superUsuario);
            System.out.println("Super usuario creado: usuario=29094728");
        }
    }

    private void seedPantallas() {
        for (String nombre : List.of("DASHBOARD", "TECNICOS", "TAREAS", "DOCUMENTOS", "CLIENTES")) {
            if (pantallaConfigRepository.findByNombre(nombre).isEmpty()) {
                PantallaConfig config = new PantallaConfig();
                config.setNombre(nombre);
                config.setHabilitada(true);
                pantallaConfigRepository.save(config);
            }
        }
    }

    private void asegurarSecuencia() {
        if (secuenciaTareaRepository.findByTipo(TipoTarea.MANTENIMIENTO).isEmpty()) {
            SecuenciaTarea secuencia = new SecuenciaTarea();
            secuencia.setTipo(TipoTarea.MANTENIMIENTO);
            secuencia.setUltimoNumero(0L);
            secuenciaTareaRepository.save(secuencia);
        }
    }
}
