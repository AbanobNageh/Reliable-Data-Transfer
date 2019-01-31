package rdt;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;

import huffman.HuffmanCompressor;
/**
 * this class is responsible for reading files and preprocessing them before they are
 * sent by the server.
 * 
 */
public class FileHandler {
	private static FileHandler instance = null;
	HuffmanCompressor huffman = new HuffmanCompressor();
	int packetSize = 1000;
	
	/**
	 * return an instance of this singleton class.
	 */
	public static FileHandler getFileHandler() {
		if (instance == null) {
			instance = new FileHandler();
		}
		return instance;
	}
	
	/**
	 * reads a file, compresses it and then split this file into an array of TCPPackets.
	 * this method is synchronized.
	 * @param filePath the path to the input file.
	 * @param sourcePort the port that the packet will be sent from, must be set in the packet.
	 * @param destinationPort the port that the packet will be sent to, must be set in the packet.
	 * @return an arraylist of TCPPackets that the file has been divided into.
	 */
	public synchronized ArrayList<TCPPacket> getFilePackets(String filePath, short sourcePort, short destinationPort) {
		byte[] byteArray = null;
		byte[] tempArray = new byte[packetSize];
		ArrayList<TCPPacket> filePackets = new ArrayList<TCPPacket>();
		short sequanceNumber = 0;
		int index = 0;
		
		try {
			File sourceFile = new File (filePath);
			String fileName = sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf("."));
			huffman.compressFile(filePath);
			
			File compressedFile = new File (fileName + "-compressed.txt");
			byteArray = Files.readAllBytes(compressedFile.toPath());
			System.out.println("number of bytes in file " + byteArray.length);
			
			if (byteArray.length < packetSize) {
				tempArray = new byte[byteArray.length];
				for (int i = 0; i < byteArray.length; i++) {
					tempArray[i] = byteArray[i];
				}
				TCPPacket packet = new TCPPacket(sourcePort, destinationPort, sequanceNumber, tempArray);
				filePackets.add(packet);
			}
			else {
				for (int i = 0; i <= byteArray.length; i++) {
					if (index == packetSize || i == byteArray.length) {
						index = 0;
						TCPPacket packet = new TCPPacket(sourcePort, destinationPort, sequanceNumber, tempArray);
						filePackets.add(packet);
						sequanceNumber++;

						if (byteArray.length - i < packetSize) {
							tempArray = new byte[byteArray.length - i];
						}
						else {
							tempArray = new byte[packetSize];
						}

						if (i == byteArray.length) {
							break;
						}
					}

					tempArray[index] = byteArray[i];
					index++;
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("number of packets " + filePackets.size());
		filePackets.get(filePackets.size() - 1).setFinalPacket(true);
		return filePackets;
	}
}
