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

/*Struttura di una richiesta*/
typedef struct{
    char nomeFile[MAX_NOMEFILE];
}Request;

int main(int argc, char **argv){
	int sd, port, len, ris, i, fd, pid;
	const int on = 1; //Serve a indicare al compilatore che il valore di una certa variabile non può essere modificato durante l'esecuzione del programm
	struct sockaddr_in cliaddr, servaddr;
	struct hostent *clienthost;
	Request* req = NULL;

	/* CONTROLLO ARGOMENTI */
	if(argc!=2){
		printf("Error: %s port\n", argv[0]);
        exit(1);
	}
    i=0;
    while( argv[1][i]!= '\0' ){
        if((argv[1][i] < '0') || (argv[1][i] > '9')){
            printf("Secondo argomento non intero\n");
            printf("Error: %s port\n", argv[1]);
            exit(2);
        }
        i++;
    }  	
    port = atoi(argv[1]);
    if (port < 1024 || port > 65535){
        printf("Error: %s port\n", argv[1]);
        printf("1024 <= port <= 65535\n");
        exit(2);  	
    }
	

	/* INIZIALIZZAZIONE INDIRIZZO SERVER */
	memset ((char *)&servaddr, 0, sizeof(servaddr));
	servaddr.sin_family = AF_INET;
	servaddr.sin_addr.s_addr = INADDR_ANY;  
	servaddr.sin_port = htons(port);  

	/* CREAZIONE, SETAGGIO OPZIONI E CONNESSIONE SOCKET */
	sd=socket(AF_INET, SOCK_DGRAM, 0);
	if(sd <0){
        perror("creazione socket "); 
        exit(1);
    }
	printf("Server: creata la socket, sd=%d\n", sd);

	if(setsockopt(sd, SOL_SOCKET, SO_REUSEADDR, &on, sizeof(on))<0){ 
        //modifico il comportamento della bind, voglio che la bind sia senza controllo di unicità di associazione 
        perror("set opzioni socket "); 
        exit(1);
    }
	printf("Server: set opzioni socket ok\n"); 

	if(bind(sd,(struct sockaddr *) &servaddr, sizeof(servaddr))<0){
        perror("Errore nella bind socket ");
        exit(1);
    }
	printf("Server: bind socket ok\n");

    /* CICLO DI RICEZIONE RICHIESTE */
    //la recvfrom riceve una richiesta senza connessione (senza specificare da chi, non c'è il randez-vous) --> Datagram
    //quindi associano una socket ad ogni richiesta 
    //delego ad un figlio, padre fa subito un'altra recv!!

	req=(Request*)malloc(sizeof(Request));
    for(;;){
		len=sizeof(struct sockaddr_in);
		if (recvfrom(sd, req, sizeof(Request), 0, (struct sockaddr *)&cliaddr, &len)<0){
            perror("errore in ricezione ");
            continue;
        }

		/*CREO FIGLIO: leggere file e trovare lunghezza max*/
        fd=open(req->nomeFile, O_RDONLY);
        if (fd < 0){
			printf("P0: Impossibile aprire il file %s\n", req->nomeFile); 	//c'è stato un errore con quel file!
		}else{
			pid=fork(); //creo figlio

			if(pid<0){
				printf("Errore nella creazione del figlio\n");
			} else if(pid==0){  	//processo figlio
                char temp[MAX_STRINGA];
                int nread, count=1;
                char c;
                ris=0;

                while(nread=(read(fd, &c, sizeof(char)))) /*se nread==0 allora ha letto EOF*/{
                    if(nread<0){
                        printf("(PID %d) impossibile leggere dal file %s\n", getpid(), req->nomeFile);	
                        perror("Errore!");
                        
                        //mando comunque esito al cliente!
                        ris=-ris; //valore calcolato fino al errore, se arriva negativo al client significa errore
                        if (sendto(sd, &ris, sizeof(ris), 0, (struct sockaddr *)&cliaddr, len)<0){
                            printf("Figlio (pid %d): errore nella sendto \n", getpid()); 
                            free(req);
                            exit(1);
                        }
                        close(fd);
                        free(req);
                        exit(3);
                    }

                    if(c != ' ' && c!='\n'){
                        count++;
                    }else{
                        if(count>ris){
                            ris=count;
                        }
                        count=1;
                    }
                }
                printf("Figlio (pid %d): Lunghezza max del file %s e' %d\n", getpid(), req->nomeFile, ris);
				
                //invio al client la lunghezza trovata:
                ris=htonl(ris);  
                if (sendto(sd, &ris, sizeof(ris), 0, (struct sockaddr *)&cliaddr, len)<0){
                    printf("Figlio (pid %d): errore nella sendto \n", getpid()); 
                    free(req);
                    exit(1);
                }
				close(fd);
                //fine figlio!

			} else{ //codice padre:
				printf("Sono il padre, ho creato il figlio (pid: %d)\n", pid);
                close(fd); 
			}
		}
		

	} //for demone
}
