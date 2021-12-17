

import java.io.*;
import java.net.*;
import java.util.*;
import java.sql.Date;
import java.sql.Time;

// Lab 1 on series 777*

// Lab 2 on series 666*

/*
File Name: ServerBApplication.java
Description:
			This is Server B application that will generate directory listing of its own directory
			and send the information to Server A. It will also sync its dirtectory with ServerA in real Time.
			The Directory listing will include last modified date, file size in byte, and file name.

*/

public class ServerBApplication implements Runnable {
	Thread thread;
	ServerSocket serverSocketSync, serverSocketB;
	Socket clientSocket, clientsyncSocket;
	DataOutputStream writerObj;
	File fileObj;
	HashMap<String, FileInfo> directoryCurrent;
	ArrayList<FileInfo> directoryMetaData;
	ArrayList<FileInfo> directoryDeletedFiles;
	String pathForServerB;

	public class FileInfo {
		File obj;
		long time;
	}

	// constructor method for Server class
	public ServerBApplication() {
		thread = new Thread(this);
		serverSocketSync = null;
		clientSocket = null;
		directoryCurrent = new HashMap<String, FileInfo>();
		directoryMetaData = new ArrayList<>();
		directoryDeletedFiles = new ArrayList<>();
		pathForServerB = "C://Users//16825//Desktop//test//ServerB";
		try {
			serverSocketSync = new ServerSocket(6662);
			serverSocketB = new ServerSocket(7772);
			System.out.println(" 2 server sockets have been started on A");
		} catch (Exception e) {
			System.out.println("error starting server on server");
		}
	}

