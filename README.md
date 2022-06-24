# sdis1920-t1g24

## Scripts

### compile.sh
Compiles program, outputing to the bin folder

### chord.sh <number_of_peers> <ip_address>
Runs peers that will constitute a chord, killing the previous running peers. The peer's access point will start counting from 1, and will be incremented from there.
* number_of_peers - number of peers to be run
* ip_address - address where the peers will be running on

### kill.sh [access_point] 
Kills a specific peer of every peer (if no access point is specified)
* access_point - access point of the peer to be killed

## Running a Peer
After compiling, you can run an independent peer as following, from the root folder:
`java -cp bin peer.Peer <access_point> <port> <address> [<existing_address> <existing_port>]`
where:
* access_point - access point of the new peer
* port - port to be used by the new peer
* address - address to be used by the new peer
* existing_address - known address from the chord's peer
* existing_port -  known port from the chord's peer
