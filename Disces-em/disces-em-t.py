from __future__ import print_function
from pyVim.connect import SmartConnect, Disconnect
from pyVim.task import WaitForTask
from pyVmomi import vmodl, vim
from samples.tools import tasks
from datetime import timedelta, datetime
from marathon import MarathonClient
import argparse
import atexit
import ssl
import mysql.connector
from math import floor, ceil
import time
import jsonpickle
import urllib.request
import requests
from dateutil.parser import parse
from datetime import datetime, timezone
import docker
import traceback
import logging
import json
import sys
import os

def print_vm_info(virtual_machine):
    """
    Print information for a particular virtual machine or recurse into a
    folder with depth protection
    """
    summary = virtual_machine.summary
    logMessage("Name       : ", summary.config.name)
    logMessage("Template   : ", summary.config.template)
    logMessage("Path       : ", summary.config.vmPathName)
    logMessage("Guest      : ", summary.config.guestFullName)
    logMessage("Instance UUID : ", summary.config.instanceUuid)
    logMessage("Bios UUID     : ", summary.config.uuid)
    annotation = summary.config.annotation
    if annotation:
        logMessage("Annotation : ", annotation)
    logMessage("State      : ", summary.runtime.powerState)
    if summary.guest is not None:
        ip_address = summary.guest.ipAddress
        tools_version = summary.guest.toolsStatus
        if tools_version is not None:
            logMessage("VMware-tools: ", tools_version)
        else:
            logMessage("Vmware-tools: None")
        if ip_address:
            logMessage("IP         : ", ip_address)
        else:
            logMessage("IP         : None")
    if summary.runtime.question is not None:
        logMessage("Question  : ", summary.runtime.question.text)
    logMessage("")

def StatCheck(perf_dict, counter_name):
    counter_key = perf_dict[counter_name]
    return counter_key

def BuildQuery(content, vchtime, counterId, instance, vm, interval):
    perfManager = content.perfManager
    metricId = vim.PerformanceManager.MetricId(counterId=counterId, instance=instance)
    startTime = vchtime - timedelta(minutes=(interval + 1))
    endTime = vchtime - timedelta(minutes=1)
    query = vim.PerformanceManager.QuerySpec(intervalId=20, entity=vm, metricId=[metricId], startTime=startTime,
                                             endTime=endTime)
    perfResults = perfManager.QueryPerf(querySpec=[query])
    if perfResults:
        return perfResults
    else:
        logMessage('vm name: ', vm.vmname)
        logMessage('ERROR: Performance results empty.  TIP: Check time drift on source and vCenter server')
        logMessage('Troubleshooting info:')
        logMessage('vCenter/host date and time: {}'.format(vchtime))
        logMessage('Start perf counter time   :  {}'.format(startTime))
        logMessage('End perf counter time     :  {}'.format(endTime))
        logMessage(query)
        exit()
        
def getVM(content, vimtype, name):
    vm = None
    container = content.viewManager.CreateContainerView(
        content.rootFolder, vimtype, True)
    for c in container.view:
        if name:
            if c.name == name:
                vm = c
                break
        else:
            vm = c
            break

    return vm

def getCpuMem(vm, perf_dict, interval):
    # Get vCenter date and time for use as baseline when querying for counters
    vchtime = si.CurrentTime()
    statInt = interval * 3  # There are 3 20s samples in each minute
           
    # cpu usage (%)
    statCpuUsage = BuildQuery(content, vchtime, (StatCheck(perf_dict, 'cpu.usage.average')), "", vm, interval)
    cpu = (float(sum(statCpuUsage[0].value[0].value)) / statInt) / 100
    
    # memory active (%)
    statMemoryActive = BuildQuery(content, vchtime, (StatCheck(perf_dict, 'mem.active.average')), "", vm, interval)
    memoryActive = (float(sum(statMemoryActive[0].value[0].value) / 1024) / statInt)
    mem = (memoryActive / vm.summary.config.memorySizeMB) * 100
    
    return cpu, mem

def update_virtual_nic_state(si, vm_obj, nic_number, new_nic_state):
    """
    :param si: Service Instance
    :param vm_obj: Virtual Machine Object
    :param nic_number: Network Interface Controller Number
    :param new_nic_state: Either Connect, Disconnect or Delete
    :return: True if success
    """
    nic_prefix_label = 'Network adapter '
    nic_label = nic_prefix_label + str(nic_number)
    virtual_nic_device = None
    for dev in vm_obj.config.hardware.device:
        if isinstance(dev, vim.vm.device.VirtualEthernetCard) \
                and dev.deviceInfo.label == nic_label:
            virtual_nic_device = dev
    if not virtual_nic_device:
        raise RuntimeError('Virtual {} could not be found.'.format(nic_label))

    virtual_nic_spec = vim.vm.device.VirtualDeviceSpec()
    virtual_nic_spec.operation = \
        vim.vm.device.VirtualDeviceSpec.Operation.remove \
        if new_nic_state == 'delete' \
        else vim.vm.device.VirtualDeviceSpec.Operation.edit
    virtual_nic_spec.device = virtual_nic_device
    virtual_nic_spec.device.key = virtual_nic_device.key
    virtual_nic_spec.device.macAddress = virtual_nic_device.macAddress
    virtual_nic_spec.device.backing = virtual_nic_device.backing
    virtual_nic_spec.device.wakeOnLanEnabled = \
        virtual_nic_device.wakeOnLanEnabled
    connectable = vim.vm.device.VirtualDevice.ConnectInfo()
    if new_nic_state == 'connect':
        connectable.connected = True
        connectable.startConnected = True
    elif new_nic_state == 'disconnect':
        connectable.connected = False
        connectable.startConnected = False
    else:
        connectable = virtual_nic_device.connectable
    virtual_nic_spec.device.connectable = connectable
    dev_changes = []
    dev_changes.append(virtual_nic_spec)
    spec = vim.vm.ConfigSpec()
    spec.deviceChange = dev_changes
    task = vm_obj.ReconfigVM_Task(spec=spec)
    tasks.wait_for_tasks(si, [task])
    return True

