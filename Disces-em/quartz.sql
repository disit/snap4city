-- MySQL dump 10.13  Distrib 5.7.20, for Linux (x86_64)
--
-- Host: localhost    Database: quartz
-- ------------------------------------------------------
-- Server version	5.7.20

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
-- Table structure for table `QRTZ_BLOB_TRIGGERS`
--

DROP TABLE IF EXISTS `QRTZ_BLOB_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_BLOB_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `BLOB_DATA` blob,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  KEY `SCHED_NAME` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `QRTZ_BLOB_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_CALENDARS`
--

DROP TABLE IF EXISTS `QRTZ_CALENDARS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_CALENDARS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `CALENDAR_NAME` varchar(200) NOT NULL,
  `CALENDAR` blob NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`CALENDAR_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_CRON_TRIGGERS`
--

DROP TABLE IF EXISTS `QRTZ_CRON_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_CRON_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `CRON_EXPRESSION` varchar(120) NOT NULL,
  `TIME_ZONE_ID` varchar(80) DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `QRTZ_CRON_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_DOCKER`
--

DROP TABLE IF EXISTS `QRTZ_DOCKER`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_DOCKER` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `ID_DOCKER` varchar(255) DEFAULT NULL,
  `CPU_PERCENT` double DEFAULT NULL,
  `MEMORY_USAGE` bigint(20) DEFAULT NULL,
  `MEMORY_MAX_USAGE` bigint(20) DEFAULT NULL,
  `MEMORY_LIMIT` bigint(20) DEFAULT NULL,
  `IO_SERVICE_BYTES_RECURSIVE_READ` bigint(20) DEFAULT NULL,
  `IO_SERVICE_BYTES_RECURSIVE_WRITE` bigint(20) DEFAULT NULL,
  `IO_SERVICE_BYTES_RECURSIVE_SYNC` bigint(20) DEFAULT NULL,
  `IO_SERVICE_BYTES_RECURSIVE_ASYNC` bigint(20) DEFAULT NULL,
  `IO_SERVICE_BYTES_RECURSIVE_TOTAL` bigint(20) DEFAULT NULL,
  `DATE` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=53520 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_DOCKER_LIST`
--

DROP TABLE IF EXISTS `QRTZ_DOCKER_LIST`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_DOCKER_LIST` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `dockerid` varchar(255) DEFAULT NULL,
  `vmname` varchar(255) DEFAULT NULL,
  `date` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_QRTZ_DOCKER_LIST_dockername` (`dockerid`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_EXE_LOGS`
--

