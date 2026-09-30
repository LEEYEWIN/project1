package kr.fast.Jejuro.RequestDTO;


import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.fast.Jejuro.Entity.RegionMode;

/**
 * POST /api/travels 요청 본문.
 * 예) { "travelName":"가을 제주", "startDate":"2026-10-20", "endDate":"2026-10-22",
 *       "regionMode":"SELECTED", "regionIds":[1,3],
 *       "companions":[{"relationCode":7,"genderCode":2,"ageGroupCode":3}],
 *       "answers":[{"preferenceId":101,"values":[1]}, {"preferenceId":201,"values":[1,3,7]}, ...] }
 */
public record TravelCreateRequest(
        @NotBlank @Size(max = 100) String travelName,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull RegionMode regionMode,
        List<Integer> regionIds,
        @Valid List<CompanionReq> companions,
        @NotEmpty @Valid List<AnswerReq> answers) {

    public record CompanionReq(
            @NotNull Integer relationCode,
            @NotNull Integer genderCode,
            @NotNull Integer ageGroupCode) {
    }

    public record AnswerReq(
            @NotNull Long preferenceId,
            @NotEmpty List<Integer> values) {
    }

    public List<Integer> regionIdsOrEmpty() {
        return regionIds == null ? List.of() : regionIds;
    }

    public List<CompanionReq> companionsOrEmpty() {
        return companions == null ? List.of() : companions;
    }
}