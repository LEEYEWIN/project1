package kr.fast.Jejuro.Service;


// [커뮤니티 신고 - 관리자 확인]

import org.springframework.stereotype.Service;

import kr.fast.Jejuro.Entity.User;
import kr.fast.Jejuro.Repository.UserRepository;

/**
 * 관리자 여부 확인 (신고된 글·차단된 글을 관리자는 내용까지 볼 수 있게)
 * (회원 제재·이용 정지 기능은 넣지 않기로 해서 20차에 삭제)
 */
@Service
public class UserStatusService {

    private final UserRepository userRepository;

    public UserStatusService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isAdmin(Long userId) {
        return userId != null && userRepository.findById(userId).map(User::isAdmin).orElse(false);
    }
}