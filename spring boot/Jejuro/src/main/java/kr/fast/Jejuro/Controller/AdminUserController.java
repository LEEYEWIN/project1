package kr.fast.Jejuro.Controller;


// [관리자 회원 관리]

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.fast.Jejuro.Config.AdminGuard;
import kr.fast.Jejuro.Config.CurrentUser;
import kr.fast.Jejuro.RequestDTO.RoleRequest;
import kr.fast.Jejuro.RequestDTO.SanctionRequest;
import kr.fast.Jejuro.ResponseDTO.AdminUserResponse;
import kr.fast.Jejuro.Service.AdminUserService;

/** 관리자 전용 (USER.role = ADMIN). 아니면 403 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService userService;
    private final AdminGuard adminGuard;
    private final CurrentUser currentUser;

    public AdminUserController(AdminUserService userService, AdminGuard adminGuard, CurrentUser currentUser) {
        this.userService = userService;
        this.adminGuard = adminGuard;
        this.currentUser = currentUser;
    }

    /** GET /api/admin/users?keyword=&filter=ALL|SUSPENDED|REPORTED|ADMIN&page=0 */
    @GetMapping
    public AdminUserResponse list(@RequestParam(name = "keyword", required = false) String keyword,
                                  @RequestParam(name = "filter", defaultValue = "ALL") String filter,
                                  @RequestParam(name = "page", defaultValue = "0") int page) {
        adminGuard.check();
        return userService.list(keyword, filter, page);
    }

    /** 회원 상세 + 제재 이력 + 조치된 신고 */
    @GetMapping("/{userId}")
    public AdminUserResponse.Detail detail(@PathVariable("userId") Long userId) {
        adminGuard.check();
        return userService.detail(userId);
    }

    /** 제재 { "type": "SUSPEND_7D", "reason": "..." } */
    @PostMapping("/{userId}/sanctions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sanction(@PathVariable("userId") Long userId, @Valid @RequestBody SanctionRequest req) {
        adminGuard.check();
        userService.sanction(currentUser.id(), userId, req.type(), req.reason());
    }

    /** 역할 변경 { "role": "ADMIN" } */
    @PutMapping("/{userId}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void role(@PathVariable("userId") Long userId, @Valid @RequestBody RoleRequest req) {
        adminGuard.check();
        userService.changeRole(currentUser.id(), userId, req.role());
    }
}