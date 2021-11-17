package es6;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;



/*
ha 2 param in ingresso che devono essere forniti 
dall’utente finale (nome file testo remoto e intero)
essi saranno passati come parametri nella procedura 
di invocazione remota,
il risultato sarà un intero che mi dice quante linee abbiamo
oppure potrebbe essere che il file non è disponibile o non è 
un file testo,... alzo eccezione remota
Hp: sequenza di caratteri separatori sono solo il bianco e 
il fine linea

 * 
 * */
public class ServerImpl extends UnicastRemoteObject implements iServer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;




	protected ServerImpl() throws RemoteException {
		super();

	}


	//codice in linea: non ho fatto nessuna classe di appoggio!!!
	/*
	 * In questo caso non ho bisogno di restituire un oggetto, in quento mi interessa riportare solo un intero
	 * per questo motivo non ho bisogno di implementare nessuna classe serializable!!!
	 * */
	@Override
	public long conta_righe(String nomeFile, long soglia) throws RemoteException {
		//leggo il file: devo restituire il numero di righe che hanno un numero di parole superiore alla soglia:

		/*leggo una stringa, dato che per ipotesi le parole sono separate dallo spazio,
		leggo una riga e la salvo in una stringa,
		poi spezzo la stringa usando il bianco come separatore (stringTokenizer)
		a questo punto, conto il numero di elementi dell'array!		
		 */
		int numero_righe=-1;//cate: imposto -1 come valore di errore

		try {


			BufferedReader b_in = new BufferedReader(new FileReader(nomeFile));


			String stringa;
			while ((stringa = b_in.readLine()) != null) {
				String[] parole = stringa.split(" ");
				if (parole.length >= soglia) numero_righe++;
			}
			b_in.close();

		} catch (IOException e) {

			e.printStackTrace();
		}

		return numero_righe;

	}




	/*
	è l’altra funzione nell’interfaccia:
	accetta il nome del file e il numero di riga da  eliminare;
	restituisce esito se linea viene eliminata o no
	+Eccezione remota!
	decidiamo noi quendo e se introdurre la mutua esclusione 
	(metodo synchronized della classe di appoggio???)

	 */

	@Override
	public synchronized  boolean elimina_righe(String nomeFile, long rigaDaEliminare) throws RemoteException {

		/*gestisco la mutua eslusione: introduco la sincronizzazione
		per evitare conflitti*/

		//eliminare la riga da un file in remoto:
		/*vuol dire che creo un nuovo file in cui scrivo tutte le righe
		 *eccetto quella che devo eliminare
		 *poi lo rinomino con il nome del file che ricevo come argomento:*/

		boolean esito=false;
		File file_appoggio = new File("file_appoggio");

		//controllo che il file esista:
		File file = new File(nomeFile);
		if (! file.exists())  throw new RemoteException(); //se il file non esiste, lancio eccezione!
		//la variabile file mi serve anche dopo per fare il rename!

		try {
			//devo scrivere sul file di appoggio
			//leggo dal file ricevuto come argomento!

			BufferedReader b_in = new BufferedReader(new FileReader(nomeFile));
			BufferedWriter b_out = new BufferedWriter(new FileWriter(file_appoggio));

			//scrivo la riga solo se non è quella da eliminare:

			String stringa;
			int count=0;
			while ((stringa = b_in.readLine()) != null) {
				if (count != rigaDaEliminare) {
					b_out.write(stringa + "\n");
					//quindi scrivo la linea solo se non è quella da eliminare
				}
				count++; //ho scritto una riga, quindi incremento
			}

			b_in.close();
			b_out.close();

			/*count è il numero di righe totale: se il numero di righe da eliminare è maggiore 
			 * del numero totale di righe, lancio remote exceptio
			 * (il numero è gratis, non ho dovuto fare niente "di più" per contare le righe:
			 * *quindi faccio il controllo e se il numero è troppo grande, lancio remote exception
			 * */
			if(rigaDaEliminare>count) throw new RemoteException(); 


			// Rename file (or directory)
			esito = file_appoggio.renameTo(file);



		} catch (IOException e) {

			e.printStackTrace();
		}




		return esito;
	}

	//main:

	public static void main (String[] args) // Codice di avvio del Server
	{ 
		//porta da default: Non la passo come argomento!

		/*-cosa deve fare il server prima di togliersi dal mezzo?
	deve creare la solita stringa fatta da host 
	(non avrò localhost ma avrò il nome dell’host), porta,
	 + nome (uguale a quello che ha il client)

	-poi si deve registrare (con stringa che è il nome completo)
		 */

		final int REGISTRYPORT = 1099;
		String registryHost ="127.0.0.1"; //non avrò localhost ma avrò il nome dell’host -> da guardare quale host!
		String serviceName = "Server";
		int registryPort=-1;
		//devo gestire il parametro del numero di porta: da specifica, tale parametro è opzionale:
		//se viene specificato, quello è la porta
		//se non viene specificato uso la porta di default

		if(args.length == 1) {
			//se specificato:
			registryPort = Integer.parseInt(args[0]);
		}else if(args.length == 0) {
			//se non specificato:
			registryPort = REGISTRYPORT;
		}else {
			//il server può avere o 0 o 1 argomento:
			System.out.println("Errore numero argomenti, la corretta invocazione è: Server [registryPort]");
			System.exit(1);
		}



		try
		{ 
			// Registrazione del servizio RMI
			String completeName = "//" + registryHost +	":" + registryPort + "/" + serviceName;

			/*creo la serverImpl (classe server)
-faccio la rebind a cui passo il nome completo, 
e siccome ho creato la serverImpl come variabile serverRMI;
quindi una volta che abbiamo il riferimento “remotizzabile” e il nome completo,
 la rebind prende i due parametri e lega su quel nome completo, dentro l’RMI registry, 
 il valore remotizzabile della variabile serverRMI che abbiamo appena creato in questo main!
			 */

			ServerImpl serverRMI = new ServerImpl();
			Naming.rebind (completeName, serverRMI);

		} // try
		catch (Exception e){ 
			e.printStackTrace();		
		}
	} /* main */ 
} // ServerImpl

