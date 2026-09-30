package kr.fast.Jejuro.RequestDTO;
import jakarta.validation.constraints.*;
public record LoginRequest(@NotBlank @Email @Size(max = 255) String email,
                           @NotBlank @Size(max = 72) String password) {}
