package kr.fast.Jejuro.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 질문 그룹과 질문당 최소/최대 선택 수. maxSelections가 null이면 상한 없음. */
@Entity
@Table(name = "preference_group")
public class PreferenceGroup {

    @Id
    private String groupCode;
    private String groupName;
    private Integer minSelections;
    private Integer maxSelections;

    protected PreferenceGroup() {
    }

    public String getGroupCode() { return groupCode; }
    public String getGroupName() { return groupName; }
    public Integer getMinSelections() { return minSelections; }
    public Integer getMaxSelections() { return maxSelections; }
}
