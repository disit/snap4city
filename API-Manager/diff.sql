use apimanager;

ALTER TABLE `apimanager`.`ratelimit` 
ADD COLUMN `ip` VARCHAR(20) NOT NULL AFTER `timeend`;

ALTER TABLE `apimanager`.`deletedratelimit` 
ADD COLUMN `ip` VARCHAR(20) NOT NULL AFTER `timeend`;
