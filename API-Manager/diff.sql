use apimanager;

--ALTER TABLE `apimanager`.`ratelimit` 
--ADD COLUMN `ip` VARCHAR(20) NOT NULL AFTER `timeend`;

ALTER TABLE `apimanager`.`deletedratelimit` 
ADD COLUMN `ip` VARCHAR(20) NOT NULL AFTER `timeend`;

ALTER TABLE `apimanager`.`timedaccess_summary` 
ADD COLUMN `beginaccess` TIMESTAMP NULL DEFAULT NULL AFTER `successful_requests`;

ALTER TABLE `apimanager`.`deletedratelimit` 
ADD COLUMN `ip` VARCHAR(20) NOT NULL DEFAULT '' AFTER `timened`;


USE `apimanager`;
DROP procedure IF EXISTS `compress_timedaccess`;

USE `apimanager`;
DROP procedure IF EXISTS `apimanager`.`compress_timedaccess`;
;

-- update because nulls broke things
DELIMITER $$
USE `apimanager`$$
CREATE DEFINER=`root`@`%` PROCEDURE `compress_timedaccess`()
BEGIN
    -- Aggregate and insert/update summary table
    INSERT INTO timedaccess_summary (user, access_day, resource, total_requests, successful_requests)
    SELECT 
        COALESCE(user, 'UNKNOWN') AS user,
        DATE(beginaccess) AS access_day,
        COALESCE(resource, 0),
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
END$$

DELIMITER ;
;

