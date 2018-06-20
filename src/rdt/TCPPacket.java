package rdt;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class TCPPacket implements Comparable<TCPPacket>{
	short checkSum;
	short sourcePort;
	short destinationPort;
	short lenght;
	short sequanceNumber;
	String fileName;
	boolean finalPacket = false;
	boolean corrupted = false;
	boolean fileData = false;
	byte[] data;
	
	public TCPPacket() {
		
	}

	public TCPPacket(short sourcePort, short destinationPort, short sequanceNumber, byte[] data) {
		this.sourcePort = sourcePort;
		this.destinationPort = destinationPort;
		this.sequanceNumber = sequanceNumber;
		this.data = data;
		this.lenght = (short) (12 + data.length);
	}
	
	// This function encodes the data in this packet into an array of bytes
	// in order to place it inside a DatagramSocket.
	public byte[] encode() {
		ArrayList<Byte> byteArrayList = new ArrayList<Byte>();
		byte[] sourcePortBytes = this.shortToBytes(this.sourcePort);
		byte[] destinationPortBytes = this.shortToBytes(this.destinationPort);
		byte[] lengthBytes = this.shortToBytes(this.lenght);
		byte[] sequanceNumberBytes = this.shortToBytes(this.sequanceNumber);
		byte finalPacketByte;
		byte fileDataByte;
		
		for (byte b: sourcePortBytes) {
			byteArrayList.add(b);
		}
		
		for (byte b: destinationPortBytes) {
			byteArrayList.add(b);
		}
		
		for (byte b: lengthBytes) {
			byteArrayList.add(b);
		}
		
		for (byte b: this.data) {
			byteArrayList.add(b);
		}
		
		byte[] byteArray = new byte[byteArrayList.size()];
		for (int i = 0; i < byteArrayList.size(); i++) {
			byteArray[i] = byteArrayList.get(i).byteValue();
		}
		
		this.checkSum = this.calculateChecksum(byteArray);
		byte[] checksumBytes = this.shortToBytes(this.checkSum);
		
		byteArrayList.clear();
		
		if (this.finalPacket) {
			finalPacketByte = 1;
			byteArrayList.add(finalPacketByte);
		}
		else {
			finalPacketByte = 0;
			byteArrayList.add(finalPacketByte);
		}
		
		if (this.fileData) {
			fileDataByte = 1;
			byteArrayList.add(fileDataByte);
		}
		else {
			fileDataByte = 0;
			byteArrayList.add(fileDataByte);
		}
		
		for (byte b: sourcePortBytes) {
			byteArrayList.add(b);
		}
		
		for (byte b: destinationPortBytes) {
			byteArrayList.add(b);
		}
		
		for (byte b: lengthBytes) {
			byteArrayList.add(b);
		}
		
		for (byte b: checksumBytes) {
			byteArrayList.add(b);
		}
		
		for (byte b: sequanceNumberBytes) {
			byteArrayList.add(b);
		}
		
		for (byte b: this.data) {
			byteArrayList.add(b);
		}
		
		byteArray = new byte[byteArrayList.size()];
		for (int i = 0; i < byteArrayList.size(); i++) {
			byteArray[i] = byteArrayList.get(i).byteValue();
		}
		
		return byteArray;
	}
	
	// Extracts the data inside the bytes array back.
	public void decode(byte data[]) {
		ArrayList<Byte> byteArrayList = new ArrayList<Byte>();
		int currentIndex = 0;
		short calculatedCheckSum = 0;
		byte[] sourcePortBytes = new byte[2];
		byte[] destinationPortBytes = new byte[2];
		byte[] lengthBytes = new byte[2];
		byte[] checksumBytes = new byte[2];
		byte[] sequanceNumberBytes = new byte[2];
		byte finalPacketByte;
		byte fileDataByte;
		
		finalPacketByte = data[currentIndex];
		if (finalPacketByte == 0) {
			this.finalPacket = false;
		}
		else {
			this.finalPacket = true;
		}
		currentIndex++;
		
		fileDataByte = data[currentIndex];
		if (fileDataByte == 0) {
			this.fileData = false;
		}
		else {
			this.fileData = true;
		}
		currentIndex++;
		
		sourcePortBytes[0] = data[currentIndex];
		byteArrayList.add(sourcePortBytes[0]);
		currentIndex++;
		sourcePortBytes[1] = data[currentIndex];
		byteArrayList.add(sourcePortBytes[1]);
		currentIndex++;
		this.sourcePort = this.bytesToShort(sourcePortBytes);
		
		destinationPortBytes[0] = data[currentIndex];
		byteArrayList.add(destinationPortBytes[0]);
		currentIndex++;
		destinationPortBytes[1] = data[currentIndex];
		byteArrayList.add(destinationPortBytes[1]);
		currentIndex++;
		this.destinationPort = this.bytesToShort(destinationPortBytes);
		
		lengthBytes[0] = data[currentIndex];
		byteArrayList.add(lengthBytes[0]);
		currentIndex++;
		lengthBytes[1] = data[currentIndex];
		byteArrayList.add(lengthBytes[1]);
		currentIndex++;
		this.lenght = this.bytesToShort(lengthBytes);
		
		checksumBytes[0] = data[currentIndex];
		currentIndex++;
		checksumBytes[1] = data[currentIndex];
		currentIndex++;
		this.checkSum = this.bytesToShort(checksumBytes);
		
		sequanceNumberBytes[0] = data[currentIndex];
		currentIndex++;
		sequanceNumberBytes[1] = data[currentIndex];
		currentIndex++;
		this.sequanceNumber = this.bytesToShort(sequanceNumberBytes);
		
		int dataSize = this.lenght - 12;
		this.data = new byte[dataSize];
		
		for (int i = 0; i < dataSize; i++) {
			this.data[i] = data[currentIndex];
			byteArrayList.add(data[currentIndex]);
			currentIndex++;
		}
		
		byte[] byteArray = new byte[byteArrayList.size()];
		for (int i = 0; i < byteArrayList.size(); i++) {
			byteArray[i] = byteArrayList.get(i).byteValue();
		}
		
		calculatedCheckSum = this.calculateChecksum(byteArray);
		if (this.checkSum != calculatedCheckSum) {
			this.corrupted = true;
		}
		
		if (this.fileData) {
			this.fileName = new String(this.data);
		}
	}
	
	// Calculate checksum.
	public short calculateChecksum(byte[] buf) {
	    int length = buf.length;
	    int i = 0;

	    long sum = 0;
	    long data;

	    while (length > 1) {
	      data = (((buf[i] << 8) & 0xFF00) | ((buf[i + 1]) & 0xFF));
	      sum += data;
	      if ((sum & 0xFFFF0000) > 0) {
	        sum = sum & 0xFFFF;
	        sum += 1;
	      }

	      i += 2;
	      length -= 2;
	    }

	    if (length > 0) {
	      sum += (buf[i] << 8 & 0xFF00);
	      if ((sum & 0xFFFF0000) > 0) {
	        sum = sum & 0xFFFF;
	        sum += 1;
	      }
	    }

	    sum = ~sum;
	    sum = sum & 0xFFFF;
	    return (short) sum;
	}
	
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
		this.fileData = true;
	}

	public short getSourcePort() {
		return sourcePort;
	}

	public short getDestinationPort() {
		return destinationPort;
	}

	public boolean isFileData() {
		return fileData;
	}

	public void setCheckSum(short checkSum) {
		this.checkSum = checkSum;
		this.corrupted = true;
	}

	public short getSequanceNumber() {
		return sequanceNumber;
	}

	public byte[] getData() {
		return data;
	}

	public boolean isCorrupted() {
		return corrupted;
	}

	public boolean isFinalPacket() {
		return finalPacket;
	}

	public void setFinalPacket(boolean finalPacket) {
		this.finalPacket = finalPacket;
	}

    public byte[] shortToBytes(short value) {
        return ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort(value).array();
    }
    
    public short bytesToShort(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getShort();
    }

	public int compareTo(TCPPacket that) {
		return this.sequanceNumber - that.sequanceNumber;
	}
}
