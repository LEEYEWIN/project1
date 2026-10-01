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
-- Table structure for table `preference_option`
--

DROP TABLE IF EXISTS `preference_option`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `preference_option` (
  `option_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `preference_id` bigint unsigned NOT NULL,
  `option_value` int unsigned NOT NULL,
  `option_name` varchar(100) NOT NULL,
  PRIMARY KEY (`option_id`),
  UNIQUE KEY `uk_preference_option_natural` (`preference_id`,`option_value`),
  CONSTRAINT `fk_preference_option_preference` FOREIGN KEY (`preference_id`) REFERENCES `preference` (`preference_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=85 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='설문 질문의 선택지';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `preference_option`
--

LOCK TABLES `preference_option` WRITE;
/*!40000 ALTER TABLE `preference_option` DISABLE KEYS */;
INSERT INTO `preference_option` VALUES (1,101,1,'자연 매우 선호'),(2,101,2,'자연 중간 선호'),(3,101,3,'자연 약간 선호'),(4,101,4,'중립'),(5,101,5,'도시 약간 선호'),(6,101,6,'도시 중간 선호'),(7,101,7,'도시 매우 선호'),(8,102,1,'새로운 지역 매우 선호'),(9,102,2,'새로운 지역 중간 선호'),(10,102,3,'새로운 지역 약간 선호'),(11,102,4,'중립'),(12,102,5,'익숙한 지역 약간 선호'),(13,102,6,'익숙한 지역 중간 선호'),(14,102,7,'익숙한 지역 매우 선호'),(15,103,1,'숨은 명소 매우 선호'),(16,103,2,'숨은 명소 중간 선호'),(17,103,3,'숨은 명소 약간 선호'),(18,103,4,'중립'),(19,103,5,'유명 명소 약간 선호'),(20,103,6,'유명 명소 중간 선호'),(21,103,7,'유명 명소 매우 선호'),(22,104,1,'휴양·휴식 매우 선호'),(23,104,2,'휴양·휴식 중간 선호'),(24,104,3,'휴양·휴식 약간 선호'),(25,104,4,'중립'),(26,104,5,'체험·활동 약간 선호'),(27,104,6,'체험·활동 중간 선호'),(28,104,7,'체험·활동 매우 선호'),(29,105,1,'사진 촬영 전혀 중요하지 않음'),(30,105,2,'사진 촬영 중요하지 않은 편'),(31,105,3,'사진 촬영 약간 중요하지 않음'),(32,105,4,'중립'),(33,105,5,'사진 촬영 약간 중요함'),(34,105,6,'사진 촬영 중요한 편'),(35,105,7,'사진 촬영 매우 중요함'),(36,106,1,'계획에 따른 여행 매우 선호'),(37,106,2,'계획에 따른 여행 중간 선호'),(38,106,3,'계획에 따른 여행 약간 선호'),(39,106,4,'중립'),(40,106,5,'상황에 따른 여행 약간 선호'),(41,106,6,'상황에 따른 여행 중간 선호'),(42,106,7,'상황에 따른 여행 매우 선호'),(43,201,1,'일상 탈출·기분 전환'),(44,201,2,'휴식·피로 회복'),(45,201,3,'친목·유대감'),(46,201,4,'자아 탐색·성찰'),(47,201,5,'SNS 공유·자랑'),(48,201,6,'운동·건강'),(49,201,7,'새로운 경험'),(50,201,8,'역사·문화·배움'),(51,201,9,'기념·특별한 목적'),(52,202,1,'쇼핑'),(53,202,2,'테마파크, 놀이시설, 동·식물원 방문'),(54,202,3,'역사 유적지 방문'),(55,202,4,'시티투어'),(56,202,5,'야외 스포츠·레포츠 활동'),(57,202,6,'지역 문화예술·공연·전시시설 관람'),(58,202,7,'유흥·오락(나이트라이프)'),(59,202,8,'캠핑'),(60,202,9,'지역 축제·이벤트 참가'),(61,202,10,'온천·스파'),(62,202,11,'교육·체험 프로그램 참가'),(63,202,12,'드라마 촬영지 방문'),(64,202,13,'종교·성지 순례'),(65,202,14,'웰니스 여행'),(66,202,15,'SNS 인생샷 여행'),(67,202,16,'호캉스 여행'),(68,202,17,'신규 여행지 발굴'),(69,202,18,'반려동물 동반 여행'),(70,202,19,'인플루언서 따라 하기 여행'),(71,202,20,'친환경 여행'),(72,202,21,'등반여행'),(73,203,1,'소득 없음'),(74,203,2,'월평균 100만 원 미만'),(75,203,3,'월평균 100만 원 이상 ~ 200만 원 미만'),(76,203,4,'월평균 200만 원 이상 ~ 300만 원 미만'),(77,203,5,'월평균 300만 원 이상 ~ 400만 원 미만'),(78,203,6,'월평균 400만 원 이상 ~ 500만 원 미만'),(79,203,7,'월평균 500만 원 이상 ~ 600만 원 미만'),(80,203,8,'월평균 600만 원 이상 ~ 700만 원 미만'),(81,203,9,'월평균 700만 원 이상 ~ 800만 원 미만'),(82,203,10,'월평균 800만 원 이상 ~ 900만 원 미만'),(83,203,11,'월평균 900만 원 이상 ~ 1,000만 원 미만'),(84,203,12,'월평균 1,000만 원 이상');
/*!40000 ALTER TABLE `preference_option` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-01 14:52:13
