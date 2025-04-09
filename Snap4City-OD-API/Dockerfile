FROM python:3.8.5

WORKDIR /usr/src/app

COPY py/requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY py .

ENV USER_INFO=/auth/realms/master/protocol/openid-connect/userinfo
ENV DEVICE_URL=/iot-directory/api/device.php?
ENV SERVICEMAP_URL=/superservicemap/api/v1/?
ENV SERVICE_URI=http://www.disit.org/km4city/resource/iot/