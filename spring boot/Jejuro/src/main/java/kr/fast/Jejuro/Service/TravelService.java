package kr.fast.Jejuro.Service;

// [1·7페이지 여행 (생성 / 채택·삭제)]

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.Entity.TravelRoute;
import kr.fast.Jejuro.Repository.TravelRouteRepository;
import kr.fast.Jejuro.Repository.RegionRepository;
import kr.fast.Jejuro.Entity.Preference;
import kr.fast.Jejuro.Entity.PreferenceOption;
import kr.fast.Jejuro.Repository.PreferenceOptionRepository;
import kr.fast.Jejuro.Repository.PreferenceRepository;
import kr.fast.Jejuro.Entity.Companion;
import kr.fast.Jejuro.Entity.RegionMode;
import kr.fast.Jejuro.Entity.Travel;
import kr.fast.Jejuro.Entity.TravelPreference;
import kr.fast.Jejuro.Entity.TravelRegion;
import kr.fast.Jejuro.RequestDTO.TravelCreateRequest;
import kr.fast.Jejuro.RequestDTO.TravelCreateRequest.AnswerReq;
import kr.fast.Jejuro.RequestDTO.TravelCreateRequest.CompanionReq;
import kr.fast.Jejuro.Repository.CompanionRepository;
import kr.fast.Jejuro.Repository.TravelPreferenceRepository;
import kr.fast.Jejuro.Repository.TravelRegionRepository;
import kr.fast.Jejuro.Repository.TravelRepository;
import kr.fast.Jejuro.Entity.User;
import kr.fast.Jejuro.Repository.UserRepository;

@Service
public class TravelService {

    private final UserRepository userRepository;
    private final TravelRepository travelRepository;
    private final TravelRegionRepository travelRegionRepository;
    private final CompanionRepository companionRepository;
    private final TravelPreferenceRepository travelPreferenceRepository;
    private final RegionRepository regionRepository;
    private final PreferenceRepository preferenceRepository;
    private final PreferenceOptionRepository optionRepository;
    private final SurveyValidator surveyValidator;
    private final TravelAccessService travelAccessService;
    private final TravelRouteRepository routeRepository;
    private final RouteService routeService;
    private final JdbcTemplate jdbcTemplate;

