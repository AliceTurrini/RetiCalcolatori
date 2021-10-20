package es2;

import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

public class Cliente {	

	public static void main(String[] args) {

		InetAddress addrS = null;
		int portS = -1;

		//controllo argomenti: IpServer PortaServer
		try{
			if(args.length == 2){
				addrS = InetAddress.getByName(args[0]);
				portS = Integer.parseInt(args[1]);
			} else{
				System.out.println("Usage: java PutFileClient serverAddr serverPort");
				System.exit(1);
			}
		}catch(Exception e){
			System.out.println("Usage: java PutFileClient serverAddr serverPort");
			System.exit(1);
		}

		// oggetti utilizzati dal client per la comunicazione e la lettura del file
		Socket socket = null;
		FileInputStream inFile = null;
		DataInputStream inSock = null;
		DataOutputStream outSock = null;
		String line = null, nomeDir=null;
		int soglia = 0;

		// creazione stream di input da tastiera:
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

		System.out.print("Inserisci nome Directory e la sua Lunghezza, oppure per uscire: \n^D(Unix)/^Z(Win): ");
		try{
			while ((line=stdIn.readLine()) != null){
				StringTokenizer st = new StringTokenizer(line);
				nomeDir = st.nextToken();
				soglia = Integer.parseInt(st.nextToken());
				if(soglia<=0) {
					System.out.println("Soglia negativa o nulla non valida");
					System.out.print("Inserisci nome Directory e la sua Lunghezza, oppure per uscire: \n^D(Unix)/^Z(Win): ");
					continue;
				}

				File dir= new File(nomeDir);
				if(dir.exists() && dir.isDirectory()){ 				// se la directory esiste creo la socket
					try{
						socket = new Socket(addrS, portS);
						//socket.setSoTimeout(30000);
						System.out.println("Creata la socket: " + socket);
					}
					catch(Exception e){
						System.out.println("Problemi nella creazione della socket: ");
						e.printStackTrace();
						System.out.print("Inserisci nome Directory e Lunghezza, oppure per uscire: \n^D(Unix)/^Z(Win): ");
						continue;
					}

					// creazione stream di input/output su socket
					try{
						inSock = new DataInputStream(socket.getInputStream());
						outSock = new DataOutputStream(socket.getOutputStream());
					}
					catch(IOException e){
						System.out.println("Problemi nella creazione degli stream su socket: ");
						e.printStackTrace();
						System.out.print("Inserisci nome Directory e Lunghezza, oppure per uscire: \n^D(Unix)/^Z(Win): ");
						continue;
					}
				}
				// se la richiesta non è corretta non proseguo!
				else{
					System.out.println("File non presente nel direttorio corrente!");
					System.out.print("Inserisci nome Directory e la sua Lunghezza, oppure per uscire: \n^D(Unix)/^Z(Win): ");
					continue;
				}

				String nomeFile = new String();
				for (File file : dir.listFiles()) {
					//controllo che lunghezza del file sia maggiore della soglia inserita dall'utente!
					if(file.isFile()) {//se è una directory non la devo inviare!
						if(file.length() >= soglia) { 
							nomeFile=file.getName();
							try{
								inFile = new FileInputStream(nomeFile); //per ogni file creo il suo stream per leggere dal file
							}catch(FileNotFoundException e){
								System.out.println("Problemi nella creazione dello stream di input dal file "+ file);
								e.printStackTrace();
								System.out.print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure immetti nome file: ");
								continue;
							}

							// trasmissione del nome del file al server:
							try{
								outSock.writeUTF(nomeFile);
								System.out.println("Inviato il nome del file " + nomeFile);
							}catch(Exception e){
								System.out.println("Problemi nell'invio del nome di " + nomeFile);
								e.printStackTrace();
								System.out.print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure immetti nome file: ");
								continue;
							}

							//aspetto che il server mi dica se va bene:
							String esito= inSock.readUTF();
							if(esito.equalsIgnoreCase("attiva")) {
								System.out.println("Inizio la trasmissione di " + nomeFile);
								// trasferimento file:
								try{
									outSock.writeLong( file.length());							 // mando lunghezza
									//									FileUtility.trasferisci(new DataInputStream(inFile), outSock);	 // mando file
									DataInputStream distream= new DataInputStream(inFile);
									int buffer;
									// esco dal ciclo all lettura di un valore negativo -> EOF
									//la funzione consuma l'EOF
									while ((buffer=distream.read()) >= 0) {
										outSock.write(buffer);
									}
									outSock.flush(); //forces data to the underlying file output stream, svuoto il buffer
									inFile.close(); 			// chiusura FileInputStream
									System.out.println("Trasmissione di " + nomeFile + " terminata ");
								}
								//							catch(SocketTimeoutException ste){
								//								System.out.println("Timeout scattato: ");
								//								ste.printStackTrace();
								//								socket.close();
								//								System.out
								//									.print("\n^D(Unix)/^Z(Win)+invio per uscire, oppure immetti nome file: ");
								//								// il client continua l'esecuzione riprendendo dall'inizio del ciclo
								//								continue;          
								//							}
								catch(Exception e){
									System.out.println("Problemi nell'invio di " + nomeFile);
									e.printStackTrace();
									continue;
								}	

							}else { //server: "Salta file"
								System.out.println("Il file "+nomeFile+" esiste già nella macchina server!");
								continue; 
							}

						}else {
							System.out.println("Il file "+file+" è di dimensione minore della soglia "+soglia+", non verrà inviato al server!");
						}
					}
				} //fuori dal ciclo for chiudo la socket (1 socket per 1 directory)
				socket.shutdownOutput(); 	// chiusura socket in upstream, invio l'EOF al server
				System.out.println("Trasmissione di " + nomeDir + " terminata ");

			}//fine while (che chiede dir all'utente)
		}catch(Exception e){
			System.err.println("Errore irreversibile, il seguente: ");
			e.printStackTrace();
			System.err.println("Chiudo!");
			System.exit(3); 
		}
	} // main


}