def get_virtual_nic_state(si, vm_obj, nic_number):
    """
    :param si: Service Instance
    :param vm_obj: Virtual Machine Object
    :param nic_number: Network Interface Controller Number
    :param new_nic_state: Either Connect, Disconnect or Delete
    :return: True if success
    """
    result = None
    try:
        nic_prefix_label = 'Network adapter '
        nic_label = nic_prefix_label + str(nic_number)
        virtual_nic_device = None
        for dev in vm_obj.config.hardware.device:
            if isinstance(dev, vim.vm.device.VirtualEthernetCard) \
                    and dev.deviceInfo.label == nic_label:
                virtual_nic_device = dev
        if not virtual_nic_device:
            #raise RuntimeError('Virtual {} could not be found.'.format(nic_label))
            logMessage(virtual_nic_device)
            return virtual_nic_device
        
        #connectable = vim.vm.device.VirtualDevice.ConnectInfo()
        result = virtual_nic_device.connectable.connected
    except Exception as e:
        logException('error get_virtual_nic_state' + str(e) )
        logMessage('error get_virtual_nic_state' + str(e) )
        #logException(e)
        #os._exit(0)
        pass
    
    finally:
        return result

# insert apps status into MySQL
def insertAppStatus(cnx, app):
    try:
        cursor = cnx.cursor()
        insert_stmt = (
            "INSERT INTO marathon_apps_status (app, healthy) "
            "VALUES (%s, %s)"
        )
        data = (app.id, app.tasks_healthy)
        cursor.execute(insert_stmt, data)
    
        # Make sure data is committed to the database
        cnx.commit()
        
    except mysql.connector.Error as e:
        logException('error insertAppStatus' + str(e) )
        logMessage('error insertAppStatus' + str(e) )
        #logException(e)
        #os._exit(0)
    
    finally:
        cursor.close()

# insert started app into MySQL
def insertApp(cnx, app):
    try:
        logMessage("inserting app "+app_id)
        cursor = cnx.cursor()
        insert_stmt = ("INSERT IGNORE INTO marathon_apps_started (app)"
                      "VALUES (%s, %s) ON DUPLICATE UPDATE date = CURRENT_TIMESTAMP")
        data = (app.id)
        cursor.execute(insert_stmt, data)
    
        # Make sure data is committed to the database
        cnx.commit()
        
    except mysql.connector.Error as e:
        logException('error insertApp' + str(e) )
        logMessage('error insertApp' + str(e) )
        #logException(e)
        #os._exit(0)
        
    finally:
        cursor.close()

# insert deleted app into MySQL
def insertDeletedApp(cnx, app):
    try:
        logMessage("inserting deleted app "+app_id)
        cursor = cnx.cursor()
        insert_stmt = (
            "INSERT IGNORE INTO marathon_apps_deleted (app, json) "
            "VALUES (%s, %s) ON DUPLICATE KEY UPDATE json = %s"
        )
        # encode the marathon app object to json
        json = jsonpickle.encode(app)
        data = (app.id, json, json)
        cursor.execute(insert_stmt, data)
    
        # Make sure data is committed to the database
        cnx.commit()
        
    except mysql.connector.Error as e:
        logException('error insertDeletedApp' + str(e) )
        logMessage('error insertDeletedApp' + str(e) )
        #logException(e)
        #os._exit(0)
        
    finally:
        cursor.close()

# remove deleted app from MySQL
def removeDeletedApp(cnx, app_id):
    try:
        logMessage("removing deleted app "+app_id)
        cursor = cnx.cursor()
        insert_stmt = ("DELETE FROM marathon_apps_deleted "
                      "WHERE app = %s")
        data = (app_id,)
        cursor.execute(insert_stmt, data)
    
        # Make sure data is committed to the database
        cnx.commit()
        
    except mysql.connector.Error as e:
        logException('error removeDeletedApp' + str(e) )
        logMessage('error removeDeletedApp' + str(e) )
        #logException(e)
        #os._exit(0)
        
    finally:
        cursor.close()

# get deleted apps
def getDeletedApps(cnx):
    apps = []
    try:
        cursor = cnx.cursor()
        query = ("SELECT json "
                 "FROM marathon_apps_deleted")
        cursor.execute(query)
        for (json) in cursor:
            apps.append(jsonpickle.decode(json[0]))
            
    except mysql.connector.Error as e:
        logException('error getDeletedApps' + str(e) )
        logMessage('error getDeletedApps' + str(e) )
        #logException(e)
        #os._exit(0)
        
    finally:
        cursor.close()
    
    return apps

# get apps status in the last n seconds
def getAppsStatus(cnx, seconds):
    apps = []
    try:
        cursor = cnx.cursor()
        query = ("SELECT app, SUM(healthy) AS healthy "
                 "FROM marathon_apps_status "
                 "WHERE date >= (NOW() - INTERVAL " + str(seconds) + " SECOND) GROUP BY app")
        cursor.execute(query)
        for (app, healthy) in cursor:
            if healthy == 0:
                apps.append(app)
                
    except mysql.connector.Error as e:
        logException('error getAppsStatus' + str(e) )
        logMessage('error getAppsStatus' + str(e) )
        #logException(e)
        #os._exit(0)
        
    finally:
        cursor.close()
    
    return apps

# get apps list
def getAppsList(cnx):
    apps = {}
    try:
        cursor = cnx.cursor()
        query = ("SELECT app, json, date "
                 "FROM marathon_apps")
        cursor.execute(query)
        for (app, json, date) in cursor:
            apps[app] = [json, date]
            
    except mysql.connector.Error as e:
        logException('error getAppsList' + str(e) )
        logMessage('error getAppsList' + str(e) )
        #logException(e)
        #os._exit(0)
        
    finally:
        cursor.close()
    
    return apps

# get the list of unhealthy apps id = that had last success >= seconds ago and
# the dictionary of vms and their number of healthy and unhealthy apps
def getUnhealthyApps(marathon_url, seconds, request_timeout):
    # list of unhealthy apps
    unhealthy_apps = []
    
    # dictionary of vms and their number of total and unhealthy apps
    vm_healthy_unhealthy = {}
    try:
        json = requests.get(marathon_url+'/v2/tasks', headers = {'accept': 'application/json'}, timeout = request_timeout)
    except Exception as e:
        logException('error getUnhealthyApps' + str(e) )
        logMessage('error getUnhealthyApps' + str(e) )
        #os._exit(0)
        #return
        
    json = json.json()['tasks']
    for j in json:
        if 'healthCheckResults' in j and len(j['healthCheckResults']) > 0 and 'lastSuccess' in j['healthCheckResults'][0]:
            now = datetime.now(timezone.utc)
            lastSuccess = parse(j['healthCheckResults'][0]['lastSuccess'])
            elapsed = now.timestamp() - lastSuccess.timestamp()
            # get vm ip (last three digits)
            key = j['host']
            key = key.split('.')
            # if this vm belongs to network 1, then append a 't' to its name
            key = key[3] if key[2] == '0' else key[3] + 't' 
            # initialize dictionary counters for this vm
            if key not in vm_healthy_unhealthy:
                vm_healthy_unhealthy[key] = {}
                vm_healthy_unhealthy[key]['unhealthy'] = 0
                vm_healthy_unhealthy[key]['healthy'] = 0
            if elapsed >= seconds:
                unhealthy_apps.append(j['appId'])
                # increment the number of unhealthy apps for this vm
                vm_healthy_unhealthy[key]['unhealthy'] = vm_healthy_unhealthy[key]['unhealthy'] + 1
            else:
                # increment the number of healthy apps for this vm
                vm_healthy_unhealthy[key]['healthy'] = vm_healthy_unhealthy[key]['healthy'] + 1
    
    return unhealthy_apps, vm_healthy_unhealthy