DROP TABLE IF EXISTS `QRTZ_EXE_LOGS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_EXE_LOGS` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `JOB_NAME` varchar(255) DEFAULT NULL,
  `JOB_GROUP` varchar(255) DEFAULT NULL,
  `fire_instance_id` varchar(255) DEFAULT NULL,
  `log` longtext,
  `timestamp` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_QRTZ_EXE_LOGS_fire_instance_id` (`fire_instance_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_FAKE_SIMPLE_TRIGGERS`
--

DROP TABLE IF EXISTS `QRTZ_FAKE_SIMPLE_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_FAKE_SIMPLE_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `REPEAT_COUNT` bigint(7) NOT NULL,
  `REPEAT_INTERVAL` bigint(12) NOT NULL,
  `TIMES_TRIGGERED` bigint(10) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_FAKE_TRIGGERS`
--

DROP TABLE IF EXISTS `QRTZ_FAKE_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_FAKE_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `JOB_NAME` varchar(200) NOT NULL,
  `JOB_GROUP` varchar(200) NOT NULL,
  `DESCRIPTION` varchar(250) DEFAULT NULL,
  `NEXT_FIRE_TIME` bigint(13) DEFAULT NULL,
  `PREV_FIRE_TIME` bigint(13) DEFAULT NULL,
  `PRIORITY` int(11) DEFAULT NULL,
  `TRIGGER_STATE` varchar(16) NOT NULL,
  `TRIGGER_TYPE` varchar(8) NOT NULL,
  `START_TIME` bigint(13) NOT NULL,
  `END_TIME` bigint(13) DEFAULT NULL,
  `CALENDAR_NAME` varchar(200) DEFAULT NULL,
  `MISFIRE_INSTR` smallint(2) DEFAULT NULL,
  `JOB_DATA` blob
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_FIRED_TRIGGERS`
--

DROP TABLE IF EXISTS `QRTZ_FIRED_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_FIRED_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `ENTRY_ID` varchar(95) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `INSTANCE_NAME` varchar(200) NOT NULL,
  `FIRED_TIME` bigint(13) NOT NULL,
  `SCHED_TIME` bigint(13) NOT NULL,
  `PRIORITY` int(11) NOT NULL,
  `STATE` varchar(16) NOT NULL,
  `JOB_NAME` varchar(200) DEFAULT NULL,
  `JOB_GROUP` varchar(200) DEFAULT NULL,
  `IS_NONCONCURRENT` varchar(1) DEFAULT NULL,
  `REQUESTS_RECOVERY` varchar(1) DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`,`ENTRY_ID`),
  KEY `IDX_QRTZ_FT_TRIG_INST_NAME` (`SCHED_NAME`,`INSTANCE_NAME`),
  KEY `IDX_QRTZ_FT_INST_JOB_REQ_RCVRY` (`SCHED_NAME`,`INSTANCE_NAME`,`REQUESTS_RECOVERY`),
  KEY `IDX_QRTZ_FT_J_G` (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_FT_JG` (`SCHED_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_FT_T_G` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  KEY `IDX_QRTZ_FT_TG` (`SCHED_NAME`,`TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_HOST_LIST`
--

DROP TABLE IF EXISTS `QRTZ_HOST_LIST`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_HOST_LIST` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `hostname` varchar(255) DEFAULT NULL,
  `date` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_JOB_DETAILS`
--

DROP TABLE IF EXISTS `QRTZ_JOB_DETAILS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_JOB_DETAILS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `JOB_NAME` varchar(200) NOT NULL,
  `JOB_GROUP` varchar(200) NOT NULL,
  `DESCRIPTION` varchar(250) DEFAULT NULL,
  `JOB_CLASS_NAME` varchar(250) NOT NULL,
  `IS_DURABLE` varchar(1) NOT NULL,
  `IS_NONCONCURRENT` varchar(1) NOT NULL,
  `IS_UPDATE_DATA` varchar(1) NOT NULL,
  `REQUESTS_RECOVERY` varchar(1) NOT NULL,
  `JOB_DATA` blob,
  PRIMARY KEY (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_J_REQ_RECOVERY` (`SCHED_NAME`,`REQUESTS_RECOVERY`),
  KEY `IDX_QRTZ_J_GRP` (`SCHED_NAME`,`JOB_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_LOCKS`
--

DROP TABLE IF EXISTS `QRTZ_LOCKS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_LOCKS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `LOCK_NAME` varchar(40) NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`LOCK_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_LOGS`
--

DROP TABLE IF EXISTS `QRTZ_LOGS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_LOGS` (
  `ID` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `JOB_NAME` varchar(255) DEFAULT NULL,
  `JOB_GROUP` varchar(255) DEFAULT NULL,
  `DATE` datetime DEFAULT NULL,
  `TRIGGER_NAME` varchar(255) DEFAULT NULL,
  `TRIGGER_GROUP` varchar(255) DEFAULT NULL,
  `PREV_FIRE_TIME` datetime DEFAULT NULL,
  `NEXT_FIRE_TIME` datetime DEFAULT NULL,
  `REFIRE_COUNT` bigint(7) unsigned DEFAULT NULL,
  `RESULT` longtext,
  `SCHEDULER_INSTANCE_ID` varchar(255) DEFAULT NULL,
  `SCHEDULER_NAME` varchar(255) DEFAULT NULL,
  `IP_ADDRESS` varchar(255) DEFAULT NULL,
  `STATUS` varchar(255) DEFAULT NULL,
  `LOGGER` varchar(255) DEFAULT NULL,
  `LEVEL` varchar(45) DEFAULT NULL,
  `MESSAGE` longtext,
  `FIRE_INSTANCE_ID` varchar(255) DEFAULT NULL,
  `JOB_DATA` blob,
  `PROGRESS` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `date` (`DATE`),
  KEY `status` (`STATUS`),
  KEY `job_status_date` (`JOB_NAME`,`JOB_GROUP`,`STATUS`,`DATE`),
  KEY `idx_QRTZ_LOGS_STATUS_DATE` (`STATUS`,`DATE`)
) ENGINE=InnoDB AUTO_INCREMENT=1468 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_NODES`
--

DROP TABLE IF EXISTS `QRTZ_NODES`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_NODES` (
  `ID` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `JOBS_EXECUTED` varchar(255) DEFAULT NULL,
  `SCHEDULER_NAME` varchar(255) DEFAULT NULL,
  `RUNNING_SINCE` datetime DEFAULT NULL,
  `CLUSTERED` varchar(45) DEFAULT NULL,
  `SCHEDULER_INSTANCE_ID` varchar(45) DEFAULT NULL,
  `PERSISTENCE` varchar(45) DEFAULT NULL,
  `REMOTE_SCHEDULER` varchar(45) DEFAULT NULL,
  `CURRENTLY_EXECUTING_JOBS` varchar(255) DEFAULT NULL,
  `CPU_LOAD_JVM` varchar(255) DEFAULT NULL,
  `SYSTEM_LOAD_AVERAGE` varchar(255) DEFAULT NULL,
  `OPERATING_SYSTEM_VERSION` varchar(255) DEFAULT NULL,
  `COMMITTED_VIRTUAL_MEMORY` varchar(255) DEFAULT NULL,
  `OPERATING_SYSTEM_NAME` varchar(255) DEFAULT NULL,
  `FREE_SWAP_SPACE` varchar(255) DEFAULT NULL,
  `PROCESS_CPU_TIME` varchar(255) DEFAULT NULL,
  `TOTAL_PHYSICAL_MEMORY` varchar(255) DEFAULT NULL,
  `NUMBER_OF_PROCESSORS` varchar(45) DEFAULT NULL,
  `FREE_PHYSICAL_MEMORY` varchar(255) DEFAULT NULL,
  `CPU_LOAD` varchar(255) DEFAULT NULL,
  `OPERATING_SYSTEM_ARCHITECTURE` varchar(255) DEFAULT NULL,
  `TOTAL_SWAP_SPACE` varchar(255) DEFAULT NULL,
  `DATE` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `IS_SCHEDULER_STANDBY` int(1) unsigned DEFAULT NULL,
  `IS_SCHEDULER_SHUTDOWN` int(1) unsigned DEFAULT NULL,
  `IS_SCHEDULER_STARTED` int(1) unsigned DEFAULT NULL,
  `IP_ADDRESS` varchar(255) NOT NULL DEFAULT '',
  `TOTAL_DISK_SPACE` varchar(255) DEFAULT NULL,
  `UNALLOCATED_DISK_SPACE` varchar(255) DEFAULT NULL,
  `USABLE_DISK_SPACE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`) USING BTREE,
  KEY `IP_ADDRESS_DATE` (`IP_ADDRESS`,`DATE`),
  KEY `DATE` (`DATE`)
) ENGINE=InnoDB AUTO_INCREMENT=590 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_PAUSED_TRIGGER_GRPS`
--

DROP TABLE IF EXISTS `QRTZ_PAUSED_TRIGGER_GRPS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_PAUSED_TRIGGER_GRPS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_PRESCHEDULING_TRIGGERS`
--

DROP TABLE IF EXISTS `QRTZ_PRESCHEDULING_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_PRESCHEDULING_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `JOB_NAME` varchar(200) NOT NULL,
  `JOB_GROUP` varchar(200) NOT NULL,
  `DESCRIPTION` varchar(250) DEFAULT NULL,
  `NEXT_FIRE_TIME` bigint(13) DEFAULT NULL,
  `PREV_FIRE_TIME` bigint(13) DEFAULT NULL,
  `PRIORITY` int(11) DEFAULT NULL,
  `TRIGGER_STATE` varchar(16) NOT NULL,
  `TRIGGER_TYPE` varchar(8) NOT NULL,
  `START_TIME` bigint(13) NOT NULL,
  `END_TIME` bigint(13) DEFAULT NULL,
  `CALENDAR_NAME` varchar(200) DEFAULT NULL,
  `MISFIRE_INSTR` smallint(2) DEFAULT NULL,
  `JOB_DATA` blob
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_REJECTED_TRIGGERS`
--

DROP TABLE IF EXISTS `QRTZ_REJECTED_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_REJECTED_TRIGGERS` (
  `TEMP_FIRE_INSTANCE_ID` varchar(95) NOT NULL DEFAULT '',
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `JOB_NAME` varchar(200) DEFAULT NULL,
  `JOB_GROUP` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`TEMP_FIRE_INSTANCE_ID`) USING BTREE,
  KEY `trigger_name_group` (`TRIGGER_NAME`,`TRIGGER_GROUP`,`TEMP_FIRE_INSTANCE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_SCHEDULER_STATE`
--

DROP TABLE IF EXISTS `QRTZ_SCHEDULER_STATE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_SCHEDULER_STATE` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `INSTANCE_NAME` varchar(200) NOT NULL,
  `LAST_CHECKIN_TIME` bigint(13) NOT NULL,
  `CHECKIN_INTERVAL` bigint(13) NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`INSTANCE_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_SIMPLE_TRIGGERS`
--

DROP TABLE IF EXISTS `QRTZ_SIMPLE_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_SIMPLE_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `REPEAT_COUNT` bigint(7) NOT NULL,
  `REPEAT_INTERVAL` bigint(12) NOT NULL,
  `TIMES_TRIGGERED` bigint(10) NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `QRTZ_SIMPLE_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_SIMPROP_TRIGGERS`
--

DROP TABLE IF EXISTS `QRTZ_SIMPROP_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_SIMPROP_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `STR_PROP_1` varchar(512) DEFAULT NULL,
  `STR_PROP_2` varchar(512) DEFAULT NULL,
  `STR_PROP_3` varchar(512) DEFAULT NULL,
  `INT_PROP_1` int(11) DEFAULT NULL,
  `INT_PROP_2` int(11) DEFAULT NULL,
  `LONG_PROP_1` bigint(20) DEFAULT NULL,
  `LONG_PROP_2` bigint(20) DEFAULT NULL,
  `DEC_PROP_1` decimal(13,4) DEFAULT NULL,
  `DEC_PROP_2` decimal(13,4) DEFAULT NULL,
  `BOOL_PROP_1` varchar(1) DEFAULT NULL,
  `BOOL_PROP_2` varchar(1) DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `QRTZ_SIMPROP_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_SPARQL`
--

DROP TABLE IF EXISTS `QRTZ_SPARQL`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_SPARQL` (
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `sla` varchar(255) NOT NULL DEFAULT '',
  `alarm` int(1) unsigned NOT NULL,
  `metric` varchar(255) NOT NULL,
  `metric_name` varchar(255) NOT NULL,
  `metric_unit` varchar(255) NOT NULL,
  `metric_timestamp` timestamp NOT NULL,
  `virtual_machine` varchar(255) NOT NULL,
  `virtual_machine_name` varchar(255) NOT NULL,
  `host_machine` varchar(255) NOT NULL,
  `value` varchar(255) NOT NULL,
  `relation` varchar(255) NOT NULL,
  `threshold` varchar(255) NOT NULL,
  `call_url` varchar(1000) NOT NULL,
  `business_configuration` varchar(255) NOT NULL,
  PRIMARY KEY (`metric_name`,`metric_timestamp`,`virtual_machine_name`,`call_url`(500)) USING BTREE,
  KEY `sla_metric_timestamp` (`sla`,`metric_timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_STATUS`
--

DROP TABLE IF EXISTS `QRTZ_STATUS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_STATUS` (
  `ID` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `FIRE_INSTANCE_ID` varchar(255) NOT NULL DEFAULT '',
  `JOB_NAME` varchar(255) NOT NULL,
  `JOB_GROUP` varchar(255) NOT NULL,
  `DATE` datetime NOT NULL,
  `TRIGGER_NAME` varchar(255) DEFAULT NULL,
  `TRIGGER_GROUP` varchar(255) DEFAULT NULL,
  `PREV_FIRE_TIME` datetime DEFAULT NULL,
  `NEXT_FIRE_TIME` datetime DEFAULT NULL,
  `REFIRE_COUNT` bigint(7) unsigned DEFAULT NULL,
  `RESULT` longtext,
  `SCHEDULER_INSTANCE_ID` varchar(255) DEFAULT NULL,
  `SCHEDULER_NAME` varchar(255) DEFAULT NULL,
  `IP_ADDRESS` varchar(255) DEFAULT NULL,
  `STATUS` varchar(255) DEFAULT NULL,
  `LOGGER` varchar(255) DEFAULT NULL,
  `LEVEL` varchar(45) DEFAULT NULL,
  `MESSAGE` longtext,
  `JOB_DATA` blob,
  `PROGRESS` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `job` (`JOB_NAME`,`JOB_GROUP`),
  KEY `FIRE_INSTANCE_ID` (`FIRE_INSTANCE_ID`),
  KEY `STATUS` (`STATUS`,`IP_ADDRESS`,`PREV_FIRE_TIME`)
) ENGINE=InnoDB AUTO_INCREMENT=394 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_TRIGGERS`
--

DROP TABLE IF EXISTS `QRTZ_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `JOB_NAME` varchar(200) NOT NULL,
  `JOB_GROUP` varchar(200) NOT NULL,
  `DESCRIPTION` varchar(250) DEFAULT NULL,
  `NEXT_FIRE_TIME` bigint(13) DEFAULT NULL,
  `PREV_FIRE_TIME` bigint(13) DEFAULT NULL,
  `PRIORITY` int(11) DEFAULT NULL,
  `TRIGGER_STATE` varchar(16) NOT NULL,
  `TRIGGER_TYPE` varchar(8) NOT NULL,
  `START_TIME` bigint(13) NOT NULL,
  `END_TIME` bigint(13) DEFAULT NULL,
  `CALENDAR_NAME` varchar(200) DEFAULT NULL,
  `MISFIRE_INSTR` smallint(2) DEFAULT NULL,
  `JOB_DATA` blob,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  KEY `IDX_QRTZ_T_J` (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_T_JG` (`SCHED_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_T_C` (`SCHED_NAME`,`CALENDAR_NAME`),
  KEY `IDX_QRTZ_T_G` (`SCHED_NAME`,`TRIGGER_GROUP`),
  KEY `IDX_QRTZ_T_STATE` (`SCHED_NAME`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_N_STATE` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_N_G_STATE` (`SCHED_NAME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_NEXT_FIRE_TIME` (`SCHED_NAME`,`NEXT_FIRE_TIME`),
  KEY `IDX_QRTZ_T_NFT_ST` (`SCHED_NAME`,`TRIGGER_STATE`,`NEXT_FIRE_TIME`),
  KEY `IDX_QRTZ_T_NFT_MISFIRE` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`),
  KEY `IDX_QRTZ_T_NFT_ST_MISFIRE` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_NFT_ST_MISFIRE_GRP` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
  CONSTRAINT `QRTZ_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) REFERENCES `QRTZ_JOB_DETAILS` (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_VMWARE_HOST`
--

DROP TABLE IF EXISTS `QRTZ_VMWARE_HOST`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_VMWARE_HOST` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `HOSTNAME` varchar(255) DEFAULT NULL,
  `TOTAL_HZ` bigint(20) DEFAULT NULL,
  `CPU_CORES` int(11) DEFAULT NULL,
  `CPU_USAGE` int(11) DEFAULT NULL,
  `TOTAL_CPU_CAPACITY` bigint(20) DEFAULT NULL,
  `CPU_USAGE_PERCENT` int(11) DEFAULT NULL,
  `MEMORY_USAGE` bigint(20) DEFAULT NULL,
  `MEMORY_SIZE` bigint(20) DEFAULT NULL,
  `DATE` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=94853 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_VMWARE_VM`
--

DROP TABLE IF EXISTS `QRTZ_VMWARE_VM`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_VMWARE_VM` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `VM_NAME` varchar(255) DEFAULT NULL,
  `STATE` varchar(255) DEFAULT NULL,
  `IP_ADDRESS` varchar(255) DEFAULT NULL,
  `USED_MEMORY` varchar(255) DEFAULT NULL,
  `TOTAL_MEMORY` varchar(255) DEFAULT NULL,
  `CPU_USAGE` int(11) DEFAULT NULL,
  `USED_DISK` bigint(20) DEFAULT NULL,
  `FREE_DISK` bigint(20) DEFAULT NULL,
  `HOSTNAME` varchar(255) DEFAULT NULL,
  `NUM_CPU` int(11) DEFAULT NULL,
  `DATE` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=261881 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_VM_LIST`
--

DROP TABLE IF EXISTS `QRTZ_VM_LIST`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_VM_LIST` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `vmname` varchar(255) DEFAULT NULL,
  `hostname` varchar(255) DEFAULT NULL,
  `date` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_QRTZ_VM_LIST_vmname` (`vmname`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `docker_list`
--

DROP TABLE IF EXISTS `docker_list`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `docker_list` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `container` varchar(255) DEFAULT NULL,
  `ip` varchar(255) DEFAULT NULL,
  `vm_uuid` varchar(255) DEFAULT NULL,
  `vm_ip` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `docker_stats`
--

DROP TABLE IF EXISTS `docker_stats`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `docker_stats` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `container` varchar(255) DEFAULT NULL,
  `vm_uuid` varchar(255) DEFAULT NULL,
  `vm_ip` varchar(255) DEFAULT NULL,
  `ip` varchar(45) DEFAULT NULL,
  `cpu` double DEFAULT NULL,
  `mem_usage` double DEFAULT NULL,
  `mem_limit` double DEFAULT NULL,
  `mem` double DEFAULT NULL,
  `net_i` double DEFAULT NULL,
  `net_o` double DEFAULT NULL,
  `date` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_docker_stats_container_ip_vm_uuid_vm_ip` (`container`,`ip`,`vm_uuid`,`vm_ip`)
) ENGINE=InnoDB AUTO_INCREMENT=63 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `host_list`
--

DROP TABLE IF EXISTS `host_list`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `host_list` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `ip` varchar(255) DEFAULT NULL,
  `vCenter_uuid` varchar(255) DEFAULT NULL,
  `monitor` int(1) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `host_stats`
--

DROP TABLE IF EXISTS `host_stats`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `host_stats` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `ip` varchar(255) DEFAULT NULL,
  `vCenter_uuid` varchar(255) DEFAULT NULL,
  `cpu_used` int(11) DEFAULT NULL,
  `cpu_total` int(11) DEFAULT NULL,
  `mem_used` int(11) DEFAULT NULL,
  `mem_total` int(11) DEFAULT NULL,
  `date` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `marathon_actions`
--

DROP TABLE IF EXISTS `marathon_actions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `marathon_actions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `vm` varchar(255) DEFAULT NULL,
  `action` varchar(255) DEFAULT NULL,
  `apps_list` longtext,
  `n_apps` int(11) DEFAULT NULL,
  `date` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=487003 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `marathon_apps`
--

DROP TABLE IF EXISTS `marathon_apps`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `marathon_apps` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `app` varchar(255) DEFAULT NULL,
  `json` longtext,
  `date` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=31759 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `marathon_apps_bk`
--

DROP TABLE IF EXISTS `marathon_apps_bk`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `marathon_apps_bk` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `app` varchar(255) DEFAULT NULL,
  `json` longtext,
  `date` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=31051 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `marathon_total_metrics`
--

DROP TABLE IF EXISTS `marathon_total_metrics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `marathon_total_metrics` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `interval` double DEFAULT NULL,
  `cpuLimit` double DEFAULT NULL,
  `container_mem` double DEFAULT NULL,
  `container_cpu` double DEFAULT NULL,
  `vm_memory_mesos` double DEFAULT NULL,
  `seconds` double DEFAULT NULL,
  `cpu_ratio` double DEFAULT NULL,
  `mem_ratio` double DEFAULT NULL,
  `total_cpu_average` double DEFAULT NULL,
  `total_memory_average` double DEFAULT NULL,
  `ram` double DEFAULT NULL,
  `delta_cpu_average` double DEFAULT NULL,
  `active_vms` int(11) DEFAULT NULL,
  `unhealthy_apps` int(11) DEFAULT NULL,
  `missing_apps` int(11) DEFAULT NULL,
  `mysql_apps` int(11) DEFAULT NULL,
  `marathon_apps` int(11) DEFAULT NULL,
  `over_cpu` double DEFAULT NULL,
  `over_mem` double DEFAULT NULL,
  `date` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22714827 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `marathon_vm_metrics`
--

DROP TABLE IF EXISTS `marathon_vm_metrics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `marathon_vm_metrics` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `vm` varchar(255) DEFAULT NULL,
  `ip` varchar(255) DEFAULT NULL,
  `uuid` varchar(255) DEFAULT NULL,
  `nic` int(1) DEFAULT NULL,
  `cpu` double DEFAULT NULL,
  `mem` double DEFAULT NULL,
  `delta_cpu` double DEFAULT NULL,
  `healthy_apps` int(11) DEFAULT NULL,
  `unhealthy_apps` int(11) DEFAULT NULL,
  `total_apps` int(11) DEFAULT NULL,
  `date` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=76915505 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `triggers`
--

DROP TABLE IF EXISTS `triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `triggers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(45) DEFAULT NULL,
  `execution_time` int(11) DEFAULT NULL,
  `deadline` datetime DEFAULT NULL,
  `cpu` int(11) DEFAULT NULL,
  `ram` int(11) DEFAULT NULL,
  `disk` int(11) DEFAULT NULL,
  `type` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=101 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `vm_list`
--

DROP TABLE IF EXISTS `vm_list`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `vm_list` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `uuid` varchar(255) DEFAULT NULL,
  `ip` varchar(255) DEFAULT NULL,
  `host` varchar(45) DEFAULT NULL,
  `group` varchar(45) DEFAULT NULL,
  `monitor` int(1) DEFAULT '0',
  `docker` int(1) DEFAULT '0',
  `date` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_vm_list_uuid` (`uuid`)
) ENGINE=InnoDB AUTO_INCREMENT=306338 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `vm_stats`
--

DROP TABLE IF EXISTS `vm_stats`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `vm_stats` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `uuid` varchar(255) DEFAULT NULL,
  `nic` varchar(255) DEFAULT NULL,
  `vCenter_uuid` varchar(255) DEFAULT NULL,
  `cpu_limit` int(11) DEFAULT NULL,
  `mem_limit` int(11) DEFAULT NULL,
  `cpu_reservation` int(11) DEFAULT NULL,
  `mem_reservation` int(11) DEFAULT NULL,
  `vcpus` int(11) DEFAULT NULL,
  `cpu_ready_avg` double DEFAULT NULL,
  `cpu_ready_max` double DEFAULT NULL,
  `cpu_usage` double DEFAULT NULL,
  `memory` int(11) DEFAULT NULL,
  `memory_shared_percentage` double DEFAULT NULL,
  `memory_shared` double DEFAULT NULL,
  `memory_balloon_percentage` double DEFAULT NULL,
  `memory_balloon` double DEFAULT NULL,
  `memory_swapped_percentage` double DEFAULT NULL,
  `memory_swapped` double DEFAULT NULL,
  `memory_active_percentage` double DEFAULT NULL,
  `memory_active` double DEFAULT NULL,
  `datastore_io_read` double DEFAULT NULL,
  `datastore_io_write` double DEFAULT NULL,
  `datastore_lat_read` double DEFAULT NULL,
  `datastore_lat_write` double DEFAULT NULL,
  `networkTx` double DEFAULT NULL,
  `networkRx` double DEFAULT NULL,
  `date` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `vms_dict`
--

DROP TABLE IF EXISTS `vms_dict`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `vms_dict` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `vm_id` varchar(100) DEFAULT NULL,
  `vm_name` varchar(255) DEFAULT NULL,
  `uuid` varchar(255) DEFAULT NULL,
  `ip` varchar(255) DEFAULT NULL,
  `offline` int(11) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_vm_list_uuid` (`uuid`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2019-10-10 16:12:45
