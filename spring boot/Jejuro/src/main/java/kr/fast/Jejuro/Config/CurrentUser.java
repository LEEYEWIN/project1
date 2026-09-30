package kr.fast.Jejuro.Config;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** 서버가 인증한 로그인 세션의 회원 ID를 사용한다. */
@Component
public class CurrentUser {
 public Long id() {
     Authentication auth = SecurityContextHolder.getContext().getAuthentication();
     if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
         throw new ApiException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
     }
     return Long.valueOf(auth.getName());
 }
}
