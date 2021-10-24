package es2;

import java.io.*;
import java.net.*;

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
				//controllo tempistiche: faccio partire il timer da quando processa la directory
				long start=System.currentTimeMillis();//controllo tempistiche
				System.out.println("Server Par: start is at "+start +" milliseconds\n");//controllo tempistiche

				try {
					new ServerParThread(clientSocket).start(); //delego ai figli sfigati
				}
				catch (Exception e) {
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
