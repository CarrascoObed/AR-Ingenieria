package com.aring.repository;

import com.aring.model.SuperUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SuperUsuarioRepository extends JpaRepository<SuperUsuario, Long> {

    Optional<SuperUsuario> findByUsername(String username);

    boolean existsByUsername(String username);
}
