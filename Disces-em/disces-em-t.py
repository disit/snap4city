/* Disces-em
Copyright (C) 2018 DISIT Lab http://www.disit.org - University of 
Florence

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>. */


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
import requests
from dateutil.parser import parse
from datetime import datetime, timezone
import docker

def print_vm_info(virtual_machine):
    """
    Print information for a particular virtual machine or recurse into a
    folder with depth protection
    """
    summary = virtual_machine.summary
    print("Name       : ", summary.config.name)
    print("Template   : ", summary.config.template)
    print("Path       : ", summary.config.vmPathName)
    print("Guest      : ", summary.config.guestFullName)
    print("Instance UUID : ", summary.config.instanceUuid)
    print("Bios UUID     : ", summary.config.uuid)
    annotation = summary.config.annotation
    if annotation:
        print("Annotation : ", annotation)
    print("State      : ", summary.runtime.powerState)
    if summary.guest is not None:
        ip_address = summary.guest.ipAddress
        tools_version = summary.guest.toolsStatus
        if tools_version is not None:
            print("VMware-tools: ", tools_version)
        else:
            print("Vmware-tools: None")
        if ip_address:
            print("IP         : ", ip_address)
        else:
            print("IP         : None")
    if summary.runtime.question is not None:
        print("Question  : ", summary.runtime.question.text)
    print("")

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
        print('vm name: ', vm.vmname)
        print('ERROR: Performance results empty.  TIP: Check time drift on source and vCenter server')
        print('Troubleshooting info:')
        print('vCenter/host date and time: {}'.format(vchtime))
        print('Start perf counter time   :  {}'.format(startTime))
        print('End perf counter time     :  {}'.format(endTime))
        print(query)
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
            print(virtual_nic_device)
            return virtual_nic_device
        
        #connectable = vim.vm.device.VirtualDevice.ConnectInfo()
        result = virtual_nic_device.connectable.connected
    except:
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
        
    except mysql.connector.Error as err:
        print("Something went wrong: {}".format(err))
    
    finally:
        cursor.close()

# insert started app into MySQL
def insertApp(cnx, app):
    try:
        print("inserting app "+app_id)
        cursor = cnx.cursor()
        insert_stmt = ("INSERT IGNORE INTO marathon_apps_started (app)"
                      "VALUES (%s, %s) ON DUPLICATE UPDATE date = CURRENT_TIMESTAMP")
        data = (app.id)
        cursor.execute(insert_stmt, data)
    
        # Make sure data is committed to the database
        cnx.commit()
        
    except mysql.connector.Error as err:
        print("Something went wrong: {}".format(err))
        
    finally:
        cursor.close()

# insert deleted app into MySQL
def insertDeletedApp(cnx, app):
    try:
        print("inserting deleted app "+app_id)
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
        
    except mysql.connector.Error as err:
        print("Something went wrong: {}".format(err))
        
    finally:
        cursor.close()

# remove deleted app from MySQL
def removeDeletedApp(cnx, app_id):
    try:
        print("removing deleted app "+app_id)
        cursor = cnx.cursor()
        insert_stmt = ("DELETE FROM marathon_apps_deleted "
                      "WHERE app = %s")
        data = (app_id,)
        cursor.execute(insert_stmt, data)
    
        # Make sure data is committed to the database
        cnx.commit()
        
    except mysql.connector.Error as err:
        print("Something went wrong: {}".format(err))
        
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
            
    except mysql.connector.Error as err:
        print("Something went wrong: {}".format(err))
        
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
                
    except mysql.connector.Error as err:
        print("Something went wrong: {}".format(err))
        
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
            
    except mysql.connector.Error as err:
        print("Something went wrong: {}".format(err))
        
    finally:
        cursor.close()
    
    return apps

# get the list of unhealthy apps id = that had last success >= seconds ago and
# the dictionary of vms and their number of healthy and unhealthy apps
def getUnhealthyApps(marathon_url, seconds):
    # list of unhealthy apps
    unhealthy_apps = []
    
    # dictionary of vms and their number of total and unhealthy apps
    vm_healthy_unhealthy = {}
    
    json = requests.get(marathon_url+'/v2/tasks', headers = {'accept': 'application/json'})
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

