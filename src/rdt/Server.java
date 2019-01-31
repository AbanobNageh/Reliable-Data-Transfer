package rdt;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * this class represents a server, this class receives requests from the clients and send 
 * the requested files.
 */
public class Server extends Thread{
	InetAddress serverIP;
	short serverPort;
	DatagramSocket sendingSocket;
	DatagramSocket receivingSocket;
	int protocol = 1;        // Protocol ID, 1 = stop and wait, 2 = selective repeat, 3 = go back N.
	int windowSize;           
	int timeOut = 500;        // Timeout until the server stops listening.
	FileHandler fileHandler = FileHandler.getFileHandler();
	
	/**
	 * cretes a new server, reads the server data from file.
	 */
	public Server() {
		try {
			this.loadServerFromFile();
			sendingSocket = new DatagramSocket();
			receivingSocket = new DatagramSocket(this.serverPort);
			receivingSocket.setSoTimeout(5000);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * creates a new server from the given arguments.
	 * @param serverIP the IP of the server.
	 * @param serverPort the port that the server will receive on.
	 * @param windowSize the number of packets that should be sent together.
	 */
	public Server (String serverIP, short serverPort, int windowSize) {
		try {
			this.serverIP = InetAddress.getByName(serverIP);
			this.serverPort = serverPort;
			this.windowSize = windowSize;
		
			sendingSocket = new DatagramSocket();
			receivingSocket = new DatagramSocket(this.serverPort);
			receivingSocket.setSoTimeout(5000);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	/**
	 * starts the server, the servers starts to listen to requests from clients, 
	 * it listens for requests, if a request arrives the server starts a new thread to 
	 * send data to the user and receive the user's Acks. 
	 * the server stops listening after a timeout. 
	 */
	@Override
	public void run() {
		try {
			// these arraylists hold the threads currently sending data to clients, only one arraylist is created based on the selected protocol.
			ArrayList<stopAndWaitHandler> handlers = null;
			ArrayList<SelectiveRepeatHandler> selectiveHandlers = null;
			ArrayList<GoBackNHandler> goBackHandlers = null;
			
			if (protocol == 1) {
				handlers = new ArrayList<stopAndWaitHandler>();
			} 
			else if (protocol == 2) {
				selectiveHandlers = new ArrayList<SelectiveRepeatHandler>();
			}
			else {
				goBackHandlers = new ArrayList<GoBackNHandler>();
			}
			
			while(true) {
				byte[] packetBuffer = new byte[100];
				DatagramPacket recivedDatagram = new DatagramPacket(packetBuffer, packetBuffer.length);
				ACKPacket receivedACKPacket = null;
				TCPPacket receivedPacket = null;
				
				// listen for new requests from clients
				receivingSocket.receive(recivedDatagram);
				byte[] encodedData = recivedDatagram.getData();
				
				// get the length of the received packet.
				int packetLength = this.getPacketLength(encodedData);
				
				// if smaller than 12 it is an ACK packet.
				if (packetLength < 12) {
					receivedACKPacket = new ACKPacket();
					receivedACKPacket.decode(encodedData);
					System.out.println("server received ACK packet " + receivedACKPacket.getAckNumber());
					
					if (!receivedACKPacket.isCorrupted()) {
						InetAddress clientIP = recivedDatagram.getAddress();
						short clientPort = receivedACKPacket.getSourcePort();
						int sequanceNumber = receivedACKPacket.getAckNumber();
						
						// pass the ACK packet to the appropriate handler.
						if (protocol == 1) {
							for (stopAndWaitHandler tempHandler: handlers){
								if (tempHandler.getClientIP().equals(clientIP) && tempHandler.getClientPort() == clientPort) {
									tempHandler.setLastACKReceived(sequanceNumber);
								}
							}
						} 
						else if (protocol == 2) {
							for (SelectiveRepeatHandler tempHandler: selectiveHandlers){
								if (tempHandler.getClientIP().equals(clientIP) && tempHandler.getClientPort() == clientPort) {
									tempHandler.addAck((short) sequanceNumber);
								}
							}
						}
						else {
							for (GoBackNHandler tempHandler: goBackHandlers){
								if (tempHandler.getClientIP().equals(clientIP) && tempHandler.getClientPort() == clientPort) {
									tempHandler.addAck((short) sequanceNumber);
								}
							}
						}
						
					}
				}
				// else it is a data packet.
				else{
					receivedPacket = new TCPPacket();
					receivedPacket.decode(encodedData);
					System.out.println("server received request packet");
					
					// depending on the selected protocol create an appropriate handler.
					if (!receivedPacket.isCorrupted()) {
						if (protocol == 1) {
							stopAndWaitHandler handler = new stopAndWaitHandler(receivedPacket, recivedDatagram.getAddress());
							handler.start();
							handlers.add(handler);
						}
						else if (protocol == 2) {
							SelectiveRepeatHandler handler = new SelectiveRepeatHandler(receivedPacket, recivedDatagram.getAddress(), this.windowSize);
							handler.start();
							selectiveHandlers.add(handler);
						}
						else {
							GoBackNHandler handler = new GoBackNHandler(receivedPacket, recivedDatagram.getAddress(), this.windowSize);
							handler.start();
							goBackHandlers.add(handler);
						}
					}
				}
			}
		} catch(SocketTimeoutException e) {
			return;
		} catch(Exception e2) {
			e2.printStackTrace();
		}
	}

	/**
	 * returns the length of a packet.
	 * @param encodedData data of the packet
	 * @return the length of the packet.
	 */
	public int getPacketLength(byte[] encodedData) {
		int firstNonZeroIndex = 0;
		for (int i = encodedData.length - 1; i > 0; i--) {
			if (encodedData[i] != 0) {
				firstNonZeroIndex = i;
				break;
			}
		}
		
		return firstNonZeroIndex + 1;
	}
	
	public void setProtocol(int protocol) {
		this.protocol = protocol;
	}
	
	/**
	 * load the server data from file.
	 */
	public void loadServerFromFile() {
		String tempString;
		
		try {
			FileReader file = new FileReader("server.txt");
			BufferedReader read = new BufferedReader(file);
			
			tempString = read.readLine();
			this.serverIP = InetAddress.getByName(tempString);
			
			tempString = read.readLine();
			this.serverPort = Short.parseShort(tempString);
			
			tempString = read.readLine();
			this.windowSize = Integer.parseInt(tempString);
			
			tempString = read.readLine();
			this.protocol = Integer.parseInt(tempString);
			
			read.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * a server thread that sends data accourding to the stop and wait protocol.
	 */
	private class stopAndWaitHandler extends Thread{
		int currentIndex = 0;
		int lastACKReceived = -1;
		InetAddress clientIP;
		short clientPort;
		ArrayList<TCPPacket> filePackets;
		DatagramSocket sendingSocket;
		
		/**
		 * creates a new stop and wait protocol thread.
		 * @param receivedPacket the request packet sent by the client.
		 * @param clientIP the IP of the client.
		 */
		public stopAndWaitHandler(TCPPacket receivedPacket, InetAddress clientIP) {
			try {
				this.clientIP = clientIP;
				this.clientPort = receivedPacket.getSourcePort();
				this.filePackets = fileHandler.getFilePackets(receivedPacket.getFileName(), this.clientPort, serverPort);
				this.sendingSocket = new DatagramSocket();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		/**
		 * starts sending the packets of the requested file to the client.
		 */
		@Override
		public void run() {
			try {
				boolean lastPacketSent = false;

				// when current packet is the last packet set flag.
				while(!lastPacketSent) {
					if (filePackets.get(currentIndex).isFinalPacket()) {
						lastPacketSent = true;
					}
					
					// encode the current packet.
					byte[] encodedData = filePackets.get(currentIndex).encode();
					DatagramPacket filePacket = new DatagramPacket(encodedData, encodedData.length, clientIP, this.clientPort);
					
					// keep sending the current packet until an ACK packet is received.
					while(this.currentIndex != this.lastACKReceived) {
						this.sendingSocket.send(filePacket);
						System.out.println("server sent packet " + filePackets.get(currentIndex).getSequanceNumber());
						Thread.sleep(timeOut);
					}
					
					this.currentIndex++;
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		public InetAddress getClientIP() {
			return clientIP;
		}

		public short getClientPort() {
			return clientPort;
		}

		public void setLastACKReceived(int lastACKReceived) {
			this.lastACKReceived = lastACKReceived;
		}
	}
	
	/**
	 * a server thread that sends data accourding to the selective repeat protocol.
	 */
	private class SelectiveRepeatHandler extends Thread{
		int windowSize;
		short clientPort;
		boolean lastPacketSent = false;
		InetAddress clientIP;
		ArrayList<Timer> timers = new ArrayList<Timer>();
		ArrayList<TCPPacket> filePackets;
		CopyOnWriteArrayList<Short> recivedACKNumbers = new CopyOnWriteArrayList<Short>();
		DatagramSocket sendingSocket;
		
		/**
		 * creates a new selective repeat protocol thread.
		 * @param receivedPacket the request packet sent by the client.
		 * @param clientIP the IP of the client.
		 * @param windowsSize the number of packets activly being sent.
		 */
		public SelectiveRepeatHandler(TCPPacket receivedPacket, InetAddress clientIP, int windowsSize) {
			try {
				this.windowSize = windowsSize;
				this.clientIP = clientIP;
				this.clientPort = receivedPacket.getSourcePort();
				this.filePackets = fileHandler.getFilePackets(receivedPacket.getFileName(), this.clientPort, serverPort);
				this.sendingSocket = new DatagramSocket();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		/**
		 * starts sending the packets of the requested file to the client.
		 */
		@Override
		public void run() {
			int currentWindowSize = 0;
			int currentBase = 0;
			
			try {
				while(!this.lastPacketSent) {
					// if current window is last window send packets and set flag.
					if (currentWindowSize == 0 && (currentBase + this.windowSize) >= this.filePackets.size()) {
						for (int i = currentBase; i < this.filePackets.size(); i++) {
							byte[] encodedData = filePackets.get(i).encode();
							DatagramPacket filePacket = new DatagramPacket(encodedData, encodedData.length, clientIP, this.clientPort);
							this.sendingSocket.send(filePacket);
							System.out.println("server sent final packet " + filePackets.get(i).getSequanceNumber());
							
							Timer timer = new Timer(timeOut, filePackets.get(i).getSequanceNumber());
							timer.start();
							timers.add(timer);
							
							currentWindowSize++;
						}
						
						this.lastPacketSent = true;
					}
					// if current window is not last window send packets only.
					else if (currentWindowSize == 0 && (currentBase + this.windowSize) < this.filePackets.size()) {
						for (int i = currentBase; i < currentBase + this.windowSize; i++) {
							byte[] encodedData = filePackets.get(i).encode();
							DatagramPacket filePacket = new DatagramPacket(encodedData, encodedData.length, clientIP, this.clientPort);
							this.sendingSocket.send(filePacket);
							System.out.println("server sent packet " + filePackets.get(i).getSequanceNumber());
							
							Timer timer = new Timer(timeOut, filePackets.get(i).getSequanceNumber());
							timer.start();
							timers.add(timer);
							
							currentWindowSize++;
						}
						
						currentBase = currentBase + this.windowSize;
					}
					
					// keep sending packets until all ACK packets are received.
					while(currentWindowSize != 0) {
						for (int i = 0; i < this.timers.size(); i++) {
							// if ACK packet received remove its timer.
							if (this.recivedACKNumbers.contains(timers.get(i).getTimerNumber())) {
								timers.remove(i);
								currentWindowSize--;
							}
							// if timer finished resend packet.
							else if (timers.get(i).isTimerFinished()) {
								short packetNumber = timers.get(i).getTimerNumber();
								byte[] encodedData = filePackets.get(packetNumber).encode();
								DatagramPacket filePacket = new DatagramPacket(encodedData, encodedData.length, clientIP, this.clientPort);
								this.sendingSocket.send(filePacket);
								System.out.println("server resent packet " + filePackets.get(packetNumber).getSequanceNumber());
								
								Timer timer = new Timer(timeOut, packetNumber);
								timer.start();
								timers.set(i, timer);
							}
						}
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		} 
		
		public short getClientPort() {
			return clientPort;
		}

		public InetAddress getClientIP() {
			return clientIP;
		}

		public void addAck(short ACKNumber) {
			if (!this.recivedACKNumbers.contains(ACKNumber)) {
				this.recivedACKNumbers.add(ACKNumber);
			}
		}
	}

	/**
	 * a server thread that sends data accourding to the go back n protocol.
	 */
	private class GoBackNHandler extends Thread{
		int windowSize;
		short clientPort;
		boolean lastPacketSent = false;
		InetAddress clientIP;
		ArrayList<Short> currentWindow = new ArrayList<Short>();
		Timer timer;
		ArrayList<TCPPacket> filePackets;
		CopyOnWriteArrayList<Short> recivedACKNumbers = new CopyOnWriteArrayList<Short>();
		DatagramSocket sendingSocket;
		
		/**
		 * creates a new selective repeat protocol thread.
		 * @param receivedPacket the request packet sent by the client.
		 * @param clientIP the IP of the client.
		 * @param windowsSize the number of packets activly being sent.
		 */
		public GoBackNHandler(TCPPacket receivedPacket, InetAddress clientIP, int windowsSize) {
			try {
				this.windowSize = windowsSize;
				this.clientIP = clientIP;
				this.clientPort = receivedPacket.getSourcePort();
				this.filePackets = fileHandler.getFilePackets(receivedPacket.getFileName(), this.clientPort, serverPort);
				this.sendingSocket = new DatagramSocket();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		/**
		 * starts sending the packets of the requested file to the client.
		 */
		@Override
		public void run() {
			int currentWindowSize = 0;
			int currentBase = 0;
			
			try {
				while(!this.lastPacketSent) {
					// if current window is last window send packets and set flag.
					if (currentWindowSize == 0 && (currentBase + this.windowSize) >= this.filePackets.size()) {
						currentWindow.clear();
						for (int i = currentBase; i < this.filePackets.size(); i++) {
							byte[] encodedData = filePackets.get(i).encode();
							DatagramPacket filePacket = new DatagramPacket(encodedData, encodedData.length, clientIP, this.clientPort);
							this.sendingSocket.send(filePacket);
							System.out.println("server sent final packet " + filePackets.get(i).getSequanceNumber());
							
							currentWindow.add(filePackets.get(i).getSequanceNumber());
							currentWindowSize++;
						}
						
						this.timer = new Timer(timeOut, (short) 0);
						timer.start();
						this.lastPacketSent = true;
					}
					// if current window is not last window send packets only.
					else if (currentWindowSize == 0 && (currentBase + this.windowSize) < this.filePackets.size()) {
						currentWindow.clear();
						for (int i = currentBase; i < currentBase + this.windowSize; i++) {
							byte[] encodedData = filePackets.get(i).encode();
							DatagramPacket filePacket = new DatagramPacket(encodedData, encodedData.length, clientIP, this.clientPort);
							this.sendingSocket.send(filePacket);
							System.out.println("server sent packet " + filePackets.get(i).getSequanceNumber());
							
							currentWindow.add(filePackets.get(i).getSequanceNumber());
							currentWindowSize++;
						}
						
						this.timer = new Timer(timeOut, (short) 0);
						timer.start();
						currentBase = currentBase + this.windowSize;
					}
					
					// re-send packets starting from 1st packet that is not ACK.
					while(currentWindowSize != 0) {
						// remove ACK packets.
						for (int i = 0; i < this.currentWindow.size(); i++) {
							if (this.recivedACKNumbers.contains(currentWindow.get(i))) {
								currentWindow.remove(i);
								i--;
								currentWindowSize--;
							}
						}
						
						// if not timer finished and all packets not sent.
						if (this.timer.isTimerFinished() && currentWindowSize != 0) {
							int currentWindowLimit;
							
							// calculate new base;
							int newBase = currentWindow.get(0);
							
							// calculate new window limit.
							if (lastPacketSent) {
								currentWindowLimit = this.filePackets.size();
							}
							else {
								if (newBase + this.windowSize >= this.filePackets.size()) {
									currentWindowLimit = this.filePackets.size();
									this.lastPacketSent = true;
								}
								else {
									currentWindowLimit = newBase + this.windowSize;
								}
							}
							
							currentWindowSize = 0;
							currentWindow.clear();
							
							// re-send packets.
							for (int j = newBase; j < currentWindowLimit; j++) {
								byte[] encodedData = filePackets.get(j).encode();
								DatagramPacket filePacket = new DatagramPacket(encodedData, encodedData.length, clientIP, this.clientPort);
								this.sendingSocket.send(filePacket);
								System.out.println("server resent packet " + filePackets.get(j).getSequanceNumber());
								
								currentWindow.add(filePackets.get(j).getSequanceNumber());
								currentWindowSize++;
							}
							
							this.timer = new Timer(timeOut, (short) 0);
							timer.start();
						}
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		public short getClientPort() {
			return clientPort;
		}

		public InetAddress getClientIP() {
			return clientIP;
		}

		public void addAck(short ACKNumber) {
			if (!this.recivedACKNumbers.contains(ACKNumber)) {
				this.recivedACKNumbers.add(ACKNumber);
			}
		}
		
	}
}
