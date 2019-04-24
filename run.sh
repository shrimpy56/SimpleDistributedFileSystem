#!/bin/bash
cd class
error_string="Parameter Error. Using \"./run.sh Client\" , \"./run.sh Coordinator\" or \"./run.sh FileServer\""
if [ $1 == 'Client' ]
then
    java -cp ".:/usr/local/Thrift/*" Client
elif [ $1 == 'FileServer' ]
then
    params = $*
    java -cp ".:/usr/local/Thrift/*" Server {$params:2}
elif [ $1 == 'Coordinator' ]
then
    params = $*
    java -cp ".:/usr/local/Thrift/*" Coordinator {$params:2}
else
    echo $error_string
fi
