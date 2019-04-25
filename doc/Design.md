# Design Document

## 0. Requirement

* Eight machines (localhost for Client, one for Coor, five for Nodes)	
* thrift 0.9.3 (CSE lab's version)
* vim, Linux command

thrift usage for this project (after generating `FileServer.java` file in `src/gen-java/` directory, please use `mv` command to move them into `src/` directory)

```bash
thrift -r --gen java FileServer.thrift
```

## 1. Client

The Client will only finish the test function for this project.

First it will open the transport to the SuperNode, and then show a command line like UI to handle the input from user. You can enter `SET`, `GET`, `FILE` and `EXIT` command. If you input `EXIT`, the client will shutdown. If you input wrong command, it will say this is incorrect and ask you input again. Below is an example.

```txt
[output] Please input GET, SET, FILE or EXIT.
[input]  Excited
[output] Wrong Command.
[output] --------------------
[input]  FILE
[output] Please input the file name:
[input]  shakespeares.txt
[output] Set from FILE finished.
[output] --------------------
[input]  SET
[output] Node information: <IP>:<Port>
[output] Please input <Book_title>:<Genre>
[input]  All's Well That Ends Well:Comedies
[output] Set operation [ All's Well That Ends Well:Comedies ]
# if there is no ':' the terminal will give a message "can't find : in string"
[output] --------------------
[input]  GET
[output] Node information: <IP>:<Port>
[output] Please input <Book_title>
[input]  All's Well That Ends Well
[output] The <Genre> of All's Well That Ends Well is Comedies
# if we don't have this book the terminal will say "Sorry we cannot find this book"
[output] --------------------
[input]  EXIT
```

## 2. Server (store files)


## 3. Coordinator (also a FileServer)

## 4. Useful scripts

### 4.1 `Makefile`

The `Makefile` gives us a easy way to compile the program and clean all the files produced. For the compile part, we can use `make` command to compile Client, Server, and Node. All source files are in `src/` directory while the result will be in `classes/` directory. For cleaning part, using `make clean` can clean the `class/` directory.

```Makefile
objects = class_dir Client FileServer Coordinator
FLAGS = -cp ".:/usr/local/Thrift/*" -d class -sourcepath src
all: $(objects)
.PHONY : all
class_dir:
	@if [ ! -d class ]; then mkdir class; else echo "/class already exists."; fi
Client:
	javac $(FLAGS) src/Client.java
FileServer:
	javac $(FLAGS) src/Server.java
Coordinator:
	javac $(FLAGS) src/Coordinator.java

.PHONY : clean
clean:
	@echo "Cleaning..."
	@rm -rf class/ data/
```

### 4.2 `run.sh`

```bash

```

The script is shown above. First . And then the script should make sure the number of parameter(s) is 1 or 2. Then run the Client, SuperNode or Node. For the Client, it need to get the IP address and port number of the SuperNode, so its parameter is the confg file of SuperNode. For the SuperNode, it needs the configuration of itself and the number of nodes in DHT (the maximum it can hold). For each Node, it only require the configuration file of itself.

## 5. Configuration

* `SuperNode.cfg` contains two lines: the IP address and the port number.
* `Node[#].cfg` ([#] part replaced by number) includes two lines: the IP address, the port number.
* the usage descriptions are in `4.2`

## 6. Other Information
NULL


