package es2;

import java.io.*;
import java.net.*;


public class ServerSeqDebug {

	public static final int PORT = 1069; //default port
	public static final String FS ="d:\\UNI\\RetiDiCalcolatori\\github\\RetiCalcolatori\\RetiCalcolatori\\Es2\\filesysServer\\";
	public static void main(String[] args) throws IOException {

		//all'inizio invocazione del serverParall (da cmd): ServerParall numPorta
		int port = -1;

		try {
			if (args.length == 1) {
				port = Integer.parseInt(args[0]);
				if (port < 1024 || port > 65535) {
					System.out.println("Usage: java ServerSeq [serverPort>1024]");
					System.exit(1);
				}
			} else if (args.length == 0) {
				port = PORT;
			} else { //errore
				System.out.println("Usage: java ServerSeq or java ServerParThread port");
				System.exit(1);
			}

		}catch (Exception e) {
			System.out.println("Problemi, i seguenti: ");
			e.printStackTrace();
			System.out.println("Usage: java <ServerSeq | ServerParThread> port");
			System.exit(1);
		}

		ServerSocket serverSocket = null; 
		Socket clientSocket = null;

		try {
			serverSocket = new ServerSocket(port);
			serverSocket.setReuseAddress(true); //si possono collegare più processi ad una certa porta (senza errori binding)
			System.out.println("ServerSeq: avviato, creata la socket: "+ serverSocket);
		}catch (Exception e) {
			System.err.println("ServerSeq: problemi nella creazione della server socket: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}

		try {
			while (true) { //demone
				System.out.println("Server: in attesa di richieste...\n");

				try {
					clientSocket = serverSocket.accept(); //bloccante fino ad una pervenuta connessione, 1 per ogni richiesta
					System.out.println("Server: connessione accettata: " + clientSocket);
				}catch (Exception e) {
					System.err.println("Server: problemi nella accettazione della connessione: "+ e.getMessage());
					e.printStackTrace();
					continue;
				}
				try {
					DataInputStream inSock;
					DataOutputStream outSock;
					String nomeFile;
					try {
						inSock = new DataInputStream(clientSocket.getInputStream());
						outSock = new DataOutputStream(clientSocket.getOutputStream());
					}catch (Exception e) {
						System.err.println("\nProblemi durante la creazione degli stream: "+ e.getMessage());
						e.printStackTrace();
						clientSocket.close();
						System.out.println("Terminata connessione con " + clientSocket);
						continue;
					}

					while((nomeFile=inSock.readUTF())!=null) { //leggo tutti i file della directory
						//PER DEBUG
						String onlyName= nomeFile.substring(nomeFile.lastIndexOf("\\")+1);
						String  pathToCheck= FS+"\\"+  onlyName;
						File curFile = new File(pathToCheck);
						//File curFile = new File(nomeFile);
						if (curFile.exists()) {
							outSock.writeUTF("Salta"); //mandiamo esito al cliente
						} else{
							outSock.writeUTF("Attiva");
							long length = inSock.readLong();

							System.out.println("Ricevo il file " + nomeFile + ", di lunghezza: "+length);
							try {
								FileOutputStream outFile = new FileOutputStream(curFile);//dopo debug metto outFile al posto di curFile!!!
								for(int i=0;i<length; i++) {
									outFile.write(inSock.read()); //scrivo sul file il byte che leggo dalla socket del cliente
								}
								outFile.close();
							}
							catch (Exception e) {
								System.err.println("\nProblemi durante la ricezione e scrittura del file: "+nomeFile+ e.getMessage());
								e.printStackTrace();
							}
						}
					} //fine while
					clientSocket.shutdownInput(); //chiusura socket (downstream)
					outSock.flush();
					clientSocket.shutdownOutput(); //chiusura socket (dupstream)
					System.out.println("\nTerminata connessione con " + clientSocket);
					clientSocket.close();

				}catch (EOFException e) {
					System.err.println("\nI file sono stati tutti processati");
					e.printStackTrace();
				}
				catch (Exception e) {  //qui catturo le eccezioni non catturate all'interno del while
					e.printStackTrace();
					System.out.println("Errore irreversibile, PutFileServerThread: termino...");
					System.exit(3);
				}

			} //fine while

		}catch (Exception e) { // qui catturo le eccezioni non catturate all'interno del while
			e.printStackTrace();
			// chiusura di stream e socket
			System.out.println("ServerPar: termino...");
			System.exit(2);
		}

	}
} // ServerSeqDebug class

