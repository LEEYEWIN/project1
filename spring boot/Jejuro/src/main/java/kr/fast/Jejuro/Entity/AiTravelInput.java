package kr.fast.Jejuro.Entity;


//[2페이지 AI 추천 중]

import java.util.List;

/** AI_TRAVEL_INPUT VIEW 한 행. AI 모델에 보낼 특성들이다. */
public record AiTravelInput(
     Long travelId,
     Integer genderCode,
     Integer ageGroupCode,
     Integer styleNatureCity,
     Integer styleNewFamiliar,
     Integer styleHiddenFamous,
     Integer styleRelaxActivity,
     Integer photoImportance,
     Integer stylePlanFree,
     Integer incomeCode,
     List<Integer> travelMotive,
     List<Integer> userMission,
     Integer companionCount,
     boolean surveyComplete) {
}