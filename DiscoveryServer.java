package proposta1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class DiscoveryServer {


	public static void main(String[] args) {
		/*-------passaggio argomenti---------*/
		/*verrà sempre invocato con coppie 
		 * di argomenti che dicono al DS
		 *  dove si deve trovare un RS e con 
		 *  che file dovrà lavorare.
		 *  
		 *  
		 *  è un po’ un name server che si tiene
		una lista dei rs che ha generato con 
		associato ad ognuno di essi la sua porta 
		e il suo file, così può dare al cliente che 
		invoca il DS specificando il nome del file 
		(il cliente dovrà conoscere la porta del DS)
		il numero della porta a cui il cliente dovrà rivolgersi.
		 */

		/*-------Controllo argomenti:---------*/
		/*Il numero di porta deve essere diverso per ogni RS.
		Il nome del file deve essere diverso per ogni RS. */

		//OSS: devo avere un numero dispari di argomenti: tutte coppie + 1
		if(args.length%2 ==0 || args.length<3 ) {
			System.out.println("Usage: java DiscoveryServer portDS fileName1 port1 ... fileNameN portN");
			System.exit(1);
		}
		//la porta deve essere "giusta" e non duplicata	
		//il nome file deve essere non duplicato

		//il primo argomento è la ds port

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


		int flg_dup=0; //0=ok (ne porte ne file duplicati)	1=file duplicati	2=porte duplicate

		//se ho più di 3 argomenti devo controllare di non avere duplicati (meno di 3 non posso neanche avere duplicati):
		if (args.length > 3) { 		  
			for (i=0; i<num_coppie; i++) { //scorro tutti i file dell array dei nome file			  
				for (j=1; j<num_coppie && flg_dup==0; j++) { //scorro rimanenze del array del file
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

		//mi creo ste cose solo se passo i controlli:
		//sicuramente avrò bisogno di una socket e di un packet:
		DatagramSocket socket=null; //comunicazione con cliente
		DatagramPacket packet=null;
		byte[] buf = new byte[256];;

		for (i=0; i<num_coppie; i++) {
			//creo tutti i "figli" e li faccio partire
			RowSwap rss = new RowSwap(num_porte[i], nomi_file[i]);
			rss.start();; 		//CONTROLLARE!
		}

		try {
			socket = new DatagramSocket( Integer.parseInt(args[0])); 
			packet = new DatagramPacket(buf, buf.length);
			System.out.println("Creata la socket: " + socket);
			System.out.println("Nella socket io ho ip "+socket.getInetAddress()+" e port "+socket.getPort());

		}catch (SocketException | NumberFormatException  e) {
			System.out.println("Problemi nella creazione della socket: ");
			e.printStackTrace();
			//		System.exit(1);
		}

		try {
			int portaRS; //risultato da comunicare al cliente
			int porta_cliente=-1;

			InetAddress ip_cliente=null;
			String richiesta = null;

			ByteArrayInputStream biStream = null;
			DataInputStream diStream = null;

			ByteArrayOutputStream boStream = null;
			DataOutputStream doStream = null;

			//mi trasformo in un DEMONE (tutto dentro al while): aspetto le richieste dai clienti
			while (true) {
				System.out.println("\nIn attesa di richieste...");

				try {
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
					/*.buf the input buffer
					 *.offset the offset in the buffer of the first byte to read
					 *.length the maximum number of bytes to read from the buffer.*/

					diStream = new DataInputStream(biStream); //converto i bytes in stringa (ne ho una sola che è il nome del file ???)
					richiesta = diStream.readUTF();
					System.out.println("Richiesto file: " + richiesta);
				}
				catch (EOFException eof ) {
					System.err.println("Utente ha terminato l'operazione con il file: "+ richiesta);
					/*
				packet.setPort(porta_cliente);
	        	packet.setAddress(ip_cliente);
	        	String message= new String("Errore nella lettura, ");
				packet.setData(message.getBytes(), 0, message.length());
				socket.send(packet);
					 */
					eof.printStackTrace();

					continue; // il server continua a fornire il servizio ricominciando dall'inizio del ciclo
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

				//ora devo preparare a risposta in cui inserire la porta_row_swap necessaria al cliente:
				//a chi la mando??
				porta_cliente = packet.getPort();
				ip_cliente = packet.getAddress();

				try {
					boStream = new ByteArrayOutputStream();
					doStream = new DataOutputStream(boStream);
					doStream.writeInt(portaRS); 
					buf = boStream.toByteArray(); //array di byte con dentro la info della portaRS!!

					packet.setPort(porta_cliente);
					packet.setAddress(ip_cliente);

					packet.setData(buf, 0, buf.length);
					socket.send(packet);
					//mandiamo la riposta al client!!
				}catch (IOException e) {
					System.err.println("Problemi nell'invio della risposta: "+ e.getMessage());
					e.printStackTrace();
				}
			} //FINE while GIGANTE!

		}catch (Exception e) { // qui catturo le eccezioni non catturate all'interno del while
			e.printStackTrace();
		}

		System.out.println("DiscoveryServer: termino...");
		socket.close();

	}
}







