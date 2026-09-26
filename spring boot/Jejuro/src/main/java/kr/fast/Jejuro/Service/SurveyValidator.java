package kr.fast.Jejuro.Service;


//[1페이지 여행 설문]

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.Entity.Preference;
import kr.fast.Jejuro.Entity.PreferenceGroup;
import kr.fast.Jejuro.Entity.PreferenceOption;
import kr.fast.Jejuro.RequestDTO.TravelCreateRequest.AnswerReq;
import kr.fast.Jejuro.RequestDTO.TravelCreateRequest;

/**
* 설문 답변 검사. 규칙은 코드에 적지 않고 PREFERENCE_GROUP의 min/max를 읽어서 적용한다.
* (여행 동기·테마는 min=max=3 → 정확히 3개. values 배열 순서가 1·2·3순위)
* DB도 최대 개수·중복·없는 선택지를 막지만, 사용자에게 알맞은 메시지를 주려고 저장 전에 먼저 검사한다.
*/
@Component
public class SurveyValidator {

 public void validate(List<Preference> questions, List<PreferenceOption> options, List<AnswerReq> answers) {
     Map<Long, AnswerReq> answerMap = new HashMap<>();
     for (AnswerReq a : answers) {
         if (answerMap.put(a.preferenceId(), a) != null) {
             throw ApiException.badRequest("같은 질문의 답변이 두 번 들어왔습니다: " + a.preferenceId());
         }
     }

     Map<Long, Set<Integer>> validValues = new HashMap<>();
     for (PreferenceOption o : options) {
         validValues.computeIfAbsent(o.getPreferenceId(), k -> new HashSet<>()).add(o.getOptionValue());
     }

     for (Preference q : questions) {
         AnswerReq a = answerMap.remove(q.getPreferenceId());
         List<Integer> values = a == null ? List.of() : a.values();
         PreferenceGroup g = q.getGroup();

         if (values.size() < g.getMinSelections()) {
             String need = g.getMinSelections() == 1 ? "질문에 답해 주세요."
                     : Objects.equals(g.getMinSelections(), g.getMaxSelections())
                             ? g.getMinSelections() + "개를 골라 주세요."
                             : "최소 " + g.getMinSelections() + "개를 골라 주세요.";
             throw ApiException.badRequest("'" + q.getPreferenceName() + "' " + need);
         }
         if (g.getMaxSelections() != null && values.size() > g.getMaxSelections()) {
             throw ApiException.badRequest("'" + q.getPreferenceName() + "'은(는) 최대 "
                     + g.getMaxSelections() + "개까지 고를 수 있습니다.");
         }
         if (new HashSet<>(values).size() != values.size()) {
             throw ApiException.badRequest("'" + q.getPreferenceName() + "'에 같은 선택지가 중복되었습니다.");
         }
         Set<Integer> allowed = validValues.getOrDefault(q.getPreferenceId(), Set.of());
         for (Integer v : values) {
             if (!allowed.contains(v)) {
                 throw ApiException.badRequest("'" + q.getPreferenceName() + "'에 없는 선택지입니다: " + v);
             }
         }
     }

     if (!answerMap.isEmpty()) {
         throw ApiException.badRequest("존재하지 않는 질문입니다: " + answerMap.keySet());
     }
 }
}