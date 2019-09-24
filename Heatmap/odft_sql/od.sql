-- MySQL dump 10.13  Distrib 5.7.25, for Linux (x86_64)
--
-- Host: localhost    Database: od
-- ------------------------------------------------------
-- Server version	5.7.25

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
-- Table structure for table `clustered_trajectories`
--

DROP TABLE IF EXISTS `clustered_trajectories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `clustered_trajectories` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `cluster_id` varchar(45) DEFAULT NULL,
  `cluster_size` int(11) DEFAULT NULL,
  `trajectory` longtext,
  `profile` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_clustered_trajectories_profile` (`profile`)
) ENGINE=InnoDB AUTO_INCREMENT=6389 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `kpidata`
--

DROP TABLE IF EXISTS `kpidata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `kpidata` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `high_level_type` varchar(128) DEFAULT NULL,
  `nature` varchar(128) DEFAULT NULL,
  `sub_nature` varchar(128) DEFAULT NULL,
  `value_name` varchar(128) DEFAULT NULL,
  `value_type` varchar(128) DEFAULT NULL,
  `data_type` varchar(128) DEFAULT NULL,
  `instance_uri` varchar(256) DEFAULT NULL,
  `get_instances` varchar(128) DEFAULT NULL,
  `last_date` datetime DEFAULT NULL,
  `last_value` varchar(128) DEFAULT NULL,
  `last_check` datetime DEFAULT NULL,
  `metric` varchar(128) DEFAULT NULL,
  `saved_direct` varchar(128) DEFAULT NULL,
  `kb_based` varchar(128) DEFAULT NULL,
  `sm_based` varchar(128) DEFAULT NULL,
  `username` varchar(128) DEFAULT NULL,
  `organizations` text,
  `app_id` varchar(255) DEFAULT NULL,
  `app_name` varchar(255) DEFAULT NULL,
  `widgets` varchar(128) DEFAULT NULL,
  `parameters` varchar(512) DEFAULT NULL,
  `healthiness` varchar(128) DEFAULT 'false',
  `microAppExtServIcon` varchar(100) DEFAULT NULL,
  `ownership` varchar(64) DEFAULT NULL,
  `description` varchar(250) DEFAULT NULL,
  `info` varchar(250) DEFAULT NULL,
  `latitude` varchar(45) DEFAULT NULL,
  `longitude` varchar(45) DEFAULT NULL,
  `insert_time` datetime DEFAULT NULL,
  `delete_time` datetime DEFAULT NULL,
  `db_values_type` varchar(128) DEFAULT NULL,
  `db_values_link` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=17055993 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `kpidatatest`
--

DROP TABLE IF EXISTS `kpidatatest`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `kpidatatest` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `high_level_type` varchar(128) DEFAULT NULL,
  `nature` varchar(128) DEFAULT NULL,
  `sub_nature` varchar(128) DEFAULT NULL,
  `value_name` varchar(128) DEFAULT NULL,
  `value_type` varchar(128) DEFAULT NULL,
  `data_type` varchar(128) DEFAULT NULL,
  `instance_uri` varchar(256) DEFAULT NULL,
  `get_instances` varchar(128) DEFAULT NULL,
  `last_date` datetime DEFAULT NULL,
  `last_value` varchar(128) DEFAULT NULL,
  `last_check` datetime DEFAULT NULL,
  `metric` varchar(128) DEFAULT NULL,
  `saved_direct` varchar(128) DEFAULT NULL,
  `kb_based` varchar(128) DEFAULT NULL,
  `sm_based` varchar(128) DEFAULT NULL,
  `username` varchar(128) DEFAULT NULL,
  `organizations` text,
  `app_id` varchar(255) DEFAULT NULL,
  `app_name` varchar(255) DEFAULT NULL,
  `widgets` varchar(128) DEFAULT NULL,
  `parameters` varchar(512) DEFAULT NULL,
  `healthiness` varchar(128) DEFAULT 'false',
  `microAppExtServIcon` varchar(100) DEFAULT NULL,
  `ownership` varchar(64) DEFAULT NULL,
  `description` varchar(250) DEFAULT NULL,
  `info` varchar(250) DEFAULT NULL,
  `latitude` varchar(45) DEFAULT NULL,
  `longitude` varchar(45) DEFAULT NULL,
  `insert_time` datetime DEFAULT NULL,
  `delete_time` datetime DEFAULT NULL,
  `db_values_type` varchar(128) DEFAULT NULL,
  `db_values_link` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `kpivalues`
--

DROP TABLE IF EXISTS `kpivalues`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `kpivalues` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `kpi_id` bigint(20) DEFAULT NULL,
  `insert_time` timestamp NULL DEFAULT NULL,
  `value` varchar(256) DEFAULT NULL,
  `data_time` timestamp NULL DEFAULT NULL,
  `elapse_time` timestamp NULL DEFAULT NULL,
  `delete_time` timestamp NULL DEFAULT NULL,
  `latitude` varchar(45) DEFAULT NULL,
  `longitude` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=838716 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `kpivaluestest`
--

DROP TABLE IF EXISTS `kpivaluestest`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `kpivaluestest` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `kpi_id` bigint(20) DEFAULT NULL,
  `insert_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `value` varchar(256) DEFAULT NULL,
  `data_time` timestamp NULL DEFAULT NULL,
  `elapse_time` timestamp NULL DEFAULT NULL,
  `delete_time` timestamp NULL DEFAULT NULL,
  `latitude` varchar(45) DEFAULT NULL,
  `longitude` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=160001 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sensors`
--

DROP TABLE IF EXISTS `sensors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sensors` (
  `idmeasure` int(11) NOT NULL AUTO_INCREMENT,
  `UUID` varchar(45) COLLATE utf8_unicode_ci DEFAULT '',
  `id` varchar(35) COLLATE utf8_unicode_ci DEFAULT '',
  `sender_IP` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
  `date` datetime NOT NULL,
  `type` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `latitude` double NOT NULL DEFAULT '0',
  `longitude` double NOT NULL DEFAULT '0',
  `network_name` varchar(45) COLLATE utf8_unicode_ci DEFAULT '',
  `sensor_name` varchar(45) COLLATE utf8_unicode_ci DEFAULT '',
  `MAC_address` varchar(45) COLLATE utf8_unicode_ci DEFAULT NULL,
  `power` varchar(45) COLLATE utf8_unicode_ci DEFAULT NULL,
  `rssi` varchar(45) COLLATE utf8_unicode_ci DEFAULT NULL,
  `minor` int(11) DEFAULT NULL,
  `major` int(11) DEFAULT NULL,
  `frequency` varchar(45) COLLATE utf8_unicode_ci DEFAULT NULL,
  `capabilities` varchar(125) COLLATE utf8_unicode_ci DEFAULT NULL,
  `speed` double DEFAULT NULL,
  `altitude` double DEFAULT NULL,
  `provider` varchar(45) COLLATE utf8_unicode_ci DEFAULT '',
  `accuracy` double DEFAULT NULL,
  `heading` double DEFAULT NULL,
  `lat_pre_scan` double DEFAULT NULL,
  `long_pre_scan` double DEFAULT NULL,
  `date_pre_scan` datetime DEFAULT NULL,
  `device_id` varchar(64) COLLATE utf8_unicode_ci DEFAULT NULL,
  `frequency_n` int(11) DEFAULT NULL,
  `power_n` int(11) DEFAULT NULL,
  `rssi_n` int(11) DEFAULT NULL,
  `device_model` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `status` varchar(45) COLLATE utf8_unicode_ci DEFAULT NULL,
  `prev_status` varchar(45) COLLATE utf8_unicode_ci DEFAULT NULL,
  `appID` varchar(45) COLLATE utf8_unicode_ci DEFAULT NULL,
  `version` varchar(45) COLLATE utf8_unicode_ci DEFAULT NULL,
  `lang` varchar(10) COLLATE utf8_unicode_ci DEFAULT NULL,
  `uid2` varchar(64) CHARACTER SET utf8 DEFAULT NULL,
  `profile` varchar(45) COLLATE utf8_unicode_ci DEFAULT NULL,
  `insert_datetime` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`idmeasure`) USING BTREE,
  KEY `idx_sensors_device_id_date` (`device_id`,`date`),
  KEY `idx_sensors_date` (`date`),
  KEY `idx_sensors_latitude_longitude` (`latitude`,`longitude`)
) ENGINE=InnoDB AUTO_INCREMENT=27662570 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `trajectories`
--

DROP TABLE IF EXISTS `trajectories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `trajectories` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `trajectory_id` int(11) NOT NULL,
  `cluster_id` int(11) DEFAULT NULL,
  `trajectory` longtext,
  `profile` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_trajectories_profile` (`profile`)
) ENGINE=InnoDB AUTO_INCREMENT=29192 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2019-09-24 16:40:18
