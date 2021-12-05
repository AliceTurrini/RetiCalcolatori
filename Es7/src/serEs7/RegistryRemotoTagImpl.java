
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;


@SuppressWarnings("deprecation")
public class RegistryRemotoTagImpl extends UnicastRemoteObject implements RegistryRemotoTagServer{ 
	
	private static final String[] tags = {"congresso", "registrazione", "programma", "speaker"};
	private static final long serialVersionUID = 1L;
	final int tableSize = 100;
	
	// Tabella: la prima colonna contiene i nomi, la seconda i riferimenti remoti, la terza i tag
	
	/* String nome		Remote riferimento		String[] tags
	 * 		s1				rif1					tag1,tag3
	 * 		...
	 * 		sn				rif_n						tag2		
	 */
	
	Object [][] table = new Object[tableSize][3];
	
	// Costruttore
	public RegistryRemotoTagImpl() throws RemoteException{
		super();
		for( int i=0; i<tableSize; i++ ){ 
			table[i][0]=null; table[i][1]=null; table[i][2]=null;
		}
	}
	
	//restituisce riferimento al primo server con stesso nome logico
	public synchronized Remote cerca(String nomeLogico) throws RemoteException{ 
		Remote risultato = null;
		if( nomeLogico == null ) return null;
		for( int i=0; i<tableSize; i++ )
			if( nomeLogico.equals((String)table[i][0]) ){
				risultato = (Remote) table[i][1];
				return risultato;
			}
		return risultato;
	}

	//restituisce tutti i riferimenti al nome logico
	public synchronized Remote[] cercaTutti(String nomeLogico) throws RemoteException{
		int cont = 0;
		if( nomeLogico == null ) return new Remote[0];
		for( int i=0; i<tableSize; i++ ) {
			if( nomeLogico.equals((String)table[i][0]) )
				cont++;
		}
		Remote[] risultato = new Remote[cont];
		// usato come indice per il riempimento
		cont=0;
		for( int i=0; i<tableSize; i++ )
			if( nomeLogico.equals((String)table[i][0]) )
				risultato[cont++] = (Remote)table[i][1];
		return risultato;
	}
	
	//restituisce nomelogico-riferimento-tag
	public synchronized Object[][] restituisciTutti() throws RemoteException{
		int cont = 0;
		for (int i = 0; i < tableSize; i++)
			if (table[i][0] != null) cont++;
		Object[][] risultato = new Object[cont][3];
		// usato come indice per il riempimento
		cont = 0;
		for (int i = 0; i < tableSize; i++)
			if (table[i][0] != null) {
				risultato[cont][0] = table[i][0];
				risultato[cont][1] = table[i][1];
				risultato[cont][2] = table[i][2];
			}
		return risultato;
	}
	
	//aggiunge una coppia
	public synchronized boolean aggiungi(String nomeLogico, Remote riferimento) throws RemoteException{
		boolean result = false;
		// Cerco la prima posizione libera e la riempio
		if((nomeLogico == null)||(riferimento == null))
			return result;
		for(int i=0; i<tableSize; i++) {
			if( table[i][0] == null ){
				table[i][0]= nomeLogico; table[i][1]=riferimento;
				result = true;
				return result;
			}
		}
		return result;
	}

	//rimuove il primo
	public synchronized boolean eliminaPrimo(String nomeLogico) throws RemoteException{
		boolean risultato = false;
		if( nomeLogico == null ) return risultato;
		for( int i=0; i<tableSize; i++ )
			if( nomeLogico.equals( (String)table[i][0]) )
			{ table[i][0]=null; table[i][1]=null; table[i][2]=null; 
			risultato=true;
			return risultato;
			}
		return risultato;
	}

	
	public synchronized boolean eliminaTutti(String nomeLogico) throws RemoteException{
		boolean risultato = false;
		if( nomeLogico == null ) return risultato;
		for( int i=0; i<tableSize; i++ )
			if( nomeLogico.equals((String)table[i][0]) )
			{ if( risultato == false ) risultato = true;
				table[i][0]=null;
				table[i][1]=null;
				table[i][2]=null;
			}
		return risultato;
	}

