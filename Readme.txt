By William Knight 21722128

1. Implementation 1 - JAVA - Located in src folder
Compile with javac StationJava.java if need to recompile
Will only read timetables in the src/timetables folder

2. Implementation 2 - Python - Located in src folder
Will only read timetables in the src/timetables folder


See the sample input text files if you want an example of how to call the programs on the command line
If a request for an unknown destination is sent nothing will happen, there is no timeout on requests

When a destination is found via UDP the latest possible trip is chosen
After this, each additional trip is the latest possible before that trip leaves
I was originally going to try and implement an additional value int he query string for the trip start time but I ran out of time!
I left it like this simply to ensure that the trips make sense from an arrivaltime/departtime perspective 
