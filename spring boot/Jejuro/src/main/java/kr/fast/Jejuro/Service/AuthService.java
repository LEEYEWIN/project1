package kr.fast.Jejuro.Service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.Entity.User;
import kr.fast.Jejuro.Repository.UserRepository;
import kr.fast.Jejuro.RequestDTO.SignupRequest;

@Service
public class AuthService {
 @Transactional
 public void withdraw(Long userId, String password) {
     var user = users.findByIdForUpdate(userId)
         .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
     if (!"ACTIVE".equals(user.getStatus()))
         throw new ApiException(HttpStatus.CONFLICT, "이미 탈퇴 요청된 계정입니다.");
     if (password.getBytes(StandardCharsets.UTF_8).length > 72 || !passwords.matches(password, user.getPasswordHash()))
         throw ApiException.badRequest("현재 비밀번호가 일치하지 않습니다.");
     user.requestWithdrawal();
     users.saveAndFlush(user);
 }

 private final UserRepository users;
 private final PasswordEncoder passwords;
 public AuthService(UserRepository users, PasswordEncoder passwords) { this.users = users; this.passwords = passwords; }

 @Transactional
 public void signup(SignupRequest req) {
     String email = req.email().strip().toLowerCase(Locale.ROOT);
     String nickname = req.nickname().strip();
     if (nickname.length() < 2) throw ApiException.badRequest("닉네임은 2자 이상 입력해 주세요.");
     if (req.password().getBytes(StandardCharsets.UTF_8).length > 72)
         throw ApiException.badRequest("비밀번호가 너무 깁니다. 영문·숫자는 72자, 한글은 24자 이내로 입력해 주세요.");
     if (users.existsByEmailIgnoreCase(email)) throw new ApiException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
     if (users.existsByNicknameIgnoreCase(nickname)) throw new ApiException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다. 다른 닉네임을 입력해 주세요.");
     users.saveAndFlush(User.register(email, passwords.encode(req.password()), nickname, req.birthDate(), req.genderCode()));
 }
}