package it.polito.se2.counter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import org.json.JSONObject;

public class CounterClient {
	private Socket clientSocket;
	private PrintWriter write;
	private BufferedReader reader;
	private ClientListener listener;
	private CounterGUI frame;
	private int CounterID = -1;
	public static final int PORT_NUMBER = 1500;
	
	public CounterClient(CounterGUI frame, String host, int portNumber) {
		this.frame = frame;
		
		if (!openConnection(host, portNumber)) {
			if (frame != null)
				frame.showPopUp("Unable to connect to the server...\nPlease retry later.\n");
			else
				System.out.println("Unable to connect to the server...\nPlease retry later.\n");
			System.exit(-1);
		}
		
		System.out.println("Connected to server...");
		listener = new ClientListener();
		System.out.println("Starting listener...");
		listener.start();
	}
	
	public int getId() {
		return CounterID;
	}
	
	public void setId(int id) {
		this.CounterID = id;
	}

	private boolean openConnection(String host, int portNumber) {
		try {
			clientSocket = new Socket(host, portNumber);
		} catch (UnknownHostException e) {
			System.err.println("Error: Unknown Host " + host);
			return false;
		} catch (IOException e) {
			System.err.println("Error: Could not open connection to " + host + " on port " + portNumber);
			return false;
		}

		try {
			write = new PrintWriter(clientSocket.getOutputStream(), true);
		} catch (IOException e) {
			System.err.println("Error: Could not open output stream");
			return false;
		}

		try {
			reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		} catch (IOException e) {
			System.err.println("Error: Could not open input stream");
			return false;
		}
		return true;
	}

	public void serveNext() {	
		if(CounterID == -1) {
			if (frame != null)
				frame.showPopUp("Please configure the counter!");
			else 
				System.out.println("Please configure the counter!");
			return;
		}
		JSONObject obj = new JSONObject();
		obj.put("operation", "serve_next");
		obj.put("id", CounterID);
		
		System.out.println("Sending json to server: " + obj);
		write.println(obj);
	}

	public void setReqTypeToCounter(String[] reqTypes) {
		JSONObject obj = new JSONObject();
		JSONObject content = new JSONObject();

		obj.put("operation", "counter_setup");
		
		if(!reqTypes[0].isEmpty() && !reqTypes[1].isEmpty())
			content.put("request_type", reqTypes[0] + "/" + reqTypes[1]);
		else if(!reqTypes[0].isEmpty())
			content.put("request_type", reqTypes[0]);
		else 
			content.put("request_type", reqTypes[1]);
		
		content.put("id", CounterID);
		obj.put("content", content);

		System.out.println("Sending json to server: " + obj);
		write.println(obj);
	}
	
	public void shutdown() {
		if(CounterID != -1) {
			JSONObject obj = new JSONObject();
			
			obj.put("operation", "goodbye");
			obj.put("counterID", CounterID);
			
			write.println(obj);
		}
	}
	
	
	class ClientListener extends Thread	{
		public void run() {
			while (read());	
			System.out.println("Server closed the connection");
//			System.exit(-1);
		}

		private boolean read()	{
			try	{
				String msg = reader.readLine();
				if (msg == null) // server closed connenction
					return false;
				System.out.println("Received from server:" +msg);
				JSONObject obj = new JSONObject(msg);
				
				String operation = obj.getString("operation");
				
				switch(operation) {
					case "setup_response":
						int id = obj.getInt("id");
						CounterID = id;
					break;
					case "serve":
						String ticketID = obj.getString("ticketID");
						if (frame != null)
							frame.servingTicket(ticketID);
					break;
					default:
						break;
				}
				
			} catch (IOException e)	{
				e.printStackTrace();
			}
			return true;
		}
	}
}