CREATE TABLE if not exists weatherSensorObservation (
sensorID    varchar(200),
acquisitionTime date not null,
serviceUri varchar(200),

observationTime date,

atmosfericPressure float,

windDirection float,

windSpeed float,
airTemperature float,
airHumidity float,
rain float,
windGust float,
dewPoint float,
precipationType float,
roadTemperature float,
freezeTemperature float,
saltConcentration float,
waterFilm float,
roadCondition float,
vdc float
constraint pk primary key (sensorID, observationTime) );
