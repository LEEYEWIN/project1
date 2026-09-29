
// [관리자 회원 관리 - 제재 이력]

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 회원 제재 이력 1건. 현재 상태는 USER.suspended_until·warning_count, 이 표는 기록용 */
@Entity
@Table(name = "USER_SANCTION")
public class UserSanction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sanctionId;
    private Long userId;
    private String sanctionType;
    private String reason;
    private String targetType;
    private Long targetId;
    private LocalDateTime untilAt;
    private Long adminId;

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected UserSanction() {
    }

    public UserSanction(Long userId, String sanctionType, String reason, String targetType, Long targetId,
                        LocalDateTime untilAt, Long adminId) {
        this.userId = userId;
        this.sanctionType = sanctionType;
        this.reason = reason;
        this.targetType = targetType;
        this.targetId = targetId;
        this.untilAt = untilAt;
        this.adminId = adminId;
    }

    public Long getSanctionId() { return sanctionId; }
    public Long getUserId() { return userId; }
    public String getSanctionType() { return sanctionType; }
    public String getReason() { return reason; }
    public String getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public LocalDateTime getUntilAt() { return untilAt; }
    public Long getAdminId() { return adminId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
