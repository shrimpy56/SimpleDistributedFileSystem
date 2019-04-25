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
	@rm -rf class/ data
