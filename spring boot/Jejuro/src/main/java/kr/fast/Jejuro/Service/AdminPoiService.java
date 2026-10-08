package kr.fast.Jejuro.Service;


//[관리자 관광지 관리 - 목록·점검·추가·수정·숨김·AI 이름 연결]

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.Entity.Code;
import kr.fast.Jejuro.Entity.Poi;
import kr.fast.Jejuro.Entity.PoiSourceMap;
import kr.fast.Jejuro.Entity.Region;
import kr.fast.Jejuro.Repository.CodeRepository;
import kr.fast.Jejuro.Repository.PoiRepository;
import kr.fast.Jejuro.Repository.PoiSourceMapRepository;
import kr.fast.Jejuro.Repository.RegionRepository;
import kr.fast.Jejuro.RequestDTO.AdminPoiRequest;
import kr.fast.Jejuro.ResponseDTO.AdminPoiResponse;

/**
* 관광지 데이터 관리 규칙
* - 행은 지우지 않는다: 여행 장소·경로·추천 기록·후기가 poi_id를 참조하므로.
*   숨김: 관광지 검색, AI 추천 결과, 새로 담기에서 빠지고, 이미 담긴 여행·확정 일정에는 그대로 보인다.
*   삭제: 숨김과 같이 빠지고, 여행 장소·경로·후기에는 "확인 불가 (삭제된 관광지)"로 표시, 상세 화면 막음. 복구 가능.
* - AI 이름 연결(POI_SOURCE_MAP): AI가 주는 장소 이름(VISIT_AREA_NM) → poi_id.
*   연결이 없는 관광지는 AI 추천에 절대 나오지 않는다(데이터 점검 "AI 이름 없음").
*   'VJ:…', 'TOUR:…'처럼 콜론이 있는 값은 원본 데이터 ID(추천에는 안 쓰임).
* - 좌표는 제주 범위만 저장, 분류는 CODE(POI_CAT), 권역은 REGION에 있는 값만.
*/
@Service
public class AdminPoiService {

 private static final int PAGE_SIZE = 10;
 /** 관리자 화면에서 사진을 올리면 받는 주소 (커뮤니티 사진 저장소를 같이 씀) */
 private static final String UPLOADED_IMAGE_PREFIX = "/api/community/images/";

 /** 데이터 점검 조건 (목록 필터와 숫자에 같이 사용) */
 private static final String NO_IMAGE = "(p.image_url IS NULL OR TRIM(p.image_url) = '')";
 private static final String NO_DESC = "(p.description IS NULL OR TRIM(p.description) = '')";
 /** AI 추천 대상(학습한 275곳) / 직접 선택만(그 밖) — POI.ai_recommend */
 private static final String AI = "p.ai_recommend = 1";
 private static final String MANUAL = "p.ai_recommend = 0";
 private static final String OUT_JEJU = "NOT (p.latitude BETWEEN 33.0 AND 34.1 AND p.longitude BETWEEN 126.0 AND 127.1)";
 private static final Map<String, String> ISSUES = Map.of(
         "NO_IMAGE", NO_IMAGE, "NO_DESC", NO_DESC, "AI", AI, "MANUAL", MANUAL, "OUT_OF_JEJU", OUT_JEJU);

 private final JdbcTemplate jdbc;
 private final PoiRepository poiRepository;
 private final PoiSourceMapRepository sourceMapRepository;
 private final RegionRepository regionRepository;
 private final CodeRepository codeRepository;
 private final CommunityImageService imageService;

 public AdminPoiService(JdbcTemplate jdbc, PoiRepository poiRepository, PoiSourceMapRepository sourceMapRepository,
                        RegionRepository regionRepository, CodeRepository codeRepository,
                        CommunityImageService imageService) {
     this.jdbc = jdbc;
     this.poiRepository = poiRepository;
     this.sourceMapRepository = sourceMapRepository;
     this.regionRepository = regionRepository;
     this.codeRepository = codeRepository;
     this.imageService = imageService;
 }

 // ------------------------------------------------------------------ 선택지

 @Transactional(readOnly = true)
 public AdminPoiResponse.Options options() {
     return new AdminPoiResponse.Options(
             regionRepository.findAll().stream()
                     .map(r -> new AdminPoiResponse.Option(String.valueOf(r.getRegionId()), r.getRegionName())).toList(),
             codeRepository.findByGroupCodeOrderByCodeId("POI_CAT").stream()
                     .map(c -> new AdminPoiResponse.Option(c.getCodeValue(), c.getCodeName())).toList());
 }

