package kr.fast.Jejuro.Entity;


//[공통]

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 공통 코드(성별 GEN, 연령대 AGE, 동반자 관계 TCR). 조회 전용. */
@Entity
@Table(name = "CODE")
public class Code {

 @Id
 private Long codeId;
 private String groupCode;
 private String codeValue;
 private String codeName;

 protected Code() {
 }

 public Long getCodeId() { return codeId; }
 public String getGroupCode() { return groupCode; }
 public String getCodeValue() { return codeValue; }
 public String getCodeName() { return codeName; }
}