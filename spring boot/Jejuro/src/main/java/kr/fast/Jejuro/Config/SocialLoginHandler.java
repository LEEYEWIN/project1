package kr.fast.Jejuro.Config;


import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kr.fast.Jejuro.Entity.User;
import kr.fast.Jejuro.Repository.UserRepository;
import kr.fast.Jejuro.Service.SocialAccountService;
import kr.fast.Jejuro.Service.SocialLinkException;
import kr.fast.Jejuro.Service.SocialLinkRequest;
import kr.fast.Jejuro.Service.SocialProvider;

/**
 * 제공자 로그인(카카오·네이버·구글)이 끝난 뒤 처리.
 * - 세션에 연동 요청이 있으면: 그 회원에 소셜 계정을 연동하고 원래 로그인을 유지한다.
 * - 없으면: 연동된 회원으로만 로그인한다. 연동이 없으면 로그인하지 않고 안내한다(자동 가입·자동 연결 없음).
 * 끝나면 React 화면(app.frontend-url)으로 돌려보낸다.
 */
@Component
public class SocialLoginHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private final SocialAccountService socialAccounts;
    private final UserRepository users;
    private final SecurityContextRepository contexts;
    private final String frontendUrl;

    public SocialLoginHandler(SocialAccountService socialAccounts, UserRepository users,
                              SecurityContextRepository contexts,
                              @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl) {
        this.socialAccounts = socialAccounts;
        this.users = users;
        this.contexts = contexts;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        SocialLinkRequest link = takeLinkRequest(request);

        SocialProvider provider;
        String providerUserId;
        try {
            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
            provider = SocialProvider.from(token.getAuthorizedClientRegistrationId());
            providerUserId = provider.providerUserId(token.getPrincipal().getAttributes());
        } catch (RuntimeException e) {
            finishWithError(request, response, link, "FAILED");
            return;
        }

        // 1) 회원정보 수정에서 시작한 연동
        if (link != null && !link.expired() && link.provider() == provider) {
            try {
                socialAccounts.link(link.userId(), provider, providerUserId);
            } catch (SocialLinkException e) {
                finishWithError(request, response, link, e.getCode());
                return;
            }
            if (!signIn(link.userId(), link.loginMethod(), request, response)) {
                redirect(response, "/login?socialError=INACTIVE");
                return;
            }
            redirect(response, "/account/profile?social=linked&provider=" + provider.name());
            return;
        }
        if (link != null) { // 시간이 지났거나 다른 제공자로 로그인한 경우
            finishWithError(request, response, link, "EXPIRED");
            return;
        }

        // 2) 로그인 화면의 소셜 로그인
        var userId = socialAccounts.findUserId(provider, providerUserId);
        if (userId.isEmpty()) {
            signOut(request, response);
            redirect(response, "/login?socialError=NOT_LINKED&provider=" + provider.name());
            return;
        }
        if (!signIn(userId.get(), provider.name(), request, response)) {
            redirect(response, "/login?socialError=INACTIVE");
            return;
        }
        redirect(response, "/travels");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        // 동의 취소, 키 오류 등. 실패하면 기존 로그인(연동 중이던 회원)은 그대로 남아 있다.
        SocialLinkRequest link = takeLinkRequest(request);
        redirect(response, (link != null ? "/account/profile" : "/login") + "?socialError=FAILED");
    }

    private SocialLinkRequest takeLinkRequest(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object value = session.getAttribute(SocialLinkRequest.SESSION_KEY);
        session.removeAttribute(SocialLinkRequest.SESSION_KEY);
        return value instanceof SocialLinkRequest link ? link : null;
    }

    /** 연동 실패: 연동하던 회원으로 다시 로그인시켜 회원정보 수정 화면으로 돌려보낸다. */
    private void finishWithError(HttpServletRequest request, HttpServletResponse response,
                                 SocialLinkRequest link, String code) throws IOException {
        if (link != null && signIn(link.userId(), link.loginMethod(), request, response)) {
            redirect(response, "/account/profile?socialError=" + code);
        } else {
            signOut(request, response);
            redirect(response, "/login?socialError=" + code);
        }
    }

    /** 이메일 로그인과 같은 형태(이름 = 회원 번호)의 세션 로그인을 만든다. */
    private boolean signIn(Long userId, String loginMethod, HttpServletRequest request, HttpServletResponse response) {
        User user = users.findById(userId).filter(u -> "ACTIVE".equals(u.getStatus())).orElse(null);
        if (user == null) {
            signOut(request, response);
            return false;
        }
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                user.getUserId().toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        HttpSession session = request.getSession();
        session.setAttribute("loginMethod", loginMethod == null ? "UNKNOWN" : loginMethod);
        // ActiveAccountFilter 가 비밀번호 변경 여부를 확인하는 값
        session.setAttribute("credentialStamp", user.getPasswordHash());
        return true;
    }

    private void signOut(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        contexts.saveContext(SecurityContextHolder.createEmptyContext(), request, response);
    }

    private void redirect(HttpServletResponse response, String path) throws IOException {
        response.sendRedirect(frontendUrl + path);
    }
}