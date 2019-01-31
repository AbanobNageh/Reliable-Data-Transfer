package rdt;

public class Main {
	private static Boolean readFromFile = true;

	public static void main(String[] args) {
		try {
			Server server = null;
			Client client = null;

			if (!readFromFile){
				server = new Server("127.0.0.1", (short) 5000, 5);
				server.setProtocol(3);
				client = new Client("127.0.0.1", (short) 5000, "127.0.0.1", (short) 6000, "input.txt");
				client.setLossProbability(0);
				client.setCorruptProbability(0);	
			}
			else{
				server = new Server();
				client = new Client();
			}
			long startTime = System.currentTimeMillis();
			
			server.start();
			client.start();
			server.join();
			client.join();

			long endTime = System.currentTimeMillis();
			System.out.println("time taken:" + (endTime - startTime) + " ms");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
