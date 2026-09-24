package kr.fast.Jejuro.Entity;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "travel_region")
public class TravelRegion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long travelRegionId;
    private Long travelId;
    private Integer regionId;

    protected TravelRegion() {
    }

    public TravelRegion(Long travelId, Integer regionId) {
        this.travelId = travelId;
        this.regionId = regionId;
    }

    public Long getTravelRegionId() { return travelRegionId; }
    public Long getTravelId() { return travelId; }
    public Integer getRegionId() { return regionId; }
}