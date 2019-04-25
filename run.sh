#!/bin/bash
cd class
params = $*
if [ $1 == 'Client' ]
then
    java -cp ".:/usr/local/Thrift/*" Client {$params:2}
elif [ $1 == 'Server' ]
then
    java -cp ".:/usr/local/Thrift/*" Server {$params:2}
elif [ $1 == 'Coordinator' ]
then
    java -cp ".:/usr/local/Thrift/*" Coordinator {$params:2}
else
    echo "Parameter Error. Using \"./run.sh Client\" , \"./run.sh Coordinator\" or \"./run.sh Server\" with parameters."
fi
