
import java.net.*; // imports networking related classes (Socket, ServerSocket)
import java.util.ArrayList;
import java.util.Scanner;

import java.io.*; // imports classes related to performing IO.

/*
File Name: Lab1.java
Description:
			This is client application that will connect with ServerA,
			and receive composite directory listing of Server A and 
			Server B in sorted order baed on file name and print the 
			same. The Directory listing will include last modified date,
			 file size in byte, and file name.

*/

public class Lab3 implements Runnable {
	Socket socketForClient; // Object to create client socket
	DataInputStream readerObject; // Object to read data from socket stream
	DataOutputStream writerObject;
	public boolean terminateRun;
	Thread thread;
	ArrayList<String> fileList, lockFileList;

	// Constructor method to initialize the objects to null.
	public Lab3() {
		thread = new Thread(this);
		terminateRun = false;
		socketForClient = null;
		readerObject = null;
		fileList = new ArrayList<>();
		lockFileList = new ArrayList<>();
	}

	// Method has server address and port number as input parameters required for
	// establishing connection with Server A.
	public void connectServer(String serverAddress, int portNumber, boolean LOCK) {
		try {
			socketForClient = new Socket(serverAddress, portNumber); // creating an instance of Socket class which is
																		// connected to Server A
			if (socketForClient == null) { // check if connection was established successfully or not
				System.out.println("failure in establishing connection");
			} else {
				// create a byte stream object connected to socket stream to receive data from
				// Server A.
				readerObject = new DataInputStream(socketForClient.getInputStream());
				writerObject = new DataOutputStream(socketForClient.getOutputStream());
				System.out.println("Connection successfully establised to server A");
				String data = "";

				System.out.printf("\n\n%-35s%-15s%-40s\n", "Last Modifies", "Size", "Name");
				System.out.println(
						"-------------------------------------------------------------------------------------");
				// the while loop runs until it receives "END" from Server A which marks end of
				// data transfer.
				int i = 0;
				while (!data.equals("END")) {
					data = readerObject.readUTF(); // read string from socket
					if (data.equals("END"))
						break; // check if the string denotes end of data transfer, if Yes break out of loop
					System.out.format("index[" + i + "]: %-35s", data); // print the last modified date with formatting

					data = readerObject.readUTF(); // read string from socket
					if (data.equals("END"))
						break; // check if the string denotes end of data transfer, if Yes break out of loop
					System.out.format("%-25s", data); // print the Size of file with formatting

					data = readerObject.readUTF(); // read string from socket
					if (data.equals("END"))
						break; // check if the string denotes end of data transfer, if Yes break out of loop
					System.out.format("%-40s", data); // print the Size of file with formatting

					if (lockFileList.contains(data))
						System.out.print("<locked>\n");
					else
						System.out.println();
					fileList.add(data);
					i++;
				}

				if (LOCK) {
					// logic to lock a file on Server to stop sync.
					Scanner in = new Scanner(System.in);
					System.out.println("1.Lock\n2.Unlock\nPlease provide your choice");
					int ch = Integer.parseInt(in.next());
					if (ch == 1) {
						System.out.println("Please provide the file index to be locked");
						int indexPosition = Integer.parseInt(in.next());
						if (indexPosition < 0 || indexPosition >= i) {
							System.out.println("wrong index!!!!!");
							writerObject.writeUTF("NA");
							writerObject.flush();
						} else {
							writerObject.writeUTF("LOCK");
							writerObject.flush();
							lockFileList.add(fileList.get(indexPosition));
							writerObject.writeUTF(fileList.get(indexPosition));
							writerObject.flush();
						}

					} else if (ch == 2) {
						System.out.println("Please provide the file index to be unlocked");
						int indexPosition = Integer.parseInt(in.next());
						if (indexPosition < 0 || indexPosition >= i) {
							System.out.println("wrong index!!!!!");
							writerObject.writeUTF("NA");
							writerObject.flush();
						} else {
							writerObject.writeUTF("UNLOCK");
							writerObject.flush();
							lockFileList.remove(fileList.get(indexPosition));
							writerObject.writeUTF(fileList.get(indexPosition));
							writerObject.flush();
						}
					} else {
						System.out.println("wrong choice!!!!!");
						writerObject.writeUTF("NA");
						writerObject.flush();
					}

				} else {
					writerObject.writeUTF("NA");
					writerObject.flush();
				}
			}
			socketForClient.close(); // close the socket connection
			readerObject.close(); // close the reader object connected to stream.
			writerObject.close();
		} catch (UnknownHostException u) {
			System.out.println("failure establishing connection");
			System.out.println(u);
		} catch (ConnectException e) {
			System.out.println("Server currently unavailable: failure establishing connection");
		} catch (IOException i) {
			System.out.println(i);
		} finally { // finally block to ensure connection is closed in situation of an unknown
					// exception
			try {
				if (socketForClient.isConnected()) {
					socketForClient.close(); // close the socket connection
				}
				if (readerObject != null) {
					readerObject.close(); // close the reader object connected to stream.
				}
			} catch (Exception e) {
				System.out.println("exception in closing resources details below \n" + e);
			}
		}
	}

	void startReadingLog() {
		thread.start();
	}

	// thread to read logs from Server
	@Override
	public void run() {
		// TODO Auto-generated method stub
		Socket socketForlog = null;
		try {
			socketForlog = new Socket("localhost", 6663);
			DataInputStream readerObject = new DataInputStream(socketForlog.getInputStream());
			for (;;) {
				while (terminateRun != true) {
					System.out.println("Log : " + (String) readerObject.readUTF());
				}
			}
		} catch (Exception e) {
			System.out.println("auto log: error in setting connection");
		} finally {
			try {
				if (socketForlog.isConnected())
					socketForlog.close();
			} catch (Exception e) {
				System.out.println("auto log: error closing connection");
			}
		}
	}

	public static void main(String args[]) {
		int choice;
		Lab3 clientObject = new Lab3(); // creating an instance of class Lab1.
		clientObject.startReadingLog();
		Scanner in = new Scanner(System.in);
		System.out.println(
				"This is the client program which read server directory listing and prints it in ascending order based on File name");
		do {
			System.out.println("1. Print List\n2. Lock/Unlock File\n3. Exit\nGive your choice");
			// System.out.println("Press Y/y to read file list from server, E to
			// terminate");
			choice = Integer.parseInt(in.next());
			switch (choice) {
				case 1:
					clientObject.connectServer("localhost", 7771, false);
					break;
				case 2:
					clientObject.connectServer("localhost", 7771, true);
					break;
				case 3:
					clientObject.terminateRun = true;
					System.exit(0);
					break;
				default:
					System.out.println("Wrong selection!!!!!");
					break;
			}

		} while (choice != 3);
		in.close();
	}

}