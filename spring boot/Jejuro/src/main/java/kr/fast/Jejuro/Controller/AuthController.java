package kr.fast.Jejuro.Controller;

import java.util.Locale;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import kr.fast.Jejuro.Config.ApiException;
import org.springframework.security.core.Authentication;
import kr.fast.Jejuro.RequestDTO.WithdrawalRequest;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import kr.fast.Jejuro.Repository.UserRepository;
import kr.fast.Jejuro.RequestDTO.LoginRequest;
import kr.fast.Jejuro.RequestDTO.SignupRequest;
import kr.fast.Jejuro.ResponseDTO.MeResponse;
import kr.fast.Jejuro.Service.AuthService;
import kr.fast.Jejuro.Service.EmailVerificationService;
import kr.fast.Jejuro.Service.LoginAttemptLimiter;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
 private final AuthService service;
 private final AuthenticationManager authentication;
 private final SecurityContextRepository contexts;
 private final SessionAuthenticationStrategy sessions;
 private final UserRepository users;
 private final EmailVerificationService emailVerificationService;
 private final LoginAttemptLimiter loginLimiter;
 public AuthController(
	        AuthService service,
	        AuthenticationManager authentication,
	        SecurityContextRepository contexts,
	        SessionAuthenticationStrategy sessions,
	        UserRepository users,
	        EmailVerificationService emailVerificationService,
	        LoginAttemptLimiter loginLimiter
	) {
	    this.service = service;
	    this.authentication = authentication;
	    this.contexts = contexts;
	    this.sessions = sessions;
	    this.users = users;
	    this.emailVerificationService = emailVerificationService;
	    this.loginLimiter = loginLimiter;
	}
 /** 닉네임 사용 가능 여부 (회원가입·내 정보 수정 화면에서 입력할 때마다 확인). 로그인 상태면 내 닉네임은 제외 */
 @GetMapping("/nickname/check")
 public Map<String, Object> checkNickname(@RequestParam("nickname") String nickname, Authentication principal) {
     String value = nickname == null ? "" : nickname.strip();
     if (value.length() < 2 || value.length() > 30) {
         return Map.of("available", false, "message", "닉네임은 2~30자로 입력해 주세요.");
     }
     Long me = null;
     if (principal != null && principal.isAuthenticated()
             && !(principal instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
         try { me = Long.valueOf(principal.getName()); } catch (NumberFormatException ignored) { }
     }
     boolean taken = me == null ? users.existsByNicknameIgnoreCase(value)
             : users.existsByNicknameIgnoreCaseAndUserIdNot(value, me);
     return taken
             ? Map.of("available", false, "message", "이미 사용 중인 닉네임입니다. 다른 닉네임을 입력해 주세요.")
             : Map.of("available", true, "message", "사용할 수 있는 닉네임이에요.");
 }

 @GetMapping("/csrf")
 public Map<String, String> csrf(CsrfToken token) {
     return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
 }

 @PostMapping("/withdraw")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void withdraw(@Valid @RequestBody WithdrawalRequest body, HttpServletRequest request,
                      HttpServletResponse response, Authentication principal) {
     service.withdraw(Long.valueOf(principal.getName()), body.password());
     new SecurityContextLogoutHandler().logout(request, response,
             SecurityContextHolder.getContext().getAuthentication());
 }

 @PostMapping("/signup")
 @ResponseStatus(HttpStatus.CREATED)
 public MeResponse signup(
         @Valid @RequestBody SignupRequest body,
         HttpServletRequest request,
         HttpServletResponse response
 ) {

     // 인증은 이 브라우저 세션에서 끝낸 것만 인정한다 (다른 사람이 같은 이메일로 가입하는 것 방지)
     if (!emailVerificationService.isVerified(request.getSession(false), body.email())) {
         throw new ApiException(
                 HttpStatus.BAD_REQUEST,
                 "이메일 인증을 완료해 주세요."
         );
     }

     service.signup(body);

     emailVerificationService.consumeVerification(request.getSession(false));

     return signIn(
             body.email(),
             body.password(),
             request,
             response
     );
 }
 @PostMapping("/login")
 public MeResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
     String email = body.email().strip().toLowerCase(Locale.ROOT);
     String ip = request.getRemoteAddr();
     loginLimiter.check(email, ip);          // 실패가 많으면 429 (비밀번호 무차별 대입 방지)
     try {
         MeResponse me = signIn(email, body.password(), request, response);
         loginLimiter.recordSuccess(email);
         return me;
     } catch (ApiException e) {
         if (e.getStatus() == HttpStatus.UNAUTHORIZED) loginLimiter.recordFailure(email, ip);
         throw e;
     }
 }

 private MeResponse signIn(String email, String password, HttpServletRequest request, HttpServletResponse response) {
     try {
         var auth = authentication.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                 email.strip().toLowerCase(Locale.ROOT), password));
         sessions.onAuthentication(auth, request, response);
         var context = SecurityContextHolder.createEmptyContext();
         context.setAuthentication(auth);
         SecurityContextHolder.setContext(context);
         contexts.saveContext(context, request, response);
         var user = users.findById(Long.valueOf(auth.getName())).orElseThrow();
         request.getSession().setAttribute("loginMethod", "EMAIL");
         request.getSession().setAttribute("credentialStamp", user.getPasswordHash());
         return new MeResponse(user.getUserId(), user.getNickname(), user.getRole());
     } catch (AuthenticationException ex) {
         throw new ApiException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호를 확인해 주세요.");
     }
 }
}