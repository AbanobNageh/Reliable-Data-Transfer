package rdt;

public class Main {

	public static void main(String[] args) {
		try {
			long startTime = System.currentTimeMillis();
			
			/* uncomment this part (and comment the 2nd part) if you want to create clients and servers here
			// creates clients and servers directly.
			Server server = new Server("127.0.0.1", (short) 5000, 5);
			server.setProtocol(3);
			Client client = new Client("127.0.0.1", (short) 5000, "127.0.0.1", (short) 6000, "input.txt");
			client.setLossProbability(0);
			client.setCorruptProbability(0);
			 */
			
			// 2nd part: reads the client and server data from file.
			Server server = new Server();
			Client client = new Client();
			
			// don't comment this part.
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
