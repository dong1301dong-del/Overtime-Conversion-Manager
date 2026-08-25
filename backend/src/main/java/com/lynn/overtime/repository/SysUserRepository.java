package com.lynn.overtime.repository;

import com.lynn.overtime.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SysUserRepository extends JpaRepository<SysUser, Long> {
    Optional<SysUser> findByUsername(String username);
    boolean existsByUsername(String username);
    List<SysUser> findByRole(String role);
    Optional<SysUser> findByMemberId(Long memberId);
    List<SysUser> findByRoleIn(List<String> roles);
    Optional<SysUser> findBySessionToken(String token);
}
