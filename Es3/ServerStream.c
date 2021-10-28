/* Server che trova la lunghezza della parola più lunga */
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <signal.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <string.h>

#define MAX_NOMEFILE 64
#define MAX_STRINGA 1024
#define MAX_FILE_LENGTH 1024
#define SIZE_BUFF 64

/*Struttura di una richiesta*/
typedef struct{
	long num_riga;//numero della riga da eliminare
}Request;

/********************************************************/
void gestore(int signo){
	int stato;
	printf("esecuzione gestore di SIGCHLD\n");
	wait(&stato);
}
/********************************************************/

int main(int argc, char **argv){
	int  listen_sd, conn_sd;
	int port, len, num;
	const int on = 1;
	struct sockaddr_in cliaddr, servaddr;
	struct hostent *host;
	char buff[SIZE_BUFF];

	Request* req = NULL;

	/* CONTROLLO ARGOMENTI ---------------------------------- */
	if(argc!=2){
		printf("Error: %s port\n", argv[0]);
		exit(1);
	}
	else{
		num=0;
		while( argv[1][num]!= '\0' ){
			if( (argv[1][num] < '0') || (argv[1][num] > '9') ){
				printf("Secondo argomento non intero\n");
				exit(2);
			}
			num++;
		}
		port = atoi(argv[1]);
		if (port < 1024 || port > 65535){
			printf("Error: %s port\n", argv[0]);
			printf("1024 <= port <= 65535\n");
			exit(2);
		}

	}


	/* INIZIALIZZAZIONE INDIRIZZO SERVER ----------------------------------------- */
	memset ((char *)&servaddr, 0, sizeof(servaddr));
	servaddr.sin_family = AF_INET;
	servaddr.sin_addr.s_addr = INADDR_ANY;
	servaddr.sin_port = htons(port);

	/* CREAZIONE E SETTAGGI SOCKET D'ASCOLTO --------------------------------------- */
	listen_sd=socket(AF_INET, SOCK_STREAM, 0);
	if(listen_sd <0)
	{perror("creazione socket "); exit(EXIT_FAILURE);}
	printf("Server: creata la socket d'ascolto per le richieste di ordinamento, fd=%d\n", listen_sd);

	if(setsockopt(listen_sd, SOL_SOCKET, SO_REUSEADDR, &on, sizeof(on))<0)
	{perror("set opzioni socket d'ascolto"); exit(EXIT_FAILURE);}
	printf("Server: set opzioni socket d'ascolto ok\n");

	if(bind(listen_sd,(struct sockaddr *) &servaddr, sizeof(servaddr))<0)
	{perror("bind socket d'ascolto"); exit(EXIT_FAILURE);}
	printf("Server: bind socket d'ascolto ok\n");

	if (listen(listen_sd, 5)<0) //creazione coda d'ascolto
	{perror("listen"); exit(EXIT_FAILURE);}
	printf("Server: listen ok\n");

	/* AGGANCIO GESTORE PER EVITARE FIGLI ZOMBIE,
	 * Quali altre primitive potrei usare? E' portabile su tutti i sistemi?
	 * Pregi/Difetti?
	 * Alcune risposte le potete trovare nel materiale aggiuntivo!
	 */
	signal(SIGCHLD, gestore);

	/* CICLO DI RICEZIONE RICHIESTE */
	//la recvfrom riceve una richiesta con connessione (senza specificare da chi, non c'è il randez-vous) --> Datagram
	//quindi associano una socket ad ogni richiesta
	//delego ad un figlio, padre fa subito un'altra recv!!

	req=(Request*)malloc(sizeof(Request));

	/* CICLO DI RICEZIONE RICHIESTE --------------------------------------------- */
	for(;;){
		len=sizeof(cliaddr);
		if((conn_sd=accept(listen_sd,(struct sockaddr *)&cliaddr,&len))<0){
			/* La accept puo' essere interrotta dai segnali inviati dai figli alla loro
			 * teminazione. Tale situazione va gestita opportunamente. Vedere nel man a cosa
			 * corrisponde la costante EINTR!*/
			if (errno==EINTR){
				perror("Forzo la continuazione della accept");
				continue;
			}
			else exit(EXIT_FAILURE);
		}

		if (fork()==0){ // figlio
			char c; int nread, riga_corrente=1;//file indicizzato con indice che parte da 1

			//leggo il numero di riga
			if((nread=read(conn_sd, &req->num_riga, sizeof(int)))<0){
				printf("(PID %d) impossibile leggere il numero della riga da eliminare dalla socket %d\n", getpid(), conn_sd);
				perror("Errore!");

				//mando comunque esito al cliente!
				strcpy(buff,"ERRORE LETTURA NUMERO DI RIGA, RIPETERE L'OPERAZIONE");
				if (write(conn_sd, buff, sizeof(buff))<0){
					printf("Figlio (pid %d): errore nella sendto \n", getpid());
					free(req);
					exit(EXIT_FAILURE);
				}
				//chiudo la connessione quando non riesce la lettura ?
				//					close(conn_sd);
				//					free(req);
				//					exit(EXIT_FAILURE);
				continue;
			}
			printf("(PID %d) ho letto il numero di linea %lu\n",getpid(), req->num_riga);

			//lettura file
			//lettura carattere a carattere perchè cerco '\n'
			while((nread=read(conn_sd, &c, sizeof(char)))) /*se nread==0 allora ha letto EOF*/{
				if(nread<0){
					printf("(PID %d) impossibile leggere il file dalla socket %d\n", getpid(), conn_sd);
					perror("Errore!");

					//mando comunque esito al cliente!
					strcpy(buff,"ERRORE LETTURA FILE, RIPETERE L'OPERAZIONE");
					if (write(conn_sd, buff, sizeof(buff))<0){
						printf("Figlio (pid %d): errore nella sendto \n", getpid());
						free(req);
						exit(EXIT_FAILURE);
					}
					//						close(conn_sd);
					//						free(req);
					//						exit(EXIT_FAILURE);
					continue;
				}

				//se arrivo qui ho letto il numero di linea e un solo carattere
				printf("%c",c);
				if(req->num_riga !=riga_corrente){//se non sono nella riga che devo eliminare allora devo scrivere il carattere letto
					if (write(conn_sd, &c, sizeof(char))<0){
						printf("Figlio (pid %d): errore nella sendto \n", getpid());
						free(req);
						exit(EXIT_FAILURE);
					}
				}
				if(c=='\n')//se leggo il terminatore di linea allora sto cominciando una nuova linea
					riga_corrente++;
			}//fine while lettura carattere
			shutdown(conn_sd, 1);//chiudo l'output
			exit(EXIT_SUCCESS);
		}//fine figlio!

	} // ciclo for infinito
	//close(conn_sd);  // padre chiude socket di connessione non di ascolto
}

