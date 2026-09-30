package kr.fast.Jejuro.Config;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** 서버가 인증한 세션만 사용한다. X-User-Id 헤더는 신뢰하지 않는다. */
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
