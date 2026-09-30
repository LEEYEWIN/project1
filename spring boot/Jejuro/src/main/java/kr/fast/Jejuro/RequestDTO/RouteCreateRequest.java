package kr.fast.Jejuro.RequestDTO;


//[5페이지 루트 짜기]

import jakarta.validation.constraints.Size;

/** POST /api/travels/{travelId}/routes  본문 예) { "routeName": "동부 2박3일" } (이름은 생략 가능) */
public record RouteCreateRequest(@Size(max = 100) String routeName) {
	
}