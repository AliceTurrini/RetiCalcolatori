// Implementazione del Client RMI

import java.rmi.*;
import java.io.*;

public class ClientTag {

  // Avvio del Client RMI
  @SuppressWarnings("deprecation")
public static void main(String[] args) {
    int registryRemotoPort = 1099;
    String registryRemotoHost = null;
    String registryRemotoName = "RegistryRemoto";
    String[] serviceName = null;
    String serviceTag = "congresso";
    BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

    // Controllo dei parametri della riga di comando
    if (args.length != 1 && args.length != 2) {
    	System.out.println("Sintassi: ClientCongresso NomeHostRegistryRemoto [registryPort], registryPort intero");
    	System.exit(1);
    }
    registryRemotoHost = args[0];
    if (args.length == 2) {
      try {
        registryRemotoPort = Integer.parseInt(args[1]);
      } catch (Exception e) {
        System.out.println("Sintassi: ClientCongresso NomeHostRegistryRemoto [registryPort], registryPort intero");
        System.exit(2);
      }
    }

    // Impostazione del SecurityManager
    if (System.getSecurityManager() == null) {
      System.setSecurityManager(new RMISecurityManager());
    }

    // Connessione al servizio RMI remoto
    try {
      String completeRemoteRegistryName = "//" + registryRemotoHost + ":"
          + registryRemotoPort + "/" + registryRemotoName;
      RegistryRemotoTagClient registryRemoto = (RegistryRemotoTagClient) Naming.lookup(completeRemoteRegistryName);
      
      //si potrebbe fare un ciclo, se non lo trovo subito attendo un certo tempo e poi riprovo fino a max di volte (?)
      serviceName = registryRemoto.cercaTag(serviceTag);
      if(serviceName.length==0) {
    	  System.out.println("Nessun server associato al tag "+ serviceTag);
          System.exit(2);
      }
      
      ServerTag serverRMI = (ServerTag) registryRemoto.cerca(serviceName[0]);
      System.out.println("ClientRMI: Servizio \"" + serviceName + "\" connesso");

      System.out.println("\nRichieste di servizio fino a fine file");

      String service;
      System.out.print("Servizio (R=Registrazione, P=Programma del congresso): ");

      while ((service = stdIn.readLine()) != null) {

        if (service.equals("R")) {

          boolean ok = false;

          int g = 0;
          System.out.print("Giornata (1-3)? ");
          while (ok != true) {
            g = Integer.parseInt(stdIn.readLine());
            if (g < 1 || g > 3) {
              System.out.println("Giornata non valida");
              System.out.print("Giornata (1-3)? ");
              continue;
            } else
              ok = true;
          }

          ok = false;
          String sess = null;
          System.out.print("Sessione (S1 - S12)? ");

          while (ok != true) {

            sess = stdIn.readLine();

            if (!sess.equals("S1") && !sess.equals("S2") && !sess.equals("S3")
                && !sess.equals("S4") && !sess.equals("S5")
                && !sess.equals("S6") && !sess.equals("S7")
                && !sess.equals("S8") && !sess.equals("S9")
                && !sess.equals("S10") && !sess.equals("S11")
                && !sess.equals("S12")) {
              System.out.println("Sessione non valida");
              System.out.print("Sessione (S1 - S12)? ");
              continue;
            } else
              ok = true;
          }

          System.out.print("Speaker? ");
          String speak = stdIn.readLine();
          // nessun controllo, qualsiasi stringa potrebbe essere un nome
          // accettabile

          // Tutto corretto
          if (serverRMI.registrazione(g, sess, speak) == 0)
            System.out.println("Registrazione di " + speak
                + " effettuata per giornata " + g + " sessione " + sess);
          else
            System.out.println("Sessione piena: giornata" + g + " sessione "
                + sess);
        } // R

        else if (service.equals("P")) {
          int g = 0;
          boolean ok = false;
          System.out.print("Programma giornata (1-3)? ");

          while (ok != true) {
            // intercettare la NumberFormatException
            g = Integer.parseInt(stdIn.readLine());
            if (g < 1 || g > 3) {
              System.out.println("Giornata non valida");
              System.out.print("Programma giornata (1-3)? ");
              continue;
            } else
              ok = true;
          }
          System.out.println("Ecco il programma: ");
          serverRMI.programma(g).stampa();

        } // P

        else
          System.out.println("Servizio non disponibile");

        System.out
            .print("Servizio (R=Registrazione, P=Programma del congresso): ");
      } // !EOF

    } catch (Exception e) {
      System.err.println("ClientRMI: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }
}