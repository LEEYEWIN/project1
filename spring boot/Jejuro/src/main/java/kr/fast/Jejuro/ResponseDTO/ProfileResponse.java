package kr.fast.Jejuro.ResponseDTO;
import java.util.List;
public record ProfileResponse(Long userId, String email, String nickname, String role,
                              String loginMethod, List<String> socialProviders) {}
