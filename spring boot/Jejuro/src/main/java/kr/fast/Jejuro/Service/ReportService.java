package kr.fast.Jejuro.Service;


// [커뮤니티 신고 · 관리자 신고 처리]

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.Entity.CommunityComment;
import kr.fast.Jejuro.Entity.CommunityPost;
import kr.fast.Jejuro.Entity.Report;
import kr.fast.Jejuro.Entity.User;
import kr.fast.Jejuro.Repository.CommunityCommentRepository;
import kr.fast.Jejuro.Repository.CommunityPostRepository;
import kr.fast.Jejuro.Repository.ReportRepository;
import kr.fast.Jejuro.Repository.UserRepository;
import kr.fast.Jejuro.RequestDTO.ReportHandleRequest;
import kr.fast.Jejuro.RequestDTO.ReportRequest;
import kr.fast.Jejuro.ResponseDTO.AdminReportResponse;

/**
 * 신고 규칙 (처리 기준 전체는 ReportPolicy · docs/20). 회원 제재(정지·경고)는 하지 않는다.
 * 1) 신고: 로그인 회원, 삭제·차단되지 않은 글·댓글, 내가 쓴 것은 불가, 같은 대상은 1번만(409).
 *    사유는 음란·불법 / 개인정보 노출 / 욕설·비방 / 스팸·광고.
 * 2) 신고가 들어오면 바로 "신고된 게시글입니다"로 표시 (hidden_at) → 관리자 확인을 기다린다.
 *    다른 회원도 같은 글을 추가로 신고할 수 있다 (사유 집계용).
 * 3) 관리자 처리(대상 단위로 대기 중인 신고를 한 번에):
 *    KEEP 반려 → 신고 REJECTED, 정상 표시 / BLOCK 차단 → ACCEPTED, 차단 사유 알림 후 목록으로(모든 회원)
 */
@Service
public class ReportService {

    private static final int PAGE_SIZE = 20;
    private static final int PREVIEW = 300;

    private final ReportRepository reportRepository;
    private final CommunityPostRepository postRepository;
    private final CommunityCommentRepository commentRepository;
    private final UserRepository userRepository;

