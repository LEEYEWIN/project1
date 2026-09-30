package kr.fast.Jejuro.Controller;



//[관리자 AI 추천 KPI 대시보드]

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.fast.Jejuro.Config.AdminGuard;
import kr.fast.Jejuro.ResponseDTO.AdminKpiResponse;
import kr.fast.Jejuro.ResponseDTO.FunnelDropResponse;
import kr.fast.Jejuro.Service.AdminKpiService;
import kr.fast.Jejuro.Service.FunnelDropService;

/** 관리자 전용 (USER.role = ADMIN). 아니면 403 */
@RestController
@RequestMapping("/api/admin/kpi")
public class AdminKpiController {

private final AdminKpiService kpiService;
private final AdminGuard adminGuard;
private final FunnelDropService funnelDropService;

public AdminKpiController(AdminKpiService kpiService, AdminGuard adminGuard, FunnelDropService funnelDropService) {
   this.kpiService = kpiService;
   this.adminGuard = adminGuard;
   this.funnelDropService = funnelDropService;
}

/** 퍼널 이탈 로그: GET /api/admin/kpi/funnel-drops?days=30&step=PLACED_ALL&page=0 (step 생략 = 전체) */
@GetMapping("/funnel-drops")
public FunnelDropResponse funnelDrops(@RequestParam(name = "days", defaultValue = "30") int days,
                                     @RequestParam(name = "step", required = false) String step,
                                     @RequestParam(name = "page", defaultValue = "0") int page) {
   adminGuard.check();
   return funnelDropService.list(days, step, page);
}

/** 퍼널 이탈 로그 CSV */
@GetMapping("/funnel-drops.csv")
public ResponseEntity<byte[]> funnelDropsCsv(@RequestParam(name = "days", defaultValue = "30") int days,
                                            @RequestParam(name = "step", required = false) String step) {
   adminGuard.check();
   byte[] body = funnelDropService.csv(days, step).getBytes(StandardCharsets.UTF_8);
   return ResponseEntity.ok()
           .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"jejuro_funnel_drops_" + LocalDate.now() + ".csv\"")
           .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
           .body(body);
}

/** GET /api/admin/kpi?days=30  (7 / 30 / 90일) */
@GetMapping
public AdminKpiResponse dashboard(@RequestParam(name = "days", defaultValue = "30") int days) {
   adminGuard.check();
   return kpiService.dashboard(days);
}

/** GET /api/admin/kpi/training-data.csv → 재학습용 CSV (AI_TRAINING_DATASET 뷰, 후기까지 끝난 여행만) */
@GetMapping("/training-data.csv")
public ResponseEntity<byte[]> trainingData() {
   adminGuard.check();
   byte[] body = kpiService.trainingCsv().getBytes(StandardCharsets.UTF_8);
   return ResponseEntity.ok()
           .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"jejuro_training_" + LocalDate.now() + ".csv\"")
           .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
           .body(body);
}
}