def getMarathonUrl(urls):
        for url in urls:
            try:
                code = urllib.request.urlopen(url, timeout=2).code
                # logMessage('Code getMarathonUrl' + str(code) )
                if code == 200:
                    return url
            except Exception as e:
                logException('error getMarathonUrl' + str(e) )
                logMessage('error getMarathonUrl' + str(e) )
                #logException(e)
                #os._exit(0)
            return urls[0]
    
# get the list of deploying and suspended apps
def getDeployingSuspendedApps(marathon_url, request_timeout):
    try:
        apps = requests.get(marathon_url+'/v2/apps', headers = {'accept': 'application/json'}, timeout = request_timeout)
    except Exception as e:
        logException('error getDeployingSuspendedApps' + str(e) )
        logMessage('error getDeployingSuspendedApps' + str(e) )
        #logException(e)
        #os._exit(0)
        
    apps = apps.json()
    deployments = []
    suspended = []
    for app in apps["apps"]:
        if len(app["deployments"]) > 0:
            deployments.append(app["id"])
        if app["instances"] == 0 and app["tasksRunning"] == 0:
            suspended.append(app["id"])
    return deployments, suspended

# get the list of waiting and delayed apps
def getWaitingDelayedApps(marathon_url, request_timeout):
    try:
        apps = requests.get(marathon_url+'/v2/queue', headers = {'accept': 'application/json'}, timeout = request_timeout)
    except Exception as e:
        logException('error getWaitingDelayedApps' + str(e) )
        logMessage('error getWaitingDelayedApps' + str(e) )
        #logException(e)
        #os._exit(0)
        
    apps = apps.json()
    waiting = []
    delayed = []
    for app in apps["queue"]:
        if app["delay"]["overdue"]:
            waiting.append(app["app"]["id"])
        elif not app["delay"]["overdue"]:
            delayed.append(app["app"]["id"])
    return waiting, delayed

# insert vm metrics into MySQL
def insertVMMetrics(cnx, vm_metrics):
    try:
        cursor = cnx.cursor()
        insert_stmt = (
            "INSERT INTO marathon_vm_metrics (vm, ip, uuid, nic, cpu, mem, delta_cpu, healthy_apps, unhealthy_apps, total_apps) "
            "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)"
        )
        data = (vm_metrics['name'], vm_metrics['ip'], vm_metrics['uuid'], vm_metrics['nic'], 
                vm_metrics['cpu'], vm_metrics['mem'], vm_metrics['delta_cpu'], 
                vm_metrics['healthy_apps'], vm_metrics['unhealthy_apps'], vm_metrics['total_apps'])
        cursor.execute(insert_stmt, data)
    
        # Make sure data is committed to the database
        cnx.commit()
        
    except mysql.connector.Error as e:
        logException('error insertVMMetrics' + str(e) )
        logMessage('error insertVMMetrics' + str(e) )
        #logException(e)
        #os._exit(0)
        
    finally:
        cursor.close()
        
# insert total metrics into MySQL
def insertTotalMetrics(cnx, total_metrics):
    try:
        cursor = cnx.cursor()
        insert_stmt = (
            "INSERT INTO marathon_total_metrics (`interval`, cpuLimit, container_mem, container_cpu, "
            "vm_memory_mesos, seconds, cpu_ratio, mem_ratio, total_cpu_average, total_memory_average, "
            "ram, delta_cpu_average, active_vms, unhealthy_apps, missing_apps, mysql_apps, "
            "marathon_apps, over_cpu, over_mem) "
            "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)"
        )
        data = (total_metrics['interval'], total_metrics['cpuLimit'],
                total_metrics['container_mem'], total_metrics['container_cpu'],
                total_metrics['vm_memory_mesos'], total_metrics['seconds'],
                total_metrics['cpu_ratio'], total_metrics['mem_ratio'], 
                total_metrics['total_cpu_average'], total_metrics['total_memory_average'], 
                total_metrics['ram'], total_metrics['delta_cpu_average'], 
                total_metrics['active_vms'], total_metrics['unhealthy_apps'], 
                total_metrics['missing_apps'], total_metrics['mysql_apps'], 
                total_metrics['marathon_apps'], total_metrics['over_cpu'],
                total_metrics['over_mem'])
        cursor.execute(insert_stmt, data)
    
        # Make sure data is committed to the database
        cnx.commit()
        
    except mysql.connector.Error as e:
        logException('error insertTotalMetrics' + str(e) )
        logMessage('error insertTotalMetrics' + str(e) )
        #logException(e)
        #os._exit(0)
        
    finally:
        cursor.close()
        
# insert action into MySQL
def insertAction(cnx, action):
    try:
        cursor = cnx.cursor()
        insert_stmt = (
            "INSERT INTO marathon_actions (vm, action, apps_list, n_apps) "
            "VALUES (%s, %s, %s, %s)"
        )
        data = (action['vm'], action['action'], action['apps_list'], action['n_apps'])
        cursor.execute(insert_stmt, data)
    
        # Make sure data is committed to the database
        cnx.commit()
        
    except mysql.connector.Error as e:
        logException('error insertAction' + str(e) )
        logMessage('error insertAction' + str(e) )
        #logException(e)
        #os._exit(0)
        
    finally:
        cursor.close()

# get timestamp of last action 'enable_nic' or 'disable_nic'
def getLastNicAction(cnx):
    result = None
    try:
        cursor = cnx.cursor()
        query = ("SELECT date FROM marathon_actions WHERE action = 'enable_nic' "
                 "OR action = 'disable_nic' ORDER BY id DESC LIMIT 1")
        cursor.execute(query)
        for (date) in cursor:
            result = date[0]
            
    except mysql.connector.Error as e:
        logException('error getLastNicAction' + str(e) )
        logMessage('error getLastNicAction' + str(e) )
        #logException(e)
        #os._exit(0)
        
    finally:
        cursor.close()
    
    return result

