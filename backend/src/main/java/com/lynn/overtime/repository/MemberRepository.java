package com.lynn.overtime.repository;

import com.lynn.overtime.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByUsername(String username);
    List<Member> findByName(String name);
    boolean existsByUsername(String username);
    List<Member> findByStatus(Integer status);
    List<Member> findByNameContainingAndDepartmentContainingAndIdIsNotNull(String name, String department);
}
