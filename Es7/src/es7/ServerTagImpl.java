package es7;


// Implementazione del Server RMI

import java.rmi.*;
import java.rmi.server.*;

public class ServerTagImpl extends UnicastRemoteObject implements ServerTag {
	private static final long serialVersionUID = 1L;
static Programma prog[];

  // Costruttore
  public ServerTagImpl() throws RemoteException {
    super();
  }

  // Richiede una prenotazione
  public int registrazione(int giorno, String sessione, String speaker)
      throws RemoteException {
    int numSess = -1;
    System.out.println("Server RMI: richiesta registrazione con parametri");
    System.out.println("giorno   = " + giorno);
    System.out.println("sessione = " + sessione);
    System.out.println("speaker  = " + speaker);

    if (sessione.startsWith("S")) {
      try {
        numSess = Integer.parseInt(sessione.substring(1)) - 1;
      } catch (NumberFormatException e) {
      }
    }

    // Se i dati sono sbagliati significa che sono stati trasmessi male e quindi
    // solleva una eccezione
    if (numSess == -1)
      throw new RemoteException();
    if (giorno < 1 || giorno > 3)
      throw new RemoteException();

    return prog[giorno - 1].registra(numSess, speaker);
  }

  // Ritorno il campo
  public Programma programma(int giorno) throws RemoteException {
    System.out.println("Server RMI: richiesto programma del giorno " + giorno);
    return prog[giorno - 1];
  }

  // Avvio del Server RMI
  @SuppressWarnings({"deprecation" })
public static void main(String[] args) {

    // creazione programma
    prog = new Programma[3];
    for (int i = 0; i < 3; i++)
      prog[i] = new Programma();
    int registryRemotoPort = 1099;
    String registryRemotoName = "RegistryRemoto";
    String serviceName = "ServerTag";
    String[] tags = {"congresso", "registrazione", "programma", "speaker"};

    // Controllo dei parametri della riga di comando
    if (args.length != 1 && args.length != 2) {
      System.out
          .println("Sintassi: ServerTagImpl NomeHostRegistryRemoto [registryPort], registryPort intero");
      System.exit(1);
    }
    String registryRemotoHost = args[0];
    if (args.length == 2) {
      try {
        registryRemotoPort = Integer.parseInt(args[1]);
      } catch (Exception e) {
        System.out
            .println("Sintassi: ServerTagImpl NomeHostRegistryRemoto [registryPort], registryPort intero");
        System.exit(2);
      }
    }

    // Impostazione del SecurityManager
    if (System.getSecurityManager() == null) {
      System.setSecurityManager(new RMISecurityManager());
    }

    // Registrazione del servizio RMI
    String completeRemoteRegistryName = "//" + registryRemotoHost + ":"
        + registryRemotoPort + "/" + registryRemotoName;

    try {
      RegistryRemotoTagServer registryRemoto = (RegistryRemotoTagServer) Naming
          .lookup(completeRemoteRegistryName);
      ServerTagImpl serverRMI = new ServerTagImpl();
      
      //aggiungo nome logico server
      registryRemoto.aggiungi(serviceName, serverRMI);
      System.out.println("Server RMI: Servizio \"" + serviceName
              + "\" registrato");
      
      //associa tag al nome logico del server
      for(int i=0;i<tags.length;i++) {
          registryRemoto.aggiungiTag(serviceName, tags[i]);
          System.out.println("Server RMI: Aggiunto tag \"" + tags[i] + "\" al nome logico "+serviceName);
      }

    } catch (Exception e) {
      System.err.println("Server RMI \"" + serviceName + "\": "+ e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }
}