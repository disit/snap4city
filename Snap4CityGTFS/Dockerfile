FROM python:3.11-slim

# Imposta la working directory
WORKDIR /app

# Copia i file necessari
COPY requirements.txt .

# Installa le dipendenze
RUN pip install --no-cache-dir -r requirements.txt

# Copia il codice sorgente
COPY . .

# Espone la porta dell'app
EXPOSE 8080

# Comando di avvio
CMD ["python", "-u", "chouette-gtfs-n3.py"]

