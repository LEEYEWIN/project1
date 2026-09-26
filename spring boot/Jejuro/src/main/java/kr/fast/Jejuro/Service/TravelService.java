package kr.fast.Jejuro.Service;


//[1·7페이지 여행 (생성 / 채택·삭제)]

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
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
 private final JdbcTemplate jdbcTemplate;

 public TravelService(UserRepository userRepository, TravelRepository travelRepository,
                      TravelRegionRepository travelRegionRepository, CompanionRepository companionRepository,
                      TravelPreferenceRepository travelPreferenceRepository, RegionRepository regionRepository,
                      PreferenceRepository preferenceRepository, PreferenceOptionRepository optionRepository,
                      SurveyValidator surveyValidator, TravelAccessService travelAccessService,
                      TravelRouteRepository routeRepository,
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
  * 7페이지: 최종 경로 채택 / 해제.
  * DB의 복합 FK가 "같은 여행의 경로"인지 검사하지만, 알맞은 메시지를 주려고 먼저 확인한다.
  * 규칙: 한 번 채택하면 확정(해제·변경 불가). 빈 경로는 채택할 수 없다.
  */
 @Transactional
 public void adoptRoute(Long travelId, Long userId, Long routeId) {
     Travel travel = travelAccessService.getOwned(travelId, userId);
     if (travel.getAdoptedRouteId() != null) {
         throw new ApiException(HttpStatus.CONFLICT, "이미 최종 경로가 채택되어 바꿀 수 없습니다.");
     }
     if (routeId == null) {
         throw ApiException.badRequest("채택할 경로를 선택하세요.");
     }
     TravelRoute route = routeRepository.findById(routeId)
             .orElseThrow(() -> ApiException.notFound("경로를 찾을 수 없습니다."));
     if (!route.getTravelId().equals(travelId)) {
         throw ApiException.badRequest("이 여행의 경로가 아닙니다.");
     }
     if (routeSpotCount(routeId) == 0) {
         throw ApiException.badRequest("관광지가 없는 경로는 채택할 수 없습니다.");
     }
     travel.adopt(routeId, LocalDateTime.now());   // 변경 감지(dirty checking)로 커밋 시 UPDATE
 }

 /** 경로에 담긴 관광지 수 (빈 경로 채택 방지용) */
 private int routeSpotCount(Long routeId) {
     Integer n = jdbcTemplate.queryForObject(
             "SELECT COUNT(*) FROM ROUTE_SPOT s JOIN ROUTE_DAY d ON d.route_day_id = s.route_day_id WHERE d.route_id = ?",
             Integer.class, routeId);
     return n == null ? 0 : n;
 }

 /** 7페이지: 여행 삭제. 순환 참조 때문에 DB 프로시저가 정해진 순서로 지운다. */
 @Transactional
 public void delete(Long travelId, Long userId) {
     travelAccessService.getOwned(travelId, userId);
     jdbcTemplate.update("CALL sp_delete_travel(?)", travelId);
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