# get apps' cpus, memory, disk totals from MySQL
def getCpusMemDisk(cnx):
    cpus = None
    mem = None
    disk = None
    try:
        cursor = cnx.cursor()
        query = ("SELECT SUM(JSON_EXTRACT(json, '$.cpus')) AS 'cpus',"
                 "SUM(JSON_EXTRACT(json, '$.mem')) AS 'mem',"
                 "SUM(JSON_EXTRACT(json, '$.disk')) AS 'disk' "
                 "FROM quartz.marathon_apps")
        cursor.execute(query)
        for (c, m, d) in cursor:
            cpus = c
            mem = m
            disk = d
            
    except mysql.connector.Error as e:
        logException('error getCpusMemDisk' + str(e) )
        logMessage('error getCpusMemDisk' + str(e) )
        #logException(e)
        #os._exit(0)
        
    finally:
        cursor.close()
    
    return cpus, mem, disk

# get apps' stats (cpus, memory, disk) from MySQL
def getAppsStats(cnx):
    apps = {}
    try:
        cursor = cnx.cursor()
        query = ("SELECT app, JSON_EXTRACT(json, '$.cpus') AS 'cpus', "
                 "JSON_EXTRACT(json, '$.mem') AS 'mem', "
                 "JSON_EXTRACT(json, '$.disk') AS 'disk' "
                 "FROM quartz.marathon_apps")
        cursor.execute(query)
        for (app, cpus, mem, disk) in cursor:
            apps[app] = {}
            apps[app]["cpus"] = cpus
            apps[app]["mem"] = mem
            apps[app]["disk"] = disk
            
    except mysql.connector.Error as e:
        logException('error getAppsStats' + str(e) )
        logMessage('error getAppsStats' + str(e) )
        #logException(e)
        #os._exit(0)
        
    finally:
        cursor.close()
    
    return apps

# get VMs dictionary from MySQL
def getVMsDict(cnx):
    vms_dict = {}
    try:
        cursor = cnx.cursor()
        query = ("SELECT vm_id, vm_name, uuid, ip FROM quartz.vms_dict WHERE offline = 0")
        cursor.execute(query)
        for (vm_id, vm_name, uuid, ip) in cursor:
            vms_dict[vm_id] = [vm_name, uuid, ip]
            
    except mysql.connector.Error as e:
        logException('error getVMsDict' + str(e) )
        logMessage('error getVMsDict' + str(e) )
        #logException(e)
        #os._exit(0)
        
    finally:
        cursor.close()
    
    return vms_dict

# get the number of apps of an active vm
def getVMApps(vm_ip, vm_port):
    # setup docker's remote API connection
    client = docker.DockerClient(base_url=vm_ip+":"+vm_port, timeout=5)
    return len(client.containers.list())

def getVMFromUUID(search_index, uuid):
    return search_index.FindByUuid(None, uuid, True, True)

def getUUIDFomVM(content, vm_name):
    vm = getVM(content, [vim.VirtualMachine], vm_name)
    return vm.summary.config.instanceUuid
        
# set vm resources, (memReservation)
'''
expandableReservation (xsd:boolean):

In a resource pool with an expandable reservation, the reservation on a resource pool can grow beyond 
the specified value, if the parent resource pool has unreserved resources. A non-expandable reservation is 
called a fixed reservation. This property is ignored for virtual machines. 

limit (xsd:long):

The utilization of a virtual machine/resource pool will not exceed this limit, even if 
there are available resources. This is typically used to ensure a consistent performance of 
virtual machines / resource pools independent of available resources. If set to -1, then there is 
no fixed limit on resource usage (only bounded by available resources and shares). 
Units are MB for memory, MHz for CPU. 

overheadLimit (xsd:long):

The maximum allowed overhead memory. For a powered on virtual machine, the overhead memory reservation 
cannot be larger than its overheadLimit. This property is only applicable to powered on virtual machines 
and is not persisted across reboots. This property is not applicable for resource pools. If set to -1, 
then there is no limit on reservation. Units are MB.

reservation:

Amount of resource that is guaranteed available to the virtual machine or resource pool. 
Reserved resources are not wasted if they are not used. 
If the utilization is less than the reservation, the resources can 
be utilized by other running virtual machines. Units are MB for memory, MHz for CPU.

shares (SharesInfo), not supported:

set (xsd:long): value of [cpu, ram] to set

Memory shares are used in case of resource contention. 
'''
# resources = {[memory, cpu] : [expandableReservation, limit, overheadLimit, reservation, set] : value}
def setVMResources(vm, resources):
    res = None
    cspec = vim.vm.ConfigSpec()
    for resource in resources:
            for allocation in resources[resource]:
                value = resources[resource][allocation]
                
                if allocation == 'expandableReservation':
                    res = vim.ResourceAllocationInfo(expandableReservation=value)
                elif allocation == 'limit':
                    res = vim.ResourceAllocationInfo(limit=value)
                elif allocation == 'overheadLimit':
                    res = vim.ResourceAllocationInfo(overheadLimit=value)
                elif allocation == 'reservation':
                    res = vim.ResourceAllocationInfo(reservation=value)
                elif allocation == 'set':
                    res = value
                    
                if resource == 'memory':
                    if allocation == 'set':
                        cspec.memoryMB = value
                    else:
                        cspec.memoryAllocation = res
                elif resource == 'cpu':
                    if allocation == 'set':
                        cspec.numCPUs = value
                        cspec.numCoresPerSocket = 1 # fixed
                    else:
                        cspec.cpuAllocation = res
                        
    WaitForTask(vm.Reconfigure(cspec))

# get the list of agents connected to the Mesos cluster
def getMesosCluster(mesos_url, request_timeout):
    slaves_list = []
    try:
        json = requests.get(mesos_url+'/slaves', headers = {'accept': 'application/json'}, timeout = request_timeout)
    except Exception as e:
        logException('error getMesosCluster' + str(e) )
        logMessage('error getMesosCluster' + str(e) )
        #logException(e)
        #os._exit(0)
        
    json = json.json()['slaves']
    for slave in json:
        slaves_list.append(slave['hostname'])
    return slaves_list

# check until timeout (s) with the specified interval (s) if an agent is in the Mesos cluster
def checkClusterAgent(agent, mesos_url, timeout, interval, request_timeout):
    start = datetime.now(timezone.utc)
    now = start
    logMessage('Checking agent: '+ str(agent)+ ' in Mesos')
    while now.timestamp() - start.timestamp() <= timeout:
        agents_list = getMesosCluster(mesos_url, request_timeout)
        logMessage('Agents List from Mesos: '+ str(agents_list))
        if agent in agents_list:
            return True
        time.sleep(interval)
        now = datetime.now(timezone.utc)
    return False

