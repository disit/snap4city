-- MySQL dump 10.13  Distrib 5.7.24, for Linux (x86_64)
--
-- Host: localhost    Database: heatmap
-- ------------------------------------------------------
-- Server version	5.7.24

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `colors`
--

DROP TABLE IF EXISTS `colors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `colors` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `metric_name` varchar(255) DEFAULT NULL,
  `min` double DEFAULT NULL,
  `max` double DEFAULT NULL,
  `rgb` varchar(100) DEFAULT NULL,
  `color` varchar(100) DEFAULT NULL,
  `order` int(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1630 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `data`
--

DROP TABLE IF EXISTS `data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `data` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `map_name` varchar(255) DEFAULT NULL,
  `metric_name` varchar(255) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `value` double DEFAULT NULL,
  `sum` double DEFAULT NULL,
  `num` double DEFAULT NULL,
  `average` double DEFAULT NULL,
  `clustered` int(1) DEFAULT '0',
  `date` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `data_UN` (`map_name`,`metric_name`,`latitude`,`longitude`,`clustered`,`date`),
  KEY `mapname_date` (`map_name`,`date`),
  KEY `mix` (`metric_name`,`latitude`,`longitude`,`date`)
) ENGINE=InnoDB AUTO_INCREMENT=1168187457 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `maps_completed`
--

DROP TABLE IF EXISTS `maps_completed`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `maps_completed` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `map_name` varchar(255) DEFAULT NULL,
  `metric_name` varchar(255) DEFAULT NULL,
  `completed` int(1) DEFAULT '0',
  `indexed` int(1) DEFAULT '0',
  `date` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `data_UN` (`map_name`,`metric_name`,`date`),
  KEY `maps_completed_completed_IDX` (`completed`,`indexed`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=109984 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `metadata`
--

DROP TABLE IF EXISTS `metadata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `metadata` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `map_name` varchar(255) DEFAULT NULL,
  `metric_name` varchar(255) DEFAULT NULL,
  `clustered` int(11) DEFAULT '0',
  `description` varchar(255) DEFAULT NULL,
  `org` varchar(100) DEFAULT NULL,
  `days` int(11) DEFAULT '0',
  `service_uris` varchar(255) DEFAULT NULL,
  `x_length` double DEFAULT '0',
  `y_length` double DEFAULT '0',
  `deleted` int(1) DEFAULT '0',
  `projection` int(11) DEFAULT '4326',
  `file` int(1) DEFAULT '0',
  `binary` int(1) DEFAULT '0',
  `insertOnDB` int(1) DEFAULT '0',
  `fileType` varchar(100) DEFAULT NULL,
  `date` timestamp NULL DEFAULT NULL,
  `insertDate` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `metadata_UN` (`map_name`,`metric_name`,`clustered`,`date`)
) ENGINE=InnoDB AUTO_INCREMENT=65448373 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stats`
--

DROP TABLE IF EXISTS `stats`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `stats` (
  `map_name` varchar(255) NOT NULL,
  `metric_name` varchar(255) NOT NULL,
  `num` bigint(20) DEFAULT NULL,
  `min_date` timestamp NULL DEFAULT NULL,
  `max_date` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`map_name`,`metric_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2019-09-24 16:37:13
