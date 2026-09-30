package kr.fast.Jejuro.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/** 관광지 마스터와 관리자 화면에서 수정하는 정보. */
@Entity
@Table(name = "POI")
public class Poi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long poiId;

    private String poiName;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String categoryCode;
    private Integer regionId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 2048)
    private String imageUrl;

    // DB에 없는 컬럼들
    @Transient
    private String detailDescription;

    @Transient
    private LocalDateTime hiddenAt;

    @Transient
    private LocalDateTime deletedAt;

    @Transient
    private boolean aiRecommend;

    protected Poi() {
    }

    public Poi(Fields fields) {
        update(fields);
    }

    public void update(Fields fields) {
        poiName = fields.poiName();
        address = fields.address();
        latitude = fields.latitude();
        longitude = fields.longitude();
        categoryCode = fields.categoryCode();
        regionId = fields.regionId();
        description = fields.description();
        detailDescription = fields.detailDescription();
        imageUrl = fields.imageUrl();
    }

    public void hide(LocalDateTime now) {
        hiddenAt = now;
    }

    public void show() {
        hiddenAt = null;
    }

    public void delete(LocalDateTime now) {
        deletedAt = now;
    }

    public void restore() {
        deletedAt = null;
    }

    public Long getPoiId() {
        return poiId;
    }

    public String getPoiName() {
        return poiName;
    }

    public String getAddress() {
        return address;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public Integer getRegionId() {
        return regionId;
    }

    public String getDescription() {
        return description;
    }

    public String getDetailDescription() {
        return detailDescription;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public LocalDateTime getHiddenAt() {
        return hiddenAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public boolean isAiRecommend() {
        return aiRecommend;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public record Fields(
        String poiName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String categoryCode,
        Integer regionId,
        String description,
        String detailDescription,
        String imageUrl
    ) {
    }
}