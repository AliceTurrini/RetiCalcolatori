package es6;

import java.rmi.Remote;
import java.rmi.RemoteException;



public interface iServer extends Remote {

	public long conta_righe(String nomeFile, long soglia) //cate: ho messo long perchè il file potrebbe essere grande
			throws RemoteException;
	//restituisce numero righe
	
	public boolean elimina_righe(String nomeFile, long riga) 
			throws RemoteException;
	//restituisce esito
	
}
