CREATE DATABASE `profiledb` /*!40100 DEFAULT CHARACTER SET utf8mb4 */;

CREATE TABLE `profiledb`.`activity` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `time` datetime DEFAULT NULL,
  `app_id` varchar(255) DEFAULT NULL,
  `username` varchar(45) DEFAULT NULL,
  `delegated_app_id` varchar(255) DEFAULT NULL,
  `delegated_username` varchar(45) DEFAULT NULL,
  `app_name` varchar(255) DEFAULT NULL,
  `delegated_app_name` varchar(255) DEFAULT NULL,
  `source_request` varchar(255) DEFAULT NULL,
  `variable_name` varchar(255) DEFAULT NULL,
  `motivation` varchar(255) DEFAULT NULL,
  `access_type` varchar(255) DEFAULT NULL,
  `domain` varchar(255) DEFAULT NULL,
  `delete_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8676151 DEFAULT CHARSET=latin1;

CREATE TABLE `profiledb`.`activity_violation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `time` datetime DEFAULT NULL,
  `app_id` varchar(255) DEFAULT NULL,
  `username` varchar(45) DEFAULT NULL,
  `source_request` varchar(255) DEFAULT NULL,
  `variable_name` varchar(255) DEFAULT NULL,
  `motivation` varchar(255) DEFAULT NULL,
  `access_type` varchar(255) DEFAULT NULL,
  `query` varchar(4096) DEFAULT NULL,
  `error_message` varchar(255) DEFAULT NULL,
  `stacktrace` blob,
  `app_name` varchar(255) DEFAULT NULL,
  `ip_address` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=951533 DEFAULT CHARSET=latin1;

CREATE TABLE `profiledb`.`data` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(45) DEFAULT NULL,
  `data_time` datetime DEFAULT NULL,
  `insert_time` datetime DEFAULT NULL,
  `delete_time` datetime DEFAULT NULL,
  `elapse_time` datetime DEFAULT NULL,
  `app_id` varchar(255) DEFAULT NULL,
  `app_name` varchar(255) DEFAULT NULL,
  `motivation` varchar(255) DEFAULT NULL,
  `variable_name` varchar(255) DEFAULT NULL,
  `variable_value` varchar(255) DEFAULT NULL,
  `variable_unit` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=327446 DEFAULT CHARSET=latin1;

CREATE TABLE `profiledb`.`delegation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username_delegator` varchar(45) DEFAULT NULL,
  `username_delegated` varchar(45) DEFAULT NULL,
  `variable_name` varchar(255) DEFAULT NULL,
  `motivation` varchar(255) DEFAULT NULL,
  `element_id` varchar(255) DEFAULT NULL,
  `element_type` varchar(20) DEFAULT NULL,
  `insert_time` datetime DEFAULT NULL,
  `delete_time` datetime DEFAULT NULL,
  `delegation_details` text,
  `groupname_delegated` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=903 DEFAULT CHARSET=latin1;

CREATE TABLE `profiledb`.`kpidata` (
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
) ENGINE=InnoDB AUTO_INCREMENT=17055895 DEFAULT CHARSET=latin1;

CREATE TABLE `profiledb`.`kpimetadata` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `kpi_id` bigint(20) DEFAULT NULL,
  `value_key` varchar(128) DEFAULT NULL,
  `value` varchar(128) DEFAULT NULL,
  `delete_time` timestamp NULL DEFAULT NULL,
  `elapse_time` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=153 DEFAULT CHARSET=latin1;

CREATE TABLE `profiledb`.`kpivalues` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `kpi_id` bigint(20) DEFAULT NULL,
  `insert_time` timestamp NULL DEFAULT NULL,
  `value` varchar(128) DEFAULT NULL,
  `data_time` timestamp NULL DEFAULT NULL,
  `elapse_time` timestamp NULL DEFAULT NULL,
  `delete_time` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=42063 DEFAULT CHARSET=utf8;