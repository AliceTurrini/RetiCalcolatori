package es1;

import java.io.*;

public class LineSwap {
	public static int swapLine(String file, int l1, int l2) {
		//restituisce -1 se errore linea,-2 se errore file, 1 se tutto ok
		if (l1 == l2) return 1;
		
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			StringBuilder sb = new StringBuilder();
			
			//leggo il file cercando linea1 e linea2
			int i=1;
			String line1=null, line2=null, temp;
			
			//mi salvo in l1 e l2 il contenuto delle rispettive linee
			while((temp=in.readLine()) != null) {
				if(i==l1) line1=temp;
				else if(i==l2) line2=temp;
				i++;
			}
			in.close();
			
			//argomento errato, numero di linea non presente nel file
			if((line1==null) || (line2==null)) return -1;
			
			//devo spostare il buffered reader all'inizio
			in = new BufferedReader(new FileReader(file));
			i=1;
			//riscrivo tutto il file in sb, invertendo l1 e l2
			while((temp=in.readLine()) != null) {
				if(i==l1) sb.append(line2);
				else if(i==l2) sb.append(line1);
				else sb.append(temp);
				
				sb.append("\n");
				i++;
			}
			in.close();
			
			//scrivo sul file il contenuto di sb
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			out.write(sb.toString());
			out.close();
			
			return 1;
		} catch (Exception e) {
			return -2;
		}
		
		
	}

}
