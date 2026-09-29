package kr.fast.Jejuro.Entity;


// [공통]

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 여행·커뮤니티에서 필요한 회원 정보만 읽는다. 회원가입 기능에서 필드를 더 채운다. */
@Entity
@DynamicUpdate   // 바뀐 칼럼만 UPDATE (신고 가림·글 수정·제재가 동시에 일어나도 서로 덮어쓰지 않게)
@Table(name = "`USER`")   // USER는 MySQL 예약어와 겹치므로 백틱으로 감싼다
public class User {

    @Id
    private Long userId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false)
    private Integer genderCode;

    @Column(nullable = false)
    private String status;

    /** USER 일반 회원 / ADMIN 관리자 (관리자 화면 접근) */
    @Column(nullable = false)
    private String role;

    /** 이 시각까지 글·댓글·신고 불가. null = 정상, 9999-12-31 = 영구 정지 */
    private LocalDateTime suspendedUntil;

    /** 누적 경고 (3회마다 7일 정지) */
    @Column(nullable = false)
    private Integer warningCount;

    protected User() {
    }

    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getNickname() { return nickname; }
    public LocalDate getBirthDate() { return birthDate; }
    public Integer getGenderCode() { return genderCode; }
    public String getStatus() { return status; }
    public String getRole() { return role; }
    public LocalDateTime getSuspendedUntil() { return suspendedUntil; }
    public int getWarningCount() { return warningCount == null ? 0 : warningCount; }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    /** 지금 이용 정지 중인가 */
    public boolean isSuspended(LocalDateTime now) {
        return suspendedUntil != null && suspendedUntil.isAfter(now);
    }

    public boolean isBanned() {
        return suspendedUntil != null && suspendedUntil.getYear() >= 9999;
    }

    // ---- 관리자 조치 (AdminUserService에서만 호출)

    /** 경고 1회 추가 → 누적 경고 수 */
    public int addWarning() {
        warningCount = getWarningCount() + 1;
        return warningCount;
    }

    /** 정지: 이미 더 긴 정지가 있으면 그대로 둔다 */
    public void suspendUntil(LocalDateTime until) {
        if (suspendedUntil == null || suspendedUntil.isBefore(until)) {
            suspendedUntil = until;
        }
    }

    /** 정지 해제 (경고 횟수는 기록으로 남긴다) */
    public void release() {
        suspendedUntil = null;
    }

    public void changeRole(String role) {
        this.role = role;
    }
}