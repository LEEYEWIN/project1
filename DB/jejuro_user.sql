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
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `user_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `nickname` varchar(50) NOT NULL,
  `birth_date` date NOT NULL,
  `gender_code` tinyint unsigned NOT NULL,
  `status` varchar(30) NOT NULL DEFAULT 'ACTIVE',
  `role` varchar(20) NOT NULL DEFAULT 'USER' COMMENT 'USER 일반 / ADMIN 관리자',
  `withdrawn_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_user_email` (`email`),
  UNIQUE KEY `uk_user_nickname` (`nickname`),
  KEY `ix_user_status_withdrawn` (`status`,`withdrawn_at`),
  CONSTRAINT `ck_user_gender_code` CHECK ((`gender_code` in (1,2))),
  CONSTRAINT `ck_user_role` CHECK ((`role` in (_utf8mb4'USER',_utf8mb4'ADMIN'))),
  CONSTRAINT `ck_user_status` CHECK ((((`status` = _utf8mb4'ACTIVE') and (`withdrawn_at` is null)) or ((`status` = _utf8mb4'WITHDRAWAL_PENDING') and (`withdrawn_at` is not null))))
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='서비스 회원. 이메일 가입 전용';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (5,'dev@example.com','$2b$12$0E12kqQh7DCHQg/OWHPmReICy0W3Zv/khB8dlblXyWJTtJuoy8gIm','개발자','1995-01-01',1,'ACTIVE','ADMIN',NULL,'2026-10-06 12:04:01',NULL),(7,'kpi1@example.com','$2b$12$0E12kqQh7DCHQg/OWHPmReICy0W3Zv/khB8dlblXyWJTtJuoy8gIm','KPI테스트1','1999-03-02',2,'ACTIVE','USER',NULL,'2026-10-06 12:06:15',NULL),(8,'kpi2@example.com','$2b$12$0E12kqQh7DCHQg/OWHPmReICy0W3Zv/khB8dlblXyWJTtJuoy8gIm','KPI테스트2','1996-07-11',1,'ACTIVE','USER',NULL,'2026-10-06 12:06:15',NULL),(9,'kpi3@example.com','$2b$12$0E12kqQh7DCHQg/OWHPmReICy0W3Zv/khB8dlblXyWJTtJuoy8gIm','KPI테스트3','1990-05-20',2,'ACTIVE','USER',NULL,'2026-10-06 12:06:15',NULL),(10,'kpi4@example.com','$2b$12$0E12kqQh7DCHQg/OWHPmReICy0W3Zv/khB8dlblXyWJTtJuoy8gIm','KPI테스트4','1987-11-02',1,'ACTIVE','USER',NULL,'2026-10-06 12:06:15',NULL),(11,'kpi5@example.com','$2b$12$0E12kqQh7DCHQg/OWHPmReICy0W3Zv/khB8dlblXyWJTtJuoy8gIm','KPI테스트5','1981-01-15',2,'ACTIVE','USER',NULL,'2026-10-06 12:06:15',NULL),(12,'kpi6@example.com','$2b$12$0E12kqQh7DCHQg/OWHPmReICy0W3Zv/khB8dlblXyWJTtJuoy8gIm','KPI테스트6','1972-09-09',1,'ACTIVE','USER',NULL,'2026-10-06 12:06:15',NULL),(13,'demo1@example.com','$2b$12$0E12kqQh7DCHQg/OWHPmReICy0W3Zv/khB8dlblXyWJTtJuoy8gIm','제주여행자','1997-04-12',2,'ACTIVE','USER',NULL,'2026-10-06 12:12:44',NULL),(14,'demo2@example.com','$2b$12$0E12kqQh7DCHQg/OWHPmReICy0W3Zv/khB8dlblXyWJTtJuoy8gIm','제주탐험가','1994-08-23',1,'ACTIVE','USER',NULL,'2026-10-06 12:12:44',NULL);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-06 15:51:51
