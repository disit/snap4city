%Phoenix (phoenix)

CREATE TABLE if not exists ChargingStationObservation (
serviceID       varchar(200),
acquisitionTime Date not null,
stationState    varchar(200),
chargingState   varchar(200),
serviceUri varchar(200),
constraint pk primary key (serviceID, acquisitionTime))
