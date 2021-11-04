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
	char buff[DIM_BUFF], nome_dir[LENGTH_DIR_NAME];
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

	/* CORPO DEL CLIENT: */
	/* chiede ciclicamente all’utente il nome del direttorio------- */


	/* Da specifica, voglio che uno stesso cliente mantenga un’unica connessione per tutte le possibili invocazioni del direttorio: 
	se un cliente chiede più direttori di fila, il server deve mantienere la stessa connessione 
	   per servire i dir che quel cliente ha richiesto di fila!!!)

	il ciclo dovrà avvenire dopo aver creato la connessione, voglio poter trattare più dir usando la stessa connessione.
	
	ORDINE:
		-socket
		-connect
		-inizio ciclo:
			>(A)cliente manda nome dir al server
			>(B)cliente riceve output e lo stampa
		-iterazione successiva: stessa connessione, nome dir diverso, dal punto (A)
	*/

	/* CREAZIONE E CONNESSIONE SOCKET (BIND IMPLICITA) */	
	/* socket stream */
	sd=socket(AF_INET, SOCK_STREAM, 0);
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

	printf("Nome del direttorio su cui operare: ");
	while (gets(nome_dir) != NULL){ 
		if (write(sd, nome_dir, (strlen(nome_dir)+1))<0){
			perror("Errore: write nomeDir al server");
			/*break: non occorre chiudere la connessione se la write va male: 
			esco dal ciclo, tanto la close(sd) viene fatta una volta alla fine!*/
		}else{
						printf("Richiesta per dir %s inviata... \n", nome_dir);
		

				//read bloccante (?)
				//Client riceve la list dei nomi di file remoti nei dir di 2° livello
					while((nread=read(sd, buff, sizeof(buff)))>0){
						//faccio una lettura e una scrittura bufferizzate!
						if ((nwrite=write(1, buff, nread))<0){ //-la stampa a video
							perror("write");
							break;
						}
					}
		
					if ( nread<0 ){
						perror("read");
						break;
						/*
						Se nread<0 vuol dire che sono uscita dal ciclo a causa di un errore.
						break: non occorre chiudere la connessione se la read va male: 
						esco dal ciclo, tanto la close(sd) viene fatta una volta alla fine!*/
					}      
		
				//chiedo ad utente un nuovo nome_dir: mantengo la stessa connessione!
				printf("Nome del direttorio su cui operare: ");
		}
	
	} //while
	
	printf("\nClient: termino...\n");
	shutdown(sd,0);
	shutdown(sd,1);
	close(sd);
	exit(0);
}
