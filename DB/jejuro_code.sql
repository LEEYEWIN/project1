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
-- Table structure for table `code`
--

DROP TABLE IF EXISTS `code`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `code` (
  `code_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `group_code` varchar(30) NOT NULL,
  `code_value` varchar(50) NOT NULL,
  `code_name` varchar(100) NOT NULL,
  PRIMARY KEY (`code_id`),
  UNIQUE KEY `uk_code_natural` (`group_code`,`code_value`),
  CONSTRAINT `fk_code_group` FOREIGN KEY (`group_code`) REFERENCES `code_group` (`group_code`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='공통 코드 값';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `code`
--

LOCK TABLES `code` WRITE;
/*!40000 ALTER TABLE `code` DISABLE KEYS */;
INSERT INTO `code` VALUES (1,'GEN','1','남자'),(2,'GEN','2','여자'),(3,'AGE','1','9세 이하'),(4,'AGE','2','10대'),(5,'AGE','3','20대'),(6,'AGE','4','30대'),(7,'AGE','5','40대'),(8,'AGE','6','50대'),(9,'AGE','7','60대'),(10,'AGE','8','70세 이상'),(11,'TCR','1','배우자'),(12,'TCR','2','자녀'),(13,'TCR','3','부모'),(14,'TCR','4','조부모'),(15,'TCR','5','형제·자매'),(16,'TCR','6','친인척'),(17,'TCR','7','친구'),(18,'TCR','8','연인'),(19,'TCR','9','동료'),(20,'TCR','10','친목 단체·모임'),(21,'TCR','11','기타'),(22,'POI_CAT','NATURE','자연관광지'),(23,'POI_CAT','HISTORY','역사·유적·종교 시설'),(24,'POI_CAT','CULTURE','문화시설'),(25,'POI_CAT','COMMERCIAL','상업지구'),(26,'POI_CAT','LEISURE','레저·스포츠 관련 시설'),(27,'POI_CAT','THEME','테마시설'),(28,'POI_CAT','TRAIL','산책로·둘레길'),(29,'POI_CAT','FESTIVAL','지역 축제·행사'),(30,'POI_CAT','EXPERIENCE','체험 활동 관광지'),(31,'ACCOM_TYPE','HOTEL','호텔'),(32,'ACCOM_TYPE','RESORT','리조트·콘도'),(33,'ACCOM_TYPE','PENSION','펜션·풀빌라'),(34,'ACCOM_TYPE','GUESTHOUSE','게스트하우스·민박'),(35,'ACCOM_TYPE','MOTEL','모텔'),(36,'ACCOM_TYPE','CAMPING','캠핑·글램핑'),(37,'ACCOM_TYPE','ETC','기타 숙소');
/*!40000 ALTER TABLE `code` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-01 14:52:17
