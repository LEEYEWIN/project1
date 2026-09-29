package kr.fast.Jejuro.ResponseDTO;


//[공통 - 로그인 회원 정보 (내 여행 프로필, 관리자 메뉴 표시)]

import java.time.LocalDateTime;

/**
* 로그인한 회원 정보. role = USER / ADMIN (ADMIN이면 상단에 관리자 메뉴)
* suspendedUntil: 커뮤니티 이용 정지 기한 (정지 중이 아니면 null, 영구 정지는 9999-12-31)
*/
public record MeResponse(Long userId, String nickname, String role, LocalDateTime suspendedUntil, int warningCount) {
}