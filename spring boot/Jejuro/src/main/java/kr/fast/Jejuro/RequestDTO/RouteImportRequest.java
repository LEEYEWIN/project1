package kr.fast.Jejuro.RequestDTO;


// [커뮤니티 - 다른 사람 경로를 내 새 여행으로 가져오기]

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * POST /api/community/posts/{postId}/import
 * 예) { "travelName": "성산 2박3일 따라가기", "startDate": "2026-11-03" }
 * 종료일은 원래 경로의 일수만큼 서버가 계산한다.
 */
public record RouteImportRequest(
        @NotBlank @Size(max = 100) String travelName,
        @NotNull LocalDate startDate) {
}