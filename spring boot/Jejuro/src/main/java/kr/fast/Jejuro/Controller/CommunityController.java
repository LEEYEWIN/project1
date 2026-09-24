package kr.fast.Jejuro.Controller;


// [8페이지 후기 게시판]

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import kr.fast.Jejuro.RequestDTO.PostCreateRequest;
import kr.fast.Jejuro.ResponseDTO.PostResponse;
import kr.fast.Jejuro.Config.CurrentUser;

import jakarta.validation.Valid;
import kr.fast.Jejuro.Entity.PostType;
import kr.fast.Jejuro.Service.CommunityService;

@RestController
@RequestMapping("/api/community/posts")
public class CommunityController {

    private final CommunityService communityService;
    private final CurrentUser currentUser;

    public CommunityController(CommunityService communityService, CurrentUser currentUser) {
        this.communityService = communityService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> create(@Valid @RequestBody PostCreateRequest req) {
        return Map.of("postId", communityService.create(currentUser.id(), req));
    }

    /** GET /api/community/posts?type=REVIEW */
    @GetMapping
    public List<PostResponse> latest(@RequestParam(name = "type", defaultValue = "REVIEW") PostType type) {
        return communityService.latest(type, currentUser.id());
    }
}