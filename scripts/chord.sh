#!/bin/bash

if [ "$#" -eq 0 ] || [ "$#" -gt 2 ];
then
    echo "Usage: $0 <number of peers> <address>"
    exit 1
fi

numberOfPeers=$1
startAddress=$2
startAP=1
startPort=9999

if [ $numberOfPeers -eq 0 ];
then
    echo "<number of peers> must be at least 1"
    exit 1
fi


pkill -f "Peer*"

valid=true
AP=$startAP
port=$startPort
while [ $valid ]
do
    echo "Starting peer with access point $AP on port $port"
    if [ $AP -eq 1 ];
    then
      java -Dname=Peer$AP -cp bin peer.Peer $AP $port $startAddress &
    else
      java -Dname=Peer$AP -cp bin peer.Peer $AP $port $startAddress $startAddress $startPort &
    fi
    if [ $AP -eq $numberOfPeers ];
    then
        break
    fi
    ((AP++))
    ((port--))

done

