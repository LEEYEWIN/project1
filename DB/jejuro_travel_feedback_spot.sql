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
) ENGINE=InnoDB AUTO_INCREMENT=120 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='후기: 확정 일정의 관광지별 방문 여부와 반응';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `travel_feedback_spot`
--

LOCK TABLES `travel_feedback_spot` WRITE;
/*!40000 ALTER TABLE `travel_feedback_spot` DISABLE KEYS */;
INSERT INTO `travel_feedback_spot` VALUES (7,2,408,1,'LIKE'),(8,2,57,1,'LIKE'),(9,2,163,1,'LIKE'),(10,3,290,1,NULL),(11,3,516,0,NULL),(12,3,503,1,'LIKE'),(13,3,483,1,NULL),(14,3,510,1,'DISLIKE'),(15,3,57,1,'LIKE'),(16,3,169,1,'LIKE'),(17,4,241,1,NULL),(18,4,430,1,'LIKE'),(19,4,142,1,NULL),(20,4,26,0,NULL),(21,4,56,1,NULL),(22,4,297,1,'LIKE'),(23,5,3,1,NULL),(24,5,57,1,'LIKE'),(25,5,257,1,'LIKE'),(26,6,142,1,NULL),(27,6,418,0,NULL),(28,6,128,1,'DISLIKE'),(29,6,535,1,'LIKE'),(30,6,16,1,'LIKE'),(31,7,241,0,NULL),(32,7,501,0,NULL),(33,7,516,1,'DISLIKE'),(34,8,421,0,NULL),(35,8,425,1,'LIKE'),(36,8,85,1,'LIKE'),(37,9,533,1,'DISLIKE'),(38,9,21,1,'LIKE'),(39,10,427,1,'DISLIKE'),(40,10,509,1,'LIKE'),(41,10,465,1,'LIKE'),(42,11,229,1,'LIKE'),(43,11,268,1,NULL),(44,11,466,1,'LIKE'),(45,12,32,1,NULL),(46,13,528,1,NULL),(47,13,468,1,NULL),(48,13,407,0,NULL),(49,14,506,1,'LIKE'),(50,14,532,1,NULL),(51,14,491,1,'LIKE'),(52,14,84,1,'LIKE'),(53,14,472,1,'DISLIKE'),(54,14,57,0,NULL),(55,15,200,1,NULL),(56,15,281,0,NULL),(57,15,22,1,'LIKE'),(58,15,432,1,'LIKE'),(59,15,203,1,'DISLIKE'),(60,15,428,0,NULL),(61,15,57,1,'LIKE'),(62,16,267,1,NULL),(63,16,433,1,'DISLIKE'),(64,16,120,1,'LIKE'),(65,16,426,0,NULL),(66,16,404,1,NULL),(67,16,418,1,'LIKE'),(68,17,24,1,'LIKE'),(69,18,233,1,NULL),(70,18,21,1,'LIKE'),(71,19,272,1,'LIKE'),(72,19,106,1,NULL),(73,19,45,1,NULL),(74,20,485,1,NULL),(75,20,533,0,NULL),(76,20,57,1,NULL),(77,21,422,1,'DISLIKE'),(78,21,217,1,'LIKE'),(79,21,273,1,'LIKE'),(80,21,535,1,'LIKE'),(81,21,331,1,NULL),(82,22,209,0,NULL),(83,22,49,1,'LIKE'),(84,22,112,1,NULL),(85,22,340,0,NULL),(86,22,515,1,'LIKE'),(87,22,212,1,'LIKE'),(88,22,69,1,NULL),(89,22,21,1,NULL),(90,23,510,1,'LIKE'),(91,23,168,1,NULL),(92,23,120,1,NULL),(93,23,509,1,'LIKE'),(94,23,9,1,NULL),(95,23,535,1,NULL),(96,23,289,1,NULL),(97,24,267,1,NULL),(98,24,32,1,'LIKE'),(99,24,432,1,'LIKE'),(100,24,129,1,NULL),(101,25,336,1,NULL),(102,25,519,1,'LIKE'),(103,25,52,0,NULL),(104,25,413,1,'LIKE'),(105,25,57,1,NULL),(106,26,522,1,NULL),(107,26,439,1,NULL),(108,26,409,1,'LIKE'),(109,26,535,1,NULL),(110,27,281,1,'LIKE'),(111,27,535,1,'LIKE'),(112,29,225,1,'LIKE'),(113,29,415,1,'LIKE'),(114,29,5,1,'LIKE'),(115,29,6,1,NULL),(116,29,103,0,NULL),(117,30,217,1,'LIKE'),(118,30,13,1,'LIKE'),(119,30,10,1,NULL);
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

-- Dump completed on 2026-10-06 15:51:53
