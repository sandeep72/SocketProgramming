

import java.io.*;
import java.net.*;
import java.util.*;

import java.sql.Date;
import java.sql.Time;

// Lab 1 on series 777*

// Lab 2 on series 666*
/*
File Name: ServerAApplication.java
Description:
			This is Server A application that will create a server socket and wait for a client connection.
			After connecting with a client, the application will generate directory listing of its own directory
			and send a request to connect with ServerB and receive directory listing from Server B. Once it has
			received directory listing of Server B, it will merge its own directory listing with the list received
			from Server B. It will then perform sorting of combined directory listing, eliminate duplicate and send the sorted list to 
			Client.It will also sync its dirtectory with ServerB in real Time.
			The Directory listing will include last modified date, file size in byte, and file name.


*/

public class ServerAApplication implements Runnable {
	Thread thread;
	Socket clientSocket, clientsyncSocket;
	ServerSocket serverSocketA, serverSocketSync;
	Socket socketAsClient;
	DataInputStream in, inClient; // Object to read data from Server B socket stream
	DataOutputStream out; // Object to write data to client socket stream
	File fileObj;
	HashMap<String, FileInfo> directoryCurrent;
	ArrayList<FileInfo> directoryMetaData;
	ArrayList<FileInfo> directoryDeletedFiles;
	LogServer logServerObj;
	String pathForServerA;
	ArrayList<String> fileLockList;

	public class FileInfo {
		File obj;
		long time;
	}

	public class LogServer implements Runnable {
		Thread t;
		ServerSocket logServer;
		Socket clientSocketLog;
		boolean clientConnectedForLog;
		DataOutputStream outLog;

		public LogServer() {
			t = new Thread(this);
			clientConnectedForLog = false;

			try {
				logServer = new ServerSocket(6663);
			} catch (IOException e) {
				System.out.println("error starting the client");
			}
		}

		void startLogServer() {
			t.start();
		}

		@Override
		public void run() {
			try {
				// TODO Auto-generated method stub
				clientSocketLog = logServer.accept();
				outLog = new DataOutputStream(clientSocketLog.getOutputStream());
				clientConnectedForLog = true;

			} catch (Exception e) {
				System.out.println("Log Server : error connecting to client");
			}
		}

		void sendString(String logLine) {
			try {

				outLog.writeUTF(logLine);
				outLog.flush();

			} catch (Exception e) {
				System.out.println("error creating outlog object");
			}
		}

	}

	// constructor method for Server class
	public ServerAApplication() {
		thread = new Thread(this);
		directoryCurrent = new HashMap<String, FileInfo>();
		directoryMetaData = new ArrayList<>();
		directoryDeletedFiles = new ArrayList<>();
		fileLockList = new ArrayList<>();
		pathForServerA = "C://Users//16825//Desktop//test//ServerA";

		try {
			serverSocketSync = new ServerSocket(6661);

			serverSocketA = new ServerSocket(7771);
			// System.out.println(" 3 server sockets have been started on A");
		} catch (IOException e) {
			System.out.println("error starting the client");
		}
	}

