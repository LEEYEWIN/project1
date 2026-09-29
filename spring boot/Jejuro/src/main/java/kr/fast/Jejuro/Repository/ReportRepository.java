package kr.fast.Jejuro.Repository;


//[커뮤니티 신고 · 관리자 신고 처리]

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kr.fast.Jejuro.Entity.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {

 boolean existsByTargetTypeAndTargetIdAndReporterId(String targetType, Long targetId, Long reporterId);

 /** 대상 하나의 신고 (처리 대기만 / 전체) */
 List<Report> findByTargetTypeAndTargetIdAndStatus(String targetType, Long targetId, String status);

 List<Report> findByTargetTypeAndTargetIdOrderByReportIdAsc(String targetType, Long targetId);

 long countByTargetTypeAndTargetIdAndStatus(String targetType, Long targetId, String status);

 /** 관리자 목록: 처리 대기 전부 */
 List<Report> findByStatusOrderByReportIdAsc(String status);

 /** 관리자 목록: 처리 완료 최근 N건 */
 List<Report> findTop500ByStatusNotOrderByHandledAtDesc(String status);

 long countByStatus(String status);

 /** 회원 한 명이 받은 신고 중 조치된 것 (회원 상세) */
 List<Report> findTop20ByTargetUserIdAndStatusOrderByReportIdDesc(Long targetUserId, String status);

 /** 작성자들이 조치받은 신고 (이전 위반 횟수 계산) */
 List<Report> findByStatusAndTargetUserIdIn(String status, Collection<Long> targetUserIds);

 /** 이 화면(글·댓글)에서 내가 이미 신고한 대상 */
 @Query("select r.targetId from Report r where r.targetType = :type and r.reporterId = :reporterId and r.targetId in :ids")
 List<Long> findReportedTargetIds(@Param("type") String type, @Param("reporterId") Long reporterId,
                                  @Param("ids") Collection<Long> ids);
}