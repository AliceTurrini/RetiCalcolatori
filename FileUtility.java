package es2;
/* FileUtility.java */

import java.io.*;

public class FileUtility {

	//Nota: sorgente e destinazione devono essere correttamente aperti e chiusi  da chi invoca questa funzione 
	
		static protected void trasferisci(DataInputStream src, DataOutputStream dest) throws IOException {
			// ciclo di lettura da sorgente e scrittura su destinazione
		    int buffer;    
		    try {
		    	// esco dal ciclo all lettura di un valore negativo -> EOF
		    	//la funzione consuma l'EOF
		    	while ((buffer=src.read()) >= 0) {
		    		dest.write(buffer);
		    	}
		    	dest.flush(); //forces data to the underlying file output stream, svuoto il buffer
		    }catch (EOFException e) {
		    	System.out.println("Problemi con FileUtility, i seguenti: ");
		    	e.printStackTrace();
		    }
		}
}