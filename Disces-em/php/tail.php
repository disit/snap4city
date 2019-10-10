<?php
$handle = popen("tail -f /root/python-vmstats/disces-em-t.log 2>&1", 'r');
while(!feof($handle)) {
    $buffer = fgets($handle);
    echo "$buffer<br/>\n";
    ob_flush();
    flush();
}
pclose($handle);
?>
