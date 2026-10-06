-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: jejuro
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `report`
--

DROP TABLE IF EXISTS `report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report` (
  `report_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `target_type` varchar(10) NOT NULL COMMENT 'POST 글 / COMMENT 댓글',
  `target_id` bigint unsigned NOT NULL COMMENT 'post_id 또는 comment_id',
  `target_user_id` bigint unsigned DEFAULT NULL COMMENT '신고 당시 작성자',
  `reporter_id` bigint unsigned NOT NULL,
  `reason_code` varchar(20) NOT NULL,
  `detail` varchar(200) DEFAULT NULL COMMENT '신고한 회원이 적은 자세한 내용(선택)',
  `status` varchar(10) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING 대기 / ACCEPTED 조치 / REJECTED 문제 없음',
  `action` varchar(10) DEFAULT NULL COMMENT '처리 결과 KEEP 유지 / BLOCK 차단 / DELETE 삭제',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `handled_at` datetime DEFAULT NULL,
  `handled_by` bigint unsigned DEFAULT NULL,
  PRIMARY KEY (`report_id`),
  UNIQUE KEY `uk_report_once` (`target_type`,`target_id`,`reporter_id`),
  KEY `fk_report_reporter` (`reporter_id`),
  KEY `fk_report_handled_by` (`handled_by`),
  KEY `ix_report_status_target` (`status`,`target_type`,`target_id`),
  KEY `ix_report_target_user` (`target_user_id`,`status`),
  CONSTRAINT `fk_report_handled_by` FOREIGN KEY (`handled_by`) REFERENCES `user` (`user_id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_report_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_report_target_user` FOREIGN KEY (`target_user_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `ck_report_action` CHECK (((`action` is null) or (`action` in (_utf8mb4'KEEP',_utf8mb4'BLOCK',_utf8mb4'DELETE')))),
  CONSTRAINT `ck_report_reason` CHECK ((`reason_code` in (_utf8mb4'SEXUAL',_utf8mb4'PRIVACY',_utf8mb4'ABUSE',_utf8mb4'SPAM'))),
  CONSTRAINT `ck_report_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'ACCEPTED',_utf8mb4'REJECTED'))),
  CONSTRAINT `ck_report_target_type` CHECK ((`target_type` in (_utf8mb4'POST',_utf8mb4'COMMENT')))
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='게시글·댓글 신고';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `report`
--

LOCK TABLES `report` WRITE;
/*!40000 ALTER TABLE `report` DISABLE KEYS */;
INSERT INTO `report` VALUES (2,'POST',6,14,13,'SPAM','숙소 홍보 글이에요','PENDING',NULL,'2026-10-05 09:00:00',NULL,NULL);
/*!40000 ALTER TABLE `report` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-06 15:51:54
