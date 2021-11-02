/* OpDatagram_client: richiede la valutazione di un'operazione tra due interi */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <string.h>
#include <fcntl.h>

#define LINE_LENGTH 256
#define STRING_LENGTH 64
#define DIM_BUFF 32
#define MAX_FILE_LENGTH 1024

//un filtro è un programma che riceve in ingresso da input e produce uno opiù risultati in output.
//Un filtro deve consumare tutto lo stream in ingresso e portare in uscita il contenuto filtrato



int main(int argc, char **argv){
	struct hostent *host;
	struct sockaddr_in clientaddr, servaddr;
	int  port, sd, num1, num2, len,  ok;
	char okstr[LINE_LENGTH];
	int num_riga;


	//mie var
	int fd, tot_righe, nread=0;
	char nomeFile[STRING_LENGTH]; char ris[MAX_FILE_LENGTH]; char buff[DIM_BUFF];
	char ch;


	/* CONTROLLO ARGOMENTI ---------------------------------- */
	if(argc!=3){
		printf("[ClientStream] Error:%s serverAddress serverPort\n", argv[0]);
		exit(1);
	}

	/* INIZIALIZZAZIONE INDIRIZZO SERVER -------------------------- */
	memset((char *)&servaddr, 0, sizeof(struct sockaddr_in));
	servaddr.sin_family = AF_INET;
	host = gethostbyname(argv[1]);

	/* Passando 0 ci leghiamo ad un qualsiasi indirizzo libero,
	 * ma cio' non funziona in tutti i sistemi.
	 * Se nel nostro sistema cio' non funziona come si puo' fare?
	 */
	clientaddr.sin_port = 0;

	memset((char *)&servaddr, 0, sizeof(struct sockaddr_in));
	servaddr.sin_family = AF_INET;
	host = gethostbyname (argv[1]);

	/* VERIFICA INTERO */
	num1=0;
	while( argv[2][num1]!= '\0' ){
		if( (argv[2][num1] < '0') || (argv[2][num1] > '9') ){
			printf("[ClientStream] Secondo argomento non intero\n");
			printf("[ClientStream] Error:%s serverAddress serverPort\n", argv[0]);
			exit(EXIT_FAILURE);
		}
		num1++;
	}
	port = atoi(argv[2]);

	/* VERIFICA PORT e HOST */
	if (port < 1024 || port > 65535){
		printf("%s = porta scorretta...\n", argv[2]);
		exit(2);
	}
	if (host == NULL){
		printf("%s not found in /etc/hosts\n", argv[1]);
		exit(2);
	}else{
		servaddr.sin_addr.s_addr=((struct in_addr *)(host->h_addr))->s_addr;
		servaddr.sin_port = htons(port);
	}

	//INTERAZIONE UTENTE: utente dice nome file e numero linea da eliminare in quel file
	//assumo che questo avvenga in 2 passaggi diversi
	printf("[ClientStream] Inserire nome file remoto, EOF per terminare: \n");
	//gets è sospensiva, prende in input il nome del file passato
	//se invece viene passato EOF allora termina
	//la funzione gets acquisisce una stringa da tastiera compresi eventuali spazi e ritorno a capo
	// il quale viene trasformato in carattere terminatore
	while(gets(nomeFile)!=NULL){
		//client cicla fino a che il cliente non immette EOF
				if((fd = open(nomeFile, O_RDONLY)) < 0){
					perror("[ClientStream] Apertura file non avvenuta\n");
					exit(EXIT_FAILURE);
				}

				//chiedo all'utente le righe da eliminare
				//le righe partono da indice 1
				printf("[ClientStream] Inserire numero riga da eliminare e premere invio:\n");
				//la scanf ritorna 0 se c'è stato input, ma non è riuscita a fare la conversione
				//la scanf ritorna un valore EOF se ha letto EOF e quindi non ha convertito nlla
				//la scanf ritorna il numero di valori letti (nel nostro caso deve ritornare 1)
				//dato che il num riga p un long unsigned int, metto il formato %lu
				while( (ok=scanf("%lu", &num_riga) !=1 )){
					/* Problema nell'implementazione della scanf.
					Se l'input contiene PRIMA dell'intero altri caratteri la testina di lettura si blocca sul primo carattere
					 * (non intero) letto. Ad esempio: ab1292\n
					 *				  ^     La testina si blocca qui
					 * Bisogna quindi consumare tutto il buffer in modo da sbloccare la testina.
					 */
					printf("[ClientStream] Non è stato possibile riconoscere il numero di riga inserito:\n");
					do {ch=getchar(); printf("%c ", ch);}
					while (ch!= '\n');
					printf("[ClientStream] Inserire numero riga da eliminare e premere invio:\n");
				}

				//trasferire contenuto file al server
				/* CREAZIONE SOCKET ------------------------------------ */
				sd=socket(AF_INET, SOCK_STREAM, 0);
				if(sd<0) {perror("apertura socket"); exit(1);}
				printf("Client: creata la socket sd=%d\n", sd);

				/* Operazione di BIND implicita nella connect */
				if(connect(sd,(struct sockaddr *) &servaddr, sizeof(struct sockaddr))<0)
				{perror("connect"); exit(1);}
				printf("Client: connect ok\n");

				//trasferimento numero riga da eliminare
				write(sd, &num_riga, sizeof(long));
				printf("[ClientStream] Spedita riga da eliminare\n");

				//faccio una scrittura bufferizzata
				/*INVIO File*/
				printf("Client: stampo e invio file da ordinare\n");
				lseek(fd, SEEK_SET, 0);
				while((nread=read(fd, buff, DIM_BUFF))>0){
					write(1,buff,nread);	//stampa
					write(sd,buff,nread);	//invio
				}
				printf("\n[ClientStream] Spedito contenuto file\n");
				close(fd);
				/* Chiusura socket in spedizione -> invio dell'EOF */
				shutdown(sd,1);

				/*RICEZIONE File*/
				printf("Client: ricevo e stampo file modificato\n");
				if((fd = open(nomeFile, O_TRUNC|O_WRONLY)) < 0){
							perror("\n[ClientStream] Apertura file non avvenuta\n");
							exit(EXIT_FAILURE);
						}

				while((nread=read(sd,buff,DIM_BUFF))>0){
					write(1,buff,nread);
					write(fd,buff,nread);
				}

				close(fd);
				/* Chiusura socket in ricezione */
				shutdown(sd, 0);
				close(sd);
				printf("\n[ClientStream] Inserisci nomefile remoto, EOF per terminare: \n");

	}
	printf("\n[ClientStream] termino...\n");
	exit(EXIT_SUCCESS);

}//ClientStream.c
