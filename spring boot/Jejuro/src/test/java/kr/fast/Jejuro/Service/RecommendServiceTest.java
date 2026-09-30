package kr.fast.Jejuro.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import kr.fast.Jejuro.Entity.*;
import kr.fast.Jejuro.Repository.*;

/** AI 이름 → POI_SOURCE_MAP → poi_id 변환, AI 순서 유지, 추천 불가·관심없음 제외 */
class RecommendServiceTest {
    private final TravelAccessService access = mock(TravelAccessService.class);
    private final TravelRegionRepository travelRegions = mock(TravelRegionRepository.class);
    private final AiTravelInputRepository inputs = mock(AiTravelInputRepository.class);
    private final AiClient ai = mock(AiClient.class);
    private final PoiSourceMapRepository sourceMaps = mock(PoiSourceMapRepository.class);
    private final PoiService poiService = mock(PoiService.class);
    private final RegionRepository regions = mock(RegionRepository.class);
    private final CompanionRepository companions = mock(CompanionRepository.class);
    private final RecommendLogService logs = mock(RecommendLogService.class);
    private final PoiRepository pois = mock(PoiRepository.class);
    private final DislikeService dislikes = mock(DislikeService.class);
    private final RecommendService service = new RecommendService(
            access, travelRegions, inputs, ai, sourceMaps, poiService, regions, companions, logs, pois, dislikes, "test");

    private void ready() {
        Travel travel = mock(Travel.class);
        when(travel.getRegionMode()).thenReturn(RegionMode.ALL);
        when(access.getOwned(1L, 1L)).thenReturn(travel);
        AiTravelInput input = new AiTravelInput(1L, 2, 3, 4, 4, 4, 4, 4, 4,
                3, 1, 1, List.of(1), List.of(1), 0, true);
        when(inputs.find(1L, 1L)).thenReturn(Optional.of(input));
        when(poiService.findSummaries(any())).thenReturn(List.of());
        when(dislikes.ids(anyLong())).thenReturn(Set.of());
    }

    @Test
    void mapsAiNamesAndKeepsAiOrder() {
        ready();
        when(ai.recommend(any())).thenReturn(List.of("정방폭포", "없는 관광지", "성산일출봉", "정방폭포", "우도"));
        when(sourceMaps.findBySourcePoiIdIn(any())).thenReturn(List.of(
                new PoiSourceMap("성산일출봉", 3L), new PoiSourceMap("정방폭포", 8L), new PoiSourceMap("우도", 5L)));
        when(pois.findNotRecommendableIds(any())).thenReturn(List.of(5L)); // 숨김·삭제·직접 선택만

        assertEquals(1L, service.recommend(1L, 1L).travelId());

        // 매핑 없는 이름·중복·추천 불가는 빼고 AI 순서 유지
        verify(poiService).findSummaries(List.of(8L, 3L));
    }

    @Test
    void dislikedPoisAreExcluded() {
        ready();
        when(ai.recommend(any())).thenReturn(List.of("정방폭포", "성산일출봉"));
        when(sourceMaps.findBySourcePoiIdIn(any())).thenReturn(List.of(
                new PoiSourceMap("성산일출봉", 3L), new PoiSourceMap("정방폭포", 8L)));
        when(pois.findNotRecommendableIds(any())).thenReturn(List.of());
        when(dislikes.ids(1L)).thenReturn(Set.of(8L));

        service.recommend(1L, 1L);

        verify(poiService).findSummaries(List.of(3L));
    }
}