	// method that will receive files to Server B
	public void requestServer() {
		try {
			System.out.println("********************Requester A*****************");
			clientsyncSocket = new Socket("localhost", 6662);
			// 22222 System.out.println("connnection established sync A");
			ObjectInputStream reader = new ObjectInputStream(clientsyncSocket.getInputStream());
			ObjectOutputStream writer = new ObjectOutputStream(clientsyncSocket.getOutputStream());

			String path = pathForServerA;
			File localDir = new File(path);
			File fileList[] = localDir.listFiles();

			HashMap<String, File> listMap = new HashMap<String, File>();
			ArrayList<FileInfo> notFoundList = new ArrayList<>();

			for (File temp : fileList) {
				// if (!fileLockList.contains(temp.getName().toString()))
				listMap.put(temp.getName(), temp);
			}
			// System.out.println("hash map table created");

			FileInfo tempFileInfo = new FileInfo();

			for (;;) {
				File obj = (File) reader.readObject();
				if (obj == null)
					break;
				if (!fileLockList.contains(obj.getName().toString())) {
					if (listMap.containsKey(obj.getName())) {
						File fileCheckTimestamp = listMap.get(obj.getName());
						if (obj.lastModified() > fileCheckTimestamp.lastModified()) {
							tempFileInfo = new FileInfo();
							tempFileInfo.obj = obj;
							tempFileInfo.time = obj.lastModified();
							notFoundList.add(tempFileInfo);
							fileCheckTimestamp = null;
						}
						// System.out.println("present " + obj.getName());
					} else {

						tempFileInfo = new FileInfo();
						tempFileInfo.obj = obj;
						tempFileInfo.time = obj.lastModified();
						notFoundList.add(tempFileInfo);

					}
				}
			}
			// 22222 System.out.println("Comparison done, below is list of files that are
			// not
			// present on server A");

			// for (int i = 0; i < notFoundList.size(); i++) {
			// System.out.println("name : " + notFoundList.get(i).obj.getName());
			// }

			if (notFoundList.size() > 0) {

				writer.writeUTF(Integer.toString(notFoundList.size()));
				writer.flush();
				System.out.println("difference count sent to Server B");

				PrintWriter out2 = null;
				for (int i = 0; i < notFoundList.size(); i++) {
					boolean deletedFlag = true;
					String newPath = path + "//" + notFoundList.get(i).obj.getName();
					File createFileObj = new File(newPath);

					if (createFileObj.exists()) {
						if (createFileObj.delete()) {
							writer.writeBoolean(true);
							writer.flush();
							// 22222 System.out.println("delete successful");
							deletedFlag = true;
						} else {
							writer.writeBoolean(false);
							writer.flush();
							// 22222 System.out.println("delete unsuccessful");
							deletedFlag = false;
						}

					} else {
						writer.writeBoolean(true);
						writer.flush();
					}
					if (deletedFlag == true && createFileObj.createNewFile()) {
						// 22222 System.out.println("file created successfully:\t" +
						// createFileObj.getName());
						writer.writeObject(notFoundList.get(i).obj);
						out2 = new PrintWriter(createFileObj);
						while (true) {
							String lineContent = (String) reader.readObject();
							if (lineContent == null) {
								break;
							}
							// System.out.println(lineContent);
							out2.write(lineContent);
							out2.flush();
						}
						createFileObj.setLastModified(notFoundList.get(i).time);
						if (logServerObj.clientConnectedForLog == true) {
							logServerObj.sendString(notFoundList.get(i).obj.getName() + " : file moved from B to A");

							// 22222 System.out.println("log sent to Client");
						}
						out2.close();
						// in.close();
					} else {
						System.out.println("error in file creation");
					}

					// below is loop end
				}

				// 22222 System.out.println("synchronization complete");

			} else {
				// 22222 System.out.println("everything is in sync");
				// writer.writeInt(notFoundList.size());
				writer.writeUTF(Integer.toString(-1));
				writer.flush();
				// 22222 System.out.println("size of not found list\t" + notFoundList.size());
				// 22222 System.out.println("difference count sent to acknowledge server B");
			}

		} catch (Exception e) {
			System.out.println(" error on server A Lab2: requester");
			e.printStackTrace();
		} finally {
			try {
				if (clientsyncSocket.isConnected())
					clientsyncSocket.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// method that will send files to Server B
	public void senderServer() {
		try {
			System.out.println("********************Sender A*****************");
			// 22222 System.out.println("waiting for connection sync");
			clientsyncSocket = serverSocketSync.accept();
			// 22222 System.out.println("client accepted at A sync");

			String path = pathForServerA;
			File obj = new File(path);
			File object[] = obj.listFiles();

			// 22222 System.out.println("sending list of files to client");
			ObjectOutputStream objectWriter = new ObjectOutputStream(clientsyncSocket.getOutputStream());
			ObjectInputStream objectReader = new ObjectInputStream(clientsyncSocket.getInputStream());

			for (File temp : object) {
				if (!fileLockList.contains(temp.getName().toString()))
					objectWriter.writeObject(temp);
			}
			objectWriter.writeObject(null);
			objectWriter.flush();
			// 22222 System.out.println("file names on server sent to client");

			// accept the number of files missing on client drive
			int countFile = Integer.parseInt(objectReader.readUTF());
			// 22222 System.out.println("difference count received:\t" + countFile);

			if (countFile == -1) {
				// 22222 System.out.println("server A has all files of server B");
			} else {

				Scanner inputFileReader = null;
				for (int i = 0; i < countFile; i++) {
					if ((boolean) objectReader.readBoolean()) {
						File objectFile = (File) objectReader.readObject();
						// 2222 System.out.println("received file object name" + objectFile.getName() +
						// " and path : "
						// 2222 + objectFile.getAbsolutePath());

						inputFileReader = new Scanner(objectFile);
						// 2222 System.out.println("file reader object created for file : " +
						// objectFile.getName());
						// 2222 System.out.println("File content");

						while (inputFileReader.hasNextLine()) {
							String dummy = inputFileReader.nextLine();
							objectWriter.writeObject(dummy);
							objectWriter.flush();
						}
						objectWriter.writeObject(null);
						objectWriter.flush();
						inputFileReader.close();
						if (logServerObj.clientConnectedForLog == true) {
							logServerObj.sendString(objectFile.getName() + " : file moved from A to B");
							// 2222 System.out.println("log sent to Client");
						}
					}

				}

				// inputFileReader.close();
			}

		} catch (Exception e) {
			System.out.println("error in server A, lab2: sender");
			e.printStackTrace();

		} finally {
			try {
				if (clientsyncSocket.isConnected())
					clientsyncSocket.close();
			} catch (Exception e) {
				System.out.println(e);
			}
		}
	}

	void startThread() {
		thread.start();
	}

	// thread that will continously look for Server A connection
	@Override
	public void run() {
		logServerObj = new LogServer();
		logServerObj.startLogServer();
		Socket socketAsServer = null;
		try {
			System.out.println("*******************started file list server A*******************");
			for (;;) {
				int i = 0;

				socketAsServer = serverSocketA.accept();
				String fileInfoList[][];
				String line = "";
				out = new DataOutputStream(socketAsServer.getOutputStream());
				inClient = new DataInputStream(socketAsServer.getInputStream());
				File filePath = new File(pathForServerA);
				File[] fileList = filePath.listFiles();

				socketAsClient = new Socket("localhost", 7772);
				if (socketAsClient == null) {
					System.out.println("failure in establishing connection to server B");
				} else {
					System.out.println("Connection establised to server B");
					in = new DataInputStream(socketAsClient.getInputStream());
					int lengthA = fileList.length;
					int lengthB = Integer.parseInt(in.readUTF());
					fileInfoList = new String[lengthA + lengthB][3];
					System.out.println("there are " + (lengthA + lengthB)
							+ " files in the parent directory (Server A + Server B)");

					for (File tempObj : fileList) { // looping through the File object and storing the information in
													// our
													// fileinfolist array
						try {
							fileInfoList[i][0] = (new Time(tempObj.lastModified()).toString()) + " "
									+ (new Date(tempObj.lastModified()).toLocalDate().toString());
							fileInfoList[i][1] = "" + tempObj.length() + " bytes ";
							fileInfoList[i][2] = tempObj.getName();
							i++;
						} catch (Exception e) {
							System.out.println(e);
						}
					}

					String data = "";
					// the while loop runs until it receives "END" from Server B which marks end of
					// data transfer.
					while (!data.equals("END")) {
						data = in.readUTF(); // read string from socket
						if (data.equals("END"))
							break; // check if the string denotes end of data transfer, if Yes break out of loop
						fileInfoList[i][0] = data; // add the last modified date to array

						data = in.readUTF();
						if (data.equals("END"))
							break;
						fileInfoList[i][1] = data;

						data = in.readUTF();
						if (data.equals("END"))
							break;
						fileInfoList[i][2] = data;
						i++;
					}

					System.out.println("\tlast modified\t\tsize\t\t\tFile Name");
					for (int j = 0; j < i; j++) {
						System.out.println("\t" + fileInfoList[j][0] + "\t\t" + fileInfoList[j][1] + "\t\t\t"
								+ fileInfoList[j][2]);
					}
					System.out.println("print done for all file names");
					System.out.println("Performing sorting based on file names");
					String name, lastModified, size; // variables to be used for swapping purpose during sorting
														// operation

					// Implementation of selection sorting algorithm
					for (int j = 0; j < i; j++) {
						for (int k = j + 1; k < i; k++) {
							if (fileInfoList[k][2].compareToIgnoreCase(fileInfoList[j][2]) < 0) { // comparing file name
																									// pointed by index
																									// k
																									// and j.
								lastModified = fileInfoList[j][0];
								size = fileInfoList[j][1];
								name = fileInfoList[j][2];
								fileInfoList[j][0] = fileInfoList[k][0];
								fileInfoList[j][1] = fileInfoList[k][1];
								fileInfoList[j][2] = fileInfoList[k][2];
								fileInfoList[k][0] = lastModified;
								fileInfoList[k][1] = size;
								fileInfoList[k][2] = name;
							}

						}
					}

					String tempRemoveDuplicate = fileInfoList[0][2];

					for (int j = 0; j < i; j++) {
						try {

							if (j == 0) {
								line = fileInfoList[j][0];
								out.writeUTF(line); // writing data to client socket
								line = fileInfoList[j][1];
								;
								out.writeUTF(line); // writing data to client socket
								line = fileInfoList[j][2];
								out.writeUTF(line); // writing data to client socket
							} else {
								if (tempRemoveDuplicate.compareToIgnoreCase(fileInfoList[j][2]) == 0) {

								} else {
									tempRemoveDuplicate = fileInfoList[j][2];
									line = fileInfoList[j][0];
									out.writeUTF(line); // writing data to client socket
									line = fileInfoList[j][1];
									;
									out.writeUTF(line); // writing data to client socket
									line = fileInfoList[j][2];
									out.writeUTF(line); // writing data to client socket
								}
							}

						} catch (Exception e) {
							System.out.println(e);
						}
					}
					System.out.println("transfer complete for file info, sending terminating string");
					line = "END"; // sending the terminating string to mark end of data transfer
					out.writeUTF(line);
					String codeString = inClient.readUTF();
					if (codeString.equals("LOCK")) {
						System.out.println("*****************LOCK**********************");
						String fileNameToLock = inClient.readUTF();
						System.out.println("file to be locked:*****************" + fileNameToLock);
						fileLockList.add(fileNameToLock);
					}
					if (codeString.equals("UNLOCK")) {
						String fileNameToLock = inClient.readUTF();
						System.out.println("file to be unlocked:*****************" + fileNameToLock);
						fileLockList.remove(fileNameToLock);
					}
					System.out.println("Closing connection");

				}

				try {
					if (socketAsClient.isConnected()) {
						socketAsClient.close(); // close the client socket object.
					}
					if (in != null) {
						in.close(); // close the reader object connected to stream.
					}
					if (out != null) {
						out.close(); // close the writer object connected to stream.
					}
				} catch (Exception e) {
					System.out.println("exception in closing resources details below \n" + e);
				}

			}

		} catch (IOException e) {
			System.out.println("error in thread: creating client socket");
		} finally {
			try {
				if (socketAsClient.isConnected()) {
					socketAsClient.close(); // close the client socket object.
				}
				if (in != null) {
					in.close(); // close the reader object connected to stream.
				}
				if (out != null) {
					out.close(); // close the writer object connected to stream.
				}
			} catch (Exception e) {
				System.out.println("exception in closing resources details below \n" + e);
			}
		}

	}

	// get directoy state
	public void initDrectoryInfo() {
		directoryMetaData.clear();
		File serverADirectory = new File(pathForServerA);
		File[] fileList = serverADirectory.listFiles();

		for (File tempFileObject : fileList) {
			FileInfo temp = new FileInfo();
			temp.obj = tempFileObject;
			temp.time = tempFileObject.lastModified();
			directoryMetaData.add(temp);
		}
	}

	// get current directoy state and check if something has been deleted
	public void isSomethingDeleted() {
		File serverADirectory = new File(pathForServerA);
		File[] fileList = serverADirectory.listFiles();
		directoryCurrent.clear();
		directoryDeletedFiles.clear();
		for (File tempFileObject : fileList) {
			FileInfo temp = new FileInfo();
			temp.obj = tempFileObject;
			temp.time = tempFileObject.lastModified();
			directoryCurrent.put(tempFileObject.getName(), temp);
		}
		if (directoryCurrent != null) {
			for (int i = 0; i < directoryMetaData.size(); i++) {
				if (!directoryCurrent.containsKey(directoryMetaData.get(i).obj.getName())) {
					// the file has been deleted and this needs to be informed to server A.
					directoryDeletedFiles.add(directoryMetaData.get(i));
				}
			}
		}
	}

	// perform the transfer of deleted file names to Server B, so that it can delete
	// them from its local directory
	void performDeletedFileTransfer() {
		isSomethingDeleted();
		try {
			clientsyncSocket = new Socket("localhost", 6662);
			// ObjectInputStream reader = new
			// ObjectInputStream(clientsyncSocket.getInputStream());
			ObjectOutputStream writer = new ObjectOutputStream(clientsyncSocket.getOutputStream());

			if (directoryDeletedFiles != null && directoryDeletedFiles.size() > 0) {
				System.out.println("connnection established send to be deleted list to server B");
				writer.writeBoolean(true);
				writer.flush();
				for (int i = 0; i < directoryDeletedFiles.size(); i++) {
					writer.writeObject(directoryDeletedFiles.get(i).obj);
					writer.flush();
					if (logServerObj.clientConnectedForLog == true) {
						logServerObj.sendString(directoryDeletedFiles.get(i).obj.getName()
								+ " : deleted in A and notified to B for Sync");
						System.out.println("log sent to Client");
					}
				}
				writer.writeObject(null);
				writer.flush();
				System.out.println("all names to be delted sent to server B");

				// update metadata file
				initDrectoryInfo();
			} else {
				writer.writeBoolean(false);
				writer.flush();
			}

		} catch (Exception e) {
			System.out.println(" error on server B Lab2: delete list sender");
			e.printStackTrace();
		} finally {
			try {
				if (clientsyncSocket.isConnected())
					clientsyncSocket.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	// receive of deleted file names from Server B, so that it can delete them from
	// its own local directory
	void deleteFilesFromLocalDirectory() {

		try {
			clientsyncSocket = serverSocketSync.accept();
			System.out.println("connection accepted");
			ObjectInputStream reader = new ObjectInputStream(clientsyncSocket.getInputStream());
			String path = pathForServerA;
			if ((boolean) reader.readBoolean()) {
				File tempFileObject;
				for (;;) {
					tempFileObject = (File) reader.readObject();
					if (tempFileObject == null) {
						break;
					}
					File delFile = new File(path + "//" + tempFileObject.getName());
					if (delFile.exists()) {
						if (delFile.delete())
							System.out.println("delete successful for file :" + tempFileObject.getName());
						if (logServerObj.clientConnectedForLog == true) {
							logServerObj.sendString(
									tempFileObject.getName() + " : deleted in B and notified to A for Sync");
							System.out.println("log sent to Client");
						}
					}
				}
				initDrectoryInfo();
			}

		} catch (IOException e) {
			System.out.println("error accepting connection from Server for File Deletion on B");
		} catch (Exception e) {
			System.out.println("unknown exception");
		} finally {
			try {
				clientsyncSocket.close();
			} catch (Exception e) {
				System.out.println("error closing the socket in delete method");
			}
		}
	}

	public static void main(String[] args) {
		ServerAApplication serverA = new ServerAApplication();
		serverA.startThread();
		serverA.requestServer();
		serverA.senderServer();

		serverA.initDrectoryInfo();
		for (int i = 0;; i++) {
			serverA.performDeletedFileTransfer();
			serverA.deleteFilesFromLocalDirectory();
			serverA.requestServer();
			serverA.senderServer();

			System.out.println("****************loop count at A:\t" + i);

			try {
				System.out.println("going f a break");
				Thread.sleep(5000);
			} catch (Exception e) {
			}
			System.out.println("execution resumed");
		}
	}
}
