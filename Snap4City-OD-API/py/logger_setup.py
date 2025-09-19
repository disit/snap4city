import logging
import os

worker_id = os.getpid()

logging.basicConfig(
    level=logging.INFO,
    format=f'[worker {worker_id}] %(asctime)s: %(message)s'
)