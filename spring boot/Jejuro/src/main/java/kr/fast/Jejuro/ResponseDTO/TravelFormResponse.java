package kr.fast.Jejuro.ResponseDTO;


import java.util.List;

/** 1페이지(여행 만들기 + 설문)를 그리는 데 필요한 값을 한 번에 내려준다. */
public record TravelFormResponse(
        List<RegionItem> regions,
        List<CodeItem> relations,
        List<CodeItem> genders,
        List<CodeItem> ageGroups,
        List<QuestionGroup> groups) {

    public record RegionItem(Integer regionId, String name) {
    }

    public record CodeItem(Integer value, String name) {
    }

    /** maxSelections가 null이면 선택 개수 상한 없음 */
    public record QuestionGroup(String groupCode, String groupName,
                                Integer minSelections, Integer maxSelections,
                                List<Question> questions) {
    }

    public record Question(Long preferenceId, String code, String name,
                           String responseType, List<Option> options) {
    }

    public record Option(Integer value, String name, String description) {
    }
    
    
}