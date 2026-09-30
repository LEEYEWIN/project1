package kr.fast.Jejuro.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** 설문 질문. preference_id는 101, 201처럼 직접 정한 번호라 자동 발급이 아니다. */
@Entity
@Table(name = "preference")
public class Preference {

    @Id
    private Long preferenceId;
    private String preferenceCode;
    private String preferenceName;

    @Enumerated(EnumType.STRING)
    private ResponseType responseType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_code")
    private PreferenceGroup group;

    protected Preference() {
    }

    public Long getPreferenceId() { return preferenceId; }
    public String getPreferenceCode() { return preferenceCode; }
    public String getPreferenceName() { return preferenceName; }
    public ResponseType getResponseType() { return responseType; }
    public PreferenceGroup getGroup() { return group; }
}