	public static void main (String[] args) {
		int registryRemotoPort = 1099;
		String registryRemotoHost = "localhost";
		String registryRemotoName = "RegistryRemoto";
		
		if (args.length != 0 && args.length != 1) // Controllo args
		{ System.out.println("..."); System.exit(1); }
		
		if (args.length == 1)
		{ try {registryRemotoPort =Integer.parseInt(args[0]); }
		catch (Exception e) {e.printStackTrace();}
		}
		
		if (System.getSecurityManager() == null)
			System.setSecurityManager(new RMISecurityManager());
		
		// Registrazione RegistryRemoto presso rmiregistry locale
		String completeName = "//" + registryRemotoHost + ":" +
				registryRemotoPort + "/" + registryRemotoName;
		try{ 
			RegistryRemotoTagImpl serverRMI = new RegistryRemotoTagImpl();
			Naming.rebind(completeName, serverRMI);
		} catch (Exception e) {e.printStackTrace();}
	}

	//ritorna NOMI LOGICI con tag cercato
	public String[] cercaTag(String tag) throws RemoteException { //ser: va synchronized??
		if( tag == null ) return new String[0];
		
		//controllo se il tag è presente tra quelli ammessi nel registri
		boolean result=false;
		for(int i=0; i<tags.length; i++) {
			if(tags[i].equals(tag))
				result=true;
		}
		if(result==false) throw new RemoteException();
	
		int cont = 0;
		String[] t;
		
		for( int i=0; i<tableSize; i++ ) {
			t=(String[]) table[i][2]; //salvo in t i tag dell'i-esimo elemento
			
			if(t!=null) {
				//scorro l'array di stringhe che contiene i tag
				for(int j=0; j<t.length; j++) {
					if(t[j]!=null && t[j].equals(tag)) { 
						cont++;
					}
				}
			}
		}
		String[] risultato = new String[cont];
		//usato come indice per il riempimento
		cont=0;
		for( int i=0; i<tableSize; i++ ) {
			t=(String[]) table[i][2];
			if(t!=null) {
				for(int j=0; j<t.length; j++) {
					if(t[j]!=null && t[j].equals(tag)) { 
						risultato[cont++] = (String) table[i][0];
					}
				}
			}
		}
		return risultato;
	}

	@Override
	public synchronized boolean aggiungiTag(String nomeLogico, String tag) throws RemoteException {
		System.out.println("Ricevuto tag "+tag+"\n");
		boolean result = false;
		// Cerco la prima posizione libera e la riempio
		if((nomeLogico == null)||(tag == null))
			return result;
		
		//controllo se il tag è presente tra quelli ammessi nel registri
		for(int i=0; i<tags.length; i++) {
			if(tags[i].equals(tag))
				result=true;
		}
		
		if(result==false) throw new RemoteException();
		result=false;
		String[] temp;
		
		for(int i=0; i<tableSize; i++) {
			//scorro elementi table finchè non trovo un server denominato nomeLogico
			if( table[i][0]!=null  && ((String) table[i][0]).equals(nomeLogico)){ 
				if(table[i][2]!=null) { //array di tag già presente
					temp=(String[]) table[i][2]; //salvo i tag associati al server in una variabile temporanea
					boolean presente=false;
					for(int j=0; j<tags.length; j++) {
						if(temp[j]!=null && temp[j].equals(tag)) presente=true;
					}
					
					if(!presente) {  //se il tag è già presente non lo devo aggiungere
						for(int j=0;j<tags.length;j++) {
							if(temp[i]==null) {
								temp[i]=tag;
								break;
							}
						}
						table[i][2]=temp;
						result = true;
					}
				}
				else { //non è ancora presente alcun tag
					temp=new String[tags.length];
					temp[0]=tag;
					table[i][2]=temp;
					result = true;
				}
			}
		}
		return result;
	}


}