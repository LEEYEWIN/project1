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
-- Table structure for table `community_post`
--

DROP TABLE IF EXISTS `community_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `community_post` (
  `post_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned DEFAULT NULL,
  `travel_id` bigint unsigned DEFAULT NULL COMMENT '후기에 첨부한 여행(채택 경로 표시용). 여행 삭제 시 NULL',
  `post_type` varchar(20) NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` text NOT NULL,
  `image_url` varchar(500) DEFAULT NULL COMMENT '첨부 사진 1장',
  `view_count` int unsigned NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL COMMENT '삭제 시각(삭제된 글은 조회 제외)',
  `hidden_at` datetime DEFAULT NULL COMMENT '신고된 시각("신고된 게시글입니다", 관리자만 내용 확인). 반려하면 NULL',
  `block_reason` varchar(20) DEFAULT NULL COMMENT '관리자 차단 사유(신고 사유 코드). NULL이면 차단 아님',
  PRIMARY KEY (`post_id`),
  KEY `fk_community_post_travel` (`travel_id`),
  KEY `ix_community_post_user_created` (`user_id`,`created_at`),
  KEY `ix_community_post_type_created` (`post_type`,`deleted_at`,`created_at`),
  CONSTRAINT `fk_community_post_travel` FOREIGN KEY (`travel_id`) REFERENCES `travel` (`travel_id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_community_post_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `ck_community_post_block_reason` CHECK (((`block_reason` is null) or (`block_reason` in (_utf8mb4'SEXUAL',_utf8mb4'PRIVACY',_utf8mb4'ABUSE',_utf8mb4'SPAM')))),
  CONSTRAINT `ck_community_post_type` CHECK ((`post_type` in (_utf8mb4'QUESTION',_utf8mb4'REVIEW')))
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='커뮤니티 질문 및 여행 후기. 탈퇴 회원 글은 user_id NULL';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `community_post`
--

LOCK TABLES `community_post` WRITE;
/*!40000 ALTER TABLE `community_post` DISABLE KEYS */;
/*!40000 ALTER TABLE `community_post` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-01 14:52:15
