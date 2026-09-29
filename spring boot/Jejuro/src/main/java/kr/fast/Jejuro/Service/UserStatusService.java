package kr.fast.Jejuro.Service;


// [커뮤니티 신고 - 이용 정지 회원 막기]

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.Entity.User;
import kr.fast.Jejuro.Repository.UserRepository;

/**
 * 이용 정지 중인 회원은 글·댓글 쓰기·수정, 신고를 할 수 없다. (읽기·여행 기능은 그대로)
 * 관리자 여부 확인도 여기서 (신고로 가린 글을 관리자는 볼 수 있게)
 */
@Service
public class UserStatusService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserRepository userRepository;

    public UserStatusService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** 글·댓글·신고 전에 호출. 정지 중이면 403 */
    public void checkCanWrite(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("회원을 찾을 수 없습니다."));
        if (u.isSuspended(LocalDateTime.now())) {
            String until = u.isBanned() ? "영구 정지된 계정입니다." : u.getSuspendedUntil().format(FMT) + "까지 이용이 정지되었습니다.";
            throw new ApiException(HttpStatus.FORBIDDEN, "커뮤니티 이용이 제한된 계정이에요. " + until);
        }
    }

    public boolean isAdmin(Long userId) {
        return userId != null && userRepository.findById(userId).map(User::isAdmin).orElse(false);
    }
}