package kr.fast.Jejuro.Controller;

// [공통 (로그인 대신 테스트 회원)]

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.fast.Jejuro.Repository.UserRepository;

/**
 * 테스트 회원 목록: 화면 상단의 "테스트 회원" 선택 상자가 사용한다.
 * app.test-login.enabled=false 로 두면 이 API 자체가 생기지 않는다(배포 시).
 */
@RestController
@RequestMapping("/api/test-users")
@ConditionalOnProperty(name = "app.test-login.enabled", havingValue = "true", matchIfMissing = true)
public class TestUserController {

    private final UserRepository userRepository;

    public TestUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<TestUser> list() {
        return userRepository.findTop20ByStatusOrderByUserId("ACTIVE").stream()
                .map(u -> new TestUser(u.getUserId(), u.getNickname(), u.getEmail()))
                .toList();
    }

    public record TestUser(Long userId, String nickname, String email) {
    }
}