<?php
header("Content-Type: text/plain");
$v = $_REQUEST["v"];
$n = $_REQUEST["n"];
$s = isset($_REQUEST["s"]) ? $_REQUEST["s"] : null;
$p = isset($_REQUEST["p"]) ? $_REQUEST["p"] : null;
if($s != null) {
 if($p != null) {
  $stats = `/usr/bin/python3.5 /root/python-vmstats/vmware-nic.py -v $v -n $n -s $s -p $p`; 
 }
 else {
  $stats = `/usr/bin/python3.5 /root/python-vmstats/vmware-nic.py -v $v -n $n -s $s`;
 }
} else {
$stats = `/usr/bin/python3.5 /root/python-vmstats/vmware-nic.py -v $v -n $n`;
}
echo $stats;
?>
