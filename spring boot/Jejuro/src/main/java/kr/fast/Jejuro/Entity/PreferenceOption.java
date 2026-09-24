package kr.fast.Jejuro.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 질문의 선택지. 답변 저장에는 option_id가 아니라 (preferenceId, optionValue)를 쓴다. */
@Entity
@Table(name = "preference_option")
public class PreferenceOption {

    @Id
    private Long optionId;
    private Long preferenceId;
    private Integer optionValue;
    private String optionName;
    private String description;

    protected PreferenceOption() {
    }

    public Long getOptionId() { return optionId; }
    public Long getPreferenceId() { return preferenceId; }
    public Integer getOptionValue() { return optionValue; }
    public String getOptionName() { return optionName; }
    public String getDescription() { return description; }
}