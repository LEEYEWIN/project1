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
-- Table structure for table `travel_feedback_spot`
--

DROP TABLE IF EXISTS `travel_feedback_spot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `travel_feedback_spot` (
  `feedback_spot_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `feedback_id` bigint unsigned NOT NULL,
  `poi_id` bigint unsigned NOT NULL,
  `visited` tinyint(1) NOT NULL,
  `reaction` varchar(10) DEFAULT NULL COMMENT 'LIKE 좋았어요 / DISLIKE 아쉬워요 / NULL 선택 안 함',
  PRIMARY KEY (`feedback_spot_id`),
  UNIQUE KEY `uk_travel_feedback_spot` (`feedback_id`,`poi_id`),
  KEY `fk_travel_feedback_spot_poi` (`poi_id`),
  CONSTRAINT `fk_travel_feedback_spot_feedback` FOREIGN KEY (`feedback_id`) REFERENCES `travel_feedback` (`feedback_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_travel_feedback_spot_poi` FOREIGN KEY (`poi_id`) REFERENCES `poi` (`poi_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_travel_feedback_spot_reaction` CHECK (((`reaction` is null) or ((`visited` = 1) and (`reaction` in (_utf8mb4'LIKE',_utf8mb4'DISLIKE')))))
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='후기: 확정 일정의 관광지별 방문 여부와 반응';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `travel_feedback_spot`
--

LOCK TABLES `travel_feedback_spot` WRITE;
/*!40000 ALTER TABLE `travel_feedback_spot` DISABLE KEYS */;
/*!40000 ALTER TABLE `travel_feedback_spot` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-01 14:52:14