# log exception to file
def logException(e):
    # get the current date and time
    now = datetime.now()
    
    # get the exception's informations
    exc_type, exc_obj, exc_tb = sys.exc_info()
    #fname = os.path.split(exc_tb.tb_frame.f_code.co_filename)[1]
    #logMessage(exc_type, fname, exc_tb.tb_lineno)
    #logging.error(traceback.format_exc())
    
    # open the log file in append mode
    f = open("disces-em-error-t.txt", "a+")
    
    # write the exception to file (date | exception | line)
    f.write(str(now) + " | " + str(e) + " | " + str(exc_tb.tb_lineno) + "\n") 
    
    # close the file
    f.close()
    
# log message to file
def logMessage(msg):
    # get the current date and time
    now = datetime.now()
    
    # open the log file in append mode
    f = open("/root/python-vmstats/disces-em-t-log.txt", "a+")
    
    # write to file
    f.write(msg + "\n") 
    
    # close the file
    f.close()

    
logMessage('\n-------------- Start script: ' + datetime.now().strftime("%Y-%m-%d %H:%M:%S.%f") + '------------\n')    

# vms nic state dictionary
vmnic = {}

# settings
import settings

# vCenter
host = settings.config["vCenter_host"]
user = settings.config["vCenter_user"]
password = settings.config["vCenter_password"]
port = settings.config["vCenter_port"]

# config
interval = settings.config["interval"] # minutes
cpuLimit = settings.config["cpuLimit"] # %
memLimit = settings.config["memLimit"] # %

# container memory (MB)
container_mem = settings.config["container_mem"]

# container cpu (MHz)
container_cpu = settings.config["container_cpu"]

#container number of cpus
container_ncpus = settings.config["container_ncpus"]

# vm memory on Mesos
vm_memory_mesos = settings.config["vm_memory_mesos"]

# seconds after which considering an app as unhealthy
seconds = settings.config["seconds"]

# timeout (s) for request calls
request_timeout = settings.config["request_timeout"]

# nic action period (s)
nicActionDownPeriod = settings.config["nicActionDownPeriod"]
nicActionUpPeriod = settings.config["nicActionUpPeriod"]

# grace period (s)
gracePeriod = settings.config["gracePeriod"]
           
# docker remote api port
docker_port = settings.config["docker_port"]

# Marathon url
#marathon_url = 'http://192.168.1.187:8080'
marathon_url = getMarathonUrl(settings.config["marathon_nodes"])
logMessage('Marathon Url selected: '+ marathon_url)

# Marathon client
#marathon_client = MarathonClient(marathon_url)

# Marathon multiple servers
marathon_client = MarathonClient(settings.config["marathon_nodes"])

# Mesos url
#mesos_url = 'http://192.168.1.187:5050'
mesos_url = getMarathonUrl(settings.config["mesos_nodes"])

# setup MySQL connection
cnx = mysql.connector.connect(user=settings.config["mysql_user"], password=settings.config["mysql_password"],
                                  host=settings.config["mysql_host"],
                                  database=settings.config["mysql_database"])

# get VMs dictionary from MySQL
vms_dict = getVMsDict(cnx)

# get the apps dictionary from MySQL
apps_dictionary = getAppsList(cnx)

# get the apps list from Marathon
apps = marathon_client.list_apps()

# get marathon apps id
apps_id = {}
for app in apps:
    apps_id[app.id] = 1

# get missing apps for a time > grace period
missing_apps = {}
for app_id in apps_dictionary:
    if app_id not in apps_id:
        now = datetime.now(timezone.utc)
        elapsed = now.timestamp() - apps_dictionary[app_id][1].timestamp()
        if elapsed > gracePeriod:
            missing_apps[app_id] = apps_dictionary[app_id][0]

# get the list of unhealthy apps id = that had last success >= seconds ago and
# the dictionary of vms and their number of healthy and unhealthy apps
unhealthy_apps, vm_healthy_unhealthy = getUnhealthyApps(marathon_url, seconds, request_timeout)

# queued apps
queued_apps = marathon_client.list_queue()

# get the lists of deploying and suspended apps
deploying_apps, suspended_apps = getDeployingSuspendedApps(marathon_url, request_timeout)

# get the list of waiting and delayed apps
waiting_apps, delayed_apps = getWaitingDelayedApps(marathon_url, request_timeout)

# print('lista delayed apps', delayed_apps)

# ram to be allocated for the queued apps
ram  =  container_mem * (len(queued_apps) + len(unhealthy_apps)) - (container_mem - 1)

# list of healthy apps found in Marathon
healthyApps = []

# connect to VMware datacenter
logMessage("Connecting to VMWare datacenter")

context = ssl._create_unverified_context()
si = SmartConnect(host=host,
                  user=user,
                  pwd=password,
                  port=int(port),
                  sslContext=context)
atexit.register(Disconnect, si)
content = si.RetrieveContent()
search_index = si.content.searchIndex

# Get all the performance counters
perf_dict = {}
perfList = content.perfManager.perfCounter
for counter in perfList:
    counter_full = "{}.{}.{}".format(counter.groupInfo.key, counter.nameInfo.key, counter.rollupType)
    perf_dict[counter_full] = counter.key

# total cpu average utilisation (%)
total_cpu = settings.config["total_cpu"]

# total memory average utilisation (%)
total_memory = settings.config["total_memory"]

# delta cpu total
delta_cpu_total = settings.config["delta_cpu_total"]

# vms metrics
vms_metrics = []

# number of active vms
active_vms = settings.config["active_vms"]

# maximum cpu usage
max_cpu = settings.config["max_cpu"]

# dictionary of vms and number of apps
vms_apps = {}
active_vms_apps = {}

# minimum number of apps found in a vm
num_vm_min = settings.config["num_vm_min"]

# name of the vm with the minimum number of apps
vm_min_id = settings.config["vm_min_id"]

# array of powered off vms, or powered on vms with disconnected nic
# {vm: n}, n = 0 (powered off), 1 (disconnected nic)
vms_powered_off_disconnected = []

