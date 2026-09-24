package kr.fast.Jejuro.Entity;


//[공통]

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 여행·커뮤니티에서 필요한 회원 정보만 읽는다. 회원가입 기능에서 필드를 더 채운다. */
@Entity
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

 protected User() {
 }

 public Long getUserId() { return userId; }
 public String getEmail() { return email; }
 public String getNickname() { return nickname; }
 public LocalDate getBirthDate() { return birthDate; }
 public Integer getGenderCode() { return genderCode; }
 public String getStatus() { return status; }
}