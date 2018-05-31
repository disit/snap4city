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


import atexit
import requests
from samples.tools import cli
from pyVmomi import vim
from pyVim.connect import SmartConnect, Disconnect
from samples.tools import tasks
import ssl
import argparse

# disable  urllib3 warnings
if hasattr(requests.packages.urllib3, 'disable_warnings'):
    requests.packages.urllib3.disable_warnings()


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
        if isinstance(dev, vim.vm.device.VirtualEthernetCard)                 and dev.deviceInfo.label == nic_label:
            virtual_nic_device = dev
    if not virtual_nic_device:
        raise RuntimeError('Virtual {} could not be found.'.format(nic_label))

    virtual_nic_spec = vim.vm.device.VirtualDeviceSpec()
    virtual_nic_spec.operation =         vim.vm.device.VirtualDeviceSpec.Operation.remove         if new_nic_state == 'delete'         else vim.vm.device.VirtualDeviceSpec.Operation.edit
    virtual_nic_spec.device = virtual_nic_device
    virtual_nic_spec.device.key = virtual_nic_device.key
    virtual_nic_spec.device.macAddress = virtual_nic_device.macAddress
    virtual_nic_spec.device.backing = virtual_nic_device.backing
    virtual_nic_spec.device.wakeOnLanEnabled =         virtual_nic_device.wakeOnLanEnabled
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
            if isinstance(dev, vim.vm.device.VirtualEthernetCard)                     and dev.deviceInfo.label == nic_label:
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

def get_args():
    parser = argparse.ArgumentParser(description='Process args for powering on a Virtual Machine')
    parser.add_argument('-v', '--vmname', required=True, action='store', help='Name of the VirtualMachine you want to change')
    parser.add_argument('-n', '--nicnumber', required=True, action='store', help='Number of the nic you want to change')
    parser.add_argument('-s', '--state', required=False, action='store', help='State you want to set')
    args = parser.parse_args()
    return args

def get_obj(content, vim_type, name):
    obj = None
    container = content.viewManager.CreateContainerView(
        content.rootFolder, vim_type, True)
    for c in container.view:
        if c.name == name:
            obj = c
            break
    return obj

def getVMFromUUID(search_index, uuid):
    return search_index.FindByUuid(None, uuid, True, True)

def main(host, user, password, port):
    # vms dictionary
    vms_dict = {
		"...VM id...": ["...VM name on VCenter....", "...VM UUID...", "...VM IP address ..."]
	}
    
    args = get_args()

    vm_name = vms_dict[args.vmname][0]
    vm_uuid = vms_dict[args.vmname][1]
    
    # connect to vc
    context = None
    context = ssl._create_unverified_context()
    si = SmartConnect(host=host,
                        user=user,
                        pwd=password,
                        port=int(port),
                        sslContext=context)
    # disconnect vc
    atexit.register(Disconnect, si)
    content = si.RetrieveContent()
    search_index = si.content.searchIndex
    #print('Searching for VM {}'.format(vm_name))
    #vm_obj = get_obj(content, [vim.VirtualMachine], vm_name)
    vm_obj = getVMFromUUID(search_index, vm_uuid)

    if vm_obj:
        if args.state is not None:
            update_virtual_nic_state(si, vm_obj, args.nicnumber, args.state)
            print('VM NIC {} successfully'                   ' state changed to {}'.format(args.nicnumber, args.state))
        else:
            print(get_virtual_nic_state(si, vm_obj, args.nicnumber))
    else:
        print('VM not found')

# start
if __name__ == "__main__":
    # vCenter
    host = '192.168.0.202'
    user = 'administrator@vsphere.local'
    password = 'C3;9jk5%'
    port = '443'
    main(host, user, password, port)

