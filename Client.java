package proposta1;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

public class Client {
	public static void main(String[] args) {

		//Usage: IPDS portDS fileName
		InetAddress ipDS=null;
		int portDS = -1;
		String filename=null;

		/*CONTROLLO ARGOMENTI*/
		try {
			if (args.length == 3) {
				ipDS = InetAddress.getByName(args[0]);//lancia exception
				portDS = Integer.parseInt(args[1]);
				filename=new String(args[2]);
			} else {
				System.out.println("Usage: java Client IPDS portDS fileName");
				System.exit(1);
			}
		} catch (UnknownHostException e) {
			System.out
			.println("Problemi nella determinazione dell'endpoint del DSserver : ");
			e.printStackTrace();
			System.out.println("LineClient: interrompo...");
			System.exit(2);
		}

		DatagramSocket socket = null;	
		DatagramPacket packet = null;

		byte[] buf = new byte[256];	

		/*INTERAZIONE CLIENTE DS*/
		// creazione della socket datagram e creazione datagram packet
		try {
			socket = new DatagramSocket(portDS,ipDS);
			//			socket.setSoTimeout(30000);
			packet = new DatagramPacket(buf, buf.length);
			System.out.println("\nLineClient: avviato");
			System.out.println("Creata la socket: " + socket);
		} catch (SocketException e) {
			System.out.println("Problemi nella creazione della socket: ");
			e.printStackTrace();
			System.out.println("LineClient: interrompo...");
			System.exit(1);
		}

		//invio richiesta da cliente a ds
		ByteArrayOutputStream boStream = null;
		DataOutputStream doStream = null;
		byte[] data = null;
		int portaRS = -1;
		ByteArrayInputStream biStream = null;
		DataInputStream diStream = null;

		//riempimento e invio del pacchetto di richiesta
		try {
			boStream = new ByteArrayOutputStream();
			doStream = new DataOutputStream(boStream);
			doStream.writeUTF(filename);//scrivo la richiesta con il filename in un data output 
			data = boStream.toByteArray();//trasformo il data output in byte
			packet.setData(data);//creo il pacchetto richiesta fatto di byte
			socket.send(packet);
			System.out.println("Richiesta inviata a " + ipDS + ", " + portDS);
		} catch (IOException e) {
			System.out.println("Problemi nell'invio della richiesta al DS: ");
			e.printStackTrace();
			System.exit(-1);
		}
		//metto il cliente in ascolto per ricevere messaggio da ds
		try {
			// settaggio del buffer di ricezione
			packet.setData(buf);//dopo che ho inviato la richiesta posso usare lo stesso buf perchè invio una sola richiesta
			socket.receive(packet);
			// sospensiva (non ho settato timeout)
		} catch (IOException e) {
			System.out.println("Problemi nella ricezione del datagramma: ");
			e.printStackTrace();
			System.exit(-1);
		}	
		//sotto codice dopo che il client ha ricevuto dal DS il numero di porta del RowSwap
		//salvataggio numero di porta del row swap ricevuto dal ds
		try {
			biStream = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());//creo dato in byte per la ricezione di byte
			diStream = new DataInputStream(biStream);//incapsulo i byte ricevuti in un oggetto data stream
			portaRS = Integer.parseInt(diStream.readUTF());
			System.out.println("Risposta: numero porta row swap " + portaRS);
			if(portaRS<0) {
				System.out.println("Il DS non supporta il file "+filename);
				System.out.println("LineClient: termino...");
				System.exit(-2);
			}
		} catch (IOException e) {
			System.out.println("Problemi nella lettura della risposta: ");
			e.printStackTrace();
			System.exit(-1);
		}

		/*INTERAZIONE CLIENTE UTENTE*/		
		//ri-creo la socket tra cliente e rs
		//		socket = new DatagramSocket(portaRS,ipDS);//NB i rowswap e il ds sono nella stessa rete locale quindi uso lo stesso ip

