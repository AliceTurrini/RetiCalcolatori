package es7;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RegistryRemotoTagClient extends Remote{
	
	public Remote cerca(String nomeLogico)throws RemoteException;

	public Remote[] cercaTutti(String nomeLogico)throws RemoteException;
	
	public String[] cercaTag(String tag)throws RemoteException;
}