package kr.fast.Jejuro.Config;


//[관리자 화면 공통 - 권한 확인]

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import kr.fast.Jejuro.Repository.UserRepository;

/**
* 관리자 API 맨 앞에서 호출: 로그인 회원의 USER.role 이 ADMIN 이 아니면 403.
* (지금은 로그인 대신 X-User-Id 헤더라 화면 상단 테스트 회원을 관리자 계정으로 바꿔서 확인)
*/
@Component
public class AdminGuard {

 private final CurrentUser currentUser;
 private final UserRepository userRepository;

 public AdminGuard(CurrentUser currentUser, UserRepository userRepository) {
     this.currentUser = currentUser;
     this.userRepository = userRepository;
 }

 public void check() {
     boolean admin = userRepository.findById(currentUser.id())
             .map(u -> u.isAdmin())
             .orElse(false);
     if (!admin) {
         throw new ApiException(HttpStatus.FORBIDDEN, "관리자만 볼 수 있는 화면입니다.");
     }
 }
}
