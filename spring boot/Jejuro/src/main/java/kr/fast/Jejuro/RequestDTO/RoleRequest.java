package kr.fast.Jejuro.RequestDTO;


// [관리자 회원 관리 - 역할]

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** PUT /api/admin/users/{userId}/role  { "role": "ADMIN" } */
public record RoleRequest(@NotBlank @Pattern(regexp = "USER|ADMIN") String role) {
}