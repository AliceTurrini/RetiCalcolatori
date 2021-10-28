/* ClientDatagram: (filtro) utente passa nomeFile remoto, e manda al server, poi riceve la lunghezza della parola più lunga */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <string.h>

#define MAX_NOMEFILE 64
#define PORT 2345

/*Struttura di una richiesta*/
typedef struct{
	char nomeFile[MAX_NOMEFILE];
}Request;

int main(int argc, char **argv){
	struct hostent *host;
	struct sockaddr_in clientaddr, servaddr;
	int  port, sd, i=0, ok, ris, len;
    char temp[MAX_NOMEFILE], c;
	Request req;

	/* CONTROLLO ARGOMENTI */
	if(argc!=3){
		printf("[ClientDatagram] Error: %s serverAddress serverPort\n", argv[0]);
		exit(1);
	}

	/* INIZIALIZZAZIONE INDIRIZZO CLIENT E SERVER */
	memset((char *)&clientaddr, 0, sizeof(struct sockaddr_in));
	clientaddr.sin_family = AF_INET;
	clientaddr.sin_addr.s_addr = INADDR_ANY;

	clientaddr.sin_port = PORT;

	memset((char *)&servaddr, 0, sizeof(struct sockaddr_in));
	servaddr.sin_family = AF_INET;
	host = gethostbyname (argv[1]);

	/* VERIFICA INTERO */
	while( argv[2][i]!= '\0' ){
		if( (argv[2][i] < '0') || (argv[2][i] > '9') ){
			printf("Secondo argomento non intero\n");
			printf("Error:%s serverAddress serverPort\n", argv[0]);
			exit(2);
		}
		i++;
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

	/* CREAZIONE SOCKET */
	if((sd=socket(AF_INET, SOCK_DGRAM, 0))<0) {
        perror("apertura socket"); 
        exit(1);
    }
	printf("Client: creata la socket sd=%d\n", sd);

	/* BIND SOCKET, a una porta scelta dal sistema */
	if(bind(sd,(struct sockaddr *) &clientaddr, sizeof(clientaddr))<0){
        perror("bind socket ");
        exit(1);
    }
	printf("Client: bind socket ok, alla porta %i\n", clientaddr.sin_port);

	/* CORPO DEL CLIENT: ciclo di accettazione di richieste da utente */
	printf("Inserire nome file remoto, EOF per terminare: ");

	/* ATTENZIONE!!
	* Cosa accade se la riga e' piu' lunga di MAX_NOMEFILE-1?
	*/
    // char a;
    // while((read(0, &a, sizeof(char)))>0){  
    //    int i=0;
    //     while((read(0, &c, sizeof(char)))>0){
    //             if(c!='\n'){
    //                 req.nomeFile[i]=c;
    //                 i++;
    //             }
    //         } 
    // }

	while (scanf("%s", &temp) != EOF){         

		strcpy(req.nomeFile, temp);
        printf("Ho letto il nomeFile: %s", req.nomeFile);

		/* richiesta operazione */
        len=sizeof(servaddr);
		if(sendto(sd, &req, sizeof(Request), 0, (struct sockaddr *)&servaddr, len)<0){
			perror("sendto");
            printf("Inserire nome file remoto, EOF per terminare: ");
			continue;
		}

		/* ricezione del risultato */
		printf("Attesa del risultato...\n");
		if ((recvfrom(sd, &ris, sizeof(ris), 0, (struct sockaddr *)&servaddr, &len))<0){
			perror("recvfrom"); 
            continue;
        }

		printf("Esito dell'operazione: lunghezza della parola più grande e' %d\n", (int)ntohl(ris), ris);
		printf("Inserisci nomefile remoto, EOF per terminare: ");
	} //while utente
	
	//CLEAN OUT
	close(sd);
	printf("\nClient: termino...\n");  
	exit(0);
}
