package kr.fast.Jejuro.Service;


import java.util.Locale;
import java.util.Map;

import kr.fast.Jejuro.Config.ApiException;

/** 연동할 수 있는 소셜 로그인 제공자. DB(SOCIAL_ACCOUNT.provider)에는 이름(KAKAO 등)을 저장한다. */
public enum SocialProvider {
    KAKAO("kakao", "카카오"),
    NAVER("naver", "네이버"),
    GOOGLE("google", "구글");

    private final String registrationId;
    private final String label;

    SocialProvider(String registrationId, String label) {
        this.registrationId = registrationId;
        this.label = label;
    }

    /** application-local.properties 의 spring.security.oauth2.client.registration.{이 값} */
    public String registrationId() { return registrationId; }

    public String label() { return label; }

    /** 주소의 kakao / KAKAO 둘 다 받는다. */
    public static SocialProvider from(String value) {
        if (value != null) {
            for (SocialProvider p : values()) {
                if (p.registrationId.equals(value.toLowerCase(Locale.ROOT))) return p;
            }
        }
        throw ApiException.badRequest("지원하지 않는 소셜 로그인입니다.");
    }

    /** 제공자가 돌려준 사용자 정보에서 회원 번호(고유 ID)를 꺼낸다. */
    public String providerUserId(Map<String, Object> attributes) {
        Object id = switch (this) {
            case KAKAO -> attributes.get("id");
            case NAVER -> attributes.get("response") instanceof Map<?, ?> response ? response.get("id") : null;
            case GOOGLE -> attributes.get("sub");
        };
        if (id == null || id.toString().isBlank()) {
            throw new IllegalStateException(label + " 사용자 번호를 받지 못했습니다.");
        }
        return id.toString();
    }
}