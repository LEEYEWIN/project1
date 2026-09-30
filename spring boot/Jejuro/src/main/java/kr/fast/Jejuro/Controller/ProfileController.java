package kr.fast.Jejuro.Controller;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;
import kr.fast.Jejuro.Config.CurrentUser;
import kr.fast.Jejuro.Service.ProfileService;
import kr.fast.Jejuro.RequestDTO.ProfileNicknameRequest;
import kr.fast.Jejuro.RequestDTO.ProfilePasswordRequest;
import kr.fast.Jejuro.ResponseDTO.ProfileResponse;

@RestController
@RequestMapping("/api/me/profile")
public class ProfileController {
 private final CurrentUser currentUser;
 private final ProfileService service;
 public ProfileController(CurrentUser currentUser, ProfileService service) { this.currentUser=currentUser; this.service=service; }
 @GetMapping
 public ProfileResponse profile(HttpServletRequest request) {
     Object method=request.getSession().getAttribute("loginMethod");
     return service.profile(currentUser.id(), method instanceof String ? (String)method : "UNKNOWN");
 }
 @PatchMapping("/nickname") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void nickname(@Valid @RequestBody ProfileNicknameRequest req) { service.nickname(currentUser.id(),req.nickname()); }
 @PostMapping("/password") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void password(@Valid @RequestBody ProfilePasswordRequest req, HttpServletRequest request, HttpServletResponse response) {
     service.password(currentUser.id(),req);
     new SecurityContextLogoutHandler().logout(request,response,SecurityContextHolder.getContext().getAuthentication());
 }
}
