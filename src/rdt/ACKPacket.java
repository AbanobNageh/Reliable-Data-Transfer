package rdt;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
/**
 * this class represents a ACK packet.
 */
public class ACKPacket {
	short checkSum;
	short ackNumber;
	short sourcePort;
	boolean finalACKPacket = false;
	boolean corrupted = false;
	
	/**
	 * Creates an empty ACKPacket, used when decoding a received packet.
	 */
	public ACKPacket() {
	}
	
	/**
	 * creates a new acknowledgment packet.
	 * @param ackNumber the number to give this packet.
	 * @param sourcePort the source port this packet is sent from.
	 */
	public ACKPacket(short ackNumber, short sourcePort) {
		this.ackNumber = ackNumber;
		this.sourcePort = sourcePort;
	}
	
	/**
	 * encodes the data of this packet into an array of bytes, in order 
	 * to send it by using a DatagramSocket.
	 * @return an array of bytes that contains the data of this packet.
	 */
	public byte[] encode() {
		ArrayList<Byte> byteArrayList = new ArrayList<Byte>();
		byte[] ackNumberBytes = this.shortToBytes(this.ackNumber);
		byte[] sourcePortBytes = this.shortToBytes(this.sourcePort);
		byte finalAckPacketByte;
		
		if (this.finalACKPacket) {
			finalAckPacketByte = 1;
			byteArrayList.add(finalAckPacketByte);
		}
		else {
			finalAckPacketByte = 0;
			byteArrayList.add(finalAckPacketByte);
		}
		
		for (byte b: ackNumberBytes) {
			byteArrayList.add(b);
		}
		
		byte[] byteArray = new byte[byteArrayList.size()];
		for (int i = 0; i < byteArrayList.size(); i++) {
			byteArray[i] = byteArrayList.get(i).byteValue();
		}
		
		this.checkSum = this.calculateChecksum(byteArray);
		byte[] checksumBytes = this.shortToBytes(this.checkSum);
		
		for (byte b: checksumBytes) {
			byteArrayList.add(b);
		}
		
		for (byte b: sourcePortBytes) {
			byteArrayList.add(b);
		}
		
		byteArray = new byte[byteArrayList.size()];
		for (int i = 0; i < byteArrayList.size(); i++) {
			byteArray[i] = byteArrayList.get(i).byteValue();
		}
		
		return byteArray;
	}
	
	/**
	 * decodes an array of encoded data back to a packet.
	 * @param data an array of bytes that represent an encoded packet.
	 */
	public void decode(byte data[]) {
		ArrayList<Byte> byteArrayList = new ArrayList<Byte>();
		int currentIndex = 0;
		short calculatedCheckSum = 0;
		byte[] ackNumberBytes = new byte[2];
		byte[] checksumBytes = new byte[2];
		byte[] sourcePortBytes = new byte[2];
		byte finalAckPacketByte;
		
		finalAckPacketByte = data[currentIndex];
		byteArrayList.add(data[currentIndex]);
		if (finalAckPacketByte == 0) {
			this.finalACKPacket = false;
		}
		else {
			this.finalACKPacket = true;
		}
		currentIndex++;
		
		ackNumberBytes[0] = data[currentIndex];
		byteArrayList.add(data[currentIndex]);
		currentIndex++;
		ackNumberBytes[1] = data[currentIndex];
		byteArrayList.add(data[currentIndex]);
		currentIndex++;
		this.ackNumber = this.bytesToShort(ackNumberBytes);
		
		checksumBytes[0] = data[currentIndex];
		currentIndex++;
		checksumBytes[1] = data[currentIndex];
		currentIndex++;
		this.checkSum = this.bytesToShort(checksumBytes);
		
		sourcePortBytes[0] = data[currentIndex];
		currentIndex++;
		sourcePortBytes[1] = data[currentIndex];
		currentIndex++;
		this.sourcePort = this.bytesToShort(sourcePortBytes);
		
		byte[] byteArray = new byte[byteArrayList.size()];
		for (int i = 0; i < byteArrayList.size(); i++) {
			byteArray[i] = byteArrayList.get(i).byteValue();
		}
		calculatedCheckSum = this.calculateChecksum(byteArray);
		
		if (this.checkSum != calculatedCheckSum) {
			this.corrupted = true;
		}
	}
	
	/**
	 * calculates the checksum of an array of bytes.
	 * @param buf the array of bytes.
	 * @return the checksum of these bytes as short.
	 */
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
	
	public boolean isFinalACKPacket() {
		return finalACKPacket;
	}

	public short getSourcePort() {
		return sourcePort;
	}

	public short getAckNumber() {
		return ackNumber;
	}

	public void setFinalACKPacket(boolean finalACKPacket) {
		this.finalACKPacket = finalACKPacket;
	}

	public boolean isCorrupted() {
		return corrupted;
	}
	/**
	 * converts a short data type to bytes.
	 * @param value the short value to be convertd to bytes.
	 * @return the short value as an array of bytes.
	 */
	public byte[] shortToBytes(short value) {
        return ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort(value).array();
    }
    /**
	 * converts a byte array back to short.
	 * @param value the array of bytes to convert to short.
	 * @return the array of bytes as a short data type.
	 */
    public short bytesToShort(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getShort();
    }
}
