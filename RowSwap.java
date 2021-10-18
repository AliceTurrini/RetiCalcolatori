package proposta1;

import java.io.*;
import java.net.*;

public class RowSwap extends Thread{
	private final String fileName;
	private final int port;
	//una volta inizializzata un'istanza di RawSwap, il nome del file e la porta non cambiano
	//per questo gli attributi sono stati etichettati con "final"

	public RowSwap(int port, String fileName) {
		this.fileName = fileName;
		this.port=port;
	}

	public void run() {
		DatagramSocket socket = null;
		DatagramPacket packet = null;
		byte[] buf_ricezione = new byte[256];

		//preparo socket per la ricezione
		try {
			socket = new DatagramSocket(port);
			packet = new DatagramPacket(buf_ricezione, buf_ricezione.length);
		}
		catch (SocketException e) {
			System.out.println("RowSwap: Problemi nella creazione della socket: ");
			e.printStackTrace();
			System.exit(1);
		}

		try {
			int line1, line2, esito;
			ByteArrayInputStream biStream = null;
			ByteArrayOutputStream boStream = null;
			DataInputStream diStream = null;
			DataOutputStream doStream = null;
			byte[] data_invio = null;

			//DEMONE
			while (true) {
				line1=-1; line2=-1; esito=-3;
				//resetto valori


				// ricezione del datagramma
				try {
					packet.setData(buf_ricezione);
					socket.receive(packet);
					System.out.println("RowSwap: sono in ascolto sulla porta: "+socket.getPort());
					System.out.println("RowSwap: in attesa di richieste...");
					//sospensiva, mi metto in attesa di richieste...
				}
				catch (IOException e) {
					System.err.println("RowSwap: Problemi nella ricezione del datagramma");
					e.printStackTrace();
					esito=-2;
				}


				try {
					biStream = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
					diStream = new DataInputStream(biStream);
					line1 = diStream.readInt();
					line2 = diStream.readInt();
					//redInt return the next four bytes of this input stream, interpreted as an int
					System.out.println("RowSwap: Cliente vuole scambiare linea " + line1 + " e linea " + line2);
				}
				catch (Exception e) {
					System.err.println("RowSwap: Problemi nella lettura della richiesta");
					e.printStackTrace();
					esito=-1;
				}

				// preparazione della linea e invio della risposta
				try {
					if(esito!=-1 && esito!=-2)
						esito = LineSwap.swapLine(fileName, line1, line2);
					//stampo messaggio dell'operazione in base all'esito di LineSwap
					if(esito==-1) System.out.println("RowSwap: Errore operazione, righe inserite non valide");
					if(esito==-2) System.out.println("RowSwap: Errore operazione, errore nell' IO da file");
					else if(esito>0) System.out.println("RowSwap: Operazione eseguita con successo");

					boStream = new ByteArrayOutputStream();
					doStream = new DataOutputStream(boStream);
					doStream.writeInt(esito);
					data_invio = boStream.toByteArray();
					packet.setData(data_invio, 0, data_invio.length);
					socket.send(packet);
				}
				catch (IOException e) {
					System.err.println("RowSwap: Errore scrittura stream");
					e.printStackTrace();
					continue;
				}

			} // while

		}
		// qui catturo le eccezioni non catturate all'interno del while
		// in seguito alle quali il server termina l'esecuzione
		catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("RowSwap: termino...");
		socket.close();

	}

}
