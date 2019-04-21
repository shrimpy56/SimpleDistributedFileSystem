#!/bin/bash
cd class
error_string="Parameter Error. Using \"./run.sh Client\" , \"./run.sh Coordinator\" or \"./run.sh FileServer\""
if [ $# -eq 1 ]
then
    if [ $1 == 'Client' ]
    then
        java -cp ".:/usr/local/Thrift/*" Client
    elif [ $1 == 'FileServer' ]
    then
        java -cp ".:/usr/local/Thrift/*" FileServer
    elif [ $1 == 'Coordinator' ]
    then
        java -cp ".:/usr/local/Thrift/*" Coordinator
    else
        echo $error_string
    fi
else
    echo $error_string
fi
