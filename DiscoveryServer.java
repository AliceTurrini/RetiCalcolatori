package proposta1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class DiscoveryServer {


	public static void main(String[] args) {
		//OSS: devo avere un numero dispari di argomenti: tutte coppie + 1
		if(args.length%2 ==0 || args.length<3 ) {
			System.out.println("Usage: java DiscoveryServer portDS fileName1 port1 ... fileNameN portN");
			System.exit(1);
		}

		int i, j=0;
		int num_coppie=(args.length-1)/2; //numero di RowSwap che verranno creati
		String nomi_file[]=new String[num_coppie]; //argomenti dispari
		int num_porte[]= new int[num_coppie]; //argomenti pari

		try {
			//nb: faccio i controlli solo se ho più di 3 argomenti
			for(i=1; i<args.length; i=i+2) {	 //mi muovo di due in due (coppie file-porta)
				//basta che ci sia un duplicato che blocco tutto e do errore

				//inserisco numeri negli array divisi:
				nomi_file[j]=args[i]; //pos 1, pos 3, pos 5
				num_porte[j]=Integer.parseInt(args[i+1]); //pos 2, pos 4, pos 6 aggiungo 1 perché devo stare sui pari

				//controllo numero porta valido
				if((num_porte[j] < 1024) || (num_porte[j] > 65535)) {
					System.out.println("Le porte devono essere comprese tra 1024 e 65535");
					System.exit(1);
				}
				j++;
			} //ho finito di riempire i due array

		}catch(NumberFormatException e) { //causato da ParseInt
			e.printStackTrace();
			System.out.println("Usage: java DiscoveryServer portDS fileName1 port1 ... fileNameN portN");
		}


		//se ho più di 3 argomenti devo controllare di non avere duplicati (meno di 3 non posso neanche avere duplicati):
		if (args.length > 3) { 		  
			for (i=0; i<num_coppie; i++) { //scorro tutti i file dell array dei nome file			  
				for (j=i+1; j<num_coppie; j++) { //scorro rimanenze del array del file
					if (nomi_file[i].equals(nomi_file[j]) ) {
						//se trovo due file uguali
						System.out.println("Devo avere nomi di file non duplicati!");
						System.exit(1);
					}
					if ( num_porte[i]==num_porte[j] ) {
						//se trovo due int uguali
						System.out.println("devo avere numeri di porte non duplicati");
						System.exit(1);
					}
				}	
			}
		}	//Fine controllo argomenti!

		DatagramSocket socket=null;
		DatagramPacket packet=null;
		byte[] buf = new byte[256];

		for (i=0; i<num_coppie; i++) {
			//creo tutti i "figli" e li faccio partire
			RowSwap rss = new RowSwap(num_porte[i], nomi_file[i]);
			rss.start();
		}

		try {
			socket = new DatagramSocket( Integer.parseInt(args[0])); 
			packet = new DatagramPacket(buf, buf.length);
			System.out.println("Creata la socket: " + socket);

		}catch (SocketException | NumberFormatException  e) {
			System.out.println("Problemi nella creazione della socket: ");
			e.printStackTrace();
			System.exit(1);
		}

		try {
			int portaRS; //risultato da comunicare al cliente
			String richiesta = null;

			ByteArrayInputStream biStream = null;
			DataInputStream diStream = null;

			ByteArrayOutputStream boStream = null;
			DataOutputStream doStream = null;

			//il discovery server si trasforma in un DEMONE (tutto dentro al while): aspetta le richieste dai clienti
			while (true) {
				System.out.println("\nIn attesa di richieste...");

				try {
					buf = new byte[256];
					packet.setData(buf);
					socket.receive(packet);
				}
				catch (IOException e) {
					System.err.println("Problemi nella ricezione del datagramma: "+ e.getMessage());
					e.printStackTrace();
					continue; // il server continua a fornire il servizio ricominciando dall'inizio del ciclo
				}
				//se arrivo qui vuol dire che ho ricevuto un pacchetto:

				try {
					//apro il pacchetto e mi salvo il nome del file dentro "richiesta"

					biStream = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
					diStream = new DataInputStream(biStream); //converto i bytes in stringa (ne ho una sola che è il nome del file ???)
					richiesta = diStream.readUTF();
					System.out.println("Richiesto file: " + richiesta);
				}
				catch( IOException e) {
					System.err.println("Problemi nella lettura della richiesta: "+ richiesta);
					e.printStackTrace();
					continue; // il server continua a fornire il servizio ricominciando dall'inizio del ciclo
				}

				//ora ricavo il numero della porta dello swap server che mi serve per quel file
				portaRS = -1; //per ogni richiesta lo devo riportare a -1?
				for (i=0; i < num_coppie && portaRS==-1; i++) {
					if (richiesta.equals(nomi_file[i])) {
						portaRS = num_porte[i];
					}
				}
				if(portaRS==-1) { //non ho trovato il file, errore!
					System.out.println("Il file richiesto dal cliente non è tra quelli disponibili!");
					System.exit(1);
				}

				try {
					boStream = new ByteArrayOutputStream();
					doStream = new DataOutputStream(boStream);
					doStream.writeInt(portaRS); 
					buf = boStream.toByteArray(); //array di byte con dentro la info della portaRS!!

					packet.setData(buf, 0, buf.length);
					socket.send(packet);

				}catch (IOException e) {
					System.err.println("Problemi nell'invio della risposta: "+ e.getMessage());
					e.printStackTrace();
				}
			} //FINE while

		}catch (Exception e) { // qui catturo le eccezioni non catturate all'interno del while
			e.printStackTrace();
		}

		System.out.println("DiscoveryServer: termino...");
		socket.close();

	}
}







