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

@RestController
@RequestMapping("/api/auth")
public class AuthController {
 private final AuthService service;
 private final AuthenticationManager authentication;
 private final SecurityContextRepository contexts;
 private final SessionAuthenticationStrategy sessions;
 private final UserRepository users;
 public AuthController(AuthService service, AuthenticationManager authentication, SecurityContextRepository contexts,
                       SessionAuthenticationStrategy sessions, UserRepository users) {
     this.service = service; this.authentication = authentication; this.contexts = contexts;
     this.sessions = sessions; this.users = users;
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
 public MeResponse signup(@Valid @RequestBody SignupRequest body, HttpServletRequest request, HttpServletResponse response) {
     service.signup(body);
     return signIn(body.email(), body.password(), request, response);
 }

 @PostMapping("/login")
 public MeResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
     return signIn(body.email(), body.password(), request, response);
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
