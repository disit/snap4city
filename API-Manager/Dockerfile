FROM python:3.11

COPY requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt

COPY app app

WORKDIR /app

CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "50000", "--workers", "10", "--log-level", "critical"]