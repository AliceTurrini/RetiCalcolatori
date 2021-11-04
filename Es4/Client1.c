/*
Primo Client: (servizio “datagram”)
-Chiede all'utente una parola es. "pippo" e il nome di un file testo es. "file.txt" (remoto)
-Chiede al server di eliminare tutte le occorrenze della parola nel file testo: 
-Attende pacchetto con Esito = numero di occorrenze eliminate (oopure -1 in caso di errore)
*/
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <string.h>
#include <dirent.h>
#include <fcntl.h>

#define LENGTH_FILE_NAME 64
#define LENGTH_BUFFER 256

int main(int argc, char **argv){
	struct hostent *host;
	struct sockaddr_in clientaddr, servaddr;
	int sd, nread, port;
	char sep[]="?";

	int len, num_eliminazioni;
	char buff[LENGTH_BUFFER], temp[LENGTH_BUFFER];

	/* CONTROLLO ARGOMENTI */
	if(argc!=3){
		printf("Error:%s serverAddress serverPort\n", argv[0]);
		exit(1);
	}

	/* INIZIALIZZAZIONE INDIRIZZO SERVER */
	memset((char *)&servaddr, 0, sizeof(struct sockaddr_in));
	servaddr.sin_family = AF_INET;
	host = gethostbyname (argv[1]);
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
		printf("Errore: Porta scorretta...");
		exit(2);
	}

	servaddr.sin_addr.s_addr=((struct in_addr *)(host->h_addr))->s_addr;
	servaddr.sin_port = htons(port);

	/* INIZIALIZZAZIONE INDIRIZZO CLIENT */
	memset((char *)&clientaddr, 0, sizeof(struct sockaddr_in));
	clientaddr.sin_family = AF_INET;
	clientaddr.sin_addr.s_addr = INADDR_ANY;  
	clientaddr.sin_port = 0;
	
	printf("--Client avviato--\n");

	/* CREAZIONE SOCKET */
	sd=socket(AF_INET, SOCK_DGRAM, 0);
	if(sd<0) {
		perror("apertura socket");
		exit(3);
	}
	printf("Creata la socket sd=%d \n", sd);
	
	/* BIND SOCKET, a una porta scelta dal sistema */
	if(bind(sd,(struct sockaddr *) &clientaddr, sizeof(clientaddr))<0){
		perror("Errore: bind socket");
		exit(1);
	}
	printf("Client: fatta bind socket alla porta %i\n", clientaddr.sin_port);

	/* CORPO DEL CLIENT: */	
	/*Dato che voglio inviare un unico datagramma, occorre salvare entrambe le info in un unico buffer
	per farlo, stabilisco un separatore da interrporre fra le due stringhe: \
	*/

	printf("Inserisci nome del file: ");
	while (gets(buff) != NULL){
		//
		strcat(buff, sep); //aggiungo il separatore //char *strcat(char *dest, const char *src);
		printf("|%s|",buff);
		printf("Inserisci la parola da eliminare sul file: ");
		gets(temp); 
		strcat(buff, temp);
		printf("Invio datagram: %s\n",buff);

		
		/* invio richiesta */
		len=sizeof(servaddr);		
		if (sendto(sd, buff, (strlen(buff)+1), 0, (struct sockaddr *)&servaddr, len)<0){
			perror("Errore: scrittura buff socket");
			printf("Nome del file su cui operare: ");
			continue; // se questo invio fallisce il client torna all'inzio del ciclo
		}

		/* ricezione del risultato */
		printf("Attesa del risultato...\n");

		if (recvfrom(sd, &num_eliminazioni, sizeof(num_eliminazioni), 0, (struct sockaddr *)&servaddr, &len)<0){
			perror("Errore: recvfrom del num eliminazioni");
			printf("Nome del file su cui operare: ");
			continue; // se questa ricezione fallisce il client torna all'inzio del ciclo
		}

		if (num_eliminazioni<0) printf("Il file è scorretto o non esiste o la parola non e' presente'\n");
		else printf("Numero eliminazioni nel file: %u\n", num_eliminazioni);
		
		printf("Inserisci il nome del file: ");

	} // while

	printf("\nClient: termino...\n");
	shutdown(sd,0);
	shutdown(sd,1);
	close(sd);
	exit(0);
}
