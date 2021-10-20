package es2;

import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

public class Cliente {	
	
	public static void main(String[] args) {
		   
		InetAddress addrS = null;
		int portS = -1;
		//commento prova
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
					continue;
				}
				
				File[] fileDir = dir.listFiles();
				String nomeFile = new String();
				
				for (File file : fileDir) {
					if(file.length() >= soglia) { //controllo che lunghezza del file sia maggiore della soglia inserita dall'utente!
						nomeFile=file.getName();
						try{
							inFile = new FileInputStream(nomeFile); //per ogni file creao il suo strem per leggere dal file
						}catch(FileNotFoundException e){
							System.out.println("Problemi nell'apetura del file "+ file);
							e.printStackTrace();
							continue;
						}
						
						// trasmissione del nome del file al server:
						try{
							outSock.writeUTF(file.getName());
							System.out.println("Inviato il nome del file " + nomeFile);
						}catch(Exception e){
							System.out.println("Problemi nell'invio del nome di " + nomeFile);
							e.printStackTrace();
							continue;
						}
						
						//aspetto che il server mi dica se va bene:
						String esito = inSock.readUTF();
						if(esito.equalsIgnoreCase("attiva")) {
							//trasmissione file:
							System.out.println("Inizio la trasmissione di " + nomeFile);
							try{ 	
								outSock.writeInt((int) file.length());	// mando lunghezza
								
								for(int i=0;i<file.length(); i++) {		// mando contenuto
									outSock.write(inFile.read());	
								}
								outSock.flush(); //sicuri?
								inFile.close();
								
								System.out.println("Trasmissione di " + nomeFile + " terminata");
							}catch(Exception e){
								System.out.println("Problemi nell'invio di " + nomeFile);
								e.printStackTrace();
								continue;
							}	
						}else { //server: "Salta"
							System.out.println("Il file "+nomeFile+" esiste già nella macchina server!");
							continue; 
						}
						
					}else { //grandezza file < soglia :(
						System.out.println("Il file "+file+" è troppo piccolo, non verrà inviato al server!");
					}
					
				}//fine for
				
				//fuori dal ciclo for chiudo la socket (1 socket - 1 directory)
				socket.shutdownInput();
				socket.shutdownOutput(); 	// chiusura socket in upstream, invio l'EOF al server
				System.out.println("Trasmissione di " + nomeDir + " terminata ");
			
			} //fine while (che chiede dir all'utente)
		}catch(Exception e){
			System.err.println("Errore irreversibile, il seguente: ");
			e.printStackTrace();
			System.err.println("Chiudo!");
			System.exit(3); 
	    }
		
	} // main
	

}
