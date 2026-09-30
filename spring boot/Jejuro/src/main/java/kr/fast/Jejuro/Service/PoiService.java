package kr.fast.Jejuro.Service;



// [3페이지 추천 목록 (2~7페이지 공용)]

import java.math.BigDecimal;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.ResponseDTO.PoiPageResponse;
import kr.fast.Jejuro.ResponseDTO.PoiSummaryResponse;
import kr.fast.Jejuro.ResponseDTO.PoiDetailResponse;
import kr.fast.Jejuro.Entity.Region;
import kr.fast.Jejuro.Repository.RegionRepository;
import kr.fast.Jejuro.Entity.Poi;
import kr.fast.Jejuro.Repository.PoiRepository;

@Service
@Transactional(readOnly = true)
public class PoiService {

    private final PoiRepository poiRepository;
    private final RegionRepository regionRepository;

    public PoiService(PoiRepository poiRepository, RegionRepository regionRepository) {
        this.poiRepository = poiRepository;
        this.regionRepository = regionRepository;
    }

    /** 요청한 ID 순서를 그대로 유지해서 돌려준다(추천 순위가 섞이지 않게). 없는 ID는 건너뛴다. */
    public List<PoiSummaryResponse> findSummaries(List<Long> poiIds) {
        Map<Long, Poi> found = poiRepository.findAllById(poiIds).stream()
                .collect(Collectors.toMap(Poi::getPoiId, Function.identity()));
        return toSummaries(poiIds.stream().map(found::get).filter(p -> p != null).toList());
    }

    /**
     * 관광지 목록·검색 (FR-32, FR-34). 비회원도 사용.
     * - 검색어: 앞뒤 공백 제거 후 이름에 포함된 곳. 비어 있으면 필터만 적용
     * - 권역과 관광 유형을 함께 고르면 두 조건 모두 만족하는 곳
     * - 정렬: 이름 가나다순(같은 이름이면 번호순), 한 페이지 size개
     */
    public PoiPageResponse search(Integer regionId, String category, String keyword, int page, int size) {
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();
        String cat = category == null || category.isBlank() ? null : category.trim().toUpperCase();
        int safeSize = Math.min(Math.max(size, 1), 50);
        Page<Poi> result = poiRepository.search(regionId, cat, kw,
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by("poiName", "poiId")));
        return new PoiPageResponse(toSummaries(result.getContent()), result.getNumber(),
                result.getSize(), result.getTotalPages(), result.getTotalElements());
    }

    /** 홈: 서로 다른 관광 유형 최대 4개에서 한 곳씩 무작위 (숨김·삭제 제외) */
    public List<PoiSummaryResponse> homeDiscoveries() {
        List<String> categories = new ArrayList<>(poiRepository.findAllCategoryCodes().stream()
                .filter(code -> code != null && !code.isBlank()).distinct().toList());
        Collections.shuffle(categories);
        List<Poi> selected = new ArrayList<>();
        for (String category : categories) {
            selected.addAll(poiRepository.findRandomInCategory(category, PageRequest.of(0, 1)));
            if (selected.size() >= 4) break;
        }
        return toSummaries(selected);
    }

    /** 분류 필터 목록 */
    public List<String> categories() {
        return poiRepository.findAllCategoryCodes();
    }

    /** 카드용 한 곳 (찜 추가 응답 등) */
    public PoiSummaryResponse findOne(Long poiId) {
        Poi poi = poiRepository.findById(poiId)
                .orElseThrow(() -> ApiException.notFound("관광지를 찾을 수 없습니다."));
        return toSummaries(List.of(poi)).get(0);
    }

    /** 관광지 상세 (FR-33). 없거나 관리자가 삭제한 관광지는 404 → 화면이 안내 후 목록으로 */
    public PoiDetailResponse findDetail(Long poiId) {
        Poi p = poiRepository.findById(poiId)
                .orElseThrow(() -> ApiException.notFound("관광지를 찾을 수 없습니다."));
        if (p.isDeleted()) {
            throw ApiException.notFound("삭제되어 정보를 확인할 수 없는 관광지입니다.");
        }
        String regionName = regionRepository.findById(p.getRegionId()).map(Region::getRegionName).orElse(null);
        return new PoiDetailResponse(p.getPoiId(), p.getPoiName(), p.getAddress(), p.getLatitude(), p.getLongitude(),
                inJeju(p.getLatitude(), p.getLongitude()), p.getCategoryCode(), p.getRegionId(), regionName,
                blankToNull(p.getImageUrl()), blankToNull(p.getDescription()), blankToNull(p.getDetailDescription()));
    }

    /** 제주도(추자도 포함) 범위 안의 좌표인지 */
    static boolean inJeju(BigDecimal lat, BigDecimal lng) {
        if (lat == null || lng == null) return false;
        double a = lat.doubleValue(), o = lng.doubleValue();
        return a >= 33.0 && a <= 34.1 && o >= 126.0 && o <= 127.1;
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    /** 엔티티 → 응답. 권역 이름은 REGION 4행을 한 번 읽어서 붙인다. */
    public List<PoiSummaryResponse> toSummaries(Collection<Poi> pois) {
        Map<Integer, String> regionNames = regionRepository.findAll().stream()
                .collect(Collectors.toMap(Region::getRegionId, Region::getRegionName));
        return pois.stream()
                .map(p -> p.isDeleted()
                        // 삭제된 관광지: 위치만 남기고(지도·동선) 이름·사진·설명은 "확인 불가"
                        ? new PoiSummaryResponse(p.getPoiId(), Poi.UNAVAILABLE_NAME, "", p.getLatitude(), p.getLongitude(),
                                p.getCategoryCode(), p.getRegionId(), regionNames.get(p.getRegionId()), "", null, null, true)
                        : new PoiSummaryResponse(p.getPoiId(), p.getPoiName(), p.getAddress(),
                                p.getLatitude(), p.getLongitude(), p.getCategoryCode(), p.getRegionId(),
                                regionNames.get(p.getRegionId()), p.getImageUrl(), p.getDescription(),
                                p.getDetailDescription(), false))
                .toList();
    }
}