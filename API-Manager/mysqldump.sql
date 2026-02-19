CREATE schema if not exists apimanager;

USE apimanager;

CREATE TABLE if not exists `apitable` (
  `idapi` int(11) NOT NULL AUTO_INCREMENT,
  `apiname` varchar(45) NOT NULL,
  `apikind` varchar(45) NOT NULL,
  `apiinternalurl` varchar(300) NOT NULL,
  `apiexternalurl` varchar(300) NOT NULL,
  `apiinfo` varchar(300) NOT NULL,
  `apicreationdate` datetime NOT NULL DEFAULT current_timestamp(),
  `apideletiondate` datetime DEFAULT NULL,
  `apistatus` varchar(45) NOT NULL DEFAULT 'inactive',
  `apiadditionalinfo` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT '{}',
  PRIMARY KEY (`idapi`,`apiname`),
  UNIQUE KEY `no_ext_urls_duplicate` (`apiexternalurl`),
  UNIQUE KEY `no_name_duplicate` (`apiname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE if not exists `apitabledeleted` (
  `idapi` int(11) NOT NULL,
  `apiname` varchar(45) NOT NULL,
  `apikind` varchar(45) NOT NULL,
  `apiinternalurl` varchar(300) NOT NULL,
  `apiexternalurl` varchar(300) NOT NULL,
  `apiinfo` varchar(300) NOT NULL,
  `apicreationdate` datetime NOT NULL DEFAULT current_timestamp(),
  `apideletiondate` datetime DEFAULT NULL,
  `apistatus` varchar(45) NOT NULL DEFAULT 'inactive',
  `apiadditionalinfo` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT '{}',
  PRIMARY KEY (`idapi`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE if not exists `ratelimit` (
  `user` varchar(45) NOT NULL,
  `resource` int(11) NOT NULL,
  `kind_of_limit` set('TotalAccesses','AccessesOverTime','ContemporaryAccess') NOT NULL,
  `additional_info` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `timebegin` timestamp NOT NULL DEFAULT current_timestamp(),
  `timeend` timestamp NULL DEFAULT NULL,
  `ip` varchar(45) NOT NULL DEFAULT "192.0.0.0/8",
  PRIMARY KEY (`user`,`resource`),
  KEY `d` (`resource`),
  CONSTRAINT `d` FOREIGN KEY (`resource`) REFERENCES `apitable` (`idapi`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE if not exists `timedaccess` (
  `idtimedaccess` int(11) NOT NULL AUTO_INCREMENT,
  `user` varchar(45) DEFAULT NULL,
  `beginaccess` datetime NOT NULL DEFAULT current_timestamp(),
  `resource` int(11) DEFAULT NULL,
  `result` longtext DEFAULT NULL,
  `extracted_id` varchar(45) DEFAULT NULL,
  `request_ok` tinyint(4) DEFAULT 0,
  PRIMARY KEY (`idtimedaccess`),
  KEY `s` (`resource`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE if not exists `requests` (
  `idnew_table` int(11) NOT NULL AUTO_INCREMENT,
  `result` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `extracted_id` varchar(45) DEFAULT NULL,
  `endaccess` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`idnew_table`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE if not exists `deletedratelimit` (
  `user` varchar(45) NOT NULL,
  `resource` int(11) NOT NULL,
  `kind_of_limit` set('TotalAccesses','AccessesOverTime','ContemporaryAccess') NOT NULL,
  `additional_info` longtext CHARACTER SET utf8mb4 NOT NULL,
  `timebegin` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `timeend` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `timedaccess_summary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `timedaccess_summary` (
  `user` varchar(45) NOT NULL,
  `access_day` date NOT NULL,
  `resource` int(11) NOT NULL,
  `total_requests` int(11) NOT NULL,
  `successful_requests` int(11) NOT NULL,
  PRIMARY KEY (`user`,`access_day`,`resource`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8mb4;

DELIMITER ;;
CREATE DEFINER=`root`@`%` PROCEDURE `compress_timedaccess`()
BEGIN
    -- Aggregate and insert/update summary table
    INSERT INTO timedaccess_summary (user, access_day, resource, total_requests, successful_requests)
    SELECT 
        user,
        DATE(beginaccess) AS access_day,
        resource,
        COUNT(*) AS total_requests,
        SUM(request_ok = 1) AS successful_requests
    FROM timedaccess
    WHERE beginaccess < DATE_SUB(CURDATE(), INTERVAL 7 DAY)
    GROUP BY user, DATE(beginaccess), resource
    ON DUPLICATE KEY UPDATE
        total_requests = total_requests + VALUES(total_requests),
        successful_requests = successful_requests + VALUES(successful_requests);

    -- Delete the old rows
    DELETE FROM timedaccess
    WHERE beginaccess < DATE_SUB(CURDATE(), INTERVAL 7 DAY);
END ;;
DELIMITER ;

CREATE EVENT `timedaccess_weekly_compress` ON SCHEDULE EVERY 1 HOUR STARTS '2000-01-01 00:00:00' ON COMPLETION NOT PRESERVE ENABLE DO CALL compress_timedaccess();

CREATE UNIQUE INDEX idx_extracted_id ON timedaccess(extracted_id);
CREATE UNIQUE INDEX idx_extracted_id ON requests(extracted_id);


CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`%` SQL SECURITY DEFINER VIEW `operative_apitable` AS select `apitable`.`idapi` AS `idapi`,`apitable`.`apiname` AS `apiname`,`apitable`.`apikind` AS `apikind`,`apitable`.`apiinternalurl` AS `apiinternalurl`,`apitable`.`apiexternalurl` AS `apiexternalurl`,`apitable`.`apiinfo` AS `apiinfo`,`apitable`.`apicreationdate` AS `apicreationdate`,`apitable`.`apideletiondate` AS `apideletiondate`,`apitable`.`apiadditionalinfo` AS `apiadditionalinfo`,case when `apitable`.`apistatus` = 'inactive' then 'inactive' when `apitable`.`apistatus` = 'active' then case when exists(select 1 from `ratelimit` where `ratelimit`.`resource` = `apitable`.`idapi` and `ratelimit`.`timebegin` < current_timestamp() and current_timestamp() < `ratelimit`.`timeend` limit 1) then 'active' else 'ready' end else 'unknown' end AS `apistatus` from `apitable`;

-- maybe truncate requests if it doesn't work (nothing of value is lost if you don't care about the past) 
ALTER TABLE `apimanager`.`requests` 
ADD CONSTRAINT `extracted_id`
  FOREIGN KEY (`extracted_id`)
  REFERENCES `apimanager`.`timedaccess` (`extracted_id`)
  ON DELETE CASCADE
  ON UPDATE NO ACTION;

GRANT ALL ON apimanager.* TO 'user'@'%';
