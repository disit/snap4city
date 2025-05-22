import psycopg2
import os
import mysql

def psgConnect(conf):
    user_psg = 'user_psg'
    if 'user_psg' in conf:
        user_psg = conf['user_psg']

    password_psg = 'password_psg'
    if 'password_psg' in conf:
        password_psg = conf['password_psg']

    host_psg = 'host_psg'
    if 'host_psg' in conf:
        host_psg = conf['host_psg']

    port_psg = 'port_psg'
    if 'port_psg' in conf:
        port_psg = conf['port_psg']

    database_psg = 'database_psg'
    if 'database_psg' in conf:
        database_psg = conf['database_psg']
        
    conn = psycopg2.connect(user=os.getenv('POSTGRES_USER', user_psg),
                            password=os.getenv('POSTGRES_PASSWORD', password_psg),
                            host=os.getenv('POSTGRES_HOST', host_psg),
                            port=os.getenv('POSTGRES_PORT', port_psg),
                            database=os.getenv('POSTGRES_DATABASE', database_psg))
    return conn

def sqlConnect(conf):
    user_sql = 'user_sql'
    if 'user_sql' in conf:
        user_sql = conf['user_sql']

    password_sql = 'password_sql'
    if 'password_sql' in conf:
        password_sql = conf['password_sql']

    host_sql = 'host_sql'
    if 'host_sql' in conf:
        host_sql = conf['host_sql']

    database_sql = 'database_sql'
    if 'database_sql' in conf:
        database_sql = conf['database_sql']

    conn = mysql.connector.connect(user=os.getenv('MYSQL_USER', user_sql),
                                   password=os.getenv('MYSQL_PASSWORD', password_sql),
                                   host=os.getenv('MYSQL_HOST', host_sql),
                                   database=os.getenv('MYSQL_DATABASE', database_sql))
    return conn