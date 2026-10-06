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
-- Table structure for table `travel_feedback`
--

DROP TABLE IF EXISTS `travel_feedback`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `travel_feedback` (
  `feedback_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `travel_id` bigint unsigned NOT NULL,
  `route_id` bigint unsigned NOT NULL,
  `execution_status` varchar(20) NOT NULL,
  `satisfaction_score` tinyint unsigned DEFAULT NULL,
  `answered_at` datetime NOT NULL,
  PRIMARY KEY (`feedback_id`),
  UNIQUE KEY `uk_travel_feedback_natural` (`travel_id`),
  KEY `fk_travel_feedback_route` (`travel_id`,`route_id`),
  CONSTRAINT `fk_travel_feedback_route` FOREIGN KEY (`travel_id`, `route_id`) REFERENCES `travel_route` (`travel_id`, `route_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_travel_feedback_travel` FOREIGN KEY (`travel_id`) REFERENCES `travel` (`travel_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `ck_travel_feedback_satisfaction` CHECK ((((`execution_status` = _utf8mb4'NOT_TAKEN') and (`satisfaction_score` is null)) or ((`execution_status` in (_utf8mb4'COMPLETED',_utf8mb4'PARTIAL')) and (`satisfaction_score` is not null) and (`satisfaction_score` between 1 and 5)))),
  CONSTRAINT `ck_travel_feedback_status` CHECK ((`execution_status` in (_utf8mb4'COMPLETED',_utf8mb4'PARTIAL',_utf8mb4'NOT_TAKEN')))
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='채택한 여행 루트의 실제 수행 결과';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `travel_feedback`
--

LOCK TABLES `travel_feedback` WRITE;
/*!40000 ALTER TABLE `travel_feedback` DISABLE KEYS */;
INSERT INTO `travel_feedback` VALUES (2,8,8,'COMPLETED',4,'2026-08-23 20:00:00'),(3,9,9,'PARTIAL',3,'2026-09-21 20:00:00'),(4,10,10,'PARTIAL',2,'2026-08-24 20:00:00'),(5,11,11,'COMPLETED',5,'2026-08-20 20:00:00'),(6,12,12,'PARTIAL',2,'2026-09-19 20:00:00'),(7,13,13,'PARTIAL',3,'2026-08-13 20:00:00'),(8,14,14,'PARTIAL',4,'2026-09-05 20:00:00'),(9,15,15,'COMPLETED',4,'2026-09-26 20:00:00'),(10,16,16,'COMPLETED',3,'2026-09-23 20:00:00'),(11,17,17,'COMPLETED',5,'2026-08-21 20:00:00'),(12,18,18,'COMPLETED',4,'2026-09-28 20:00:00'),(13,19,19,'PARTIAL',2,'2026-09-05 20:00:00'),(14,20,20,'PARTIAL',4,'2026-09-27 20:00:00'),(15,21,21,'PARTIAL',3,'2026-09-23 20:00:00'),(16,22,22,'PARTIAL',3,'2026-09-26 20:00:00'),(17,23,23,'COMPLETED',5,'2026-09-30 20:00:00'),(18,24,24,'COMPLETED',4,'2026-09-28 20:00:00'),(19,25,25,'COMPLETED',5,'2026-09-06 20:00:00'),(20,26,26,'PARTIAL',4,'2026-09-30 20:00:00'),(21,27,27,'COMPLETED',5,'2026-09-10 20:00:00'),(22,28,28,'PARTIAL',3,'2026-09-27 20:00:00'),(23,29,29,'COMPLETED',5,'2026-10-04 20:00:00'),(24,30,30,'COMPLETED',3,'2026-09-24 20:00:00'),(25,31,31,'PARTIAL',2,'2026-10-01 20:00:00'),(26,32,32,'COMPLETED',3,'2026-10-03 20:00:00'),(27,33,33,'COMPLETED',5,'2026-10-05 20:00:00'),(28,34,34,'NOT_TAKEN',NULL,'2026-09-26 20:00:00'),(29,48,45,'PARTIAL',4,'2026-09-19 20:00:00'),(30,53,50,'COMPLETED',5,'2026-09-10 20:00:00');
/*!40000 ALTER TABLE `travel_feedback` ENABLE KEYS */;
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
