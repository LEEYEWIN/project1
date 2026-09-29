package kr.fast.Jejuro.Repository;


// [관리자 회원 관리 - 제재 이력]

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.fast.Jejuro.Entity.UserSanction;

public interface UserSanctionRepository extends JpaRepository<UserSanction, Long> {

    List<UserSanction> findByUserIdOrderBySanctionIdDesc(Long userId);
}