 // ------------------------------------------------------------------ 목록

 /**
  * @param visibility ALL / VISIBLE / HIDDEN
  * @param issue      AI(AI 추천 대상) / MANUAL(직접 선택만) / NO_IMAGE / NO_DESC / OUT_OF_JEJU (생략 가능)
  */
 @Transactional(readOnly = true)
 public AdminPoiResponse list(String keyword, Integer regionId, String category, String visibility,
                              String issue, int page) {
     List<String> conds = new ArrayList<>();
     List<Object> args = new ArrayList<>();
     if (keyword != null && !keyword.isBlank()) {
         String kw = keyword.trim();
         if (kw.matches("\\d+")) {                         // 숫자면 번호로도 찾기
             conds.add("(p.poi_id = ? OR p.poi_name LIKE ?)");
             args.add(Long.valueOf(kw));
         } else {
             conds.add("(p.poi_name LIKE ? OR p.address LIKE ?)");
             args.add("%" + kw + "%");
         }
         args.add("%" + kw + "%");
     }
     if (regionId != null) {
         conds.add("p.region_id = ?");
         args.add(regionId);
     }
     if (category != null && !category.isBlank()) {
         conds.add("p.category_code = ?");
         args.add(category.trim().toUpperCase());
     }
     if ("DELETED".equalsIgnoreCase(visibility)) {
         conds.add("p.deleted_at IS NOT NULL");
     } else {
         conds.add("p.deleted_at IS NULL");      // 전체·보임·숨김은 삭제한 곳 제외
         if ("VISIBLE".equalsIgnoreCase(visibility)) conds.add("p.hidden_at IS NULL");
         if ("HIDDEN".equalsIgnoreCase(visibility)) conds.add("p.hidden_at IS NOT NULL");
     }
     if (issue != null && ISSUES.containsKey(issue.toUpperCase())) conds.add(ISSUES.get(issue.toUpperCase()));
     String where = conds.isEmpty() ? "" : " WHERE " + String.join(" AND ", conds);

     Long totalObj = jdbc.queryForObject("SELECT COUNT(*) FROM poi p" + where, Long.class, args.toArray());
     long total = totalObj == null ? 0 : totalObj;

     int safePage = Math.max(page, 0);
     List<Object> pageArgs = new ArrayList<>(args);
     pageArgs.add(PAGE_SIZE);
     pageArgs.add(safePage * PAGE_SIZE);
     Map<String, String> catNames = categoryNames();
     Map<Integer, String> regionNames = regionRepository.findAll().stream()
             .collect(Collectors.toMap(Region::getRegionId, Region::getRegionName));
     List<AdminPoiResponse.Row> rows = jdbc.query("""
             SELECT p.poi_id, p.poi_name, p.address, p.category_code, p.region_id, p.image_url, p.hidden_at, p.deleted_at,
                    p.ai_recommend, %s AS no_image, %s AS no_desc, %s AS out_jeju,
                    (SELECT COUNT(*) FROM travel_bookmark b WHERE b.poi_id = p.poi_id) AS bookmark_count,
                    (SELECT COUNT(*) FROM recommend_item i WHERE i.poi_id = p.poi_id AND i.shown = 1) AS recommend_count
               FROM poi p
             """.formatted(NO_IMAGE, NO_DESC, OUT_JEJU) + where + " ORDER BY p.poi_id DESC LIMIT ? OFFSET ?",
             (rs, n) -> new AdminPoiResponse.Row(rs.getLong("poi_id"), rs.getString("poi_name"),
                     rs.getString("address"), rs.getString("category_code"),
                     catNames.getOrDefault(rs.getString("category_code"), rs.getString("category_code")),
                     rs.getInt("region_id"), regionNames.get(rs.getInt("region_id")), rs.getString("image_url"),
                     rs.getTimestamp("hidden_at") != null, rs.getTimestamp("deleted_at") != null,
                     rs.getBoolean("ai_recommend"), rs.getBoolean("no_image"), rs.getBoolean("no_desc"),
                     rs.getBoolean("out_jeju"), rs.getLong("bookmark_count"), rs.getLong("recommend_count")),
             pageArgs.toArray());

     // 데이터 점검 숫자: 숨긴 곳 제외한 전체 기준
     AdminPoiResponse.Checks checks = jdbc.query("""
             SELECT COUNT(*) AS total,
                    (SELECT COUNT(*) FROM poi h WHERE h.hidden_at IS NOT NULL AND h.deleted_at IS NULL) AS hidden,
                    (SELECT COUNT(*) FROM poi d WHERE d.deleted_at IS NOT NULL) AS deleted,
                    COALESCE(SUM(%s), 0) AS ai, COALESCE(SUM(%s), 0) AS manual,
                    COALESCE(SUM(%s), 0) AS no_image, COALESCE(SUM(%s), 0) AS no_desc, COALESCE(SUM(%s), 0) AS out_jeju
               FROM poi p WHERE p.hidden_at IS NULL AND p.deleted_at IS NULL
             """.formatted(AI, MANUAL, NO_IMAGE, NO_DESC, OUT_JEJU),
             (rs, n) -> new AdminPoiResponse.Checks(rs.getLong("total"), rs.getLong("hidden"), rs.getLong("deleted"),
                     rs.getLong("ai"), rs.getLong("manual"),
                     rs.getLong("no_image"), rs.getLong("no_desc"), rs.getLong("out_jeju")))
             .get(0);

     return new AdminPoiResponse(safePage, (int) Math.ceil(total / (double) PAGE_SIZE), total, checks, rows);
 }

