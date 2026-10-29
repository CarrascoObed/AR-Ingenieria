package com.aring.config;

import com.aring.model.Admin;
import com.aring.model.EstadoTarea;
import com.aring.model.PantallaConfig;
import com.aring.model.SecuenciaTarea;
import com.aring.model.SuperUsuario;
import com.aring.model.Tecnico;
import com.aring.model.TipoTarea;
import com.aring.repository.AdminRepository;
import com.aring.repository.PantallaConfigRepository;
import com.aring.repository.SecuenciaTareaRepository;
import com.aring.repository.SuperUsuarioRepository;
import com.aring.repository.TareaRepository;
import com.aring.repository.TecnicoRepository;
import com.aring.service.TareaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final SuperUsuarioRepository superUsuarioRepository;
    private final PantallaConfigRepository pantallaConfigRepository;
    private final TecnicoRepository tecnicoRepository;
    private final TareaRepository tareaRepository;
    private final SecuenciaTareaRepository secuenciaTareaRepository;
    private final TareaService tareaService;
    private final PasswordEncoder passwordEncoder;
    private final boolean seedDemoData;

    public DataSeeder(AdminRepository adminRepository,
                      SuperUsuarioRepository superUsuarioRepository,
                      PantallaConfigRepository pantallaConfigRepository,
                      TecnicoRepository tecnicoRepository,
                      TareaRepository tareaRepository,
                      SecuenciaTareaRepository secuenciaTareaRepository,
                      TareaService tareaService,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.seed-demo-data:true}") boolean seedDemoData) {
        this.adminRepository = adminRepository;
        this.superUsuarioRepository = superUsuarioRepository;
        this.pantallaConfigRepository = pantallaConfigRepository;
        this.tecnicoRepository = tecnicoRepository;
        this.tareaRepository = tareaRepository;
        this.secuenciaTareaRepository = secuenciaTareaRepository;
        this.tareaService = tareaService;
        this.passwordEncoder = passwordEncoder;
        this.seedDemoData = seedDemoData;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin();
        seedSuperUsuario();
        seedPantallas();
        asegurarSecuencia();
        if (seedDemoData) {
            seedDemo();
        }
    }

    private void seedAdmin() {
        if (!adminRepository.existsByUsername("12345678")) {
            Admin admin = new Admin();
            admin.setNombre("Luis");
            admin.setApellido("Rojas");
            admin.setUsername("12345678");
            admin.setPassword(passwordEncoder.encode("45678"));
            adminRepository.save(admin);
            System.out.println("Administrador creado: usuario=12345678");
        }
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

    private void seedDemo() {
        if (tecnicoRepository.count() > 0 || tareaRepository.count() > 0) {
            return;
        }

        Tecnico juan = new Tecnico();
        juan.setNombre("Juan");
        juan.setApellido("Pérez");
        juan.setRut("12345678-9");
        juan.setPassword(passwordEncoder.encode("tecnico1"));
        juan.setContactoNombre("Carlos");
        juan.setContactoApellido("Pérez");
        juan.setContactoTelefono("+56 9 1111 2222");
        juan.setFechaCreacion(Instant.now());
        tecnicoRepository.save(juan);

        Tecnico maria = new Tecnico();
        maria.setNombre("María");
        maria.setApellido("González");
        maria.setRut("98765432-1");
        maria.setPassword(passwordEncoder.encode("tecnico2"));
        maria.setContactoNombre("Pedro");
        maria.setContactoApellido("González");
        maria.setContactoTelefono("+56 9 3333 4444");
        maria.setFechaCreacion(Instant.now());
        tecnicoRepository.save(maria);

        com.aring.dto.TareaRequest t1 = new com.aring.dto.TareaRequest();
        t1.setTipo(TipoTarea.MANTENIMIENTO);
        t1.setDireccion("Av. Vicuña Mackenna 3001. Ñuñoa.");
        t1.setNombreRecinto("Edificio Central");
        t1.setDescripcion("Mantenimiento de aire acondicionado del piso 3.");
        t1.setTecnicoCabeceraId(juan.getId());
        var tarea1 = tareaService.crear(t1);

        com.aring.dto.TareaRequest t2 = new com.aring.dto.TareaRequest();
        t2.setTipo(TipoTarea.MANTENIMIENTO);
        t2.setDireccion("Av. Providencia 1234. Providencia.");
        t2.setNombreRecinto("Bodega Norte");
        t2.setDescripcion("Cambio de luminarias del sector de despacho.");
        t2.setTecnicoCabeceraId(maria.getId());
        var tarea2 = tareaService.crear(t2);

        com.aring.dto.TareaRequest t3 = new com.aring.dto.TareaRequest();
        t3.setTipo(TipoTarea.MANTENIMIENTO);
        t3.setDireccion("Calle San Pablo 900. Santiago.");
        t3.setNombreRecinto("Local Comercial Sur");
        t3.setDescripcion("Revisión del tablero eléctrico principal.");
        t3.setTecnicoCabeceraId(maria.getId());
        tareaService.crear(t3);

        com.aring.dto.EstadoRequest enProceso = new com.aring.dto.EstadoRequest();
        enProceso.setEstado(EstadoTarea.EN_PROCESO);
        tareaService.cambiarEstado(tarea2.getId(), enProceso);
        tareaService.cambiarEstado(tarea1.getId(), enProceso);

        com.aring.dto.EstadoRequest finalizada = new com.aring.dto.EstadoRequest();
        finalizada.setEstado(EstadoTarea.FINALIZADA);
        tareaService.cambiarEstado(tarea1.getId(), finalizada);

        System.out.println("Datos de demostración sembrados.");
    }
}
