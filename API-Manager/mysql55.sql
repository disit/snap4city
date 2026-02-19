CREATE  schema if not exists apimanager;

USE apimanager;


CREATE TABLE if not exists `apitable` (
  `idapi` int(11) NOT NULL AUTO_INCREMENT,
  `apiname` varchar(45) NOT NULL,
  `apikind` varchar(45) NOT NULL,
  `apiinternalurl` varchar(300) NOT NULL,
  `apiexternalurl` varchar(300) NOT NULL,
  `apiinfo` varchar(300) NOT NULL,
  `apicreationdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `apideletiondate` timestamp,
  `apistatus` varchar(45) NOT NULL DEFAULT 'inactive',
  `apiadditionalinfo` longtext CHARACTER SET ascii COLLATE ascii_general_ci,
  PRIMARY KEY (`idapi`,`apiname`),
  UNIQUE KEY `no_ext_urls_duplicate` (`apiexternalurl`),
  UNIQUE KEY `no_name_duplicate` (`apiname`)
) ENGINE=InnoDB DEFAULT CHARSET=ascii;

CREATE TABLE if not exists `apitabledeleted` (
  `idapi` int(11) NOT NULL,
  `apiname` varchar(45) NOT NULL,
  `apikind` varchar(45) NOT NULL,
  `apiinternalurl` varchar(300) NOT NULL,
  `apiexternalurl` varchar(300) NOT NULL,
  `apiinfo` varchar(300) NOT NULL,
  `apicreationdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `apideletiondate` timestamp DEFAULT 0,
  `apistatus` varchar(45) NOT NULL DEFAULT 'inactive',
  `apiadditionalinfo` longtext CHARACTER SET ascii COLLATE ascii_general_ci,
  PRIMARY KEY (`idapi`)
) ENGINE=InnoDB DEFAULT CHARSET=ascii;


CREATE TABLE if not exists `ratelimit` (
  `user` varchar(45) NOT NULL,
  `resource` int(11) NOT NULL,
  `kind_of_limit` set('TotalAccesses','AccessesOverTime','ContemporaryAccess') NOT NULL,
  `additional_info` longtext CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  `timebegin` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `timeend` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`user`,`resource`),
  KEY `d` (`resource`),
  CONSTRAINT `d` FOREIGN KEY (`resource`) REFERENCES `apitable` (`idapi`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=ascii;


CREATE TABLE if not exists `timedaccess` (
  `idtimedaccess` int(11) NOT NULL AUTO_INCREMENT,
  `user` varchar(45) DEFAULT NULL,
  `beginaccess` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `resource` int(11) DEFAULT NULL,
  `result` longtext DEFAULT NULL,
  `extracted_id` varchar(45) DEFAULT NULL,
  `request_ok` int(4) DEFAULT 0,
  PRIMARY KEY (`idtimedaccess`),
  KEY `s` (`resource`)
) ENGINE=InnoDB DEFAULT CHARSET=ascii;

CREATE TABLE if not exists `requests` (
  `idnew_table` int(11) NOT NULL AUTO_INCREMENT,
  `result` longtext CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  `extracted_id` varchar(45) DEFAULT NULL,
  `endaccess` timestamp DEFAULT current_timestamp,
  PRIMARY KEY (`idnew_table`)
) ENGINE=InnoDB DEFAULT CHARSET=ascii;

CREATE TABLE if not exists `deletedratelimit` (
  `user` varchar(45) NOT NULL,
  `resource` int(11) NOT NULL,
  `kind_of_limit` set('TotalAccesses','AccessesOverTime','ContemporaryAccess') NOT NULL,
  `additional_info` longtext CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  `timebegin` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `timeend` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00'
) ENGINE=InnoDB DEFAULT CHARSET=ascii;


CREATE UNIQUE INDEX idx_extracted_id ON timedaccess(extracted_id);
CREATE UNIQUE INDEX idx_extracted_id ON requests(extracted_id);


CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`%` SQL SECURITY DEFINER VIEW `operative_apitable` AS select `apitable`.`idapi` AS `idapi`,`apitable`.`apiname` AS `apiname`,`apitable`.`apikind` AS `apikind`,`apitable`.`apiinternalurl` AS `apiinternalurl`,`apitable`.`apiexternalurl` AS `apiexternalurl`,`apitable`.`apiinfo` AS `apiinfo`,`apitable`.`apicreationdate` AS `apicreationdate`,`apitable`.`apideletiondate` AS `apideletiondate`,`apitable`.`apiadditionalinfo` AS `apiadditionalinfo`,case when `apitable`.`apistatus` = 'inactive' then 'inactive' when `apitable`.`apistatus` = 'active' then case when exists(select 1 from `ratelimit` where `ratelimit`.`resource` = `apitable`.`idapi` and `ratelimit`.`timebegin` < current_timestamp() and current_timestamp() < `ratelimit`.`timeend` limit 1) then 'active' else 'ready' end else 'unknown' end AS `apistatus` from `apitable`;

GRANT ALL ON apimanager.* TO 'user'@'%';