# get the list of deploying and suspended apps
def getDeployingSuspendedApps(marathon_url):
    apps = requests.get(marathon_url+'/v2/apps', headers = {'accept': 'application/json'})
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
def getWaitingDelayedApps(marathon_url):
    apps = requests.get(marathon_url+'/v2/queue', headers = {'accept': 'application/json'})
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
        
    except mysql.connector.Error as err:
        print("Something went wrong: {}".format(err))
        
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
        
    except mysql.connector.Error as err:
        print("Something went wrong: {}".format(err))
        
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
        
    except mysql.connector.Error as err:
        print("Something went wrong: {}".format(err))
        
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
            
    except mysql.connector.Error as err:
        print("Something went wrong: {}".format(err))
        
    finally:
        cursor.close()
    
    return result

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
def getMesosCluster(mesos_url):
    slaves_list = []
    json = requests.get(mesos_url+'/slaves', headers = {'accept': 'application/json'})
    json = json.json()['slaves']
    for slave in json:
        slaves_list.append(slave['hostname'])
    return slaves_list

# check until timeout (s) with the specified interval (s) if an agent is in the Mesos cluster
def checkClusterAgent(agent, mesos_url, timeout, interval):
    start = datetime.now(timezone.utc)
    now = start
    while now.timestamp() - start.timestamp() <= timeout:
        agents_list = getMesosCluster(mesos_url)
        if agent in agents_list:
            return True
        time.sleep(interval)
        now = datetime.now(timezone.utc)
    return False

# vms dictionary

vms_dict = {
    "...VM id...": ["...VM name on VCenter....", "... VM UUID...", "... VM IP address ..."]
}

# vms nic state dictionary
vmnic = {}

# vCenter
host = ''
user = ''
password = ''
port = ''

# config
interval = 1 # minutes
cpuLimit = 75 # %
memLimit = 75 # %

# container memory (MB)
container_mem = 140

# container cpu (MHz)
container_cpu = 0.085

#container number of cpus
container_ncpus = 6

# vm memory on Mesos
vm_memory_mesos = 15.3 - 0.5

# seconds after which considering an app as unhealthy
seconds = 30

# nic action period (s)
nicActionDownPeriod = 180
nicActionUpPeriod = 120

# grace period (s)
gracePeriod = 300

# docker remote api port
docker_port = '3000'

# Marathon url
marathon_url = 'http://localhost:8080'

# Marathon client
#marathon_client = MarathonClient(marathon_url)

# Marathon multiple servers
marathon_client = MarathonClient(['http://localhost:8080'])

# Mesos url
mesos_url = 'http://localhost:5050'

# setup MySQL connection
cnx = mysql.connector.connect(user='user', password='password',
                                  host='localhost',
                                  database='quartz')

# get the apps dictionary from MySQL
apps_dictionary = getAppsList(cnx)

# get the apps list from Marathongetl
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
unhealthy_apps, vm_healthy_unhealthy = getUnhealthyApps(marathon_url, seconds)

# queued apps
queued_apps = marathon_client.list_queue()

# get the lists of deploying and suspended apps
deploying_apps, suspended_apps = getDeployingSuspendedApps(marathon_url)

# get the list of waiting and delayed apps
waiting_apps, delayed_apps = getWaitingDelayedApps(marathon_url)

# ram to be allocated for the queued apps
ram  =  container_mem * (len(queued_apps) + len(unhealthy_apps)) - (container_mem - 1)

# list of healthy apps found in Marathon
healthyApps = []

# connect to VMware datacenter
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
total_cpu = 0

# total memory average utilisation (%)
total_memory = 0

# delta cpu total
delta_cpu_total = 0

# vms metrics
vms_metrics = []

# number of active vms
active_vms = 0

# maximum cpu usage
max_cpu = 0

# dictionary of vms and number of apps
vms_apps = {}
active_vms_apps = {}

# minimum number of apps found in a vm
num_vm_min = 1000000

