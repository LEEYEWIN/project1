package kr.fast.Jejuro.Repository;

// [1페이지 여행 설문]

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.fast.Jejuro.Entity.TravelPreference;

public interface TravelPreferenceRepository extends JpaRepository<TravelPreference, Long> {

    /** 여행의 설문 답변 (질문 순서 → 순위 순서). 추천 기준 안내에 사용 */
    List<TravelPreference> findByTravelIdOrderByPreferenceIdAscAnswerRankAsc(Long travelId);
}
