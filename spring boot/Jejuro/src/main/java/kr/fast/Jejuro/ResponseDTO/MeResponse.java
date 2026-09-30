package kr.fast.Jejuro.ResponseDTO;

//[공통 - 로그인 회원 정보 (내 여행 프로필, 관리자 메뉴 표시)]

/**
* 로그인한 회원 정보. role = USER / ADMIN (ADMIN이면 상단에 관리자 메뉴)
*/
public record MeResponse(Long userId, String nickname, String role) {
}