package kr.fast.Jejuro.Entity;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "companion")
public class Companion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long companionId;
    private Long travelId;
    private Integer companionSeq;
    private Integer relationCode;
    private Integer genderCode;
    private Integer ageGroupCode;

    protected Companion() {
    }

    public Companion(Long travelId, int companionSeq, int relationCode, int genderCode, int ageGroupCode) {
        this.travelId = travelId;
        this.companionSeq = companionSeq;
        this.relationCode = relationCode;
        this.genderCode = genderCode;
        this.ageGroupCode = ageGroupCode;
    }

    public Long getCompanionId() { return companionId; }
    public Long getTravelId() { return travelId; }
    public Integer getCompanionSeq() { return companionSeq; }
    public Integer getRelationCode() { return relationCode; }
    public Integer getGenderCode() { return genderCode; }
    public Integer getAgeGroupCode() { return ageGroupCode; }
}
