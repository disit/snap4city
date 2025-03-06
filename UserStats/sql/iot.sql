
CREATE DATABASE  IF NOT EXISTS `iot` /*!40100 DEFAULT CHARACTER SET utf8mb4 */;
USE `iot`;

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
-- Table structure for table `dashboards`
--

DROP TABLE IF EXISTS `dashboards`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dashboards` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `elementId` int(11) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  `nAccesses` int(11) DEFAULT NULL,
  `nMinutes` int(11) DEFAULT NULL,
  `hasBrokerWidget` int(1) DEFAULT NULL,
  `hasNodeREDWidget` int(1) DEFAULT NULL,
  `date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_dashboards_elementId_username_date` (`elementId`,`username`,`date`)
) ENGINE=InnoDB AUTO_INCREMENT=121170 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `data`
--

DROP TABLE IF EXISTS `data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) DEFAULT NULL,
  `iot_db_storage_tx` double DEFAULT NULL,
  `iot_db_storage_rx` double DEFAULT NULL,
  `iot_filesystem_storage_tx` double DEFAULT NULL,
  `iot_filesystem_storage_rx` double DEFAULT NULL,
  `iot_db_request_tx` double DEFAULT NULL,
  `iot_db_request_rx` double DEFAULT NULL,
  `iot_ascapi_tx` double DEFAULT NULL,
  `iot_ascapi_rx` double DEFAULT NULL,
  `iot_disces_tx` double DEFAULT NULL,
  `iot_disces_rx` double DEFAULT NULL,
  `iot_dashboard_tx` double DEFAULT NULL,
  `iot_dashboard_rx` double DEFAULT NULL,
  `iot_datagate_tx` double DEFAULT NULL,
  `iot_datagate_rx` double DEFAULT NULL,
  `iot_external_service_tx` double DEFAULT NULL,
  `iot_external_service_rx` double DEFAULT NULL,
  `iot_iot_service_tx` double DEFAULT NULL,
  `iot_iot_service_rx` double DEFAULT NULL,
  `iot_mapping_tx` double DEFAULT NULL,
  `iot_mapping_rx` double DEFAULT NULL,
  `iot_microserviceusercreated_tx` double DEFAULT NULL,
  `iot_microserviceusercreated_rx` double DEFAULT NULL,
  `iot_mydata_tx` double DEFAULT NULL,
  `iot_mydata_rx` double DEFAULT NULL,
  `iot_notificator_tx` double DEFAULT NULL,
  `iot_notificator_rx` double DEFAULT NULL,
  `iot_rstatistics_tx` double DEFAULT NULL,
  `iot_rstatistics_rx` double DEFAULT NULL,
  `iot_sigfox_tx` double DEFAULT NULL,
  `iot_sigfox_rx` double DEFAULT NULL,
  `iot_undefined_tx` double DEFAULT NULL,
  `iot_undefined_rx` double DEFAULT NULL,
  `iot_tx` double DEFAULT NULL,
  `iot_rx` double DEFAULT NULL,
  `iot_apps` int(11) DEFAULT NULL,
  `devices_public` int(11) DEFAULT NULL,
  `devices_private` int(11) DEFAULT NULL,
  `dashboards_public` int(11) DEFAULT NULL,
  `dashboards_private` int(11) DEFAULT NULL,
  `dashboards_accesses` int(11) DEFAULT NULL,
  `dashboards_minutes` int(11) DEFAULT NULL,
  `iot_reads` int(11) DEFAULT NULL,
  `iot_writes` int(11) DEFAULT NULL,
  `etl_writes` int(11) DEFAULT NULL,
  `date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_syslog_user_id_date` (`username`,`date`)
) ENGINE=InnoDB AUTO_INCREMENT=528025 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `data_general`
--

DROP TABLE IF EXISTS `data_general`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data_general` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `totalUsers` bigint(20) DEFAULT NULL,
  `totalUsersWithMetrics` bigint(20) DEFAULT NULL,
  `totalDashboardsWithMetrics` bigint(20) DEFAULT NULL,
  `totalIoTAppsWithMetrics` bigint(20) DEFAULT NULL,
  `totalIoTTxWithMetrics` double DEFAULT NULL,
  `totalIoTRxWithMetrics` double DEFAULT NULL,
  `totalDashboardsAccessesWithMetrics` bigint(20) DEFAULT NULL,
  `totalDashboardsMinutesWithMetrics` bigint(20) DEFAULT NULL,
  `date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `data_general_UN` (`date`)
) ENGINE=InnoDB AUTO_INCREMENT=4490 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `etl`
--

DROP TABLE IF EXISTS `etl`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `etl` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `num` varchar(45) DEFAULT NULL,
  `tx` double DEFAULT NULL,
  `rx` double DEFAULT NULL,
  `date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_etl_date` (`date`)
) ENGINE=InnoDB AUTO_INCREMENT=5776 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `heatmap`
--

