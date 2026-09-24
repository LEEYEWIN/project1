package kr.fast.Jejuro.Entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "travel")
public class Travel {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // AUTO_INCREMENT
    private Long travelId;

    private Long userId;
    private Integer travelNo;
    private String travelName;
    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private RegionMode regionMode;

    private Integer ageGroupSnapshot;

    /** 최종 채택 경로. TRAVEL ↔ TRAVEL_ROUTE 순환 참조라서 객체 연결 대신 ID만 둔다(7페이지에서 사용). */
    private Long adoptedRouteId;
    private LocalDateTime adoptedAt;

    /** DB 기본값(CURRENT_TIMESTAMP)이 채우므로 JPA는 읽기만 한다. */
    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected Travel() {
    }

    public static Travel create(Long userId, int travelNo, String travelName,
                                LocalDate startDate, LocalDate endDate,
                                RegionMode regionMode, int ageGroup) {
        Travel t = new Travel();
        t.userId = userId;
        t.travelNo = travelNo;
        t.travelName = travelName;
        t.startDate = startDate;
        t.endDate = endDate;
        t.regionMode = regionMode;
        t.ageGroupSnapshot = ageGroup;
        return t;
    }

    /** 여행 일수. 10/20~10/22 → 3 */
    public int tripDays() {
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /** N일차의 실제 날짜 */
    public LocalDate dateOf(int dayNo) {
        return startDate.plusDays(dayNo - 1L);
    }

    /** 최종 경로 채택. DB CHECK 규칙대로 경로 ID와 채택 시각을 항상 함께 바꾼다. */
    public void adopt(Long routeId, LocalDateTime now) {
        this.adoptedRouteId = routeId;
        this.adoptedAt = now;
    }

    public void clearAdoption() {
        this.adoptedRouteId = null;
        this.adoptedAt = null;
    }

    public boolean isAdopted(Long routeId) {
        return routeId != null && routeId.equals(adoptedRouteId);
    }

    public Long getTravelId() { return travelId; }
    public Long getUserId() { return userId; }
    public Integer getTravelNo() { return travelNo; }
    public String getTravelName() { return travelName; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public RegionMode getRegionMode() { return regionMode; }
    public Integer getAgeGroupSnapshot() { return ageGroupSnapshot; }
    public Long getAdoptedRouteId() { return adoptedRouteId; }
    public LocalDateTime getAdoptedAt() { return adoptedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}