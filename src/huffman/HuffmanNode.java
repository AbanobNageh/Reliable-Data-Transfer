package huffman;

public class HuffmanNode implements Comparable<HuffmanNode>{
	private String character;
	private int frequency;
	private HuffmanNode left, right;
	
	public HuffmanNode(String character, int frequency) {
		this.character = character;
		this.frequency = frequency;
		this.left = null;
		this.right = null;
	}
	
	public int compareTo(HuffmanNode that) {
		return this.frequency - that.frequency;
	}
	
	public String getCharacter() {
		return character;
	}

	public int getFrequency() {
		return frequency;
	}

	public HuffmanNode getLeft() {
		return left;
	}

	public HuffmanNode getRight() {
		return right;
	}

	public Boolean isLeaf() {
		if (left == null && right == null) {
			return true;
		}
		
		return false;
	}

	public void setLeft(HuffmanNode left) {
		this.left = left;
	}

	public void setRight(HuffmanNode right) {
		this.right = right;
	}
	
}
