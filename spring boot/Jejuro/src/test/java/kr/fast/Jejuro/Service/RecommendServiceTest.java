package kr.fast.Jejuro.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import kr.fast.Jejuro.Entity.*;
import kr.fast.Jejuro.Repository.*;

class RecommendServiceTest {
    private final TravelAccessService access = mock(TravelAccessService.class);
    private final TravelRegionRepository travelRegions = mock(TravelRegionRepository.class);
    private final AiTravelInputRepository inputs = mock(AiTravelInputRepository.class);
    private final AiClient ai = mock(AiClient.class);
    private final PoiRepository pois = mock(PoiRepository.class);
    private final PoiService poiService = mock(PoiService.class);
    private final RegionRepository regions = mock(RegionRepository.class);
    private final CompanionRepository companions = mock(CompanionRepository.class);
    private final RecommendLogService logs = mock(RecommendLogService.class);
    private final RecommendService service = new RecommendService(
            access, travelRegions, inputs, ai, pois, poiService, regions, companions, logs, "test");

    private void ready() {
        Travel travel = mock(Travel.class);
        when(travel.getRegionMode()).thenReturn(RegionMode.ALL);
        when(access.getOwned(1L, 1L)).thenReturn(travel);
        AiTravelInput input = new AiTravelInput(1L, 2, 3, 4, 4, 4, 4, 4, 4,
                3, 1, 1, List.of(1), List.of(1), 0, true);
        when(inputs.find(1L, 1L)).thenReturn(Optional.of(input));
        when(poiService.findSummaries(any())).thenReturn(List.of());
    }

    private Poi poi(long id, String name) {
        Poi poi = mock(Poi.class);
        when(poi.getPoiId()).thenReturn(id);
        when(poi.getPoiName()).thenReturn(name);
        return poi;
    }

    @Test
    void matchesNamesWithoutSourceMapAndPreservesAiRanking() {
        ready();
        List<String> names = List.of("정방폭포", "없는 관광지", "성산일출봉", "정방폭포", "성산");
        when(ai.recommend(any())).thenReturn(names);
        List<Poi> matches = List.of(
                poi(3L, "성산일출봉"), poi(8L, "정방폭포"), poi(12L, "정방폭포"));
        when(pois.findByPoiNameInOrderByPoiIdAsc(names)).thenReturn(matches);

        assertEquals(1L, service.recommend(1L, 1L).travelId());

        // Skip missing/partial names and repeated results, preserving AI order.
        verify(poiService).findSummaries(List.of(8L, 3L));
    }

    @Test
    void emptyRecommendationsDoNotQueryNames() {
        ready();
        when(ai.recommend(any())).thenReturn(List.of());
        service.recommend(1L, 1L);
        verifyNoInteractions(pois);
        verify(poiService).findSummaries(List.of());
    }
}