# for each vm
for key in vms_dict:
    try:
        vm_metrics = {}

        vm_metrics['name'] = key
        
        #vm = getVM(content, [vim.VirtualMachine], vms_dict[key])
        vm = getVMFromUUID(search_index, vms_dict[key][1])
        
        # if this vm is powered off, then continue
        if vm.runtime.powerState == vim.VirtualMachinePowerState.poweredOff:
            vms_powered_off_disconnected.append({key: 0})
            pass
        
        # else if this vm is powered on
        else:
            # get vm nic state and write it in the vm_metrics dictionary
            vm_metrics['nic'] = get_virtual_nic_state(si, vm, 1)
            if vm_metrics['nic']:
                active_vms += 1
            # if this vm nic is not connected, then continue
            else:
                vms_powered_off_disconnected.append({key: 1})
                pass
        
        vm_metrics['uuid'] = vm.summary.config.instanceUuid
        
        if(hasattr(vm, 'runtime') and 
               hasattr(vm.guest, 'ipAddress') and 
               vm.guest.ipAddress != 'None' and 
               vm.guest.ipAddress != '' and 
               vm.guest.ipAddress is not None):
           vm_ip = vm.guest.ipAddress
        else:
            vm_ip = None
        vm_metrics['ip'] = vm_ip 
        
        #logMessage(vm.summary.quickStats.overallCpuUsage)
        
        # get cpu and memory usage (%)
        cpu, mem = getCpuMem(vm, perf_dict, interval)

        # write cpu and memory in vm_metrics dictionary
        vm_metrics['cpu'] = cpu
        vm_metrics['mem'] = mem
        
        # update total cpu and memory counts
        if vm_metrics['nic']:
            total_cpu += cpu
            total_memory += mem
        
        # update max cpu usage
        if cpu > max_cpu:
            max_cpu = cpu
        
        # calculate delta cpu and write it in vm_metrics dictionary
        vm_metrics['delta_cpu'] = cpu - cpuLimit
        
        # get the number of apps of this vm
        vm_metrics['total_apps'] = 0
        vm_metrics['healthy_apps'] = 0
        vm_metrics['unhealthy_apps'] = 0
        if key in vm_healthy_unhealthy:
            vm_metrics['total_apps'] = vm_healthy_unhealthy[key]['healthy'] + vm_healthy_unhealthy[key]['unhealthy']
            vm_metrics['healthy_apps'] = vm_healthy_unhealthy[key]['healthy']
            vm_metrics['unhealthy_apps'] = vm_healthy_unhealthy[key]['unhealthy']
            
        # if this vm has a number of apps less than the minimum found since now, and its nic is connected
        if vm_metrics['total_apps'] < num_vm_min and vm_metrics['nic'] == 1:
            num_vm_min = vm_metrics['total_apps']
            vm_min_id = key
            
        # add the number of apps of this vm to a dictionary
        # for calcuting the vm with the minimum number of apps
        vms_apps[key] = 9999
        if vm_metrics['healthy_apps'] is not None:
            vms_apps[key] = vm_metrics['healthy_apps']
        
        if vm_metrics['total_apps'] is None:
            vms_apps[key] = 0
        
        # add this vm_metrics to vms_metrics array
        vms_metrics.append(vm_metrics)
        
        # update delta cpu total count
        delta_cpu_total += vm_metrics['delta_cpu']
        
        # insert vm metrics dictionary into MySQL
        insertVMMetrics(cnx, vm_metrics)
                
    except:
        pass

total_cpu_average = None
total_memory_average = None
delta_cpu_average = None
over_cpu = None
over_mem = None
deltaapps = None

if active_vms > 0:
    total_cpu_average = total_cpu / active_vms
    total_memory_average = total_memory / active_vms
    # negative if there is available cpu, positive if missing, above the limit
    delta_cpu_average = delta_cpu_total / active_vms
    # over 2 VM
    #over_cpu = (active_vms - 2.2) * cpuLimit - total_cpu 
    #over_mem = (active_vms - 2.2) * memLimit - total_memory 
    # over 3 VM
    over_cpu = (active_vms - 3.2) * cpuLimit - total_cpu 
    over_mem = (active_vms - 3.2) * memLimit - total_memory    
    #over_cpu_up = active_vms * cpuLimit - total_cpu - 1.0 * cpuLimit
    #over_mem_up = active_vms * memLimit - total_memory - 0.9 * memLimit
    
cpu_ratio = None
mem_ratio = None
if len(apps_dictionary) > 0:
    # if mem_ratio is >= 1 then memory is sufficient, otherwise is insufficient
    # TODO: (len(apps_dictionary) * container_mem) dovrebbe essere come somma dei valori dei singoli container allocati
    mem_ratio = active_vms * vm_memory_mesos * 1024 / (len(apps_dictionary) * container_mem)
    # if cpu_ratio is >= 1 then cpu is sufficient, otherwise is insufficient
    # TODO: (len(apps_dictionary) * (container_cpu+0.0055) ) 
    #       ora container_cpu * 1,0648  che implica aggiungere il 6,48% di overhead per CPU
    #       dovrebbe essere come somma dei valori dei singoli container allocati
    cpu_ratio = active_vms * container_ncpus / (len(apps_dictionary) * (container_cpu+0.0055) )
    
# insert total metrics into MySQL
total_metrics = {}
total_metrics['interval'] = str(interval)
total_metrics['cpuLimit'] = str(cpuLimit)
total_metrics['container_mem'] = str(container_mem)
total_metrics['container_cpu'] = str(container_cpu)
total_metrics['vm_memory_mesos'] = str(vm_memory_mesos)
total_metrics['seconds'] = str(seconds)
total_metrics['cpu_ratio'] = str(cpu_ratio) if(cpu_ratio is not None) else None
total_metrics['mem_ratio'] = str(mem_ratio) if(mem_ratio is not None) else None
total_metrics['total_cpu_average'] = str(total_cpu_average) if(total_cpu_average is not None) else None
total_metrics['total_memory_average'] = str(total_memory_average) if(total_memory_average is not None) else None
total_metrics['ram'] = str(ram) if(ram is not None) else None
total_metrics['delta_cpu_average'] = str(delta_cpu_average) if(delta_cpu_average is not None) else None
total_metrics['active_vms'] = str(active_vms) if(active_vms is not None) else None
total_metrics['unhealthy_apps'] = str(len(unhealthy_apps)) if(unhealthy_apps is not None) else None
total_metrics['missing_apps'] = str(len(missing_apps)) if(missing_apps is not None) else None
total_metrics['mysql_apps'] = str(len(apps_dictionary)) if(apps_dictionary is not None) else None
total_metrics['marathon_apps'] = str(len(apps)) if(apps is not None) else None
total_metrics['over_cpu'] = str(over_cpu) if(over_cpu is not None) else None
total_metrics['over_mem'] = str(over_mem) if(over_mem is not None) else None

insertTotalMetrics(cnx, total_metrics)

logMessage('total_cpu_average: ' + str(total_cpu_average))
logMessage('total_cpu: ' + str(total_cpu))
logMessage('total_memory_average: ' + str(total_memory_average))
logMessage('total_memory: ' + str(total_memory))
logMessage('ram: ' + str(ram))
logMessage('delta_cpu_average: ' + str(delta_cpu_average))
logMessage('unhealthy apps: ' + str(len(unhealthy_apps)))
logMessage('missing apps: ' + str(len(missing_apps)))
logMessage('deploying apps: ' + str(len(deploying_apps)))
logMessage('suspended apps: ' + str(len(suspended_apps)))
logMessage('mysql apps: ' + str(len(apps_dictionary)))
logMessage('marathon apps: ' + str(len(apps)))
#logMessage('over cpu_up:', over_cpu_up)
#logMessage('over mem_up:', over_mem_up)
logMessage('vms_apps:' + str(vms_apps))

