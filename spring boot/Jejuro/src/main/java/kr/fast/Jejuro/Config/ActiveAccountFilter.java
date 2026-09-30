package kr.fast.Jejuro.Config;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;
import kr.fast.Jejuro.Repository.UserRepository;

/** 탈퇴한 회원의 다른 브라우저 세션도 다음 요청부터 거부한다. */
public class ActiveAccountFilter extends OncePerRequestFilter {
 private final UserRepository users;
 public ActiveAccountFilter(UserRepository users) { this.users = users; }
 @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                           FilterChain chain) throws ServletException, IOException {
     var auth = SecurityContextHolder.getContext().getAuthentication();
     if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
         boolean active;
         try { active = users.findById(Long.valueOf(auth.getName()))
                 .map(user -> "ACTIVE".equals(user.getStatus())).orElse(false); }
         catch (NumberFormatException ex) { active = false; }
         if (!active) {
             new SecurityContextLogoutHandler().logout(request, response, auth);
             response.setStatus(401);
             response.setContentType("application/json;charset=UTF-8");
             response.getWriter().write("{\"message\":\"탈퇴 요청된 계정입니다. 계정 이용이 중단되었습니다.\"}");
             return;
         }
     }
     chain.doFilter(request, response);
 }
}
