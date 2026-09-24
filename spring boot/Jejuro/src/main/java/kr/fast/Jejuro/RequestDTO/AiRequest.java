package kr.fast.Jejuro.RequestDTO;


//[2페이지 AI 추천 중]

import java.util.List;
import kr.fast.Jejuro.Entity.AiTravelInput;

/**
* AI 서버로 보내는 요청 본문. user_id 같은 개인 식별값은 넣지 않는다.
* regionIds는 "제주 전체"면 빈 배열.
*/
public record AiRequest(
     Integer genderCode,
     Integer ageGroupCode,
     Integer incomeCode,
     Integer styleNatureCity,
     Integer styleNewFamiliar,
     Integer styleHiddenFamous,
     Integer styleRelaxActivity,
     Integer photoImportance,
     Integer stylePlanFree,
     List<Integer> travelMotive,
     List<Integer> userMission,
     Integer companionCount,
     List<Integer> regionIds,
     int limit) {

 public static AiRequest of(AiTravelInput in, List<Integer> regionIds, int limit) {
     return new AiRequest(in.genderCode(), in.ageGroupCode(), in.incomeCode(),
             in.styleNatureCity(), in.styleNewFamiliar(), in.styleHiddenFamous(),
             in.styleRelaxActivity(), in.photoImportance(), in.stylePlanFree(),
             in.travelMotive(), in.userMission(), in.companionCount(), regionIds, limit);
 }
}