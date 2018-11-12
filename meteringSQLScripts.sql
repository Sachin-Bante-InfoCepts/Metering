--
-- Table structure for table `user_meter_time`
--

DROP TABLE IF EXISTS `saasuser`;

CREATE TABLE `aws_saas`.`user_meter_time` (
  `userName` text NOT NULL,
  `customerID` tinytext NOT NULL,
  `productID` tinytext NOT NULL,
  `loginTime` TIMESTAMP,
  `logoutTime` TIMESTAMP NULL DEFAULT NULL,
  `meterFlag` int(11) NOT NULL,
  PRIMARY KEY (`userName`(20))
) ENGINE=InnoDB DEFAULT CHARSET=utf8;