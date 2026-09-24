package kr.fast.Jejuro.Service;


//[공통 (2~8페이지 소유권 검사)]

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.Entity.Travel;
import kr.fast.Jejuro.Repository.TravelRepository;

/**
* "이 여행이 로그인한 회원의 것인가?"를 검사하는 공통 서비스.
* 2~8페이지의 모든 API가 가장 먼저 호출한다. 남의 여행이면 존재 자체를 숨기려고 404를 준다.
*/
@Service
@Transactional(readOnly = true)
public class TravelAccessService {

 private final TravelRepository travelRepository;

 public TravelAccessService(TravelRepository travelRepository) {
     this.travelRepository = travelRepository;
 }

 public Travel getOwned(Long travelId, Long userId) {
     return travelRepository.findByTravelIdAndUserId(travelId, userId)
             .orElseThrow(() -> ApiException.notFound("여행을 찾을 수 없습니다."));
 }
}