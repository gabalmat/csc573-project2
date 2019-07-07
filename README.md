# CSC 573 Project 2

### Getting Started
This project was implemented using Java.
- The p2mpserver and p2mpclient are located in ./src folder in the project.

To download project and build:
```bash
$ git clone git@github.com:gabalmat/csc573-project2.git
$ cd csc573-project2/src
$ javac p2mpserver.java
$ javac p2mpclient.java
```

After building, to run client and server
```bash
$ java p2mpserver port# file-name p
$ java p2mpclient server-1 server-2 server-3 server-port# file-name MSS
```

### Table of Contents  
- [Client](#client)
    * [Command Line Args](#client-command-line-inputs)
    * [Overview](#overview)
    * [Segment Handling](#segment-handling)
    * [Segment Timeout](#segment-timeout)
    * [Segment Header](#client-segment-header)
- [Server](#server)
    * [Command Line Args](#server-command-line-inputs)
    * [Segment Handling](#server-segment-handling)
    * [Server Segment Header (ACK)](#server-segment-header-ack)
- [Tasks](#tasks)
    * [Task 1](#task-1)
    * [Task 2](#task-2)
    * [Task 3](#task-3)
- [Attribution](#attribution)
    

## Client

Required command example
```bash
p2mpclient server-1 server-2 server-3 server-port# file-name MSS
```

Whenever a timeout occurs for a packet with sequence number Y , the client should
print the following line:
```bash
Timeout, sequence number = Y
```

##### Client Command Line Inputs
- MSS size
- File name to be transferred
- server-i is the host name where the i-th server (receiver) runs, i = 1, 2, 3
- server-port# is the port number of the server (i.e., 7735)

#### Overview
- rdt_send() provides data from the file on a byte basis
- implements Stop-and-Wait (SAW) protocol
- receiving data from rdt_send()
- ensuring that the data is received correctly at the server
- reads the value of the maximum segment size (MSS) from the command line

#### Segment Handling
1. SAW protocol buffers the data it receives from rdt send() until it 
has at least one MSS worth of bytes
2. SAW protocol forms a segment that includes a header and MSS bytes of data
    - all segments sent, except possibly for the very last one, 
    will have exactly MSS bytes of data
3. SAW protocol transmits each segment separately to each of the receivers, 
and waits until it has received ACKs from every receiver 
before it can transmit the next segment
    - a timeout counter is set for each segment

##### Segment Timeout
If the counter expires before ACKs from all receivers have been received, 
the sender re-transmits the segment. 
**Only to those receivers from which it has not received an ACK yet**


##### Client Segment Header
Each segment has a header which contains...
* a 32-bit sequence number, starts at 0
* a 16-bit checksum of the data part, computed in the same way as the UDP checksum, and 
* a 16-bit field that has the value **0101010101010101**, 
indicating that this is a data packet.


## Server
The server listens on the well-known port 7735. 

Required command example
```bash
p2mpserver port# file-name p
```

Whenever a packet with sequence number X is discarded by the probabilistic loss service, 
the server should print the following line:
```bash
Packet loss, sequence number = X
```

#### Server Command Line Inputs
- port # is always 7735
- file name for received data
- probability number **p, 0 < p < 1** representing the probability that a packet is lost

#### Server Segment Handling
When the server receives a data packet **(multiple segments!)** from the client... 
1. the server will generate a random number **r in (0,1)**
    - if r ≤ p, then this received packet is discarded and no other action is taken 
    - if r > p, the packet is accepted and processed according to the Stop-and-Wait rules
2. it computes the checksum and checks it
    - if incorrect the receiver does nothing
    On the client, associated timer for segment sent will expire
    - **if checksum is correct, move on to step 3**
3. checks whether it is in-sequence,
    - if so, it sends an ACK segment response (using UDP) to the client
    - if not, ACK for the last received in-sequence packet is sent
4. then writes the received data into a file whose name is provided in the command line


#### Server Segment Header (ACK) 
ACK has no data
* the 32-bit sequence number that is being ACKed
* a 16-bit field that is all zeroes, and
* a 16-bit field that has the value **1010101010101010**, 
indicating that this is an ACK packet.

## Tasks
For all tasks below...
- Select a file that is approximately 10MB in size, 
- Run the client and n servers on different hosts, 
such that the client is separated from the servers by several router hops
- Record the size of the file transferred and the round-trip time (RTT) 
between client and servers (e.g., as reported by traceroute), 
and include these in your report.

#### Task 1
- set the MSS to 500 bytes
- set the loss probability p = 0.05

###### TODO: 
1. Run the P2MP-FTP protocol to transfer the file you selected, 
and vary the number of receivers n = 1, 2, 3, 4, 5. For each value of n, 
    - transmit the file 5 times,
    - time the data transfer (i.e., delay),
    - compute the average delay over the five transmissions
2. Plot the average delay against n and submit the plot with your report. 
Explain how the value of n affects the delay and the shape of the curve

#### Task 2
- set number of receivers n = 3
- set the loss probability p = 0.05. 

###### TODO:
1. Run the P2MP-FTP protocol to transfer the same file,
vary the MSS from 100 bytes to 1000 bytes in increments of 100 bytes.
For each value of MSS, 
    - transmit the file 5 times,
    - compute the average delay over the five transmissions. 
2. Plot the average delay against the MSS value, and submit the plot with your report. 
Discuss the shape of the curve; are the results expected?

#### Task 3
- set the MSS to 500 bytes 
- set the number of receivers n = 3. 

###### TODO:
1. Run the P2MP-FTP protocol to transfer the same file, 
and vary the loss probability from p = 0.01 to p = 0.10 in increments of 0.01. 
For each value of p 
    - transmit the file 5 times,
    - compute the average delay over the five transfers. 
2. Plot the average delay against p, and submit the plot with your report. 
Discuss and explain the results and shape of the curve.


###### Attribution
Referenced [Stack Overflow](#https://stackoverflow.com/questions/4113890/how-to-calculate-the-internet-checksum-from-a-byte-in-java)
 for help on creating udp checksum