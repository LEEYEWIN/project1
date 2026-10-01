package kr.fast.Jejuro.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** @Scheduled 예약 작업 켜기 (탈퇴 회원 자동 삭제 등) */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