 // ------------------------------------------------------------------ 사진 점검

 /**
  * 사진 주소가 있는 관광지 전부 (삭제한 곳 제외, 번호 순).
  * 외부 사이트 사진(블로그·검색 썸네일 등)은 만료되거나 다른 사이트에서 못 쓰게 막혀 깨질 수 있어서,
  * 관리자 화면이 브라우저에서 한 장씩 불러 보고 깨진 곳만 모아 보여 준다 (서버에서 외부로 요청하지 않음).
  */
 @Transactional(readOnly = true)
 public List<AdminPoiResponse.ImageRow> images() {
     return jdbc.query("""
             SELECT poi_id, poi_name, image_url, hidden_at IS NOT NULL AS hidden
               FROM poi
              WHERE deleted_at IS NULL AND image_url IS NOT NULL AND TRIM(image_url) <> ''
              ORDER BY poi_id
             """, (rs, n) -> new AdminPoiResponse.ImageRow(rs.getLong("poi_id"), rs.getString("poi_name"),
             rs.getString("image_url").trim(), rs.getBoolean("hidden")));
 }

 // ------------------------------------------------------------------ 상세

 @Transactional(readOnly = true)
 public AdminPoiResponse.Detail detail(Long poiId) {
     Poi p = get(poiId);
     List<AdminPoiResponse.Mapping> mappings = sourceMapRepository.findByPoiIdOrderBySourcePoiId(poiId).stream()
             .map(m -> new AdminPoiResponse.Mapping(m.getSourcePoiId(), !m.getSourcePoiId().contains(":")))
             .toList();
     AdminPoiResponse.Usage usage = jdbc.query("""
             SELECT (SELECT COUNT(*) FROM travel_bookmark WHERE poi_id = ?) AS bookmarks,
                    (SELECT COUNT(*) FROM route_spot WHERE poi_id = ?) AS spots,
                    (SELECT COUNT(*) FROM recommend_item WHERE poi_id = ? AND shown = 1) AS recommended,
                    (SELECT COUNT(*) FROM travel_feedback_spot WHERE poi_id = ?) AS feedback
             """, (rs, n) -> new AdminPoiResponse.Usage(rs.getLong("bookmarks"), rs.getLong("spots"),
                     rs.getLong("recommended"), rs.getLong("feedback")), poiId, poiId, poiId, poiId).get(0);
     return new AdminPoiResponse.Detail(p.getPoiId(), p.getPoiName(), p.getAddress(), p.getLatitude(),
             p.getLongitude(), p.getCategoryCode(), p.getRegionId(), p.getDescription(), p.getDetailDescription(),
             p.getImageUrl(), p.getHiddenAt(), p.getDeletedAt(), p.isAiRecommend(), mappings, usage);
 }

 // ------------------------------------------------------------------ 추가·수정·숨김

 @Transactional
 public Long create(AdminPoiRequest req) {
     return poiRepository.save(new Poi(validate(req))).getPoiId();
 }

