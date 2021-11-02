/*
Primo Client:
servizio “datagram”  è quello in cui il cliente:
-Chiede all'utente una parola es. "pippo" e il nome di un file testo es. "file.txt" (sul file system del ervitore)
-dà al server queste due informazioni (unico datagramma)
-chiede al server di eliminare tutte le occorrenze di una parola in un file testo: 
	es. se gli dico “pippo”, do un nome di un file "file.txt"
		il servitore elimina tutte le occorrenze della parola “pippo” dal file che ho passato come argomento
-Attende pacchetto con Esito= numero di occorrenze eliminate
(se il file non esiste, il server mi invia un intero negativo)

NB: il file risiede sul file system del servitore!
 */



/* Client per richiedere il numero di file in un direttorio remoto */

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

	int len, num_eliminazioni;
	char buffer[LENGTH_BUFFER];

	/* CONTROLLO ARGOMENTI ---------------------------------- */
	if(argc!=3){
		printf("Error:%s serverAddress serverPort\n", argv[0]);
		exit(1);
	}

	/* INIZIALIZZAZIONE INDIRIZZO SERVER--------------------- */
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
	if (port < 1024 || port > 65535)
	{printf("Porta scorretta...");exit(2);}

	servaddr.sin_addr.s_addr=((struct in_addr *)(host->h_addr))->s_addr;
	servaddr.sin_port = htons(port);

	/* INIZIALIZZAZIONE INDIRIZZO CLIENT--------------------- */
	memset((char *)&clientaddr, 0, sizeof(struct sockaddr_in));
	clientaddr.sin_family = AF_INET;
	clientaddr.sin_addr.s_addr = INADDR_ANY;  
	clientaddr.sin_port = 0;
	
	printf("Client avviato\n");

	/* CREAZIONE SOCKET ---------------------------- */
	sd=socket(AF_INET, SOCK_DGRAM, 0);
	if(sd<0) {perror("apertura socket"); exit(3);}
	printf("Creata la socket sd=%d\n", sd);
	
	/* BIND SOCKET, a una porta scelta dal sistema --------------- */
	if(bind(sd,(struct sockaddr *) &clientaddr, sizeof(clientaddr))<0)
	{perror("bind socket "); exit(1);}
	printf("Client: bind socket ok, alla porta %i\n", clientaddr.sin_port);

	/* CORPO DEL CLIENT: */
	
	printf("Nome del file su cui operare: ");

	/*Dato che voglio inviare un unico datagramma, occorre salvare entrambe le info in un unico buffer
	per farlo, stabilisco la lunghezza massima della stringa "nome file" e la inserisco nella prima parte del buffer.
	La parola la inserisco nella seconda parte, ovvero da LENGTH_FILE_NAME+1 in poi */
	
	/*
	-Tutto questo per non fare la struct (dato che il prof ha detto che non le vuole -.-):
	forse qua ci stava farla dato che abbiamo due info??
	-Ovviamente il server deve conoscere il valore di LENGTH_FILE_NAME e LENGTH_BUFFER e avere le stesse define:
			#define LENGTH_FILE_NAME 64
			#define LENGTH_BUFFER 256
	
	*/

	while (gets(buffer) != NULL){ //la facciamo così??
	//inserisco la nome_dir all'inizio del buffer!
	
		//chiedo anche la parola da eliminare:
		printf("Parola da eliminare sul file: ");
		gets(&buffer[LENGTH_FILE_NAME+1]);
		//inserisco la parola nella parte dopo del buffer, quando sono sicura che nome_file è finita!
		//come faccio a sapere che nnon sforo? Non lo so!
		
		/* invio richiesta */
		len=sizeof(servaddr);
		
		
		if (sendto(sd, buffer, (strlen(buffer)+1) /*PERCHE' IL +1???*/, 0, (struct sockaddr *)&servaddr, len)<0){
			perror("scrittura socket");
			printf("Nome del file su cui operare: ");
			continue; // se questo invio fallisce il client torna all'inzio del ciclo
		}

		/* ricezione del risultato */
		printf("Attesa del risultato...\n");
		if (recvfrom(sd, &num_eliminazioni, sizeof(num_eliminazioni), 0, (struct sockaddr *)&servaddr, &len)<0){
			perror("recvfrom");
			printf("Nome del file su cui operare: ");
			continue; // se questa ricezione fallisce il client torna all'inzio del ciclo
		}

		if (num_eliminazioni<0) printf("Il file è scorretto o non esiste\n");

		else printf("Numero eliminazioni nel file: %u\n", ntohl(num_eliminazioni));
		//ntohl:  serve farlo?????????????????????????????????????
		
		printf("Nome del file su cui operare: ");

	} // while

	printf("\nClient: termino...\n");
	shutdown(sd,0);
	shutdown(sd,1);
	close(sd);
	exit(0);
}
