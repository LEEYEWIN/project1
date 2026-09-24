package kr.fast.Jejuro.Repository;


//[2페이지 AI 추천 중]

import java.util.List;
import java.util.Optional;

import java.util.Arrays;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import kr.fast.Jejuro.Entity.AiTravelInput;

/**
* VIEW는 JPA 엔티티로 만들기보다 JdbcTemplate로 읽는 편이 간단하다.
* travel_id와 user_id를 같이 걸어서 "내 여행"일 때만 결과가 나온다.
*/
@Repository
public class AiTravelInputRepository {

 private static final String SQL = """
         SELECT travel_id, gender_code, age_group_code,
                style_nature_city, style_new_familiar, style_hidden_famous,
                style_relax_activity, photo_importance, style_plan_free, income_code,
                travel_motive, user_mission, companion_count, is_survey_complete
         FROM AI_TRAVEL_INPUT
         WHERE travel_id = ? AND user_id = ?
         """;

 private final JdbcTemplate jdbcTemplate;

 public AiTravelInputRepository(JdbcTemplate jdbcTemplate) {
     this.jdbcTemplate = jdbcTemplate;
 }

 public Optional<AiTravelInput> find(Long travelId, Long userId) {
     List<AiTravelInput> rows = jdbcTemplate.query(SQL, (rs, i) -> new AiTravelInput(
             rs.getLong("travel_id"),
             (Integer) rs.getObject("gender_code", Integer.class),
             (Integer) rs.getObject("age_group_code", Integer.class),
             (Integer) rs.getObject("style_nature_city", Integer.class),
             (Integer) rs.getObject("style_new_familiar", Integer.class),
             (Integer) rs.getObject("style_hidden_famous", Integer.class),
             (Integer) rs.getObject("style_relax_activity", Integer.class),
             (Integer) rs.getObject("photo_importance", Integer.class),
             (Integer) rs.getObject("style_plan_free", Integer.class),
             (Integer) rs.getObject("income_code", Integer.class),
             jsonToList(rs.getString("travel_motive")),
             jsonToList(rs.getString("user_mission")),
             rs.getInt("companion_count"),
             rs.getInt("is_survey_complete") == 1), travelId, userId);
     return rows.stream().findFirst();
 }

 /**
  * VIEW의 JSON 숫자 배열 문자열 "[1, 3, 7]" → List<Integer> (순서는 정렬해서 고정).
  * 숫자 배열뿐이라 JSON 라이브러리 없이 직접 나눈다(스프링부트 버전의 Jackson 2/3 차이와 무관).
  */
 private List<Integer> jsonToList(String json) {
     if (json == null) {
         return List.of();
     }
     String inner = json.replace("[", "").replace("]", "").trim();
     if (inner.isEmpty()) {
         return List.of();
     }
     return Arrays.stream(inner.split(","))
             .map(String::trim)
             .map(Integer::valueOf)
             .sorted()
             .toList();
 }
}