####################################################################################################################

logMessage('\n-------------- Computing conditions: ' + datetime.now().strftime("%Y-%m-%d %H:%M:%S.%f") + '------------\n')

#logMessage("apps stats:")
#logMessage(json.dumps(getAppsStats(cnx)))
apps_stats = getAppsStats(cnx)
#logMessage(str(apps_stats))
cpus, mem, disk = getCpusMemDisk(cnx)
logMessage("Total requested CPU in %: "+str(cpus))
logMessage("Total requested mem in MiByte: "+str(mem))
logMessage("Total requested HD in MiByte: "+str(disk))
logMessage("Number of Apps on Marathon active: " + str(len(apps_stats)))

# if the last nic action was < nicActionDownPeriod seconds ago, don't do any nic action (enable/disable)
now = datetime.now(timezone.utc)
lastNicAction = getLastNicAction(cnx)
logMessage('lastNicAction: ' + str(lastNicAction))
elapsed = now.timestamp() - lastNicAction.timestamp() if (lastNicAction != None) else -1
nic_down_allowed = False if elapsed > 0 and elapsed <= nicActionDownPeriod else True
nic_up_allowed = False if elapsed > 0 and elapsed <= nicActionUpPeriod else True
#minvmdynamic=2

#NEW parameters
minvmdynamic = mem/(vm_memory_mesos-vm_memory_mesos*0.20)/1000 + 0.1 * active_vms
maxvmdynamic = minvmdynamic + 0.1 * active_vms
#minvmdynamic = len(apps) / 70 + 0.1 * active_vms
#maxvmdynamic = len(apps) / 70 + 0.20 * active_vms
deltaapps = len(apps) - len(waiting_apps)
mismatchapps = len(apps) - len(apps_dictionary)
logMessage('CRITICAL CRITICAL CRITICAL CRITICAL mismatchapps Marathon ha APP che non sono eliminabili:' + str(mismatchapps))
mem_ratio = len(vms_apps) * (vm_memory_mesos-vm_memory_mesos*0.35)*1000 / (mem)
logMessage('new_mem_ratio:' + str(mem_ratio))
over_mem = (vm_memory_mesos-vm_memory_mesos*0.30)*1000*active_vms - mem
logMessage('new_over_mem:' + str(over_mem))
over_cpu = (container_ncpus-container_ncpus*0.30)*active_vms - cpus
logMessage('new_over_cpu:' + str(over_cpu))

logMessage('Elapsed Time since last action: ' + str(elapsed))

logMessage('max_cpu:' + str(max_cpu))
#logMessage('new_minvmdynamic:' + str(new_minvmdynamic))
logMessage('minvmdynamic:' + str(minvmdynamic))
logMessage('maxvmdynamic:' + str(maxvmdynamic))
logMessage('deltaapps:' + str(deltaapps))
logMessage('elapsed: ' + str(elapsed))
logMessage('nicActionDownPeriod: ' + str(nicActionDownPeriod))

avg_container_cpu = cpus / len(apps_stats)
avg_container_mem = mem / len(apps_stats)
logMessage("Avg Mem: "+str(avg_container_cpu))
logMessage("Avg Cpu: "+str(avg_container_mem))

logMessage(' ')
logMessage('---Reasons to up:---')
logMessage('nic_up_allowed or waiting apps>70: ' + str(nic_up_allowed) + ' or ' + str(len(delayed_apps)>70) )
logMessage('waiting apps>0 or delayed apps>0: ' + str(len(waiting_apps)>0) + ' or ' + str(len(delayed_apps)>0))
logMessage('-- delayed apps>0: ' + str(len(delayed_apps)))
logMessage('active_vms<maxvmdynamic: ' + str(active_vms) + ' < ' + str(maxvmdynamic))
logMessage('[\ncpu_ratio= '+ str(cpu_ratio) + ' <1 ) or')
logMessage('mem_ratio= ' + str(mem_ratio) + ' <1 ) or' )
logMessage('max_cpu= ' + str(max_cpu) + ' >95\n]')

logMessage(' ')
logMessage('---Reasons to down:---')
logMessage('nic_down_allowed: ' + str(nic_down_allowed))
logMessage('over cpu= ' + str(over_cpu) + ' >0')
logMessage('over mem= ' + str(over_mem) + ' >0')
logMessage('active_vms= ' + str(active_vms) + ' > 3 and active_vms=' + str(active_vms) +' >maxvmdynamic=' + str(maxvmdynamic))

logMessage('\n-------------- Taking decisions: ' + datetime.now().strftime("%Y-%m-%d %H:%M:%S.%f") + '------------\n')

#for vm_powered_off_disconnected in vms_powered_off_disconnected:
#    for k, v in vm_powered_off_disconnected.items():
#        agent_state = checkClusterAgent(vms_dict[k][2], mesos_url, 180, 10, request_timeout) #nesi
#        logMessage("# Agent State"+str(agent_state) )

