import socket
import threading
import sys


#By William Knight 21722128

def main():
    
    
    #Command Line arguments
    argc = len(sys.argv)
    if argc < 5:
        print("Not enough arguments to create station server")
    
    stationName = sys.argv[1]
    tcpPort = int(sys.argv[2])
    udpPort = int(sys.argv[3])
    neighbours = []
    
    for n in range(4, argc):
        try:
            neighbours.append(int(sys.argv[n]))
        except ValueError:
            print("Could not add neighbour")
        
            
            
    if len(neighbours) < 1:
        
        print("No neighbours for station, cannot create")
        return
    
    timetable = readTimetable(stationName)
    if timetable == None:
        print("Could not read timetable")
        return
    
    #Start the Server
    tcpServer = TCPServer(stationName, tcpPort, udpPort, neighbours, timetable)
    print(f"Listening {stationName} listening TCP on {tcpPort} UDP on {udpPort} connected to {neighbours} \n")
    try:    
        tcpServer.listenTCP()
    except Exception:
        print(f"Server failed {stationName} \n")
            

#-------------------------------------------------

#Helper method for creating a string used as a datagram
def createDatagram(requestId, originalSenderPort, originalSenderName, destination):
    datagram = f"REQUESTID={requestId}\n" + "RESOLVED=0\n" + f"CURRENTSENDERNAME={originalSenderName}\n" + f"CURRENTSENDERPORT={originalSenderPort}\n" + f"ORIGINALSENDERPORT={originalSenderPort}\n" + f"ORIGINALSENDERNAME={originalSenderName}\n" + f"DESTINATION={destination}\n" + "JOURNEYCOUNT=0\n" + "END:"
    return datagram

#Append a trip to the datagram
#Also append the current sender so that we can trace the path
#If the destination has been found mark as resolved =1
def appendDatagram(originalDatagram, currentSenderName, currentSenderPort, journey, resolved=0):
    split = originalDatagram.split("\n")
    numJourneys = int(split[7].split("=")[1]);
    resultGram = split[0] + "\n"
    resultGram += f"RESOLVED={resolved}\n"
    resultGram += f"CURRENTSENDERNAME={currentSenderName}\n"
    resultGram += f"CURRENTSENDERNAME={currentSenderPort}\n"   
    for n in range(4, 7):
        resultGram += split[n]
        resultGram += "\n"
        
    resultGram += f"JOURNEYCOUNT={numJourneys+1}\n" 
    for n in range(8, 8 + numJourneys):
        resultGram += split[n]
        resultGram += "\n"
        
    resultGram += journey
    resultGram += "\n"   
    resultGram += split[len(split) -1]
    return resultGram

#Change the current sender of the datagram so it can be broadcast to neighbours
def appendCurrentSender(originalDatagram, currentSenderName, currentSenderPort):
    split = originalDatagram.split("\n")
    resultGram = split[0] + "\n"
    resultGram += split[1] + "\n"
    resultGram += f"CURRENTSENDERNAME={currentSenderName}\n"
    resultGram += f"CURRENTSENDERNAME={currentSenderPort}\n"
    
    for n in range(4, len(split) - 1):   
        resultGram += split[n] + "\n"
    resultGram += "END:"
    return resultGram
    
            
#Read the timetable file for the current station name in the Timetables folder
def readTimetable(stationName):
    import os   
    fileName = f"tt-{stationName}"
    filePath = os.path.join(os.path.abspath('./Timetables'), fileName)    
    journeys = []
    try:
        with open(filePath, "r") as file:          
            next(file)
            for line in file:
 
                if line.endswith("\n"):
                    journeys.append(line[:len(line)-1])         
                else:
                    journeys.append(line)                
    except:
        print(f"Could not find {fileName} in {filePath}\n")
        print("Retrying in current directory\n")
    else:
        return journeys
     
    try:    
        with open(fileName, "r") as file:
            next(file)
            for line in file:
                if line.endswith("\n"):
                    journeys.append(line[:len(line)-1])         
                else:
                    journeys.append(line)
    except:     
        print(f"Could not find {fileName}\n")
    return None

#Get all trips in the current timetable that match the destination of stationname
def getJourneys(timetable, stationName):
    result = []
    for line in timetable:
        destination = line.split(",")[4]
        if destination == stationName:
            result.append(line)
    return result

#Get the latest (by time ) trip to the destination
def getLastJourney(timetable, stationName):
    journeys = getJourneys(timetable, stationName)
    if len(journeys) == 0: return None
    return journeys[len(journeys)-1]    
                   