# name of the vm with the minimum number of apps
vm_min_id = None

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
        if(vm.runtime.powerState == vim.VirtualMachinePowerState.poweredOff):
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
        
        #print(vm.summary.quickStats.overallCpuUsage)
        
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
        vm_metrics['total_apps'] = None
        vm_metrics['healthy_apps'] = None
        vm_metrics['unhealthy_apps'] = None
        if key in vm_healthy_unhealthy:
            vm_metrics['total_apps'] = vm_healthy_unhealthy[key]['healthy'] + vm_healthy_unhealthy[key]['unhealthy']
            vm_metrics['healthy_apps'] = vm_healthy_unhealthy[key]['healthy']
            vm_metrics['unhealthy_apps'] = vm_healthy_unhealthy[key]['unhealthy']
            
        # if this vm has a number of apps less than the minimum found since now, and its nic is connected
        if vm_metrics['total_apps'] is not None and vm_metrics['total_apps'] < num_vm_min and vm_metrics['nic'] == 1:
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
    over_cpu = (active_vms - 2.2) * cpuLimit - total_cpu 
    over_mem = (active_vms - 2.2) * memLimit - total_memory 
    #over_cpu_up = active_vms * cpuLimit - total_cpu - 1.0 * cpuLimit
    #over_mem_up = active_vms * memLimit - total_memory - 0.9 * memLimit
    
cpu_ratio = None
mem_ratio = None
if len(apps_dictionary) > 0:
    # if mem_ratio is >= 1 then memory is sufficient, otherwise is insufficient
    mem_ratio = active_vms * vm_memory_mesos * 1024 / (len(apps_dictionary) * container_mem)
    # if cpu_ratio is >= 1 then cpu is sufficient, otherwise is insufficient
    cpu_ratio = active_vms * container_ncpus / (len(apps_dictionary) * (container_cpu+0.005) )
    
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

print('total_cpu_average:', total_cpu_average)
print('total_cpu:', total_cpu)
print('total_memory_average:', total_memory_average)
print('total_memory:', total_memory)
print('ram:', ram)
print('delta_cpu_average:', delta_cpu_average)
print('unhealthy apps:', len(unhealthy_apps))
print('missing apps:', len(missing_apps))
print('deploying apps:', len(deploying_apps))
print('suspended apps:', len(suspended_apps))
print('mysql apps:', len(apps_dictionary))
print('marathon apps:', len(apps))
#print('over cpu_up:', over_cpu_up)
#print('over mem_up:', over_mem_up)
print('vms_apps:', vms_apps)

####################################################################################################################


# if the last nic action was < nicActionDownPeriod seconds ago, don't do any nic action (enable/disable)
now = datetime.now(timezone.utc)
lastNicAction = getLastNicAction(cnx)
print('lastNicAction: ', lastNicAction)
elapsed = now.timestamp() - lastNicAction.timestamp() if (lastNicAction != None) else -1
nic_down_allowed = False if elapsed > 0 and elapsed <= nicActionDownPeriod else True
nic_up_allowed = False if elapsed > 0 and elapsed <= nicActionUpPeriod else True

#minvmdynamic=2
minvmdynamic = len(apps)/70 + 0.1*active_vms
maxvmdynamic = len(apps)/70 + 0.20*active_vms
deltaapps = len(apps) - len(waiting_apps)
print('max_cpu:', max_cpu)
print('minvmdynamic:', minvmdynamic)
print('deltaapps:', deltaapps)
print('elapsed: ', elapsed)
print('nicActionDownPeriod: ', nicActionDownPeriod)

print(' ')
print('Reasons to UP')
print('nic_up_allowed: ', nic_up_allowed)
print('waiting apps: ', len(waiting_apps))
print('delayed apps: ', len(delayed_apps))
print('active_vms <: ', active_vms)
print('maxvmdynamic: ', maxvmdynamic)
print('cpu_ratio<1: ', cpu_ratio)
print('mem_ratio<1: ', mem_ratio)
print('max_cpu>95: ', max_cpu)

print(' ')
print('Reasons to down:')
print('nic_down_allowed: ', nic_down_allowed)
print('over cpu>0:', over_cpu)
print('over mem>0:', over_mem)
print('active_vms: ', active_vms, '>2 and >: ')
print('maxvmdynamic: ', maxvmdynamic)

