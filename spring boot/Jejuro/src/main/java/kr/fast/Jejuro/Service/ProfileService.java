package kr.fast.Jejuro.Service;

import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.Repository.UserRepository;
import kr.fast.Jejuro.RequestDTO.ProfilePasswordRequest;
import kr.fast.Jejuro.ResponseDTO.ProfileResponse;

@Service
public class ProfileService {
 private final UserRepository users;
 private final PasswordEncoder passwords;
 private final JdbcTemplate jdbc;
 public ProfileService(UserRepository users, PasswordEncoder passwords, JdbcTemplate jdbc) {
     this.users=users; this.passwords=passwords; this.jdbc=jdbc;
 }
 @Transactional(readOnly=true)
 public ProfileResponse profile(Long userId, String loginMethod) {
     var user=users.findById(userId).orElseThrow(() -> ApiException.notFound("회원을 찾을 수 없습니다."));
     var providers=jdbc.queryForList("SELECT provider FROM SOCIAL_ACCOUNT WHERE user_id = ? ORDER BY provider", String.class, userId);
     return new ProfileResponse(user.getUserId(),user.getEmail(),user.getNickname(),user.getRole(),loginMethod,providers);
 }
 @Transactional
 public void nickname(Long userId, String nickname) {
     String value=nickname.strip();
     if(value.length()<2 || value.length()>30) throw ApiException.badRequest("닉네임은 2~30자로 입력해 주세요.");
     var user=users.findByIdForUpdate(userId).orElseThrow(() -> ApiException.notFound("회원을 찾을 수 없습니다."));
     user.changeNickname(value);
     users.saveAndFlush(user);
 }
 @Transactional
 public void password(Long userId, ProfilePasswordRequest req) {
     if(!req.newPassword().equals(req.confirmPassword())) throw ApiException.badRequest("새 비밀번호가 서로 다릅니다.");
     if(req.newPassword().getBytes(StandardCharsets.UTF_8).length>72) throw ApiException.badRequest("새 비밀번호는 UTF-8 기준 72바이트 이내로 입력해 주세요.");
     var user=users.findByIdForUpdate(userId).orElseThrow(() -> ApiException.notFound("회원을 찾을 수 없습니다."));
     if(req.currentPassword().getBytes(StandardCharsets.UTF_8).length>72 || !passwords.matches(req.currentPassword(), user.getPasswordHash()))
         throw ApiException.badRequest("현재 비밀번호가 일치하지 않습니다.");
     if(passwords.matches(req.newPassword(),user.getPasswordHash())) throw ApiException.badRequest("기존과 다른 새 비밀번호를 입력해 주세요.");
     user.changePassword(passwords.encode(req.newPassword()));
     users.saveAndFlush(user);
 }
}
