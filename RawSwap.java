package es1;

import java.io.*;
import java.net.*;
//import java.util.*;

public class RawSwap extends Thread{
	private final String fileName;
	private final int port;
	
	public RawSwap(int port, String fileName) {
		this.fileName = fileName;
		this.port=port;
	}
	
	public void run() {
		DatagramSocket socket = null;
		DatagramPacket packet = null;
		byte[] buf = new byte[256];
		int portClient = -1;
		InetAddress addClient = null;
		
		//preparo socket per la ricezione
		try {
			socket = new DatagramSocket(port);
			packet = new DatagramPacket(buf, buf.length);
			System.out.println("Creata la socket: " + socket);
		}
		catch (SocketException e) {
			System.out.println("Problemi nella creazione della socket: ");
			e.printStackTrace();
			System.exit(1);
		}
		
		try {
			int line1 = -1;
			int line2 = -1;
			int esito = 0;
			ByteArrayInputStream biStream = null;
			ByteArrayOutputStream boStream = null;
			DataInputStream diStream = null;
			DataOutputStream doStream = null;
			byte[] data = null;

			while (true) {
				System.out.println("\nIn attesa di richieste...");
				
				// ricezione del datagramma
				try {
					packet.setData(buf);
					socket.receive(packet);
				}
				catch (IOException e) {
					System.err.println("Problemi nella ricezione del datagramma: "
							+ e.getMessage());
					e.printStackTrace();
					continue;
					// il server continua a fornire il servizio ricominciando dall'inizio
					// del ciclo
				}
				
				portClient=packet.getPort();
				addClient=packet.getAddress();

				try {
					biStream = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
					diStream = new DataInputStream(biStream);
					line1 = diStream.readInt();
					line2 = diStream.readInt();
					System.out.println("Cliente vuole scambiare linea " + line1 + "e linea " + line2);
				}
				catch (Exception e) {
					System.err.println("Problemi nella lettura della richiesta: ");
					e.printStackTrace();
					continue;
					// il server continua a fornire il servizio ricominciando dall'inizio
					// del ciclo
				}

				// preparazione della linea e invio della risposta
				try {
					esito = LineSwap.swapLine(fileName, line1, line2); 
					//swap line lancia IO Exe, cosa faccio se avviene
					boStream = new ByteArrayOutputStream();
					doStream = new DataOutputStream(boStream);
					doStream.writeInt(esito);
					data = boStream.toByteArray();
					packet.setData(data, 0, data.length);
					socket.send(packet);
				}
				catch (IOException e) {
					System.err.println("File non esiste");
					e.printStackTrace();
					
					continue;
					// il server continua a fornire il servizio ricominciando dall'inizio
					// del ciclo
				}

			} // while

		}
		// qui catturo le eccezioni non catturate all'interno del while
		// in seguito alle quali il server termina l'esecuzione
		catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("RawSwap: termino...");
		socket.close();

	}
	
	


}
