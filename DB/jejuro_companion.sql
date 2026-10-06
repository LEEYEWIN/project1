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
-- Table structure for table `companion`
--

DROP TABLE IF EXISTS `companion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `companion` (
  `companion_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `travel_id` bigint unsigned NOT NULL,
  `companion_seq` smallint unsigned NOT NULL,
  `relation_code` tinyint unsigned NOT NULL,
  `gender_code` tinyint unsigned NOT NULL,
  `age_group_code` tinyint unsigned NOT NULL,
  PRIMARY KEY (`companion_id`),
  UNIQUE KEY `uk_companion_natural` (`travel_id`,`companion_seq`),
  CONSTRAINT `fk_companion_travel` FOREIGN KEY (`travel_id`) REFERENCES `travel` (`travel_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `ck_companion_age_group` CHECK ((`age_group_code` between 1 and 8)),
  CONSTRAINT `ck_companion_gender` CHECK ((`gender_code` in (1,2))),
  CONSTRAINT `ck_companion_relation` CHECK ((`relation_code` between 1 and 11)),
  CONSTRAINT `ck_companion_seq` CHECK ((`companion_seq` >= 1))
) ENGINE=InnoDB AUTO_INCREMENT=68 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='여행별 동행자';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `companion`
--

LOCK TABLES `companion` WRITE;
/*!40000 ALTER TABLE `companion` DISABLE KEYS */;
INSERT INTO `companion` VALUES (3,9,1,7,1,3),(4,9,2,7,2,3),(5,10,1,8,2,3),(6,11,1,3,1,7),(7,11,2,3,2,7),(8,12,1,1,1,4),(9,12,2,2,2,1),(10,13,1,5,2,4),(11,13,2,6,1,4),(12,15,1,7,1,3),(13,15,2,7,2,3),(14,16,1,8,2,3),(15,17,1,3,1,7),(16,17,2,3,2,7),(17,18,1,1,1,4),(18,18,2,2,2,1),(19,19,1,5,2,4),(20,19,2,6,1,4),(21,21,1,7,1,3),(22,21,2,7,2,3),(23,22,1,8,2,3),(24,23,1,3,1,7),(25,23,2,3,2,7),(26,24,1,1,1,4),(27,24,2,2,2,1),(28,25,1,5,2,4),(29,25,2,6,1,4),(30,27,1,7,1,3),(31,27,2,7,2,3),(32,28,1,8,2,3),(33,29,1,3,1,7),(34,29,2,3,2,7),(35,30,1,1,1,4),(36,30,2,2,2,1),(37,31,1,5,2,4),(38,31,2,6,1,4),(39,33,1,7,1,3),(40,33,2,7,2,3),(41,34,1,7,1,3),(42,34,2,7,2,3),(43,36,1,7,1,3),(44,36,2,7,2,3),(45,37,1,8,2,3),(46,38,1,8,2,3),(47,39,1,3,1,7),(48,39,2,3,2,7),(49,40,1,1,1,4),(50,40,2,2,2,1),(51,43,1,7,1,3),(52,43,2,7,2,3),(53,44,1,8,2,3),(54,45,1,3,1,7),(55,45,2,3,2,7),(56,46,1,1,1,4),(57,46,2,2,2,1),(58,47,1,5,2,4),(59,47,2,6,1,4),(60,48,1,7,2,3),(61,49,1,3,1,7),(62,49,2,3,2,7),(63,50,1,7,1,3),(64,50,2,7,2,3),(65,51,1,8,1,3),(66,54,1,3,1,7),(67,54,2,3,2,7);
/*!40000 ALTER TABLE `companion` ENABLE KEYS */;
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
