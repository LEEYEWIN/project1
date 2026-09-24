package kr.fast.Jejuro.Repository;


//[공통]

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import kr.fast.Jejuro.Entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

 /** 테스트 회원 선택 상자용 */
 List<User> findTop20ByStatusOrderByUserId(String status);

 /**
  * 회원 행을 잠근다(SELECT ... FOR UPDATE).
  * 같은 회원이 여행을 동시에 두 개 만들어도 travel_no가 겹치지 않게 하기 위함.
  */
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select u from User u where u.userId = :userId")
 Optional<User> findByIdForUpdate(@Param("userId") Long userId);
}