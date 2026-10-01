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
-- Table structure for table `recommend_item`
--

DROP TABLE IF EXISTS `recommend_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recommend_item` (
  `item_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `request_id` bigint unsigned NOT NULL,
  `rank_no` smallint unsigned NOT NULL,
  `place_name` varchar(200) NOT NULL,
  `poi_id` bigint unsigned DEFAULT NULL COMMENT '우리 관광지와 연결 안 되면 NULL (매핑 실패)',
  `shown` tinyint(1) NOT NULL DEFAULT '0' COMMENT '화면에 보여 준 10곳이면 1',
  PRIMARY KEY (`item_id`),
  UNIQUE KEY `uk_recommend_item_rank` (`request_id`,`rank_no`),
  KEY `ix_recommend_item_poi` (`poi_id`),
  CONSTRAINT `fk_recommend_item_poi` FOREIGN KEY (`poi_id`) REFERENCES `poi` (`poi_id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_recommend_item_request` FOREIGN KEY (`request_id`) REFERENCES `recommend_request` (`request_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=106 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI가 추천한 관광지 (순위 순)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `recommend_item`
--

LOCK TABLES `recommend_item` WRITE;
/*!40000 ALTER TABLE `recommend_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `recommend_item` ENABLE KEYS */;
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
