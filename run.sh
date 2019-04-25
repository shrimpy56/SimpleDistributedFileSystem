#!/bin/bash
cd class
if [ $1 == 'Client' ]
then
    java -cp ".:/usr/local/Thrift/*" Client ${*:2}
elif [ $1 == 'Server' ]
then
    java -cp ".:/usr/local/Thrift/*" Server ${*:2}
elif [ $1 == 'Coordinator' ]
then
    java -cp ".:/usr/local/Thrift/*" Coordinator ${*:2}
else
    echo "Parameter Error. Using \"./run.sh Client\" , \"./run.sh Coordinator\" or \"./run.sh Server\" with parameters."
fi
