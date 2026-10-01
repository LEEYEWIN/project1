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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='여행별 동행자';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `companion`
--

LOCK TABLES `companion` WRITE;
/*!40000 ALTER TABLE `companion` DISABLE KEYS */;
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

-- Dump completed on 2026-10-01 14:52:13
