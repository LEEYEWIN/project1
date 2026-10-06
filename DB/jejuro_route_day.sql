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
-- Table structure for table `route_day`
--

DROP TABLE IF EXISTS `route_day`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `route_day` (
  `route_day_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `route_id` bigint unsigned NOT NULL,
  `day_no` smallint unsigned NOT NULL,
  `primary_region_id` tinyint unsigned NOT NULL,
  PRIMARY KEY (`route_day_id`),
  UNIQUE KEY `uk_route_day_no` (`route_id`,`day_no`),
  KEY `fk_route_day_region` (`primary_region_id`),
  CONSTRAINT `fk_route_day_region` FOREIGN KEY (`primary_region_id`) REFERENCES `region` (`region_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_route_day_route` FOREIGN KEY (`route_id`) REFERENCES `travel_route` (`route_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `ck_route_day_no` CHECK ((`day_no` >= 1))
) ENGINE=InnoDB AUTO_INCREMENT=135 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='여행 루트의 일차별 일정';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `route_day`
--

LOCK TABLES `route_day` WRITE;
/*!40000 ALTER TABLE `route_day` DISABLE KEYS */;
INSERT INTO `route_day` VALUES (39,8,1,4),(40,8,2,2),(41,8,3,1),(42,9,1,3),(43,9,2,3),(44,9,3,2),(45,10,1,3),(46,10,2,3),(47,11,1,1),(48,11,2,2),(49,11,3,1),(50,12,1,3),(51,13,1,3),(52,14,1,3),(53,14,2,3),(54,14,3,3),(55,15,1,1),(56,15,2,1),(57,16,1,1),(58,16,2,3),(59,16,3,1),(60,17,1,4),(61,17,2,4),(62,18,1,2),(63,19,1,4),(64,20,1,1),(65,20,2,1),(66,21,1,2),(67,21,2,1),(68,21,3,2),(69,22,1,3),(70,22,2,3),(71,23,1,4),(72,24,1,1),(73,24,2,1),(74,25,1,4),(75,26,1,3),(76,26,2,1),(77,26,3,2),(78,27,1,2),(79,28,1,4),(80,28,2,4),(81,28,3,4),(82,29,1,3),(83,29,2,3),(84,29,3,2),(85,30,1,1),(86,30,2,2),(87,30,3,2),(88,31,1,2),(89,31,2,2),(90,31,3,2),(91,31,4,2),(92,32,1,1),(93,33,1,3),(94,33,2,2),(95,34,1,1),(96,35,1,2),(97,35,2,1),(98,36,1,2),(99,36,2,2),(100,37,1,1),(101,37,2,1),(102,38,1,1),(103,39,1,2),(104,39,2,2),(105,39,3,2),(106,40,1,2),(107,40,2,2),(108,40,3,2),(109,41,1,3),(110,41,2,1),(111,42,1,2),(112,42,2,2),(113,43,1,1),(114,43,2,1),(115,44,1,3),(116,44,2,3),(117,45,1,1),(118,45,2,1),(119,45,3,1),(120,46,1,2),(121,46,2,2),(122,46,3,2),(123,47,1,1),(124,47,2,1),(125,48,1,1),(126,48,2,1),(127,50,1,2),(128,50,2,2),(129,51,1,1),(130,51,2,1),(131,51,3,1),(133,52,1,2),(134,52,2,4);
/*!40000 ALTER TABLE `route_day` ENABLE KEYS */;
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
