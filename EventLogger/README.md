# EventLogger
## configuration
  - the machine running apache with php (exposing the api) must have installed rsyslog
  - the file /etc/rsyslog.d/nifi-rsyslog.conf should contain local0.* @nifi:7780
  - on nifi should be loaded and instatiated the template to manage the rsyslog notifications (revise the configuration as needed)
  - add the index syslog-data to opensearch with the mapping reported in file mapping_syslog.json, for example using curl
    ```curl --insecure -u admin -H 'Content-Type: application/json' -X PUT 'https://localhost:9200/syslog-data' -d @mapping_syslog.json```
  - change settings.js of the nodered applications and set `eventLogUri: 'http://<api_ip>/RsyslogAPI/rsyslog.php'`
