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
				return;
			}
			
			while((nomeFile=inSock.readUTF())!=null) { //leggo tutti i file della directory
				File curFile = new File(nomeFile);
				if (curFile.exists()) {
					outSock.writeUTF("Salta"); //mandiamo esito al cliente
				} else{
					outSock.writeUTF("Attiva");
					long length = inSock.readLong();
					
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