    public ReportService(ReportRepository reportRepository, CommunityPostRepository postRepository,
                         CommunityCommentRepository commentRepository, UserRepository userRepository) {
        this.reportRepository = reportRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    // ================================================================== 회원: 신고하기

    /** 신고 → 대상은 관리자 확인 전까지 "신고된 게시글입니다"로 표시 (항상 true) */
    @Transactional
    public boolean report(Long userId, ReportRequest req) {
        String detail = req.detail() == null || req.detail().isBlank() ? null : req.detail().trim();

        Target t = loadTarget(req.targetType(), req.targetId());   // 대상 행을 잠가 동시 신고도 정확히 센다
        if (t.deleted()) {
            throw ApiException.notFound("삭제되었거나 없는 " + t.label() + "입니다.");
        }
        if (t.blocked()) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 관리자가 차단한 " + t.label() + "입니다.");
        }
        if (userId.equals(t.authorId())) {
            throw ApiException.badRequest("내가 쓴 " + t.label() + "은(는) 신고할 수 없습니다.");
        }
        if (reportRepository.existsByTargetTypeAndTargetIdAndReporterId(req.targetType(), req.targetId(), userId)) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 신고한 " + t.label() + "입니다. 관리자가 확인 중이에요.");
        }
        reportRepository.save(new Report(req.targetType(), req.targetId(), t.authorId(), userId, req.reasonCode(), detail));
        hide(t, LocalDateTime.now());   // 신고된 게시글 (이미 신고된 상태면 그대로)
        return true;
    }

    // ================================================================== 관리자: 목록

    /**
     * @param status PENDING(처리 대기) / DONE(처리 완료, 최근 500건)
     * @param type   POST / COMMENT / null(전체)
     */
    @Transactional(readOnly = true)
    public AdminReportResponse list(String status, String type, int page) {
        boolean pendingOnly = !"DONE".equalsIgnoreCase(status);
        List<Report> reports = pendingOnly
                ? reportRepository.findByStatusOrderByReportIdAsc(Report.PENDING)
                : reportRepository.findTop500ByStatusNotOrderByHandledAtDesc(Report.PENDING);
        if (type != null && Report.TARGET_TYPES.contains(type)) {
            reports = reports.stream().filter(r -> r.getTargetType().equals(type)).toList();
        }

        // 대상(글·댓글)별로 묶기
        Map<String, List<Report>> byTarget = new LinkedHashMap<>();
        for (Report r : reports) {
            byTarget.computeIfAbsent(key(r.getTargetType(), r.getTargetId()), k -> new ArrayList<>()).add(r);
        }
        List<List<Report>> groups = new ArrayList<>(byTarget.values());
        // 대기: 신고 많은 순 → 오래된 순 / 완료: 최근 처리 순
        if (pendingOnly) {
            groups.sort(Comparator.<List<Report>>comparingInt(List::size).reversed()
                    .thenComparing(g -> g.get(0).getReportId()));
        } else {
            groups.sort(Comparator.comparing((List<Report> g) -> latestHandled(g).getHandledAt(),
                    Comparator.nullsLast(Comparator.reverseOrder())));
        }

        long total = groups.size();
        int safePage = Math.max(page, 0);
        List<List<Report>> pageGroups = groups.stream().skip((long) safePage * PAGE_SIZE).limit(PAGE_SIZE).toList();

        List<AdminReportResponse.Target> items = toTargets(pageGroups);
        long pendingTargets = pendingOnly && type == null ? total : countPendingTargets();
        return new AdminReportResponse(pendingTargets, safePage, (int) Math.ceil(total / (double) PAGE_SIZE), total, items);
    }

    // ================================================================== 관리자: 처리

    @Transactional
    public void handle(Long adminId, String targetType, Long targetId, ReportHandleRequest req) {
        if (!Report.TARGET_TYPES.contains(targetType)) {
            throw ApiException.badRequest("신고 대상은 POST 또는 COMMENT 입니다.");
        }
        Target t = loadTarget(targetType, targetId);   // 먼저 대상 행을 잠가 두 관리자가 동시에 처리해도 한 번만
        List<Report> pending = reportRepository.findByTargetTypeAndTargetIdAndStatus(targetType, targetId, Report.PENDING);
        if (pending.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 처리된 신고입니다. 목록을 새로고침해 주세요.");
        }

        LocalDateTime now = LocalDateTime.now();
        Map<String, Long> counts = pending.stream()
                .collect(Collectors.groupingBy(Report::getReasonCode, Collectors.counting()));
        switch (req.action()) {
            case Report.KEEP -> unhide(t);   // 반려 → 정상 표시
            case Report.BLOCK -> block(t, req.blockReason() != null ? req.blockReason() : ReportPolicy.mainReason(counts), now);
            default -> throw ApiException.badRequest("처리 방법은 KEEP(반려) / BLOCK(차단) 입니다.");
        }
        pending.forEach(r -> r.handle(req.action(), adminId, now));
    }

    /** 차단 해제 (잘못 차단했을 때): 가림·차단 사유를 지워 다시 보이게. 신고 기록은 그대로 둔다 */
    @Transactional
    public void unblock(String targetType, Long targetId) {
        if (!Report.TARGET_TYPES.contains(targetType)) {
            throw ApiException.badRequest("신고 대상은 POST 또는 COMMENT 입니다.");
        }
        Target t = loadTarget(targetType, targetId);
        if (!t.blocked()) {
            throw new ApiException(HttpStatus.CONFLICT, "차단된 " + t.label() + "이 아닙니다.");
        }
        unhide(t);
    }

    // ================================================================== 내부

    /** 신고 대상 공통 모양 */
    private record Target(String type, Long id, Long authorId, boolean deleted, boolean blocked,
                          CommunityPost post, CommunityComment comment) {
        String label() {
            return Report.POST.equals(type) ? "글" : "댓글";
        }
    }

    /** 대상 글·댓글을 잠가서 읽는다 (SELECT … FOR UPDATE) */
    private Target loadTarget(String type, Long id) {
        if (Report.POST.equals(type)) {
            CommunityPost p = postRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> ApiException.notFound("삭제되었거나 없는 글입니다."));
            return new Target(type, id, p.getUserId(), p.isDeleted(), p.isBlocked(), p, null);
        }
        CommunityComment c = commentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> ApiException.notFound("삭제되었거나 없는 댓글입니다."));
        // 글이 지워졌으면 댓글도 지워진 것으로 본다
        boolean postDeleted = postRepository.findById(c.getPostId()).map(CommunityPost::isDeleted).orElse(true);
        return new Target(type, id, c.getUserId(), c.isDeleted() || postDeleted, c.isBlocked(), null, c);
    }

    private static void hide(Target t, LocalDateTime now) {
        if (t.post() != null) t.post().hide(now);
        else t.comment().hide(now);
    }

    private static void block(Target t, String reason, LocalDateTime now) {
        if (t.post() != null) t.post().block(reason, now);
        else t.comment().block(reason, now);
    }

    private static void unhide(Target t) {
        if (t.post() != null) t.post().unhide();
        else t.comment().unhide();
    }

    private static String key(String type, Long id) {
        return type + ":" + id;
    }

    private static Report latestHandled(List<Report> g) {
        return g.stream().filter(r -> r.getHandledAt() != null)
                .max(Comparator.comparing(Report::getHandledAt)).orElse(g.get(0));
    }

    private long countPendingTargets() {
        return reportRepository.findByStatusOrderByReportIdAsc(Report.PENDING).stream()
                .map(r -> key(r.getTargetType(), r.getTargetId())).distinct().count();
    }

    /** 묶은 신고 → 화면 한 줄 (글·댓글·작성자·이전 조치 수는 한 번에 모아서 읽는다) */
    private List<AdminReportResponse.Target> toTargets(List<List<Report>> groups) {
        Set<Long> postIds = new HashSet<>();
        Set<Long> commentIds = new HashSet<>();
        for (List<Report> g : groups) {
            Report r = g.get(0);
            (Report.POST.equals(r.getTargetType()) ? postIds : commentIds).add(r.getTargetId());
        }
        Map<Long, CommunityComment> comments = commentRepository.findAllById(commentIds).stream()
                .collect(Collectors.toMap(CommunityComment::getCommentId, Function.identity()));
        comments.values().forEach(c -> postIds.add(c.getPostId()));
        Map<Long, CommunityPost> posts = postRepository.findAllById(postIds).stream()
                .collect(Collectors.toMap(CommunityPost::getPostId, Function.identity()));

        Set<Long> userIds = new HashSet<>();
        for (List<Report> g : groups) {
            g.forEach(r -> {
                if (r.getTargetUserId() != null) userIds.add(r.getTargetUserId());
                if (r.getHandledBy() != null) userIds.add(r.getHandledBy());
            });
        }
        posts.values().forEach(p -> { if (p.getUserId() != null) userIds.add(p.getUserId()); });
        comments.values().forEach(c -> { if (c.getUserId() != null) userIds.add(c.getUserId()); });
        Map<Long, User> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));

        // 작성자별로 조치받은 대상 (이번 대상 제외하고 셀 때 사용)
        Map<Long, Set<String>> acceptedByAuthor = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (Report r : reportRepository.findByStatusAndTargetUserIdIn(Report.ACCEPTED, userIds)) {
                acceptedByAuthor.computeIfAbsent(r.getTargetUserId(), k -> new HashSet<>())
                        .add(key(r.getTargetType(), r.getTargetId()));
            }
        }

        List<AdminReportResponse.Target> result = new ArrayList<>();
        for (List<Report> g : groups) {
            Report first = g.get(0);
            String type = first.getTargetType();
            Long id = first.getTargetId();

            CommunityPost post;
            CommunityComment comment = null;
            if (Report.POST.equals(type)) {
                post = posts.get(id);
            } else {
                comment = comments.get(id);
                post = comment == null ? null : posts.get(comment.getPostId());
            }
            Long authorId = comment != null ? comment.getUserId()
                    : post != null ? post.getUserId() : first.getTargetUserId();
            User author = authorId == null ? null : users.get(authorId);

            String content;
            boolean hidden;
            boolean deleted;
            String blockReason = comment != null ? comment.getBlockReason() : post != null ? post.getBlockReason() : null;
            if (comment != null) {
                content = cut(comment.getContent());
                hidden = comment.isHidden();
                deleted = comment.isDeleted() || post == null || post.isDeleted();
            } else if (post != null) {
                content = cut(post.getContent());
                hidden = post.isHidden();
                deleted = post.isDeleted();
            } else {
                content = null;
                hidden = false;
                deleted = true;
            }

            Map<String, Long> counts = g.stream()
                    .collect(Collectors.groupingBy(Report::getReasonCode, LinkedHashMap::new, Collectors.counting()));
            List<AdminReportResponse.ReasonCount> reasons = ReportPolicy.REASONS.keySet().stream()
                    .filter(counts::containsKey)
                    .map(code -> new AdminReportResponse.ReasonCount(code, ReportPolicy.label(code), counts.get(code)))
                    .sorted(Comparator.comparingLong(AdminReportResponse.ReasonCount::count).reversed())
                    .toList();
            List<String> details = g.stream().map(Report::getDetail).filter(Objects::nonNull).limit(5).toList();

            long prior = acceptedByAuthor.getOrDefault(authorId, Set.of()).stream()
                    .filter(k -> !k.equals(key(type, id))).count();

            Report handled = latestHandled(g);
            boolean isPending = Report.PENDING.equals(first.getStatus());
            User handler = isPending || handled.getHandledBy() == null ? null : users.get(handled.getHandledBy());

            result.add(new AdminReportResponse.Target(type, id, post == null ? null : post.getPostId(),
                    post == null ? null : post.getTitle(), content,
                    authorId, author == null ? (authorId == null ? "탈퇴한 회원" : "알 수 없음") : author.getNickname(),
                    prior,
                    hidden, blockReason, deleted, g.size(), reasons, details,
                    g.stream().map(Report::getCreatedAt).filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null),
                    g.stream().map(Report::getCreatedAt).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null),
                    isPending ? Report.PENDING : handled.getStatus(),
                    isPending ? null : handled.getAction(),
                    isPending ? null : handled.getHandledAt(),
                    handler == null ? null : handler.getNickname(),
                    ReportPolicy.mainReason(counts)));
        }
        return result;
    }

    private static String cut(String s) {
        if (s == null) return null;
        return s.length() <= PREVIEW ? s : s.substring(0, PREVIEW) + "…";
    }
}