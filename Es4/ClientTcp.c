/*
Secondo client:
-chiede ciclicamente all’utente il nome del direttorio
-invia al server la richiesta con quel nome di direttorio
-riceve la list dei nomi di file remoti nei dir di 2° livello
-la stampa a video
*/

/* Client per richiedere l'invio di un file (get, versione 1) */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>

#define DIM_BUFF 100
#define LENGTH_DIR_NAME 20


int main(int argc, char *argv[]){
	int sd, nread, nwrite, port;
	char buff[DIM_BUFF], nome_dir[LENGTH_DIR_NAME], c;
	struct hostent *host;
	struct sockaddr_in servaddr;

	/* CONTROLLO ARGOMENTI ---------------------------------- */
	if(argc!=3){
		printf("Error:%s serverAddress serverPort\n", argv[0]);
		exit(1);
	}
	printf("Client avviato\n");

	/* PREPARAZIONE INDIRIZZO SERVER ----------------------------- */
	memset((char *)&servaddr, 0, sizeof(struct sockaddr_in));
	servaddr.sin_family = AF_INET;
	host = gethostbyname(argv[1]);
	if (host == NULL){
		printf("%s not found in /etc/hosts\n", argv[1]);
		exit(2);
	}
	
	nread = 0;
	while (argv[2][nread] != '\0'){
		if ((argv[2][nread] < '0') || (argv[2][nread] > '9')){
			printf("Secondo argomento non intero\n");
			exit(2);
		}
		nread++;
	}
	port = atoi(argv[2]);
	if (port < 1024 || port > 65535){
		printf("Porta scorretta...");
		exit(2);
	}

	servaddr.sin_addr.s_addr=((struct in_addr*) (host->h_addr))->s_addr;
	servaddr.sin_port = htons(port);

	//chiedo ciclicamente all’utente il nome della directory*/
	//Una connessione per tutte le directory

	sd=socket(AF_INET, SOCK_STREAM, 0); //creazione socket STREAM
	if (sd <0){
		perror("Errore: apertura socket ");
		exit(3);
	}
	printf("Creata la socket sd=%d\n", sd);

	if (connect(sd,(struct sockaddr *) &servaddr, sizeof(struct sockaddr))<0){
		perror("Errore: connect "); 
		exit(4);
	}
	printf("Connect ok\n");

	printf("Inserire nome directory: ");
	while (gets(nome_dir) != NULL){ 
		if (write(sd, nome_dir, (strlen(nome_dir)+1))<0){
			perror("Errore: write nomeDir al server");
			printf("\nInserire nome directory: ");
			continue;
		}else{
			printf("Richiesta per dir %s inviata... \n", nome_dir);
			// while((nread=read(sd, buff, sizeof(buff)))>0){
			// 	write(1, buff, strlen(buff));
			// }
			while((nread=read(sd, &c, 1))>0){
				putchar(c);
				//printf("%c", c);
				//write(1, &c, 1);
			}

			if(nread<0){
				perror("Lettura da stream");
			}
			//chiedo ad utente un nuovo nome_dir: mantengo la stessa connessione!
			printf("\nInserire nome directory: ");
		}
	
	} //while
	
	printf("\nClient: termino...\n");
	shutdown(sd,0);
	shutdown(sd,1);
	close(sd);
	exit(0);
}
