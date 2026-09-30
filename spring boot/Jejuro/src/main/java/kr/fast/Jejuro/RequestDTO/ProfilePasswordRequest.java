package kr.fast.Jejuro.RequestDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record ProfilePasswordRequest(
 @NotBlank @Size(max=72) String currentPassword,
 @NotBlank @Size(min=8, max=72) String newPassword,
 @NotBlank @Size(max=72) String confirmPassword) {}