# enable a vm nic
#if (nic_up and (ram > 0 or delta_cpu_average > 0) and (mem_ratio < 1 or cpu_ratio < 1)) or (nic_up and max_cpu > 95):
#if (nic_up and (over_mem_up < 0 or over_cpu_up < 0 or max_cpu > 95) ):
if (nic_up_allowed or len(waiting_apps)>70) and (len(waiting_apps) > 0 or len(delayed_apps) > 0) and active_vms < maxvmdynamic and (mem_ratio < 1 or cpu_ratio < 1 or max_cpu > 95):
    logMessage("# enabling a vm or nic")
    #logMessage('vmsdisc: ', vms_powered_off_disconnected)
    #logMessage('vmsdict: ', vms_dict)
    for vm_powered_off_disconnected in vms_powered_off_disconnected:
        #logMessage('selected: ', list(vm_powered_off_disconnected.keys())[0])
        try:
            # flag that indicates that a new vm agent has joined the cluster, and we can exit from this for cycle
            flag = False
            key = list(vm_powered_off_disconnected.keys())[0]
            # get the vm from UUID
            logMessage('vms_dict[vm_powered_off_disconnected][1]: ' + str(vms_dict[key][1]) )
            vm = getVMFromUUID(search_index, vms_dict[key][1])
            logMessage('vm da lanciare: ' + str(vm) )
            # get name and status of this vm (0, powered off, 1 = powered on with disconnected nic)
            for k, v in vm_powered_off_disconnected.items():
                # if this vm is powered off
                if(v == 0):
                    # power on the vm and wait for completion
                    WaitForTask(vm.PowerOn())
                    logMessage('Allocated VM: ' + str(vm))
                    
                    # if the vm has been successfully powered on
                    if(vm.runtime.powerState == vim.VirtualMachinePowerState.poweredOn):
                        # insert action into MySQL
                        action = {}
                        action['vm'] = k
                        action['action'] = 'poweredOn'
                        action['apps_list'] = None
                        action['n_apps'] = None
                        insertAction(cnx, action)
                
                # if the nic is disabled, then enable it
                nic = get_virtual_nic_state(si, vm, 1)
                if not nic:
                    update_virtual_nic_state(si, vm, 1, 'connect')
                    nic = get_virtual_nic_state(si, vm, 1)
                    # if the nic has been successfully connected
                    if nic:
                        # insert action into MySQL
                        action = {}
                        action['vm'] = k
                        action['action'] = 'enable_nic'
                        action['apps_list'] = None
                        action['n_apps'] = None
                        insertAction(cnx, action)
            
                # wait for the agent joining the Mesos cluster
                agent_state = checkClusterAgent(vms_dict[k][2], mesos_url, 360, 10, request_timeout)
                logMessage("# Agent State"+str(agent_state) )
                
                # if this agent is connected to the Mesos cluster
                if agent_state:
                    flag = True
                    # don't need to break, this cycle has one element
                    
                # else if this agent is not connected to the Mesos cluster
                else:
                    logMessage("# Else Agent State: reset della VM that was not starting")
                    # reboot guest OS
                    #WaitForTask(vm.RebootGuest())
                    # reset the vm, more drastic but safer
                    WaitForTask(vm.Reset())
                    # don't need to break, this cycle has one element
                        
            if flag:  
                break
        except Exception as e: 
            logMessage('Error: '+str(e))
            logException('Error: '+str(e))
            #print (e)
            pass
    logMessage("# done enabling a vm nic")
    # sleep 30 seconds
    #time.sleep(30)
    
# disable the nic of the vm with the fewest number of containers
elif nic_down_allowed and over_mem > 0 and over_cpu > 0 and active_vms > maxvmdynamic and active_vms > 3:
    # prima era active_vms > 2 in modo da scendere a 2 VM minime
    vm = getVMFromUUID(search_index, vms_dict[vm_min_id][1])
    
    # disable the nic of this vm
    logMessage("# disabling vm or nic")
    update_virtual_nic_state(si, vm, 1, 'disconnect')
    nic = get_virtual_nic_state(si, vm, 1)
    logMessage("# done disabling vm or nic")
    
    # if the nic has been successfully disconnected
    if not nic:
        # insert action into MySQL
        logMessage("# inserting nic has been successfully disconnected action into MySQL")
        action = {}
        action['vm'] = vm_min_id
        action['action'] = 'disable_nic'
        action['apps_list'] = None
        action['n_apps'] = vms_apps[vm_min_id]
        insertAction(cnx, action)
        logMessage("# done inserting nic has been successfully disconnected action into MySQL")
        
    # power off the vm
    logMessage("# powering off the vm")
    WaitForTask(vm.PowerOff())
    logMessage('Down VM: '+ str(vm))
    logMessage("# done powering off the vm")
    
    # if the vm has been successfully powered off
    if(vm.runtime.powerState == vim.VirtualMachinePowerState.poweredOff):
        # insert action into MySQL
        logMessage("# inserting powered off action into MySQL")
        action = {}
        action['vm'] = vm_min_id
        action['action'] = 'poweredOff'
        action['apps_list'] = None
        action['n_apps'] = None
        insertAction(cnx, action)
        logMessage("# done inserting powered off action into MySQL")
        
# unallocate unhealthy apps
#for u in unhealthy_apps:
    #requests.delete(marathon_url+'/v2/apps/'+u[1:])
    # qui da un errore vedi shapshot
    #marathon_client.delete_app(u, force = True)
# insert action into MySQL
#if len(unhealthy_apps) > 0:
#    action = {}
#    action['vm'] = None
#    action['action'] = 'unallocate_unhealthy'
#    action['apps_list'] = jsonpickle.encode(unhealthy_apps)
#    action['n_apps'] = str(len(unhealthy_apps))
#    insertAction(cnx, action)

# unallocate waiting apps
#for u in waiting_apps:
    #marathon_client.delete_app(u, force = True)
# insert action into MySQL
#if len(waiting_apps) > 0:
#    action = {}
#    action['vm'] = None
#    action['action'] = 'unallocate_waiting'
#    action['apps_list'] = jsonpickle.encode(waiting_apps)
#    action['n_apps'] = str(len(waiting_apps))
#    insertAction(cnx, action)
    
# unallocate delayed apps
# UCCIDERE LE APP DELAYED SOLO SE LO SONO DA 10 MINUTI
recover = False
if (recover):
    logMessage("# kill delayed apps from too long")
    for u in delayed_apps:
        try:
            marathon_client.delete_app(u, force = True)
        except Exception as e:
            logMessage('Error from Marathon in killing App:'+str(e))
            logException('Error from Marathon in killing App:'+str(e))
            pass #os._exit(0)    
    # insert action into MySQL
    if len(delayed_apps) > 0:
        action = {}
        action['vm'] = None
        action['action'] = 'unallocate_delayed'
        action['apps_list'] = jsonpickle.encode(delayed_apps)
        action['n_apps'] = str(len(delayed_apps))
        insertAction(cnx, action)
    logMessage("# DONE kill delayed apps from too long")
    
# allocate missing
logMessage("# allocate missing apps")
for app_id in missing_apps:
    try:
        logMessage("# true allocate missing apps")
        requests.post(marathon_url+'/v2/apps', 
                      data=missing_apps[app_id], 
                      headers = {'Content-type': 'application/json'}, 
                      timeout = request_timeout)
    except Exception as e:
        logMessage('Error from Marathon in getting Up App:'+str(e))
        logException('Error from Marathon in getting Up App:'+str(e))
        pass #os._exit(0)
logMessage("# done allocating missing apps")

# insert action into MySQL
if len(missing_apps) > 0:
    logMessage("# inserting action into MySQL")
    action = {}
    action['vm'] = None
    action['action'] = 'allocate_missing'
    action['apps_list'] = jsonpickle.encode(list(missing_apps))
    action['n_apps'] = str(len(missing_apps))
    insertAction(cnx, action)
    logMessage("# done inserting action into MySQL")

#########################other log################################################################################


logMessage('--------------------------------')    
# close MySQL connection    
cnx.close()
