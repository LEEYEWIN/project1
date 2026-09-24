package kr.fast.Jejuro.Service;


//[3페이지 추천 목록 (2~7페이지 공용)]

import java.util.Collection;
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

 /** 3-1페이지: 전체 관광지 검색 (이름순, 한 페이지 size개) */
 public PoiPageResponse search(Integer regionId, String category, String keyword, int page, int size) {
     String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();
     String cat = category == null || category.isBlank() ? null : category;
     int safeSize = Math.min(Math.max(size, 1), 50);
     Page<Poi> result = poiRepository.search(regionId, cat, kw,
             PageRequest.of(Math.max(page, 0), safeSize, Sort.by("poiName")));
     return new PoiPageResponse(toSummaries(result.getContent()), result.getNumber(),
             result.getSize(), result.getTotalPages(), result.getTotalElements());
 }

 /** 분류 필터 목록 */
 public List<String> categories() {
     return poiRepository.findAllCategoryCodes();
 }

 public PoiSummaryResponse findOne(Long poiId) {
     Poi poi = poiRepository.findById(poiId)
             .orElseThrow(() -> ApiException.notFound("관광지를 찾을 수 없습니다."));
     return toSummaries(List.of(poi)).get(0);
 }

 /** 엔티티 → 응답. 권역 이름은 REGION 4행을 한 번 읽어서 붙인다. */
 public List<PoiSummaryResponse> toSummaries(Collection<Poi> pois) {
     Map<Integer, String> regionNames = regionRepository.findAll().stream()
             .collect(Collectors.toMap(Region::getRegionId, Region::getRegionName));
     return pois.stream()
             .map(p -> new PoiSummaryResponse(p.getPoiId(), p.getPoiName(), p.getAddress(),
                     p.getLatitude(), p.getLongitude(), p.getCategoryCode(), p.getRegionId(),
                     regionNames.get(p.getRegionId()), p.getImageUrl(), p.getDescription()))
             .toList();
 }
}