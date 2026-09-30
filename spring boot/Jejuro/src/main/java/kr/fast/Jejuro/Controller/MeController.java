package kr.fast.Jejuro.Controller;


//[7페이지 내 여행 - 달력 옆 프로필]

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.Config.CurrentUser;
import kr.fast.Jejuro.Repository.UserRepository;
import kr.fast.Jejuro.ResponseDTO.MeResponse;

/** GET /api/me → { userId, nickname, role }  (로그인 세션 기준) */
@RestController
public class MeController {

 private final UserRepository userRepository;
 private final CurrentUser currentUser;

 public MeController(UserRepository userRepository, CurrentUser currentUser) {
     this.userRepository = userRepository;
     this.currentUser = currentUser;
 }

 @GetMapping("/api/me")
 public MeResponse me() {
     return userRepository.findById(currentUser.id())
             .map(u -> new MeResponse(u.getUserId(), u.getNickname(), u.getRole()))
             .orElseThrow(() -> ApiException.notFound("회원을 찾을 수 없습니다."));
 }
}