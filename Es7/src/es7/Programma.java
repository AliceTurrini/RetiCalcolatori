package es7;

/* Classe che implementa il Programma
 N.B.: è fondamentale che sia serializzabile perchè 
 deve essere restituita dal metodo remoto programma() */

import java.io.*;

public class Programma implements Serializable {
  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
public String speaker[][] = new String[12][5];

  public Programma() {
    for(int i = 0; i < 5; i++)
      for(int e = 0; e < 12; e++)
        speaker[e][i] = "";
  }

  public synchronized int registra(int sessione, String nome) {
    System.out.println("Programma: registrazione di " + nome
        + " per la sessione " + sessione);

    for(int k = 0; k < 5; k++){
      if(speaker[sessione][k] == ""){
        speaker[sessione][k] = nome;
        return 0;
      }
    }
    return 1;
  }

  public void stampa() {

    System.out
        .println("Sessione\tIntervento1\tIntervento2\tIntervento3\tIntervento4\tIntervento5\n");
    for(int k = 0; k < 12; k++){
      String line = new String("S" + (k + 1));
      for(int j = 0; j < 5; j++){
        line = line + "\t\t" + speaker[k][j];
      }
      System.out.println(line);

    }

  }

}