#Get the journey a close as possible before a certain time
#This is used to ensure that multiple trips lineup
def getJourneyBeforeTime(timetable, stationName, time):
    timeSplit = time.split(":")
    hours = int(timeSplit[0])
    minutes = int(timeSplit[1])
    print(hours)
    print(minutes)
    
    currentJourney = ""
    for journey in getJourneys(timetable, stationName):
        tSplit = journey.split(",")[3].split(":")
        jHours = int(tSplit[0])
        hMinutes = int(tSplit[1])
        if jHours > hours: break
        if jHours == hours:
            if hMinutes > minutes: break
        currentJourney = journey
    
    if currentJourney == "": return None
    return currentJourney


#Start UDP Server
class UDPServer:
    def __init__(self, stationName, udpPort, stationTimetable, neighbours, tcpServer):
        self.stationName = stationName
        self.udpPort = udpPort
        self.stationTimetable = stationTimetable
        self.neighbours = neighbours
        #seen and resolved seen are used to handle redundant broadcasts
        self.seen = []
        self.resolvedSeen = []
        self.tcpServer = tcpServer    
        #Bind to localhost on the specified udp port
        self.serverSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.serverSocket.bind((socket.gethostname(), self.udpPort))
        
    def listenUDP(self):
       
        while True:
            datagram = self.serverSocket.recv(2048)
            data = [datagram.decode()]             
            #Handle udp on a new thread
            thread = threading.Thread(target=self.handleUDP, args=(data))
            thread.start()
            
        
    def handleUDP(self, data):
        split = data.split("\n")
              
        print(f"PYTHON: Received UDP\n {data}\n")

        #Read relevant parts of datagram
        requestId = split[0].split("=")[1]
        resolved = int(split[1].split("=")[1])
        originalSenderPort = int(split[4].split("=")[1])
        currentSenderName = split[2].split("=")[1]
        currentSenderPort = int(split[3].split("=")[1])       
        destination = split[6].split("=")[1]
        journeyCount = int(split[7].split("=")[1])
        #Journey found
        if resolved == 1:
            #Finished
            if self.udpPort == originalSenderPort:
                if requestId in self.resolvedSeen: return
                self.resolvedSeen.append(requestId)
                #Attempt to line up the times of the journey by getting a journey prior to the latest journey in the datagram (as journeys are appended in reverse)
                #If cannot find one just add any journey
                journey = ""       
                lastTime = split[7 + journeyCount][:4]
                print(lastTime)
                lastJourney = getJourneyBeforeTime(self.stationTimetable, currentSenderName, lastTime)
                if lastJourney != None:
                    journey = lastJourney
                else:              
                    journey = getJourneys(self.stationTimetable, currentSenderName)[0]  
                toJourney = getJourneys(self.stationTimetable, currentSenderName)[0]
                allJourneys = []
                for n in range(8, len(split) - 1):
                    allJourneys.append(split[n])          
                
                allJourneys.append(journey)
                self.tcpServer.onUDPComplete(allJourneys, requestId)
            else:
                #The destination was found by some other station, we need to pass it back to the original sender by broadcasting to neighbours
                if requestId in self.resolvedSeen:
                    return
                self.resolvedSeen.append(requestId)
                #Broadcast to neighbours and append journey to the current sender to build the path
                journey = ""
                
                lastTime = split[7 + journeyCount][:4]
                lastJourney = getJourneyBeforeTime(self.stationTimetable, currentSenderName, lastTime)
                if lastJourney != None:
                    journey = lastJourney
                else:              
                    journey = getJourneys(self.stationTimetable, currentSenderName)[0]             
                updatedGram = appendDatagram(data, self.stationName, self.udpPort, journey, 1)           
                for neighbour in self.neighbours:
                    if neighbour == currentSenderPort: continue
                    self.sendUDP(updatedGram, neighbour)       
        #UDP has not resolved yet
        if resolved == 0:      
            if self.udpPort == originalSenderPort: return
            else:
                if requestId in self.seen: return
                               
                else:
                    #If the current station knowns the destination mark as complete
                    #Append a journey to the previous sender to build the path
                    self.seen.append(requestId)
                    toJourneys = getJourneys(self.stationTimetable, destination)
                    if len(toJourneys) > 0:
                        updatedGram = appendDatagram(data, self.stationName, self.udpPort, toJourneys[len(toJourneys)-1], 1)
                        self.sendUDP(updatedGram, currentSenderPort)
                    else:
                        #Destination is not known, continue broadcasting
                        updatedGram = appendCurrentSender(data, self.stationName, self.udpPort)
                        for neighbour in self.neighbours:
                            if neighbour == currentSenderPort: continue
                            self.sendUDP(updatedGram, neighbour)

        return
        
        

     #This function is called by the TCP server to start UDP     
    def getDestination(self, destination, requestId):
        journeys = getJourneys(self.stationTimetable, destination)
        if len(journeys) > 0:
            #Make HTTP no UDP required - destination is connected
            self.tcpServer.onUDPComplete([journeys[0]], requestId)
        #Perform UDP
        else:
            datagram = createDatagram(requestId, self.udpPort, self.stationName, destination)
            for neighbour in self.neighbours:
                self.sendUDP(datagram, neighbour)
            
    #Send the datagram to the port        
    def sendUDP(self, datagram, port):
        print(f"PYTHON: Sending UDP {datagram} \n to {port} \n")
        print()
        self.serverSocket.sendto(datagram.encode(), ("localhost", port))
        
        
    