	// method that will send files to Server A
	public void senderServer() {
		try {
			System.out.println("********************Sender B*****************");
			System.out.println("waiting for connection sync");
			clientsyncSocket = serverSocketSync.accept();
			System.out.println("client accepted at B sync");

			String path = pathForServerB;
			File obj = new File(path);
			File object[] = obj.listFiles();

			System.out.println("sending list of files to client");
			ObjectOutputStream objectWriter = new ObjectOutputStream(clientsyncSocket.getOutputStream());
			ObjectInputStream objectReader = new ObjectInputStream(clientsyncSocket.getInputStream());

			for (File temp : object) {
				objectWriter.writeObject(temp);
			}
			objectWriter.writeObject(null);
			objectWriter.flush();
			System.out.println("file names on server sent to client");

			// accept the number of files missing on client drive
			// System.out.println("value : " + Integer.parseInt(objectReader.readUTF()));
			int countFile = Integer.parseInt(objectReader.readUTF());
			System.out.println("difference count received:\t" + countFile);

			if (countFile == -1) {
				System.out.println("server A has all files of server B");
			} else {
				// FileInputStream inputFileReader = null;
				Scanner inputFileReader = null;
				for (int i = 0; i < countFile; i++) {
					if ((boolean) objectReader.readBoolean()) {
						File objectFile = (File) objectReader.readObject();
						System.out.println("received file object name" + objectFile.getName() + " and path : "
								+ objectFile.getAbsolutePath());

						inputFileReader = new Scanner(objectFile);
						System.out.println("file reader object created for file : " + objectFile.getName());
						System.out.println("File content");

						while (inputFileReader.hasNextLine()) {
							String dummy = inputFileReader.nextLine();
							objectWriter.writeObject(dummy);
							objectWriter.flush();
						}
						objectWriter.writeObject(null);
						objectWriter.flush();
						inputFileReader.close();
					}
					// end of for loop
				}
				// inputFileReader.close();
			}

		} catch (Exception e) {
			System.out.println("error in server B, lab2: sender");
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

	// method that will receive files to Server A
	public void requestServer() {
		try {
			System.out.println("********************Requester B*****************");
			clientsyncSocket = new Socket("localhost", 6661);
			System.out.println("connnection established with sync at B");
			ObjectInputStream reader = new ObjectInputStream(clientsyncSocket.getInputStream());
			ObjectOutputStream writer = new ObjectOutputStream(clientsyncSocket.getOutputStream());

			String path = pathForServerB;
			File localDir = new File(path);
			File fileList[] = localDir.listFiles();

			HashMap<String, File> listMap = new HashMap<String, File>();
			ArrayList<FileInfo> notFoundList = new ArrayList<>();

			for (File temp : fileList) {
				listMap.put(temp.getName(), temp);
			}
			// System.out.println("hash map table created");

			FileInfo tempFileInfo = new FileInfo();

			for (;;) {
				File obj = (File) reader.readObject();
				if (obj == null)
					break;
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
			System.out.println("Comparison done");

			if (notFoundList.size() > 0) {

				writer.writeUTF(Integer.toString(notFoundList.size()));
				writer.flush();
				System.out.println("difference count sent to Server A");
				PrintWriter out2 = null;
				for (int i = 0; i < notFoundList.size(); i++) {
					boolean deletedFlag = true;
					String newPath = path + "//" + notFoundList.get(i).obj.getName();
					File createFileObj = new File(newPath);

					if (createFileObj.exists()) {
						if (createFileObj.delete()) {
							writer.writeBoolean(true);
							writer.flush();
							System.out.println("delete successful");
							deletedFlag = true;
						} else {
							writer.writeBoolean(false);
							writer.flush();
							System.out.println("delete unsuccessful");
							deletedFlag = false;
						}

					} else {
						writer.writeBoolean(true);
						writer.flush();
					}
					if (deletedFlag == true && createFileObj.createNewFile()) {
						System.out.println("file created successfully:\t" + createFileObj.getName());
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
						// in.close();
						out2.close();
					} else {
						System.out.println("error in file creation");
					}
					// for loop end.

				}

				System.out.println("synchronization complete");

			} else {
				System.out.println("everything is in sync");
				// writer.writeInt(notFoundList.size());
				writer.writeUTF(Integer.toString(-1)); // sending -1 instead of zero
				writer.flush();
				System.out.println("size of not found list\t" + notFoundList.size());
				System.out.println("difference count sent to acknowledge server B");
			}

		} catch (Exception e) {
			System.out.println(" error on server B Lab2: requester");
			e.printStackTrace();
		} finally {
			try {
				clientsyncSocket.close();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	void startThread() {
		thread.start();
	}

	// thread that will continously look for Server A connection
	@Override
	public void run() {

		Socket socketAsServer = null;
		try {
			System.out.println("*******************started file list server B*******************");
			for (;;) {
				socketAsServer = serverSocketB.accept();
				System.out.println("Client connection accepted at Server B sync");
				writerObj = new DataOutputStream(socketAsServer.getOutputStream());
				String line = "";

				File filePath = new File(pathForServerB);
				File[] fileList = filePath.listFiles();
				writerObj.writeUTF("" + fileList.length);
				for (File tempObj : fileList) { // looping through the File object and sending the information to Server
												// A
					try {
						line = (new Time(tempObj.lastModified()).toString()) + " "
								+ (new Date(tempObj.lastModified()).toLocalDate().toString());
						writerObj.writeUTF(line); // writing data to socket connected with Server A
						line = "" + tempObj.length() + " bytes ";
						writerObj.writeUTF(line); // writing data to socket connected with Server A
						line = tempObj.getName();
						writerObj.writeUTF(line); // writing data to socket connected with Server A
					} catch (IOException i) {
						System.out.println(i);
					}
				}
				line = "END"; // sending the terminating string to mark end of data transfer
				writerObj.writeUTF(line);
				System.out.println("Closing connection at server B");

				try {
					if (socketAsServer.isConnected()) {
						socketAsServer.close(); // close the client socket object.
					}
					if (writerObj != null) {
						writerObj.close(); // close the writer object connected to stream.
					}
				} catch (Exception e) {
					System.out.println("exception in closing resources details below \n" + e);
				}

			}
		} catch (IOException e) {
			System.out.println("error in creating a client socket at server B");
		} finally {
			try {
				if (socketAsServer.isConnected()) {
					socketAsServer.close(); // close the client socket object.
				}
				if (writerObj != null) {
					writerObj.close(); // close the writer object connected to stream.
				}
			} catch (Exception e) {
				System.out.println("exception in closing resources details below \n" + e);
			}
		}

	}

	// get directoy state
	public void initDrectoryInfo() {
		directoryMetaData.clear();
		File serverADirectory = new File(pathForServerB);
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
		File serverADirectory = new File(pathForServerB);
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

	// perform the transfer of deleted file names to Server A, so that it can delete
	// them from its local directory
	void performDeletedFileTransfer() {
		isSomethingDeleted();
		try {
			clientsyncSocket = new Socket("localhost", 6661);
			// ObjectInputStream reader = new
			// ObjectInputStream(clientsyncSocket.getInputStream());
			ObjectOutputStream writer = new ObjectOutputStream(clientsyncSocket.getOutputStream());

			if (directoryDeletedFiles != null && directoryDeletedFiles.size() > 0) {
				System.out.println("connnection established send to be deleted list to server A");
				writer.writeBoolean(true);
				writer.flush();
				for (int i = 0; i < directoryDeletedFiles.size(); i++) {
					writer.writeObject(directoryDeletedFiles.get(i).obj);
					writer.flush();
				}
				writer.writeObject(null);
				writer.flush();
				System.out.println("all names to be delted sent to server A");

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

	// receive of deleted file names from Server A, so that it can delete them from
	// its own local directory
	void deleteFilesFromLocalDirectory() {

		try {
			clientsyncSocket = serverSocketSync.accept();
			System.out.println("connection accepted");
			ObjectInputStream reader = new ObjectInputStream(clientsyncSocket.getInputStream());
			String path = pathForServerB;
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

	public static void main(String args[]) {
		ServerBApplication serverB = new ServerBApplication();
		serverB.startThread();
		serverB.senderServer();
		serverB.requestServer();

		serverB.initDrectoryInfo();
		for (int i = 0;; i++) {
			serverB.deleteFilesFromLocalDirectory();
			serverB.performDeletedFileTransfer();
			serverB.senderServer();
			serverB.requestServer();
			System.out.println("******************************loop count at B \t" + i);

			serverB.initDrectoryInfo();
			try {
				System.out.println("going for a break");
				Thread.sleep(5000);
			} catch (Exception e) {
			}
			System.out.println("execution resumed");
		}
	}

}
