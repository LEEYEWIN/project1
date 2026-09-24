package kr.fast.Jejuro.Entity;


import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 여행별 설문 답변 한 개 = 한 행.
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

    protected TravelPreference() {
    }

    public TravelPreference(Long travelId, Long preferenceId, ResponseType responseType, Integer answerValue) {
        this.travelId = travelId;
        this.preferenceId = preferenceId;
        this.responseType = responseType;
        this.answerValue = answerValue;
    }

    public Long getTravelPreferenceId() { return travelPreferenceId; }
    public Long getTravelId() { return travelId; }
    public Long getPreferenceId() { return preferenceId; }
    public ResponseType getResponseType() { return responseType; }
    public Integer getAnswerValue() { return answerValue; }
}