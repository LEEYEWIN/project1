package kr.fast.Jejuro.Service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 탈퇴 요청 후 보관 기간(기본 30일)이 지난 회원을 매일 새벽 자동으로 완전 삭제한다.
 * 삭제 내용은 DB 프로시저 sp_purge_user 가 처리한다.
 *  - 삭제: 여행·경로·여행 후기·동행자·설문·AI 추천 기록·찜·관심없음·소셜 연동·좋아요·신고
 *  - 남김: 게시글·댓글 (작성자는 "탈퇴한 회원"으로 표시)
 */
@Service
public class WithdrawalPurgeService {

    private static final Logger log = LoggerFactory.getLogger(WithdrawalPurgeService.class);

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final int retentionDays;

    public WithdrawalPurgeService(JdbcTemplate jdbc, PlatformTransactionManager transactionManager,
                                  @Value("${withdrawal.retention-days:30}") int retentionDays) {
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(transactionManager);
        this.retentionDays = retentionDays;
    }

    /** 매일 04:00 (한국 시간). 시간은 application.properties 의 withdrawal.purge-cron 으로 바꿀 수 있다. */
    @Scheduled(cron = "${withdrawal.purge-cron:0 0 4 * * *}", zone = "Asia/Seoul")
    public void purgeExpiredWithdrawals() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(retentionDays);
        List<Long> userIds = jdbc.queryForList(
                "SELECT user_id FROM `user` WHERE status = 'WITHDRAWAL_PENDING' AND withdrawn_at <= ? ORDER BY user_id",
                Long.class, deadline);
        if (userIds.isEmpty()) return;

        int done = 0;
        for (Long userId : userIds) {
            try {
                // 회원 한 명씩 한 트랜잭션: 중간에 실패하면 그 회원만 되돌리고 다음 회원으로 넘어간다.
                tx.executeWithoutResult(status -> jdbc.update("CALL sp_purge_user(?)", userId));
                done++;
            } catch (RuntimeException e) {
                log.error("탈퇴 회원 자동 삭제 실패 user_id={}: {}", userId, e.getMessage(), e);
            }
        }
        log.info("탈퇴 회원 자동 삭제: 대상 {}명 중 {}명 삭제 (탈퇴 후 {}일 경과)", userIds.size(), done, retentionDays);
    }
}
