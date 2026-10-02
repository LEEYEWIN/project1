package kr.fast.Jejuro.Repository;


//[공통]

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import kr.fast.Jejuro.Entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

 Optional<User> findByEmailIgnoreCase(String email);
 boolean existsByEmailIgnoreCase(String email);
 /** 닉네임 중복 확인 (대소문자 구분 없이) */
 boolean existsByNicknameIgnoreCase(String nickname);
 /** 닉네임 변경 시: 나를 뺀 다른 회원이 쓰는지 */
 boolean existsByNicknameIgnoreCaseAndUserIdNot(String nickname, Long userId);

 /**
  * 회원 행을 잠근다(SELECT ... FOR UPDATE).
  * 같은 회원이 여행을 동시에 두 개 만들어도 travel_no가 겹치지 않게 하기 위함.
  */
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select u from User u where u.userId = :userId")
 Optional<User> findByIdForUpdate(@Param("userId") Long userId);
}