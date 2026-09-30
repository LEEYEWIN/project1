package kr.fast.Jejuro.Entity;


//[2페이지 AI 추천 중]

import java.util.List;

/**
* AI_TRAVEL_INPUT VIEW 한 행. AI 모델에 보낼 특성들이다.
* travelMotive1 / userMission1 = 사용자가 처음 고른 1순위 → 모델 TRAVEL_MOTIVE_1 / TRAVEL_MISSION_PRIORITY_WEB
*/
public record AiTravelInput(
     Long travelId,
     Integer genderCode,
     Integer ageGroupCode,          // 1~8 그대로 (모델 학습 범위 20~60대 밖은 AI 서버가 처리)
     Integer styleNatureCity,       // 101 → TRAVEL_STYL_1
     Integer styleNewFamiliar,      // 102 → TRAVEL_STYL_3
     Integer styleHiddenFamous,     // 103 → TRAVEL_STYL_6
     Integer styleRelaxActivity,    // 104 → TRAVEL_STYL_5
     Integer photoImportance,       // 105 → TRAVEL_STYL_8
     Integer stylePlanFree,         // 106 → TRAVEL_STYL_7
     Integer incomeCode,            // 203 → income_code
     Integer travelMotive1,         // 201 1순위
     Integer userMission1,          // 202 1순위
     List<Integer> travelMotive,    // 201 전체(순서 무관)
     List<Integer> userMission,     // 202 전체(순서 무관)
     Integer companionCount,
     boolean surveyComplete) {
}