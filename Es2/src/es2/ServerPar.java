package es2;

import java.io.*;
import java.net.*;

//Thread lanciato per ogni richiesta accettata
//versione per il trasferimento di file binari
class ServerParThread extends Thread{

	private Socket clientSocket = null;

	public ServerParThread(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}

	public void run() {
		DataInputStream inSock;
		DataOutputStream outSock;
		
		try {
			String nomeFile;
			try {
				inSock = new DataInputStream(clientSocket.getInputStream());
				outSock = new DataOutputStream(clientSocket.getOutputStream());
			}catch (Exception e) {
				System.err.println("\nProblemi durante la creazione degli stream: "+ e.getMessage());
				e.printStackTrace();
				clientSocket.close();
				System.out.println("Terminata connessione con " + clientSocket);
				return; //CHE SUCCEDE QUI IN CASO?
			}
			
			while((nomeFile=inSock.readUTF())!=null) { //leggo tutti i file della directory
				File curFile = new File(nomeFile);
				if (curFile.exists()) {
					outSock.writeUTF("Salta"); //mandiamo esito al cliente
				} else{
					outSock.writeUTF("Attiva");
					int length = inSock.readInt();
					
					System.out.println("Ricevo il file " + nomeFile + ", di lunghezza: "+length);
					try {
						FileOutputStream outFile = new FileOutputStream(nomeFile);
						for(int i=0;i<length; i++) {
							outFile.write(inSock.read()); //scrivo sul file il byte che leggo dalla socket del cliente
						}
						outFile.close();
					}catch (Exception e) {
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
			
		}catch (Exception e) {  //qui catturo le eccezioni non catturate all'interno del while
	    	e.printStackTrace();
	    	System.out.println("Errore irreversibile, PutFileServerThread: termino...");
	    	System.exit(3);
	    }
		
	} // run

} // ServerParThread class


public class ServerPar {
	public static final int PORT = 1069; //default port

	public static void main(String[] args) throws IOException {

		int port = -1;

	    try {
	    	if (args.length == 1) {
	    		port = Integer.parseInt(args[0]);
	    		if (port < 1024 || port > 65535) {
	    			System.out.println("Usage: java ServerPar [serverPort>1024]");
	    			System.exit(1);
	    		}
	    	} else if (args.length == 0) {
	    		port = PORT;
	    	} else { //errore
	    		System.out.println("Usage: java ServerPar or java ServerParThread port");
	    		System.exit(1);
	    	}
	    	
	    }catch (Exception e) {
	    	System.out.println("Problemi, i seguenti: ");
	    	e.printStackTrace();
	    	System.out.println("Usage: java ServerPar or java ServerParThread port");
	    	System.exit(1);
	    }

	    ServerSocket serverSocket = null; 
	    Socket clientSocket = null;

	    try {
	    	serverSocket = new ServerSocket(port);
	    	serverSocket.setReuseAddress(true); //si possono collegare più processi ad una certa porta (senza errori binding)
	    	System.out.println("ServerPar: avviato, creata la socket: "+ serverSocket);
	    }catch (Exception e) {
	    	System.err.println("Server: problemi nella creazione della server socket: " + e.getMessage());
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
	    			new ServerParThread(clientSocket).start(); //delego ai figli sfigati
	    		}catch (Exception e) {
	    			System.err.println("Server: problemi nel server thread: "+ e.getMessage());
	    			e.printStackTrace();
	    			continue;
	    		}

	    	} //fine while
	    	
	    }catch (Exception e) { // qui catturo le eccezioni non catturate all'interno del while
	    	e.printStackTrace();
	    	// chiusura di stream e socket
	    	System.out.println("ServerPar: termino...");
	    	System.exit(2);
	    }
	    
	}
} // ServerPar class