 @Transactional
 public void update(Long poiId, AdminPoiRequest req) {
     get(poiId).update(validate(req));
 }

 @Transactional
 public void setHidden(Long poiId, boolean hidden) {
     Poi p = get(poiId);
     if (hidden) p.hide(LocalDateTime.now());
     else p.show();
 }

 /** 삭제: 행은 남기고 deleted_at 기록 → 여행 장소·경로·후기에는 "확인 불가" */
 @Transactional
 public void delete(Long poiId) {
     get(poiId).delete(LocalDateTime.now());
 }

 /** 삭제 취소 */
 @Transactional
 public void restore(Long poiId) {
     get(poiId).restore();
 }

 // ------------------------------------------------------------------ AI 이름 연결

 @Transactional
 public void addMapping(Long poiId, String sourcePoiId) {
     if (!get(poiId).isAiRecommend()) {
         throw ApiException.badRequest("AI가 학습하지 않은 관광지(직접 선택만)는 AI 이름을 연결할 수 없습니다.");
     }
     String name = sourcePoiId.trim();
     sourceMapRepository.findById(name).ifPresent(m -> {
         String owner = poiRepository.findById(m.getPoiId()).map(Poi::getPoiName).orElse("?");
         throw new ApiException(HttpStatus.CONFLICT, m.getPoiId().equals(poiId)
                 ? "이미 연결된 이름입니다."
                 : "'" + name + "'은(는) 이미 " + m.getPoiId() + "번 관광지(" + owner + ")에 연결되어 있습니다. 그쪽에서 먼저 빼 주세요.");
     });
     sourceMapRepository.save(new PoiSourceMap(name, poiId));
 }

 @Transactional
 public void removeMapping(Long poiId, String sourcePoiId) {
     PoiSourceMap m = sourceMapRepository.findById(sourcePoiId)
             .filter(x -> x.getPoiId().equals(poiId))
             .orElseThrow(() -> ApiException.notFound("이 관광지에 연결된 이름이 아닙니다."));
     sourceMapRepository.delete(m);
 }

 // ------------------------------------------------------------------ 공통

 private Poi get(Long poiId) {
     return poiRepository.findById(poiId)
             .orElseThrow(() -> ApiException.notFound("관광지를 찾을 수 없습니다."));
 }

 private Map<String, String> categoryNames() {
     return codeRepository.findByGroupCodeOrderByCodeId("POI_CAT").stream()
             .collect(Collectors.toMap(Code::getCodeValue, Code::getCodeName, (a, b) -> a));
 }

 /** 입력 검사 + 정리(앞뒤 공백, 빈 값 → null). 사진 주소는 DB가 NOT NULL이라 빈 문자열로 */
 private Poi.Fields validate(AdminPoiRequest r) {
     if (!PoiService.inJeju(r.latitude(), r.longitude())) {
         throw ApiException.badRequest("좌표가 제주 범위(위도 33.0~34.1, 경도 126.0~127.1)를 벗어났습니다. 위도·경도가 바뀌지 않았는지 확인하세요.");
     }
     String cat = r.categoryCode().trim().toUpperCase();
     if (!categoryNames().containsKey(cat)) {
         throw ApiException.badRequest("없는 분류입니다: " + cat);
     }
     if (!regionRepository.existsById(r.regionId())) {
         throw ApiException.badRequest("없는 권역입니다.");
     }
     String image = trim(r.imageUrl());
     if (image != null && !image.startsWith("http://") && !image.startsWith("https://")) {
         if (!image.startsWith(UPLOADED_IMAGE_PREFIX)) {
             throw ApiException.badRequest("사진 주소는 http:// 또는 https:// 로 시작하거나, 사진을 올려서 받은 주소여야 합니다.");
         }
         image = imageService.validateUrl(image);   // 실제로 올라간 파일인지 확인
     }
     return new Poi.Fields(r.poiName().trim(), r.address().trim(), scale(r.latitude()), scale(r.longitude()),
             cat, r.regionId(), r.description().trim(), trim(r.detailDescription()),
             image == null ? "" : image);
 }

 private static BigDecimal scale(BigDecimal v) {
     return v.setScale(7, java.math.RoundingMode.HALF_UP);   // DB DECIMAL(10,7)
 }

 private static String trim(String v) {
     return v == null || v.isBlank() ? null : v.trim();
 }
}
