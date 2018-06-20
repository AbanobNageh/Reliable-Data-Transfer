package huffman;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.PriorityQueue;

public class HuffmanCompressor {
	int[] frequencyTable = new int[256];
	PriorityQueue<HuffmanNode> nodes = new PriorityQueue<HuffmanNode>();
	String[] codeTable = new String[256];
	
	public void compressAndDecompressFile(String filePath) {
		long startTime = System.currentTimeMillis();
		File sourceFile = new File (filePath);
		String fileName = sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf("."));
		this.compressFile(filePath);
		this.deCommpressFile(fileName + "-compressed.txt");
		long endTime = System.currentTimeMillis();
		System.out.println("time taken:" + (endTime - startTime) + " ms");
	}
	
	public void compressAndDecompressFolder(String folderPath) {
		long startTime = System.currentTimeMillis();
		File sourceFolder = new File (folderPath);
		String FolderName = sourceFolder.getName();
		this.compressFolder(folderPath);
		this.deCompressFolder(FolderName + "-compressed.txt");
		long endTime = System.currentTimeMillis();
		System.out.println("time taken:" + (endTime - startTime) + " ms");
	}
	
	private void buildFrequencyTable(int[] textBytes) {
		for (int i = 0; i < textBytes.length; i++) {
			if (textBytes[i] == 1111) {
				continue;
			}
			frequencyTable[textBytes[i]]++;
		}
		
		for (int i = 0; i < this.frequencyTable.length; i++) {
			if (frequencyTable[i] > 0) {
				HuffmanNode node = new HuffmanNode(((char) i) + "", frequencyTable[i]);
				this.nodes.add(node);
			}
		}
	}
	
	private HuffmanNode buildHuffmanTree() {
		int nodeCount = this.nodes.size();
		HuffmanNode left, right;
		
		while (this.nodes.size() > 1) {
			left = this.nodes.poll();
			right = this.nodes.poll();
			HuffmanNode parent = new HuffmanNode("000", left.getFrequency() + right.getFrequency());
			parent.setLeft(left);
			parent.setRight(right);
			this.nodes.add(parent);
		}
		
		return this.nodes.poll();
	}
	
	public void compressFile(String filePath) {
		for (int i = 0; i < this.codeTable.length; i++) {
			this.codeTable[i] = "";
			this.frequencyTable[i] = 0;
		}
		
		int[] fileBytes = this.readFile(filePath);
		buildFrequencyTable(fileBytes);
		HuffmanNode root = buildHuffmanTree();
		generateCodeTable(root, "");
		printCompressedFile(filePath, fileBytes);
	}
	
	private void compressFolder (String folderPath) {
		File folder = new File(folderPath);
		int[] fileBytes = readFiles(folder);
		buildFrequencyTable(fileBytes);
		HuffmanNode root = buildHuffmanTree();
		generateCodeTable(root, "");
		printCompressedFolder(folder, fileBytes);
	}
	
	public void deCommpressFile(String filePath) {
		String[] compressedFileData;
		
		for (int i = 0; i < this.codeTable.length; i++) {
			this.codeTable[i] = "";
			this.frequencyTable[i] = 0;
		}
		
		compressedFileData = this.readCompressedFile(filePath);
		HuffmanNode root = buildHuffmanTree();
		printDecompressedFile(compressedFileData[0], compressedFileData);
	}
	
	private void deCompressFolder(String folderPath) {
		String[] compressedFileData;
		
		for (int i = 0; i < this.codeTable.length; i++) {
			this.codeTable[i] = "";
			this.frequencyTable[i] = 0;
		}
		
		compressedFileData = this.readCompressedFolder(folderPath);
		HuffmanNode root = buildHuffmanTree();
		printDecompressedFolder(compressedFileData[0], compressedFileData);
		
	}
	
	private void generateCodeTable(HuffmanNode node, String code) {
		if (node == null) {
			return;
		}
		
		if (!node.isLeaf()) {
			generateCodeTable(node.getLeft(), code + "0");
			generateCodeTable(node.getRight(), code + "1");
		}
		else {
			if (!node.getCharacter().equals("000")) {
				this.codeTable[node.getCharacter().charAt(0)] = code;
			}
		}
	}
	
	private int getCharacterIndex(String code) {
		for (int i = 0; i < this.codeTable.length; i++){
			if (codeTable[i].equals(code)) {
				return i;
			}
		}
		
		return -1;
	}
	
	private void printCompressedFile(String filePath, int[] fileBytes) {
		try {
			File sourceFile = new File(filePath);
			String fileName = sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf("."));
			String fileFormat = sourceFile.getName().substring(sourceFile.getName().lastIndexOf("."));
			
			File File = new File(fileName + "-compressed.txt");
			FileWriter writer = new FileWriter(File);
			BufferedWriter bufferedWriter = new BufferedWriter(writer);
			
			bufferedWriter.write(fileName);
			bufferedWriter.newLine();
			bufferedWriter.write(fileFormat);
			bufferedWriter.newLine();
			
			for (int i = 0; i < this.frequencyTable.length; i++) {
				if (this.frequencyTable[i] == 0) {
					continue;
				}
				bufferedWriter.write(i + " " + this.frequencyTable[i] + " " + this.codeTable[i]);
				bufferedWriter.newLine();
			}
			bufferedWriter.write("--");
			bufferedWriter.newLine();
			bufferedWriter.close();
			
			FileOutputStream writer2 = new FileOutputStream(File, true);
			BufferedOutputStream bufferedWriter2 = new BufferedOutputStream(writer2);
			
			int index = 0;
			String code = this.codeTable[fileBytes[index]];
			while(index < fileBytes.length) {
				if (code.length() < 8 && index + 1 == fileBytes.length) {
					if (code.length() > 0) {
						code = "**" + code;
						for (char tempChar: code.toCharArray()) {
							bufferedWriter2.write(tempChar);
						}
						bufferedWriter2.close();
						break;
					}
					else {
						break;
					}
				}
				
				if (code.length() < 8) {
					index++;
					code = code + this.codeTable[fileBytes[index]];
				}
				else if (code.length() == 8) {
					int integerCode = Integer.parseInt(code, 2);
					code = "";
					bufferedWriter2.write((char) integerCode);
				}
				else {
					int integerCode = Integer.parseInt(code.substring(0, 8), 2);
					code = code.substring(8);
					bufferedWriter2.write((char) integerCode);;
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	private void printCompressedFolder(File sourceFolder, int[] fileBytes) {
		int currentIndex = 0;
		int filesCount = sourceFolder.listFiles().length;
		try {
			String folderName = sourceFolder.getName();
			
			File File = new File(folderName + "-compressed.txt");
			FileWriter writer = new FileWriter(File);
			BufferedWriter bufferedWriter = new BufferedWriter(writer);
			
			bufferedWriter.write(folderName);
			bufferedWriter.newLine();
			bufferedWriter.write(Integer.toString(filesCount));
			bufferedWriter.newLine();
			
			for (int i = 0; i < this.frequencyTable.length; i++) {
				if (this.frequencyTable[i] == 0) {
					continue;
				}
				bufferedWriter.write(i + " " + this.frequencyTable[i] + " " + this.codeTable[i]);
				bufferedWriter.newLine();
			}
			bufferedWriter.write("==");
			bufferedWriter.newLine();
			bufferedWriter.close();
			
			for (File subFile: sourceFolder.listFiles()) {
				String fileName = subFile.getName().substring(0, subFile.getName().lastIndexOf("."));
				String fileFormat = subFile.getName().substring(subFile.getName().lastIndexOf("."));
				
				FileWriter fileWriter = new FileWriter(File, true);
				BufferedWriter bufferedFileWriter = new BufferedWriter(fileWriter);
				
				bufferedFileWriter.write(fileName);
				bufferedFileWriter.newLine();
				bufferedFileWriter.write(fileFormat);
				bufferedFileWriter.newLine();
				bufferedFileWriter.close();
				
				FileOutputStream writer2 = new FileOutputStream(File, true);
				BufferedOutputStream bufferedWriter2 = new BufferedOutputStream(writer2);
				
				String code = this.codeTable[fileBytes[currentIndex]];
				while(currentIndex < fileBytes.length) {
					if (code.length() < 8 && fileBytes[currentIndex + 1] == 1111) {
						if (code.length() > 0) {
							code = "**" + code + "\n";
							for (char tempChar: code.toCharArray()) {
								bufferedWriter2.write(tempChar);
							}
							code = "==" + "\r" + "\n";
							for (char tempChar: code.toCharArray()) {
								bufferedWriter2.write(tempChar);
							}
							bufferedWriter2.close();
							currentIndex = currentIndex + 2;
							break;
						}
						else {
							bufferedWriter2.close();
							currentIndex = currentIndex + 2;
							break;
						}
					}
					
					if (code.length() < 8) {
						currentIndex++;
						code = code + this.codeTable[fileBytes[currentIndex]];
					}
					else if (code.length() == 8) {
						int integerCode = Integer.parseInt(code, 2);
						code = "";
						bufferedWriter2.write((char) integerCode);
					}
					else {
						int integerCode = Integer.parseInt(code.substring(0, 8), 2);
						code = code.substring(8);
						bufferedWriter2.write((char) integerCode);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void printDecompressedFile(String fullFileName, String[] compressedFileData) {
		try {
			String temp = "";
			int index;
			
			String fileName = fullFileName.substring(0, fullFileName.lastIndexOf("."));
			String fileFormat = fullFileName.substring(fullFileName.lastIndexOf("."));
			
			File File = new File(fileName + "-decompressed" + fileFormat);
			FileOutputStream writer = new FileOutputStream(File);
			BufferedOutputStream bufferedWriter = new BufferedOutputStream(writer);
			for (int i = 0; i < compressedFileData[1].length(); i++) {
				temp = temp +  compressedFileData[1].charAt(i);
				
				if ((index = this.getCharacterIndex(temp)) != -1) {
					bufferedWriter.write((char) index);
					temp = "";
				}
			}
			bufferedWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void printDecompressedFolder(String folderName, String[] compressedFolderData) {
		folderName = folderName + "-decompressed";
		File folder = new File(folderName);
		folder.mkdir();
		
		String[] fileName = compressedFolderData[1].split(",");
		String[] fileData = compressedFolderData[2].split(",");
		int filesCount = fileName.length;
		
		for (int i = 0; i < filesCount; i++) {
			try {
				String temp = "";
				int index;
				
				File File = new File(folderName + "\\" + fileName[i]);
				FileOutputStream writer = new FileOutputStream(File);
				BufferedOutputStream bufferedWriter = new BufferedOutputStream(writer);
				for (int j = 0; j < fileData[i].length(); j++) {
					temp = temp +  fileData[i].charAt(j);
					
					if ((index = this.getCharacterIndex(temp)) != -1) {
						bufferedWriter.write((char) index);
						temp = "";
					}
				}
				
				bufferedWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private String[] readCompressedFile(String filePath) {
		int startIndex = 0, endIndex = 0;
		String encodedText = "";
		String extraCode = "";
		StringBuffer buffer = new StringBuffer();
		String[] result = new String[3];
		CharBuffer charBuffer = null;
		
		try {
			File file = new File(filePath);
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			
			String fileName = bufferedReader.readLine();
			String fileFormat = bufferedReader.readLine();
			fileName = fileName + fileFormat;
			result[0] = fileName;
			
			String line;
			while (!(line = bufferedReader.readLine()).equals("--")) {
				String[] tokens = line.split(" ");
				int index = Integer.parseInt(tokens[0]);
				this.frequencyTable[index] = Integer.parseInt(tokens[1]);
				this.codeTable[index] = tokens[2];
			}
			
			bufferedReader.close();
			
			FileChannel channel = new FileInputStream(filePath).getChannel();
			MappedByteBuffer mappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
			if (mappedByteBuffer != null) {
		        charBuffer = Charset.forName("ISO-8859-1").decode(mappedByteBuffer);
		    }
			
			for (int i = 0; i < charBuffer.length(); i++) {
				if (charBuffer.charAt(i) == '-') {
					startIndex = i + 4;
					break;
				}
			}
			
			for (int i = charBuffer.length() - 1; i > 0; i--) {
				if (charBuffer.charAt(i) == '*') {
					endIndex = i - 2;
					break;
				}
				else {
					extraCode = charBuffer.charAt(i) + extraCode;
				}
			}
			
			channel.close();
			

			String tempCode = "";
			for (int i = startIndex; i <= endIndex; i++) {
				tempCode = Integer.toBinaryString(charBuffer.charAt(i));
				while (tempCode.length() < 8) {
					tempCode = "0" + tempCode;
				}
				buffer.append(tempCode);
			}
			
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		encodedText = buffer.toString();
		encodedText = encodedText + extraCode;
		result[1] = encodedText;
		return result;
	}
	
	private String[] readCompressedFolder(String filePath) {
		int startIndex = 0, endIndex = 0;
		String extraCode = "";
		StringBuffer buffer = new StringBuffer();
		StringBuffer tempBuffer = new StringBuffer();
		String[] result = new String[3];
		CharBuffer charBuffer = null;
		
		try {
			File file = new File(filePath);
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			
			String folderName = bufferedReader.readLine();
			int filesCount = Integer.parseInt(bufferedReader.readLine());
			result[0] = folderName;
			
			String line;
			while (!(line = bufferedReader.readLine()).equals("==")) {
				String[] tokens = line.split(" ");
				int index = Integer.parseInt(tokens[0]);
				this.frequencyTable[index] = Integer.parseInt(tokens[1]);
				this.codeTable[index] = tokens[2];
			}
			
			bufferedReader.close();
			
			FileChannel channel = new FileInputStream(filePath).getChannel();
			MappedByteBuffer mappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
			if (mappedByteBuffer != null) {
		        charBuffer = Charset.forName("ISO-8859-1").decode(mappedByteBuffer);
		    }
			channel.close();
			
			for (int i = 0; i < filesCount; i++) {
				for (int j = startIndex; j < charBuffer.length(); j++) {
					if (charBuffer.get(j) == '=' && charBuffer.get(j + 1) == '=') {
						startIndex = j + 4;
						break;
					}
				}
				
				while (charBuffer.get(startIndex) != '\r') {
					tempBuffer.append(charBuffer.get(startIndex));
					startIndex++;
				}
				startIndex = startIndex + 2;
				while (charBuffer.get(startIndex) != '\r') {
					tempBuffer.append(charBuffer.get(startIndex));
					startIndex++;
				}
				startIndex = startIndex + 2;
				
				if (i + 1 != filesCount) {
					tempBuffer.append(',');
				}
				
				endIndex = endIndex + 3;
				for (int j = endIndex; j < charBuffer.length(); j++) {
					if (charBuffer.get(j) == '*' && charBuffer.get(j + 1) == '*') {
						endIndex = j - 1;
						break;
					}
				}
				
				int extraCodeIndex = endIndex + 3;
				while (charBuffer.get(extraCodeIndex) == '0' || charBuffer.get(extraCodeIndex) == '1') {
					extraCode = charBuffer.charAt(extraCodeIndex) + extraCode;
					extraCodeIndex++;
				}
				
				String tempCode = "";
				for (int j = startIndex; j <= endIndex; j++) {
					tempCode = Integer.toBinaryString(charBuffer.charAt(j));
					while (tempCode.length() < 8) {
						tempCode = "0" + tempCode;
					}
					buffer.append(tempCode);
				}
				
				buffer.append(extraCode);
				if (i + 1 != filesCount) {
					buffer.append(',');
				}
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		result[1] = tempBuffer.toString();
		result[2] = buffer.toString();
		return result;
	}
	
	private int[] readFile(String filePath) {
		int[] unsignedBytes = null;
		try {
			File file = new File(filePath);
			byte[] byteArray = Files.readAllBytes(file.toPath());
			unsignedBytes = new int[byteArray.length];
			for (int i = 0; i < byteArray.length; i++) {
				unsignedBytes[i] = byteArray[i] & 0xff;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return unsignedBytes;
	}
	
	private int[] readFiles(File sourceFolder) {
		ArrayList<Integer> byteList = new ArrayList<Integer>();
		int[] unsignedBytes = null;
		try {
			for (File file: sourceFolder.listFiles()) {
				byte[] byteArray = Files.readAllBytes(file.toPath());
				for (int i = 0; i < byteArray.length; i++) {
					byteList.add(byteArray[i] & 0xff);
				}
				byteList.add(1111);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		int[] intArray = new int[byteList.size()];
		for (int i = 0; i < byteList.size(); i++) {
			intArray[i] = byteList.get(i).intValue();
		}
		
		return intArray;
	}
}

