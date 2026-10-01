package kr.fast.Jejuro.Service;


import java.util.List;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.Config.ApiException;

/** SOCIAL_ACCOUNT 조회·연동·해제. 소셜로 새 회원을 만들지 않는다. */
@Service
public class SocialAccountService {

    private final JdbcTemplate jdbc;

    public SocialAccountService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 이 소셜 계정이 연동된 회원 번호. 없으면 비어 있다. */
    @Transactional(readOnly = true)
    public Optional<Long> findUserId(SocialProvider provider, String providerUserId) {
        List<Long> ids = jdbc.queryForList(
                "SELECT user_id FROM SOCIAL_ACCOUNT WHERE provider = ? AND provider_user_id = ?",
                Long.class, provider.name(), providerUserId);
        return ids.stream().findFirst();
    }

    @Transactional(readOnly = true)
    public boolean hasProvider(Long userId, SocialProvider provider) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM SOCIAL_ACCOUNT WHERE user_id = ? AND provider = ?",
                Integer.class, userId, provider.name());
        return count != null && count > 0;
    }

    /** 로그인한 회원에 소셜 계정을 붙인다. 같은 계정을 다시 연동하면 그대로 둔다. */
    @Transactional
    public void link(Long userId, SocialProvider provider, String providerUserId) {
        Optional<Long> owner = findUserId(provider, providerUserId);
        if (owner.isPresent()) {
            if (owner.get().equals(userId)) return;
            throw new SocialLinkException("ALREADY_LINKED_OTHER",
                    "이 " + provider.label() + " 계정은 이미 다른 회원에 연동되어 있습니다.");
        }
        if (hasProvider(userId, provider)) {
            throw new SocialLinkException("PROVIDER_ALREADY_LINKED",
                    "이미 연동한 " + provider.label() + " 계정이 있습니다. 해제한 뒤 다시 연동해 주세요.");
        }
        try {
            jdbc.update("INSERT INTO SOCIAL_ACCOUNT (user_id, provider, provider_user_id) VALUES (?, ?, ?)",
                    userId, provider.name(), providerUserId);
        } catch (DuplicateKeyException e) {
            // 동시에 두 번 눌렀거나 그 사이 다른 회원이 연동한 경우
            throw new SocialLinkException("ALREADY_LINKED_OTHER",
                    "이 " + provider.label() + " 계정은 이미 연동되어 있습니다.");
        }
    }

    /** 연동 해제. 모든 회원은 비밀번호가 있으므로 항상 해제할 수 있다. */
    @Transactional
    public void unlink(Long userId, SocialProvider provider) {
        int deleted = jdbc.update("DELETE FROM SOCIAL_ACCOUNT WHERE user_id = ? AND provider = ?",
                userId, provider.name());
        if (deleted == 0) {
            throw ApiException.notFound("연동된 " + provider.label() + " 계정이 없습니다.");
        }
    }
}