#Start TCP Server-----------------------------------------------------------------------
class TCPServer:
    def __init__(self, stationName, tcpPort, udpPort, neighbours, stationTimetable):
        self.stationName = stationName
        self.tcpPort = tcpPort
        self.stationTimetable = stationTimetable
        self.requestId = ""
        self.requestCount = 0
        self.requestDict = {}
        self.serverSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.serverSocket.bind(("localhost", self.tcpPort))
        
        #Start UDP Server on a new thread
        self.udpServer = UDPServer(stationName, udpPort, stationTimetable, neighbours, self)
        thread = threading.Thread(target=self.udpServer.listenUDP, args=())
        thread.start()
    
    
#----------------------------------------------------
    def listenTCP(self):
        
        #Max clients helps to make sure that there arent too many threads
        self.serverSocket.listen(10)

        while True:
            clientConnection, address = self.serverSocket.accept()
            print("PYTHON: Found Client \n")
            self.requestId = f"{self.stationName}{self.requestCount}"
            self.requestDict[self.requestId] = clientConnection
            
            #Handle request on a new thread passing in a unique requestId
            thread = threading.Thread(target=self.handleTCP, args=(clientConnection, address, self.requestId))
            self.requestCount += 1
            thread.start()
    

#----------------------------------------------------
    def handleTCP(self, clientSocket, address, requestId):
        print("PYTHON: Processing client \n")
        while True:
            message = clientSocket.recv(1024).decode("utf-8")
            break
        getRequest = message.split("\n")[0].split(" ")
        #If there is no query string or html is invalid
        if len(getRequest) < 3:
            clientSocket.close()
            return
        #If query string is invalid ignore request
        if  getRequest[1].startswith("/?to") == False:
            print("Invalid request")
            clientSocket.close()
            return
        #Get target destination from the query string
        destination = getRequest[1].split("=")[1]
        if destination == self.stationName:
            httpResult = "HTTP/1.1 200 OK\r\n\r\n"
            httpResult += "<h1>Already at destination<h1>"
            clientSocket.send(bytes(httpResult, "utf-8"))
            clientSocket.close()          
            return
        self.udpServer.getDestination(destination, requestId)
        
    #This function is called by the UDP server to notify the TCP server that UDP is complete
    def onUDPComplete(self, allJourneys, requestId):
        print("UDP Complete")
        if requestId in self.requestDict:
            #Destination has been found - send back a 200 OK response
            clientSocket = self.requestDict[requestId]       
            httpResult = "HTTP/1.1 200 OK\r\n\r\n"
            httpResult += "<html><body>"
            if len(allJourneys) == 1:
                jSplit = allJourneys[0].split(",")
                httpResult += f"<h1>Found a journey from {self.stationName} to {jSplit[4]}</h1>"
                httpResult += allJourneys[0] + "<br>"
                httpResult += f"Arrival time is {jSplit[3]}"
            else:
                httpResult += f"<h1>Depart from {self.stationName}</h1>"
                for n in range(len(allJourneys)-1, -1, -1):
                    httpResult += allJourneys[n] + "<br>"
                    if n == 0:
                        jSplit = allJourneys[n].split(",")
                        httpResult += f"<h1>Arrive at {jSplit[4]} at {jSplit[3]}</h1>"

            httpResult += "</body></html>"
            clientSocket.send(bytes(httpResult, "utf-8"))
            clientSocket.close()
        print("TCP Complete\n")
            
        

#----------------------------------------------------
#End TCP Server-----------------------------------------------------------------------


main()

