package kr.fast.Jejuro.Service;


import java.io.Serializable;

/**
 * 회원정보 수정에서 [연동]을 누르면 세션에 잠깐 저장한다.
 * 제공자 로그인 후 돌아왔을 때 "로그인"이 아니라 "이 회원에 연동"으로 처리하기 위한 표시다.
 */
public record SocialLinkRequest(Long userId, SocialProvider provider, String loginMethod, long createdAt)
        implements Serializable {

    public static final String SESSION_KEY = "socialLinkRequest";
    private static final long VALID_MILLIS = 10 * 60 * 1000L;

    public boolean expired() {
        return System.currentTimeMillis() - createdAt > VALID_MILLIS;
    }
}