#!/bin/bash

if [ "$#" -eq 0 ];
then
   pkill -f "Peer*"
else
    if [ "$#" -eq 1 ];
    then
        pkill -f "Peer"$1
    else
        echo "Usage: $0 [peer_access_point]"
        exit 1
    fi
fi