		int righe[]=new int[2];
		String line=null;
		StringTokenizer st = new StringTokenizer(line);
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		System.out
		.print("\n^D(Unix)/^Z(Win)+invio per uscire, altrimenti inserisci le righe da scambiare: ");

		try {
			while ((line = stdIn.readLine()) != null) {
				//non uso for perchè troppo costoso
				try {
					righe[0] = Integer.parseInt(st.nextToken(line));
					righe[1] = Integer.parseInt(st.nextToken(line));
				} catch (NumberFormatException ne) {
					System.out.println("Problemi nella parseInt()");
					ne.printStackTrace();
					System.out
					.print("\n^D(Unix)/^Z(Win)+invio per uscire, altrimenti inserisci le righe da scambiare: ");
					continue;
				}

				/*INTERAZIONE CLIENTE ROW SWAP*/
				// riempimento e invio del pacchetto di richiesta dal cliente al RowSwap
				try {
					//siccome l'utente non deve più interagire con il DS allora utilizzo gli stessi stream e variabili

					doStream.writeInt(righe[0]);//scrivo il primo intero
					doStream.writeInt(righe[1]);//scrivo il secondo intero
					data = boStream.toByteArray();//trasformo il data output in byte
					packet.setPort(portaRS);
					packet.setData(data);//creo il pacchetto richiesta fatto di byte
					socket.send(packet);
					System.out.println("Richiesta inviata a " + ipDS + ", " + portaRS);
				} catch (IOException e) {
					System.out.println("Problemi nell'invio della richiesta: ");
					e.printStackTrace();
					System.out
					.print("\n^D(Unix)/^Z(Win)+invio per uscire, altrimenti inserisci le righe da scambiare: ");
					continue;
				}

				int esito=0;
				//metto il client in ascolto per l'esito dell'operazione mandata dal rs (un intero >0 se bene, -1 se male)
				try {
					// settaggio del buffer di ricezione
					packet.setData(buf);//dopo che ho inviato la richiesta posso usare lo stesso buf
					socket.receive(packet);
					// sospensiva sempre
					// SocketException
				} catch (IOException e) {
					System.out.println("Problemi nella ricezione del datagramma: ");
					e.printStackTrace();
					System.out
					.print("\n^D(Unix)/^Z(Win)+invio per uscire, altrimenti inserisci le righe da scambiare: ");
					continue;
				}
				try {
					biStream = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());//creo dato in byte per la ricezione di byte
					diStream = new DataInputStream(biStream);//incapsulo i byte ricevuti in un oggetto data stream
					esito = diStream.readInt();
					System.out.println("Esito: " + esito);
				} catch (IOException e) {
					System.out.println("Problemi nella lettura della risposta: ");
					e.printStackTrace();
					System.out
					.print("\n^D(Unix)/^Z(Win)+invio per uscire, altrimenti inserisci le righe da scambiare: ");
					continue;
				}
				//analizzo esito
				if(esito==-1) {
					System.out.println("L'esito è negativo: Row Swap con porta "+portaRS+"non è riuscito a scambiare le righe "+righe[0]+" "+righe[1]);
					System.out.println("\n^D(Unix)/^Z(Win)+invio per uscire, altrimenti inserisci altre righe da scambiare: ");
				}
				if(esito==-2) {
					System.out.println("L'esito è negativo: Row Swap con porta "+portaRS+"ha riscontrato degli errori di I/O nel file "+filename);
					System.exit(-2);//per specifiche del programma
				}
				else// tutto ok, pronto per nuova richiesta
					System.out
					.print("\n^D(Unix)/^Z(Win)+invio per uscire, altrimenti inserisci altre righe da scambiare: ");
			}
		}
		// qui catturo le eccezioni non catturate all'interno del while (tipo la ParseException)
		// in seguito alle quali il client termina l'esecuzione
		catch ( Exception  e) {
			e.printStackTrace();
		}


		System.out.println("LineClient: termino...");
		socket.close();
	}
}