    public TravelService(UserRepository userRepository, TravelRepository travelRepository,
                         TravelRegionRepository travelRegionRepository, CompanionRepository companionRepository,
                         TravelPreferenceRepository travelPreferenceRepository, RegionRepository regionRepository,
                         PreferenceRepository preferenceRepository, PreferenceOptionRepository optionRepository,
                         SurveyValidator surveyValidator, TravelAccessService travelAccessService,
                         TravelRouteRepository routeRepository, RouteService routeService,
                         JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.travelRepository = travelRepository;
        this.travelRegionRepository = travelRegionRepository;
        this.companionRepository = companionRepository;
        this.travelPreferenceRepository = travelPreferenceRepository;
        this.regionRepository = regionRepository;
        this.preferenceRepository = preferenceRepository;
        this.optionRepository = optionRepository;
        this.surveyValidator = surveyValidator;
        this.travelAccessService = travelAccessService;
        this.routeRepository = routeRepository;
        this.routeService = routeService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 여행 + 권역 + 동반자 + 설문 답변을 한 트랜잭션으로 저장한다.
     * 중간에 하나라도 실패하면 전부 취소(롤백)되어 "설문 없는 여행"이 남지 않는다.
     */
    @Transactional
    public Long create(Long userId, TravelCreateRequest req) {
        // 1) 입력 검사
        if (req.startDate().isAfter(req.endDate())) {
            throw ApiException.badRequest("여행 시작일이 종료일보다 늦습니다.");
        }
        validateRegions(req);
        validateCompanions(req.companionsOrEmpty());

        List<Preference> questions = preferenceRepository.findAllWithGroup();
        List<PreferenceOption> options = optionRepository.findAllByOrderByPreferenceIdAscOptionValueAsc();
        surveyValidator.validate(questions, options, req.answers());

        // 2) 회원 행 잠금 → 여행 순번 계산 (동시에 두 여행을 만들어도 번호가 겹치지 않음)
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> ApiException.notFound("회원을 찾을 수 없습니다."));
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "탈퇴 처리 중인 계정입니다.");
        }
        rejectOverlap(travelRepository, userId, req.startDate(), req.endDate());
        int travelNo = travelRepository.nextTravelNo(userId);
        int ageGroup = AgeGroup.of(user.getBirthDate(), req.startDate());

        // 3) 여행 저장 → travelId 발급
        Travel travel = travelRepository.save(Travel.create(userId, travelNo, req.travelName().trim(),
                req.startDate(), req.endDate(), req.regionMode(), ageGroup));
        Long travelId = travel.getTravelId();

        // 4) 권역 (ALL이면 저장하지 않음)
        if (req.regionMode() == RegionMode.SELECTED) {
            travelRegionRepository.saveAll(req.regionIdsOrEmpty().stream()
                    .map(regionId -> new TravelRegion(travelId, regionId))
                    .toList());
        }

        // 5) 동반자 (순번 1부터)
        List<CompanionReq> cs = req.companionsOrEmpty();
        for (int i = 0; i < cs.size(); i++) {
            CompanionReq c = cs.get(i);
            companionRepository.save(new Companion(travelId, i + 1,
                    c.relationCode(), c.genderCode(), c.ageGroupCode()));
        }

        // 6) 설문 답변: 선택지 하나당 한 행, 응답 방식은 질문 정보에서 복사
        //    values 배열 순서 = 사용자가 고른 순서 → answer_rank 1, 2, 3 (1순위를 AI에 보냄)
        Map<Long, Preference> questionMap = questions.stream()
                .collect(Collectors.toMap(Preference::getPreferenceId, Function.identity()));
        for (AnswerReq a : req.answers()) {
            List<Integer> values = a.values();
            for (int rank = 1; rank <= values.size(); rank++) {
                travelPreferenceRepository.save(new TravelPreference(travelId, a.preferenceId(),
                        questionMap.get(a.preferenceId()).getResponseType(), values.get(rank - 1), rank));
            }
        }

        return travelId;
    }

    /**
     * 7페이지: 일정 확정(최종 경로 채택).
     * DB의 복합 FK가 "같은 여행의 경로"인지 검사하지만, 알맞은 메시지를 주려고 먼저 확인한다.
     * 규칙: 한 번 확정하면 되돌릴 수 없다. 여행 장소를 모두 경로에 배치해야 확정할 수 있다.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)   // 여행 행 잠금 뒤 최신 데이터를 읽도록 (REPEATABLE READ면 잠금 전 스냅샷을 읽음)
    public void adoptRoute(Long travelId, Long userId, Long routeId) {
        // 여행 행을 잠근 뒤 검사한다: 다른 탭의 경로 저장·장소 빼기와 동시에 확정되지 않게
        Travel travel = travelRepository.findOwnedForUpdate(travelId, userId)
                .orElseThrow(() -> ApiException.notFound("여행을 찾을 수 없습니다."));
        if (travel.getAdoptedRouteId() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 일정을 확정해 바꿀 수 없습니다.");
        }
        if (routeId == null) {
            throw ApiException.badRequest("확정할 경로가 없습니다.");
        }
        TravelRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> ApiException.notFound("경로를 찾을 수 없습니다."));
        if (!route.getTravelId().equals(travelId)) {
            throw ApiException.badRequest("이 여행의 경로가 아닙니다.");
        }
        RouteService.Placement placement = routeService.placement(travelId);
        if (placement.placeCount() == 0) {
            throw ApiException.badRequest("여행 장소를 먼저 추가하고 경로에 배치해 주세요.");
        }
        if (!placement.complete()) {
            throw ApiException.badRequest("여행 장소 " + placement.placeCount() + "곳 중 "
                    + placement.unplacedPoiIds().size() + "곳이 아직 경로에 없습니다. 모두 배치한 뒤 확정할 수 있어요.");
        }
        travel.adopt(routeId, LocalDateTime.now());   // 변경 감지(dirty checking)로 커밋 시 UPDATE
    }

    /** 7페이지: 여행 삭제. 순환 참조 때문에 DB 프로시저가 정해진 순서로 지운다. */
    @Transactional
    public void delete(Long travelId, Long userId) {
        travelAccessService.getOwned(travelId, userId);
        jdbcTemplate.update("CALL sp_delete_travel(?)", travelId);
    }

    /** 같은 회원의 여행끼리 기간이 겹치면 거부 (회원 행을 잠근 뒤 호출해 동시 생성도 막는다) */
    static void rejectOverlap(TravelRepository travels, Long userId, java.time.LocalDate start, java.time.LocalDate end) {
        travels.findFirstByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAsc(userId, end, start)
                .ifPresent(t -> {
                    throw new ApiException(HttpStatus.CONFLICT, "'" + t.getTravelName() + "' 여행(" + t.getStartDate()
                            + " ~ " + t.getEndDate() + ")과 기간이 겹쳐요. 같은 기간에는 여행을 하나만 만들 수 있어요."
                            + " 날짜를 바꾸거나 기존 여행을 이용해 주세요.");
                });
    }

    private void validateRegions(TravelCreateRequest req) {
        List<Integer> ids = req.regionIdsOrEmpty();
        if (req.regionMode() == RegionMode.ALL) {
            if (!ids.isEmpty()) {
                throw ApiException.badRequest("제주 전체를 선택하면 권역을 따로 고르지 않습니다.");
            }
            return;
        }
        if (ids.isEmpty()) {
            throw ApiException.badRequest("권역을 하나 이상 선택하세요.");
        }
        if (new HashSet<>(ids).size() != ids.size()) {
            throw ApiException.badRequest("같은 권역이 중복되었습니다.");
        }
        long exists = regionRepository.findAllById(ids).size();
        if (exists != ids.size()) {
            throw ApiException.badRequest("존재하지 않는 권역이 있습니다.");
        }
    }

    /** AI 모델이 동반자를 최대 18명(18-slot)까지 받으므로 18명까지만 허용 */
    private static final int MAX_COMPANIONS = 18;

    private void validateCompanions(List<CompanionReq> companions) {
        if (companions.size() > MAX_COMPANIONS) {
            throw ApiException.badRequest("동반자는 최대 " + MAX_COMPANIONS + "명까지 입력할 수 있습니다.");
        }
        for (CompanionReq c : companions) {
            if (c.relationCode() < 1 || c.relationCode() > 11
                    || c.genderCode() < 1 || c.genderCode() > 2
                    || c.ageGroupCode() < 1 || c.ageGroupCode() > 8) {
                throw ApiException.badRequest("동반자 정보를 다시 확인하세요.");
            }
        }
    }
}
