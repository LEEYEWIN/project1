package kr.fast.Jejuro.Service;


//[경로 - 구간별 예상 택시비]

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
* 제주 중형택시 예상 요금 (거리 기준, 주간).
* 기본요금 4,300원(2km까지) + 이후 126m마다 100원 — 2024.07.01 시행 제주 요금
* - 요금이 바뀌면 application.properties의 taxi.* 값만 고친다.
* - 시속 15km 이하일 때 붙는 시간요금(31초당 100원)과 심야할증(밤 11시~새벽 4시 20%)은 넣지 않은 "예상값"
*/
@Component
public class TaxiFareCalculator {

 private final int baseFare;
 private final int baseDistanceM;
 private final int unitDistanceM;
 private final int unitFare;

 public TaxiFareCalculator(@Value("${taxi.base-fare:4300}") int baseFare,
                           @Value("${taxi.base-distance-m:2000}") int baseDistanceM,
                           @Value("${taxi.unit-distance-m:126}") int unitDistanceM,
                           @Value("${taxi.unit-fare:100}") int unitFare) {
     this.baseFare = baseFare;
     this.baseDistanceM = baseDistanceM;
     this.unitDistanceM = unitDistanceM;
     this.unitFare = unitFare;
 }

 /** 거리(m) → 예상 요금(원) */
 public int fare(int distanceM) {
     if (distanceM <= 0) return 0;
     int extra = Math.max(distanceM - baseDistanceM, 0);
     int units = (extra + unitDistanceM - 1) / unitDistanceM;   // 올림
     return baseFare + units * unitFare;
 }
}