# enable a vm nic
#if (nic_up and (ram > 0 or delta_cpu_average > 0) and (mem_ratio < 1 or cpu_ratio < 1)) or (nic_up and max_cpu > 95):
#if (nic_up and (over_mem_up < 0 or over_cpu_up < 0 or max_cpu > 95) ):
if (nic_up_allowed or len(waiting_apps) > 70) and (len(waiting_apps) > 0 or len(delayed_apps) > 0) and active_vms<maxvmdynamic and (mem_ratio < 1 or cpu_ratio < 1 or max_cpu > 95):
    #print('vmsdisc: ', vms_powered_off_disconnected)
    #print('vmsdict: ', vms_dict)
    for vm_powered_off_disconnected in vms_powered_off_disconnected:
        #print('selected: ', list(vm_powered_off_disconnected.keys())[0])
        try:
            # flag that indicates that a new vm agent has joined the cluster, and we can exit from this for cycle
            flag = False
            key = list(vm_powered_off_disconnected.keys())[0]
            # get the vm from UUID
            print('vms_dict[vm_powered_off_disconnected][1]: ', vms_dict[key][1])
            vm = getVMFromUUID(search_index, vms_dict[key][1])
            print ('vm da lanciare: ', vm)
            # get name and status of this vm (0, powered off, 1 = powered on with disconnected nic)
            for k, v in vm_powered_off_disconnected.items():
                # if this vm is powered off
                if(v == 0):
                    # power on the vm and wait for completion
                    WaitForTask(vm.PowerOn())
                    
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
                agent_state = checkClusterAgent(vms_dict[k][2], mesos_url, 120, 5)
                    
                # if this agent is connected to the Mesos cluster
                if agent_state:
                    flag = True
                    # don't need to break, this cycle has one element
                    
                # else if this agent is not connected to the Mesos cluster
                else:
                    # reboot guest OS
                    #WaitForTask(vm.RebootGuest())
                    # reset the vm, more drastic but safer
                    WaitForTask(vm.Reset())
                    # don't need to break, this cycle has one element
                        
            if flag:  
                break
        except Exception as e: 
            #print (e)
            pass
    # sleep 30 seconds
    #time.sleep(30)
    
# disable the nic of the vm with the fewest number of containers
elif nic_down_allowed and over_mem > 0 and over_cpu > 0 and active_vms > maxvmdynamic and active_vms > 2:
    vm = getVMFromUUID(search_index, vms_dict[vm_min_id][1])
    
    # disable the nic of this vm
    update_virtual_nic_state(si, vm, 1, 'disconnect')
    nic = get_virtual_nic_state(si, vm, 1)
    
    # if the nic has been successfully disconnected
    if not nic:
        # insert action into MySQL
        action = {}
        action['vm'] = vm_min_id
        action['action'] = 'disable_nic'
        action['apps_list'] = None
        action['n_apps'] = vms_apps[vm_min_id]
        insertAction(cnx, action)
        
    # power off the vm
    WaitForTask(vm.PowerOff())
    
    # if the vm has been successfully powered off
    if(vm.runtime.powerState == vim.VirtualMachinePowerState.poweredOff):
        # insert action into MySQL
        action = {}
        action['vm'] = vm_min_id
        action['action'] = 'poweredOff'
        action['apps_list'] = None
        action['n_apps'] = None
        insertAction(cnx, action)
        
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
#for u in delayed_apps:
    #marathon_client.delete_app(u, force = True)
# insert action into MySQL
#if len(delayed_apps) > 0:
#    action = {}
#    action['vm'] = None
#    action['action'] = 'unallocate_delayed'
#    action['apps_list'] = jsonpickle.encode(delayed_apps)
#    action['n_apps'] = str(len(delayed_apps))
#    insertAction(cnx, action)
    
# allocate missing
for app_id in missing_apps:
    requests.post(marathon_url+'/v2/apps', data=missing_apps[app_id], headers = {'Content-type': 'application/json'})
# insert action into MySQL
if len(missing_apps) > 0:
    action = {}
    action['vm'] = None
    action['action'] = 'allocate_missing'
    action['apps_list'] = jsonpickle.encode(list(missing_apps))
    action['n_apps'] = str(len(missing_apps))
    insertAction(cnx, action)

    
print('--------------------------------')    
# close MySQL connection    
cnx.close()
