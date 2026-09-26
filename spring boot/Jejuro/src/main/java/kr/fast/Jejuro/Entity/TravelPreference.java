package kr.fast.Jejuro.Entity;


//[1페이지 여행 설문]

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
* 여행별 설문 답변 한 개 = 한 행. 다중 선택은 answerRank에 고른 순서(1순위부터)를 저장한다.
* DB의 생성 열 single_preference_id는 DB가 계산하므로 엔티티에 매핑하지 않는다.
*/
@Entity
@Table(name = "travel_preference")
public class TravelPreference {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long travelPreferenceId;
 private Long travelId;
 private Long preferenceId;

 @Enumerated(EnumType.STRING)
 private ResponseType responseType;   // 질문의 응답 방식을 그대로 복사 (DB 복합 FK가 검사)

 private Integer answerValue;
 private Integer answerRank;          // 고른 순서. 1 = 1순위(처음 고른 것). 단일 선택은 1

 protected TravelPreference() {
 }

 public TravelPreference(Long travelId, Long preferenceId, ResponseType responseType,
                         Integer answerValue, Integer answerRank) {
     this.travelId = travelId;
     this.preferenceId = preferenceId;
     this.responseType = responseType;
     this.answerValue = answerValue;
     this.answerRank = answerRank;
 }

 public Long getTravelPreferenceId() { return travelPreferenceId; }
 public Long getTravelId() { return travelId; }
 public Long getPreferenceId() { return preferenceId; }
 public ResponseType getResponseType() { return responseType; }
 public Integer getAnswerValue() { return answerValue; }
 public Integer getAnswerRank() { return answerRank; }
}