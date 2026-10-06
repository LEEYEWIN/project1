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
-- Table structure for table `travel_feedback_reason`
--

DROP TABLE IF EXISTS `travel_feedback_reason`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `travel_feedback_reason` (
  `feedback_reason_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `feedback_id` bigint unsigned NOT NULL,
  `reason_code` varchar(30) NOT NULL,
  `reason_text` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`feedback_reason_id`),
  UNIQUE KEY `uk_travel_feedback_reason_natural` (`feedback_id`,`reason_code`),
  CONSTRAINT `fk_travel_feedback_reason_feedback` FOREIGN KEY (`feedback_id`) REFERENCES `travel_feedback` (`feedback_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `ck_travel_feedback_reason_code` CHECK ((`reason_code` in (_utf8mb4'TIME_SHORTAGE',_utf8mb4'CHANGE_OF_MIND',_utf8mb4'PERSONAL_REASON',_utf8mb4'WEATHER',_utf8mb4'POI_ISSUE',_utf8mb4'OTHER')))
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='여행 루트를 일부 수행했거나 여행하지 못한 이유';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `travel_feedback_reason`
--

LOCK TABLES `travel_feedback_reason` WRITE;
/*!40000 ALTER TABLE `travel_feedback_reason` DISABLE KEYS */;
INSERT INTO `travel_feedback_reason` VALUES (1,3,'POI_ISSUE',NULL),(2,3,'TIME_SHORTAGE',NULL),(3,4,'TIME_SHORTAGE',NULL),(4,4,'CHANGE_OF_MIND',NULL),(5,6,'WEATHER',NULL),(6,6,'TIME_SHORTAGE',NULL),(7,7,'CHANGE_OF_MIND',NULL),(8,7,'TIME_SHORTAGE',NULL),(9,8,'CHANGE_OF_MIND',NULL),(10,8,'WEATHER',NULL),(11,13,'WEATHER',NULL),(12,13,'TIME_SHORTAGE',NULL),(13,14,'CHANGE_OF_MIND',NULL),(14,14,'TIME_SHORTAGE',NULL),(15,15,'TIME_SHORTAGE',NULL),(16,15,'POI_ISSUE',NULL),(17,16,'POI_ISSUE',NULL),(18,16,'WEATHER',NULL),(19,20,'WEATHER',NULL),(20,20,'POI_ISSUE',NULL),(21,22,'TIME_SHORTAGE',NULL),(22,25,'TIME_SHORTAGE',NULL),(23,25,'POI_ISSUE',NULL),(24,28,'PERSONAL_REASON',NULL),(25,29,'TIME_SHORTAGE',NULL);
/*!40000 ALTER TABLE `travel_feedback_reason` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-06 15:51:52
