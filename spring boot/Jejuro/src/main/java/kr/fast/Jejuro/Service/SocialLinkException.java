package kr.fast.Jejuro.Service;


/** 소셜 연동 실패. code 는 화면 주소(?socialError=)로 넘겨 안내 문구를 고른다. */
public class SocialLinkException extends RuntimeException {
    private final String code;

    public SocialLinkException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }
}