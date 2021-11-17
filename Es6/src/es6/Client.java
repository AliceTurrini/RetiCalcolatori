package es6;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.rmi.Naming;

public class Client {
	public static void main(String[] args ){
		final int REGISTRYPORT = 1099;
		String registryHost = null;
		String serviceName = "Server";
		BufferedReader stdIn = new BufferedReader(new InputStreamReader (System.in));



		//1)contare le righe che contengono un numero di parole superiore ad un intero espresso dal cliente
		//2)eliminare una riga da un file remoto. Il client invia file e numero di riga

		// Controllo dei parametri della riga di comando
		try{
			if ( args.length != 1){
				System.out.println ("Usage: registryHost\n" );
				System.exit (1);
			}
			registryHost= args [0];
			//Connessione al servizio RMI remoto
			String completeName = "//" + registryHost + ":" +REGISTRYPORT + "/" +serviceName;
			iServer serverRMI =(iServer ) Naming.lookup( completeName);
			System.out.println("\nI servizi disponibili sono:");
			System.out.print("(C=Conta righe, E=Elimina righe)\n");
			System.out.println("Selezionare il servizio con la lettera corrispondente oppure EOF per terminare\n");

			//variabili utilizzate nella logica di programma
			//1) String nomeFile; int soglia 
			String service; boolean ok;

			// Ciclo di interazione con l’utente per chiedere operazioni
			while((service=stdIn.readLine ())!=null){
				if (service.equals ("C")){
					ok=false; long soglia=-1;// lettura soglia
					System.out.print("Soglia? ");
					while (ok!=true){
						soglia =Integer.parseInt (stdIn.readLine());
						if(soglia < 0)//considero file indicizzati da riga 0
						{
							System.out.println ("Soglia non valida");
							System.out.print("Soglia?");
							continue;
						} else ok=true;
					} // while interno
					System.out.println("Ho letto il numero di soglia: "+soglia);
					ok=false;
					String nomeFile=null ; // lettura nome file
					System.out.print("Nome file? ");
					while (ok!=true){
						nomeFile= stdIn.readLine();
						if ( !nomeFile.equals (""))//controllo che si riesco ad aprire il file					
							continue; 
						else ok=true;
					}// while interno
					System.out.println("Ho letto il nome file: "+nomeFile);
					long ris=-1;
					// Parametri corretti, invoco il servizio remoto
					if((ris=serverRMI.conta_righe(nomeFile, soglia))>=0) {
						System.out.println("Risultato conta righe: "+ris);
					}
					else
						System.out.println ("Operazione non effettuata");
				}
				else if (service.equals ("E")){
					ok=false; long riga=-1;// lettura riga
					System.out.print("Riga? ");
					while (ok!=true){
						riga =Integer.parseInt (stdIn.readLine());
						if(riga < 0)//considero file indicizzati da riga 0
						{
							System.out.println ("Riga non valida");
							System.out.print("Riga?");
							continue;
						} else ok=true;
					} // while interno riga op 2
					System.out.println("Ho letto il numero di riga: "+riga);
					ok=false;
					String nomeFile=null ; // lettura nome file
					System.out.print("Nome file? ");
					while (ok!=true){
						nomeFile= stdIn.readLine();
						if ( !nomeFile.equals (""))//controllo che si riesco ad aprire il file					
							continue; 
						else ok=true;
					}// while interno nome file op 2
					System.out.println("Ho letto il nome file: "+nomeFile);

					// Parametri corretti, invoco il servizio remoto
					if((serverRMI.elimina_righe(nomeFile, riga))==true) {
						System.out.println("Operazione effettuata con successo");
						System.out.println("Riga "+riga+" eliminata dal file "+nomeFile);
					}
					else
						System.out.println ("Operazione non effettuata");
				}

				
				System.out.println("\nI servizi disponibili sono:");
				System.out.print("(C=Conta righe, E=Elimina righe)\n");
				System.out.println("Selezionare il servizio con la lettera corrispondente oppure EOF per terminare\n");
			}//while
		}//try
		catch (Exception e){ e.printStackTrace(); System.exit(2); }

	}
}
