package kr.fast.Jejuro.RequestDTO;


//[2페이지 AI 추천 중]

import java.util.List;
import kr.fast.Jejuro.Entity.AiTravelInput;

/**
* AI 추천에 필요한 입력을 한곳에 모은 값. user_id 같은 개인 식별값은 넣지 않는다.
* - HttpAiClient: FastAPI /recommend 형식(JSON 키 이름)으로 바꿔서 보낸다.
* - MockAiClient: regionIds, limit만 사용한다.
* regionMode가 ALL이면 regionCodes·regionIds는 빈 배열.
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
     Integer travelMotive1,
     Integer userMission1,
     String regionMode,
     List<String> regionCodes,
     List<Integer> regionIds,
     List<CompanionInput> companions,
     int limit) {

 /** 동반자 1명 (DB 코드 그대로). 정렬·18-slot 변환은 AI 서버가 한다. */
 public record CompanionInput(Integer relationCode, Integer genderCode, Integer ageGroupCode) {
 }

 public static AiRequest of(AiTravelInput in, String regionMode, List<String> regionCodes,
                            List<Integer> regionIds, List<CompanionInput> companions, int limit) {
     return new AiRequest(in.genderCode(), in.ageGroupCode(), in.incomeCode(),
             in.styleNatureCity(), in.styleNewFamiliar(), in.styleHiddenFamous(),
             in.styleRelaxActivity(), in.photoImportance(), in.stylePlanFree(),
             in.travelMotive1(), in.userMission1(),
             regionMode, regionCodes, regionIds, companions, limit);
 }
}