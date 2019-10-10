# config array
config = {}

# vCenter
config["vCenter_host"] = 'localhost'
config["vCenter_user"] = 'user'
config["vCenter_password"] = 'password'
config["vCenter_port"] = '443'

# config
config["interval"] = 1 # minutes
config["cpuLimit"] = 75 # %
config["memLimit"] = 75 # %

# container memory (MB)
config["container_mem"] = 140

# container cpu (MHz)
config["container_cpu"] = 0.085

#container number of cpus
config["container_ncpus"] = 6

# vm memory on Mesos
config["vm_memory_mesos"] = 15.3 - 0.5

# seconds after which considering an app as unhealthy
config["seconds"] = 30

# timeout (s) for request calls
config["request_timeout"] = 600

# nic action period (s)
config["nicActionDownPeriod"] = 180
config["nicActionUpPeriod"] = 120

# grace period (s)
config["gracePeriod"] = 300

# docker remote api port
config["docker_port"] = '3000'

# marathon nodes
config["marathon_nodes"] = ['http://localhost:8080']

# mesos nodes
config["mesos_nodes"] = ['http://localhost:5050']

# setup MySQL connection
config["mysql_user"] = 'user'
config["mysql_password"] = 'password'
config["mysql_host"] = 'localhost'
config["mysql_database"] = 'quartz'
       
# total cpu average utilisation (%)
config["total_cpu"] = 0

# total memory average utilisation (%)
config["total_memory"] = 0

# delta cpu total
config["delta_cpu_total"] = 0

# number of active vms
config["active_vms"] = 0

# maximum cpu usage
config["max_cpu"] = 0

# minimum number of apps found in a vm
config["num_vm_min"] = 1000000

# name of the vm with the minimum number of apps
config["vm_min_id"] = None

# vms dictionary
'''
vms_dict = {
}
'''

'''
vms_dict = {
}
'''