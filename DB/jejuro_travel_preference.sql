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
-- Table structure for table `travel_preference`
--

DROP TABLE IF EXISTS `travel_preference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `travel_preference` (
  `travel_preference_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `travel_id` bigint unsigned NOT NULL,
  `preference_id` bigint unsigned NOT NULL,
  `response_type` varchar(30) NOT NULL,
  `answer_value` int unsigned NOT NULL,
  `answer_rank` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '다중 선택 순위(사용자가 고른 순서). 1 = 1순위. 단일 선택은 1',
  `single_preference_id` bigint unsigned GENERATED ALWAYS AS ((case when (`response_type` = _utf8mb4'SINGLE_SELECT') then `preference_id` else NULL end)) STORED,
  PRIMARY KEY (`travel_preference_id`),
  UNIQUE KEY `uk_travel_preference_natural` (`travel_id`,`preference_id`,`answer_value`),
  UNIQUE KEY `uk_travel_preference_rank` (`travel_id`,`preference_id`,`answer_rank`),
  UNIQUE KEY `uk_travel_preference_single` (`travel_id`,`single_preference_id`),
  KEY `fk_travel_preference_type` (`preference_id`,`response_type`),
  KEY `fk_travel_preference_option` (`preference_id`,`answer_value`),
  CONSTRAINT `fk_travel_preference_option` FOREIGN KEY (`preference_id`, `answer_value`) REFERENCES `preference_option` (`preference_id`, `option_value`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_travel_preference_travel` FOREIGN KEY (`travel_id`) REFERENCES `travel` (`travel_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_travel_preference_type` FOREIGN KEY (`preference_id`, `response_type`) REFERENCES `preference` (`preference_id`, `response_type`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_travel_preference_rank` CHECK ((`answer_rank` >= 1))
) ENGINE=InnoDB AUTO_INCREMENT=92 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='여행별 설문 응답';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `travel_preference`
--

LOCK TABLES `travel_preference` WRITE;
/*!40000 ALTER TABLE `travel_preference` DISABLE KEYS */;
/*!40000 ALTER TABLE `travel_preference` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `tr_travel_preference_max_insert` BEFORE INSERT ON `travel_preference` FOR EACH ROW BEGIN
    DECLARE v_max INT DEFAULT NULL;
    DECLARE v_count INT DEFAULT 0;

    SELECT g.`max_selections` INTO v_max
      FROM `preference` q
      JOIN `preference_group` g ON g.`group_code` = q.`group_code`
     WHERE q.`preference_id` = NEW.`preference_id`;

    IF v_max IS NOT NULL THEN
        SELECT COUNT(*) INTO v_count FROM `travel_preference`
         WHERE `travel_id` = NEW.`travel_id`
           AND `preference_id` = NEW.`preference_id`;
        IF v_count >= v_max THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Too many answers for this preference';
        END IF;
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `tr_travel_preference_max_update` BEFORE UPDATE ON `travel_preference` FOR EACH ROW BEGIN
    DECLARE v_max INT DEFAULT NULL;
    DECLARE v_count INT DEFAULT 0;

    IF NOT (OLD.`travel_id` = NEW.`travel_id`
            AND OLD.`preference_id` = NEW.`preference_id`) THEN
        SELECT g.`max_selections` INTO v_max
          FROM `preference` q
          JOIN `preference_group` g ON g.`group_code` = q.`group_code`
         WHERE q.`preference_id` = NEW.`preference_id`;

        IF v_max IS NOT NULL THEN
            SELECT COUNT(*) INTO v_count FROM `travel_preference`
             WHERE `travel_id` = NEW.`travel_id`
               AND `preference_id` = NEW.`preference_id`;
            IF v_count >= v_max THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'Too many answers for this preference';
            END IF;
        END IF;
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-01 14:52:16
