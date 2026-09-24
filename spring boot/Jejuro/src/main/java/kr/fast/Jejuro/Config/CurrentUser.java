package kr.fast.Jejuro.Config;


//[공통]

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import kr.fast.Jejuro.Entity.User;

/**
* 로그인한 회원 ID를 돌려준다. (로그인 기능 대신 쓰는 테스트용 방식)
*
* 화면이 요청마다 보내는 헤더  X-User-Id: 2  를 읽어 그 회원으로 처리한다.
* 헤더가 없으면 application.properties의 app.test-login.default-user-id(기본 1) 회원.
*
* 컨트롤러는 모두 currentUser.id()만 부르므로, 나중에 진짜 로그인을 만들면 이 클래스만 바꾸면 된다.
* 주의: 헤더만 바꾸면 누구든 될 수 있으므로 실제 서비스(배포)에서는 쓰면 안 된다.
*/
@Component
public class CurrentUser {

 public static final String HEADER = "X-User-Id";

 private final HttpServletRequest request;   // 스프링이 "지금 처리 중인 요청"으로 연결해 준다
 private final Long defaultUserId;

 public CurrentUser(HttpServletRequest request,
                    @Value("${app.test-login.default-user-id:1}") Long defaultUserId) {
     this.request = request;
     this.defaultUserId = defaultUserId;
 }

 public Long id() {
     String header = request.getHeader(HEADER);
     if (header == null || header.isBlank()) {
         return defaultUserId;
     }
     try {
         return Long.valueOf(header.trim());
     } catch (NumberFormatException e) {
         throw ApiException.badRequest(HEADER + " 헤더는 숫자여야 합니다.");
     }
 }
}
