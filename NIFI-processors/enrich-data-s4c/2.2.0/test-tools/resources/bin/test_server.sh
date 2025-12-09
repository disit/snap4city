#!/bin/bash

# Check java installation
if type -p java; then
    _java=java
    echo "INFO: found java executable in PATH ($_java)"
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]]; then
    _java = "$JAVA_HOME/bin/java"
    echo "INFO: found java executable in JAVA_HOME ($_java)"
else
    echo "ERR: cannot find an installation of java in PATH or JAVA_HOME"
    exit 1
fi

# Check java version 
if [[ "$_java" ]]; then
    print_version=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
    version=$(java -version 2>&1 | sed -n ';s/.* version "\(.*\)\.\(.*\)\..*"/\1\2/p;')
    if [[ "$version" < "18" ]]; then
        echo "ERR: java version must be >= 1.8, but the installed version is $print_version"
        exit 1
    fi
fi

# Working dir
bin_dir=$(pwd)/"$(dirname "$0")"
work_dir="$(dirname "$bin_dir")"
# work_dir="$(dirname "$work_dir")"
echo "INFO: Bin dir: $bin_dir"
echo "INFO: Work dir: $work_dir"

# Check jar
jar_name="../lib/test-tools.jar"
jar_path=$work_dir/lib/test-tools.jar
if [[ -e "$jar_path" ]]; then
    echo "INFO: $jar_path found."
else
    echo "INFO: $jar_path not found."
    exit 1
fi

#echo "INFO: Using server configuration file: $conf_file"
"$_java" -jar -ea $jar_path $work_dir