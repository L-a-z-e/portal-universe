package com.portal.universe.authservice.auth.repository;

import com.portal.universe.authservice.auth.domain.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {

    Optional<PermissionEntity> findByPermissionKey(String permissionKey);

    boolean existsByPermissionKey(String permissionKey);

    List<PermissionEntity> findByService(String service);

    List<PermissionEntity> findByServiceAndResource(String service, String resource);

    List<PermissionEntity> findByActiveTrue();
}