DROP TABLE IF EXISTS `heatmap`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `heatmap` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `cc_x` int(11) DEFAULT NULL,
  `cc_y` int(11) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `num` varchar(45) DEFAULT NULL,
  `dataset` varchar(45) DEFAULT NULL,
  `date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_heatmap_cc_x_cc_y_date` (`cc_x`,`cc_y`,`date`),
  KEY `idx_heatmap_date_dataset_latitude_longitude` (`date`,`dataset`,`latitude`,`longitude`)
) ENGINE=InnoDB AUTO_INCREMENT=2225404 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `keycloak_logins`
--

DROP TABLE IF EXISTS `keycloak_logins`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `keycloak_logins` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) DEFAULT NULL,
  `userId` varchar(255) DEFAULT NULL,
  `num_login` varchar(255) DEFAULT NULL,
  `date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_keycloak_events_username_date` (`username`,`date`)
) ENGINE=InnoDB AUTO_INCREMENT=4083 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `links`
--

DROP TABLE IF EXISTS `links`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `links` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `level` int(11) DEFAULT NULL,
  `link` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `order` int(11) DEFAULT NULL,
  `action` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=73 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `metrics_levels`
--

DROP TABLE IF EXISTS `metrics_levels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metrics_levels` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `order` int(11) DEFAULT NULL,
  `role` varchar(45) DEFAULT NULL,
  `group_membership` longtext,
  `level` int(11) DEFAULT NULL,
  `dashboards` int(11) DEFAULT NULL,
  `dashboards_public` int(11) DEFAULT NULL,
  `dashboards_private` int(11) DEFAULT NULL,
  `dashboards_accesses` int(11) DEFAULT NULL,
  `dashboards_minutes` int(11) DEFAULT NULL,
  `iot_devices` int(11) DEFAULT NULL,
  `iot_devices_public` int(11) DEFAULT NULL,
  `iot_devices_private` int(11) DEFAULT NULL,
  `iot_tx` double DEFAULT NULL,
  `iot_rx` double DEFAULT NULL,
  `iot_applications` int(11) DEFAULT NULL,
  `ETL` int(11) DEFAULT NULL,
  `AMMA` int(11) DEFAULT NULL,
  `DevDash` int(11) DEFAULT NULL,
  `ResDash` int(11) DEFAULT NULL,
  `IOTBlocks` int(11) DEFAULT NULL,
  `Microservice` int(11) DEFAULT NULL,
  `IOTApp` int(11) DEFAULT NULL,
  `R` int(11) DEFAULT NULL,
  `BrokerWidgets` int(11) DEFAULT NULL,
  `NodeREDWidgets` int(11) DEFAULT NULL,
  `node` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `nodered`
--

DROP TABLE IF EXISTS `nodered`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nodered` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `num` int(11) DEFAULT NULL,
  `tx` double DEFAULT NULL,
  `rx` double DEFAULT NULL,
  `date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_node-red_date` (`date`)
) ENGINE=InnoDB AUTO_INCREMENT=5770 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `r`
--

DROP TABLE IF EXISTS `r`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `r` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `num` varchar(45) DEFAULT NULL,
  `date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_r_date` (`date`)
) ENGINE=InnoDB AUTO_INCREMENT=5779 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `roles_levels`
--

DROP TABLE IF EXISTS `roles_levels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles_levels` (
  `username` varchar(255) NOT NULL,
  `role` varchar(255) DEFAULT NULL,
  `organizations` longtext,
  `level` int(11) DEFAULT NULL,
  `date` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `roles_levels_old`
--

DROP TABLE IF EXISTS `roles_levels_old`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles_levels_old` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) DEFAULT NULL,
  `role` varchar(255) DEFAULT NULL,
  `organizations` longtext,
  `level` int(11) DEFAULT NULL,
  `date` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_roles_levels_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=8155691 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `rules`
--

DROP TABLE IF EXISTS `rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rules` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `iot_apps` int(11) DEFAULT NULL,
  `iot_devices` int(11) DEFAULT NULL,
  `dashboards` int(11) DEFAULT NULL,
  `dashboards_broker` int(11) DEFAULT NULL,
  `dashboards_iot_app` int(11) DEFAULT NULL,
  `dashboards_minutes` int(11) DEFAULT NULL,
  `dashboards_traffic` double DEFAULT NULL,
  `datagate_datasets` int(11) DEFAULT NULL,
  `etl` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nodered` int(11) DEFAULT NULL,
  `dashboards` int(11) DEFAULT NULL,
  `devices` int(11) DEFAULT NULL,
  `total` int(11) DEFAULT NULL,
  `date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_users_date` (`date`)
) ENGINE=InnoDB AUTO_INCREMENT=5570 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

