package kr.fast.Jejuro.Repository;


//[1·7페이지 여행]

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kr.fast.Jejuro.Entity.Travel;

public interface TravelRepository extends JpaRepository<Travel, Long> {

 /** 회원의 다음 여행 순번. 회원 행을 잠근 트랜잭션 안에서 호출해야 한다. */
 @Query("select coalesce(max(t.travelNo), 0) + 1 from Travel t where t.userId = :userId")
 int nextTravelNo(@Param("userId") Long userId);

 /** 소유권 검사용: 내 여행일 때만 찾는다. */
 Optional<Travel> findByTravelIdAndUserId(Long travelId, Long userId);

 /** 내 여행 목록 (최근 여행 순서) */
 List<Travel> findByUserIdOrderByStartDateDescTravelIdDesc(Long userId);
}