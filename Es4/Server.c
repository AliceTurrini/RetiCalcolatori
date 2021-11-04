/* Server Select 
 * 	Un solo figlio per tutti i file.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>
#include <errno.h>
#include <fcntl.h>
#include <dirent.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>

#define DIM_BUFF 256
#define DIM_STRING 64
#define max(a,b) ((a) > (b) ? (a) : (b))

void gestore(int signo){
	int stato;
	printf("Gestore di SIGCHLD (padre fa wait del figlio morto)\n");
	wait(&stato);
}
/********************************************************/

int main(int argc, char **argv){
	int  tcpSd, connfd, udpSd, maxSd, i, j;
	const int on = 1;
	char temp_file[DIM_STRING],nome_dir[DIM_STRING], line[DIM_BUFF], c, parola[DIM_STRING], nomeFile[DIM_STRING], sep='?', word[DIM_STRING];
	fd_set rset;
	int len, nread, port, fdFile, temp, numEliminated=0;
	struct sockaddr_in cliaddr, servaddr;


	/* CONTROLLO ARGOMENTI ---------------------------------- */
	if(argc!=2){
		printf("Error: %s port\n", argv[0]);
		exit(1);
	}
	nread = 0;
	while (argv[1][nread] != '\0'){
		if ((argv[1][nread] < '0') || (argv[1][nread] > '9')){
			printf("Terzo argomento non intero\n");
			exit(2);
		}
		nread++;
	}
	port = atoi(argv[1]);
	if (port < 1024 || port > 65535){
		printf("Porta scorretta...");
		exit(2);
	}

	/* INIZIALIZZAZIONE INDIRIZZO SERVER ----------------------------------------- */
	memset ((char *)&servaddr, 0, sizeof(servaddr));
	servaddr.sin_family = AF_INET;
	servaddr.sin_addr.s_addr = INADDR_ANY;
	servaddr.sin_port = htons(port);

	printf("Server avviato\n");

	/* CREAZIONE SOCKET TCP ------------------------------------------------------ */
	tcpSd=socket(AF_INET, SOCK_STREAM, 0);
	if (tcpSd <0)
	{perror("apertura socket TCP "); exit(1);}
	printf("Creata la socket TCP d'ascolto, fd=%d\n", tcpSd);

	if (setsockopt(tcpSd, SOL_SOCKET, SO_REUSEADDR, &on, sizeof(on))<0)
	{perror("set opzioni socket TCP"); exit(2);}
	printf("Set opzioni socket TCP ok\n");

	if (bind(tcpSd,(struct sockaddr *) &servaddr, sizeof(servaddr))<0)
	{perror("bind socket TCP"); exit(3);}
	printf("Bind socket TCP ok\n");

	if (listen(tcpSd, 5)<0)
	{perror("listen"); exit(4);}
	printf("Listen ok\n");

	/* CREAZIONE SOCKET UDP ------------------------------------------------ */
	udpSd=socket(AF_INET, SOCK_DGRAM, 0);
	if(udpSd <0)
	{perror("apertura socket UDP"); exit(5);}
	printf("Creata la socket UDP, fd=%d\n", udpSd);

	if(setsockopt(udpSd, SOL_SOCKET, SO_REUSEADDR, &on, sizeof(on))<0)
	{perror("set opzioni socket UDP"); exit(6);}
	printf("Set opzioni socket UDP ok\n");

	if(bind(udpSd,(struct sockaddr *) &servaddr, sizeof(servaddr))<0)
	{perror("bind socket UDP"); exit(7);}
	printf("Bind socket UDP ok\n");

	/* AGGANCIO GESTORE PER EVITARE FIGLI ZOMBIE -------------------------------- */
	signal(SIGCHLD, gestore);

	/* PULIZIA E SETTAGGIO MASCHERA DEI FILE DESCRIPTOR ------------------------- */
	FD_ZERO(&rset);
	maxSd=max(tcpSd, udpSd)+1;

	/* CICLO DI RICEZIONE EVENTI DALLA SELECT ----------------------------------- */
	for(;;){
		FD_SET(tcpSd, &rset);
		FD_SET(udpSd, &rset);
		if (select(maxSd, &rset, NULL, NULL, NULL)<0){
			if (errno==EINTR) continue;
			else {
				perror("select"); 
				exit(8);
			}
		}

		/* GESTIONE RICHIESTE DI NOMI FILE NELLE DIRECTORY DI II LIVELLOv ------- */
		if (FD_ISSET(tcpSd, &rset)){
			printf("Ricevuto nome directory\n");
			len = sizeof(struct sockaddr_in);

			if((connfd = accept(tcpSd,(struct sockaddr *)&cliaddr,&len))<0){ //connfd -> fd socket cliente
				if (errno==EINTR) continue;
				else {
					perror("accept"); 
					exit(9);
				}
			}

			if (fork()==0){ /* processo figlio che serve la richiesta di operazione */
				DIR *dir, *dir2;
				struct dirent * dd;
				struct dirent * dd2;

				close(tcpSd);
				printf("Dentro il figlio, pid=%i\n", getpid());

				while((read(connfd, &nome_dir, sizeof(nome_dir)))>=0){ //continuo finch� client non invia EOF (read ritorna -1)
					printf("Richiesta la directory %s\n", nome_dir);

					if((dir = opendir(nome_dir))==NULL){ //in caso dir non esiste NON chiudo la connessione ma procedo
						printf("La dir richiesta non esiste");
						char error[]="\nSERVER: La dir richiesta non esiste\n";
						write(connfd, error , sizeof(error));

					} else {
						while ((dd = readdir(dir)) != NULL){
							if((dir2 = opendir(dd->d_name))!=NULL){ // dir2 � una directory, non un file!
								while ((dd2 = readdir(dir2)) != NULL){
									printf("Trovato il file %s\n", dd2-> d_name);
									write(connfd, dd2-> d_name , sizeof(dd2-> d_name));
								}
							}
						}
					}
				}//while
				printf("Figlio %i: chiudo connessione e termino\n", getpid());
				close(connfd);
				exit(0);
			}//figlio
			/* padre chiude la socket dell'operazione */
			close(connfd);
		} /* fine gestione richieste di file */


		/* GESTIONE RICHIESTE DI ELIMINAZIONE OCCORRENZE PAROLA */
		if (FD_ISSET(udpSd, &rset)){
			printf("Server: ricevuta richiesta di eliminazione occorrenza parola\n");
			len=sizeof(struct sockaddr_in);
			if (recvfrom(udpSd, &line, sizeof(line), 0, (struct sockaddr *)&cliaddr, &len)<0){
				perror("Errore: recvfrom"); 
				continue;
			}
			//sistemo i due dati:
			i=0;
			while(line[i]!=sep){	//sep = '?'
				nomeFile[i]=line[i++];
			}
			i++; //mangio sep
			nomeFile[i]='\0';
			printf("Ho letto il nome del file %s\n",nomeFile);
			j=0;
			while(line[i]!='\0'){
				parola[j++]=line[i++];
			}
			parola[j++]='\0';
			printf("Ho letto parola: %s\n",parola);

			if ((fdFile=open(nomeFile, O_RDONLY))<0){
				printf("File %s inesistente\n", nomeFile);
				numEliminated=-1;
				if (sendto(udpSd, &numEliminated, sizeof(numEliminated), 0, (struct sockaddr *)&cliaddr, len)<0){
					perror("Errore: sendto (per esito negativo, problema open)");
					continue;
				}
			}else{
				strcpy(temp_file, "temp.txt");
				if((temp=open(temp_file, O_WRONLY))<0){
					printf("Errore apertura file temp\n");
					numEliminated=-1;
					if (sendto(udpSd, &numEliminated, sizeof(numEliminated), 0, (struct sockaddr *)&cliaddr, len)<0){
						perror("Errore: sendto");
						continue;
					}
				}else{
					i=0;
					while(read(fdFile, &c, 1) > 0){
						if(c!=' ' || c!='\n'){
							word[i++]=c;
						}
						else{ //ho letto una parola intera, ora confronto:  (strcmp ritorna -1 se la prima stringa � minore, 0 se sono uguali, 1 se maggiori)
							if(strcmp(word, parola)!=0){ //la parola va bene, scrivo su file:
								if(write(temp, word, strlen(word)+1)<0) //strlen ritorna il numero di caratteri escluso il terminatore \0
									printf("Errore nella scrittura di %s sul file temp", word);

							}else
								numEliminated++;

						}
					} //fine while lettura dal file

					//mando esito finale:
					if (sendto(udpSd, &numEliminated, sizeof(numEliminated), 0, (struct sockaddr *)&cliaddr, len)<0){
						perror("Errore: sendto esito finale");
						continue;
					}
					close(temp);
					close(fdFile);
					remove(temp_file);//int remove(const char *filename)
					rename(temp_file, nomeFile);//int rename(const char *old_filename, const char *new_filename)
				}
			}

		} /* fine gestione richieste di conteggio */
	} /* ciclo for della select */
}
