FROM python:3.8.5

WORKDIR /usr/src/app

COPY py/requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY py .

CMD [ "python", "-u", "./od-insert.py" ]
