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
-- Table structure for table `preference`
--

DROP TABLE IF EXISTS `preference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `preference` (
  `preference_id` bigint unsigned NOT NULL,
  `preference_code` varchar(50) NOT NULL,
  `preference_name` varchar(100) NOT NULL,
  `response_type` varchar(30) NOT NULL,
  `group_code` varchar(30) NOT NULL,
  PRIMARY KEY (`preference_id`),
  UNIQUE KEY `uk_preference_code` (`preference_code`),
  UNIQUE KEY `uk_preference_type` (`preference_id`,`response_type`),
  KEY `fk_preference_group` (`group_code`),
  CONSTRAINT `fk_preference_group` FOREIGN KEY (`group_code`) REFERENCES `preference_group` (`group_code`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_preference_response_type` CHECK ((`response_type` in (_utf8mb4'SINGLE_SELECT',_utf8mb4'MULTI_SELECT')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='여행 설문 질문';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `preference`
--

LOCK TABLES `preference` WRITE;
/*!40000 ALTER TABLE `preference` DISABLE KEYS */;
INSERT INTO `preference` VALUES (101,'STYLE_NATURE_CITY','자연 중심 ↔ 도시 중심','SINGLE_SELECT','STYLE'),(102,'STYLE_NEW_FAMILIAR','새로운 장소 ↔ 익숙한 장소','SINGLE_SELECT','STYLE'),(103,'STYLE_HIDDEN_FAMOUS','숨은 명소 ↔ 유명 인기 명소','SINGLE_SELECT','STYLE'),(104,'STYLE_RELAX_ACTIVITY','휴식·힐링 ↔ 체험·액티비티','SINGLE_SELECT','STYLE'),(105,'PHOTO_IMPORTANCE','사진 촬영 중요도','SINGLE_SELECT','STYLE'),(106,'STYLE_PLAN_FREE','계획대로 ↔ 상황에 따라 자유롭게','SINGLE_SELECT','STYLE'),(201,'TRAVEL_MOTIVE','여행 동기','MULTI_SELECT','MOTIVE'),(202,'USER_MISSION','테마 선호도','MULTI_SELECT','THEME'),(203,'INCOME_CODE','월평균 소득 구간','SINGLE_SELECT','INCOME');
/*!40000 ALTER TABLE `preference` ENABLE KEYS */;
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
