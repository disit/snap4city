CREATE TABLE if not exists Electric_vehicle_charging (
serviceID       VARCHAR,
actualDate      Date,
stationState    VARCHAR,
chargingState   VARCHAR,
constraint pk primary key (serviceID) );
