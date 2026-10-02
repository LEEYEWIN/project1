package kr.fast.Jejuro.Repository;

// [1·7페이지 여행]

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import kr.fast.Jejuro.Entity.Travel;

public interface TravelRepository extends JpaRepository<Travel, Long> {

    /** 회원의 다음 여행 순번. 회원 행을 잠근 트랜잭션 안에서 호출해야 한다. */
    @Query("select coalesce(max(t.travelNo), 0) + 1 from Travel t where t.userId = :userId")
    int nextTravelNo(@Param("userId") Long userId);

    /**
     * 내 여행을 잠가서 읽는다(SELECT ... FOR UPDATE).
     * 경로 만들기(여행당 1개)를 동시에 두 번 요청해도 한 요청씩 차례로 처리되게 하기 위함.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Travel t where t.travelId = :travelId and t.userId = :userId")
    Optional<Travel> findOwnedForUpdate(@Param("travelId") Long travelId, @Param("userId") Long userId);

    /**
     * 경로 번호로 그 경로의 여행 행을 잠근다(내 여행일 때만).
     * 경로 저장·장소 추가/빼기·일정 확정이 같은 여행에서 동시에 일어나지 않게 차례로 처리하는 공통 잠금.
     * 이 잠금을 트랜잭션의 첫 조회로 해야 뒤의 조회가 최신 데이터를 본다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Travel t where t.userId = :userId"
            + " and t.travelId = (select r.travelId from TravelRoute r where r.routeId = :routeId)")
    Optional<Travel> findOwnedByRouteForUpdate(@Param("routeId") Long routeId, @Param("userId") Long userId);

    /** 기간이 겹치는 내 여행 (시작일 ≤ 새 종료일 이고 종료일 ≥ 새 시작일). 같은 기간 중복 생성 막기 */
    Optional<Travel> findFirstByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAsc(
            Long userId, java.time.LocalDate endDate, java.time.LocalDate startDate);

    /** 소유권 검사용: 내 여행일 때만 찾는다. */
    Optional<Travel> findByTravelIdAndUserId(Long travelId, Long userId);

    /** 내 여행 목록 (최근 여행 순서) */
    List<Travel> findByUserIdOrderByStartDateDescTravelIdDesc(Long userId);

    /** 커뮤니티 글의 경로를 가져가 만든 여행 수 */
    long countBySourcePostId(Long sourcePostId);
}
