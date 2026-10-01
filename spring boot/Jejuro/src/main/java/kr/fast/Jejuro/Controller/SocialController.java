package kr.fast.Jejuro.Controller;


import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.Config.CurrentUser;
import kr.fast.Jejuro.Config.SocialProviderRegistry;
import kr.fast.Jejuro.Service.SocialAccountService;
import kr.fast.Jejuro.Service.SocialLinkRequest;
import kr.fast.Jejuro.Service.SocialProvider;

/** 소셜 계정 연동. 소셜 로그인 자체는 Spring Security(/oauth2/authorization/{provider})가 처리한다. */
@RestController
public class SocialController {

    private final SocialProviderRegistry registry;
    private final SocialAccountService socialAccounts;
    private final CurrentUser currentUser;

    public SocialController(SocialProviderRegistry registry, SocialAccountService socialAccounts,
                            CurrentUser currentUser) {
        this.registry = registry;
        this.socialAccounts = socialAccounts;
        this.currentUser = currentUser;
    }

    /** 키가 설정되어 사용할 수 있는 제공자 (로그인 화면 버튼용, 비로그인 허용) */
    @GetMapping("/api/auth/social/providers")
    public Map<String, Object> providers() {
        return Map.of("providers", registry.enabledProviders().stream().map(Enum::name).toList());
    }

    /** 연동 시작: 세션에 연동 표시를 남기고, 브라우저가 이동할 제공자 로그인 주소를 돌려준다. */
    @PostMapping("/api/me/social/{provider}/link")
    public Map<String, String> startLink(@PathVariable("provider") String provider, HttpServletRequest request) {
        SocialProvider p = SocialProvider.from(provider);
        if (!registry.enabled(p)) {
            throw ApiException.badRequest(p.label() + " 로그인이 아직 설정되지 않았습니다.");
        }
        Long userId = currentUser.id();
        if (socialAccounts.hasProvider(userId, p)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "이미 연동한 " + p.label() + " 계정이 있습니다. 해제한 뒤 다시 연동해 주세요.");
        }
        Object method = request.getSession().getAttribute("loginMethod");
        request.getSession().setAttribute(SocialLinkRequest.SESSION_KEY,
                new SocialLinkRequest(userId, p, method instanceof String s ? s : "EMAIL", System.currentTimeMillis()));
        return Map.of("url", "/oauth2/authorization/" + p.registrationId());
    }

    @DeleteMapping("/api/me/social/{provider}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(@PathVariable("provider") String provider) {
        socialAccounts.unlink(currentUser.id(), SocialProvider.from(provider));
    }
}