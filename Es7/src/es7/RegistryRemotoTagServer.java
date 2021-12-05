package es7;


import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RegistryRemotoTagServer extends RegistryRemotoTagClient{
// Tabella: la prima colonna i nomi, la seconda i riferimenti remoti

	public Object[][] restituisciTutti() throws RemoteException;

	public boolean aggiungi(String nomeLogico, Remote riferimento) throws RemoteException;

	public boolean eliminaPrimo(String nomeLogico) throws RemoteException;

	public boolean eliminaTutti(String nomeLogico) throws RemoteException;
	
	/* aggiungiTag() torna false se:
	 * parametri nulli
	 * tag non presente tra quelli ammessi dal server
	 * non esiste almeno un server con nomeLogico
	 */
	public boolean aggiungiTag(String nomeLogico, String tag) throws RemoteException;
}