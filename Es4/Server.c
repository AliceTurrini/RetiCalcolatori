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
		printf("Error: %s port!\n", argv[0]);
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
				perror("Errore grave: select"); 
				exit(8);
			}
		}

		/* GESTIONE RICHIESTE DI NOMI FILE NELLE DIRECTORY DI II LIVELLOv ------- */
		if (FD_ISSET(tcpSd, &rset)){
			printf("--->\n\nRicevuta richiesta di directory\n");
			len = sizeof(struct sockaddr_in);

			if((connfd = accept(tcpSd,(struct sockaddr *)&cliaddr,&len))<0){ //connfd -> fd socket cliente
				if (errno==EINTR) continue;
				else {
					perror("Errore: accept"); 
					exit(9);
				}
			}

			if (fork()==0){ /* processo figlio che serve la richiesta di operazione */
				DIR *dir, *dir2;
				struct dirent * dd, * dd2;
				char dirServer[DIM_STRING];
				getcwd(dirServer, DIM_STRING);

				close(tcpSd);
				printf("Dentro il figlio, pid=%i\n", getpid());
				int count=0;
				while((read(connfd, &nome_dir, sizeof(nome_dir)))>=0){ //continuo finchè client non invia EOF (read ritorna -1)
					printf("Richiesta la directory %s\n", nome_dir);

					if((dir = opendir(nome_dir))==NULL){ //in caso dir non esiste NON chiudo la connessione ma procedo
						printf("La dir richiesta non esiste");
						char error[]="\nSERVER: La dir richiesta non esiste\n";
						write(connfd, error , sizeof(error));

					} else {
						printf("Ho aperto %s la dir di 1° livello, ora leggo contenuto..\n", nome_dir);
						while ((dd = readdir(dir)) != NULL){
							
							printf("dentro a dir1: trovato %s  \t\t", dd->d_name);
							if(dd->d_type == DT_DIR && strcmp(dd->d_name, ".")!=0 && strcmp(dd->d_name, "..")!=0){ 
								printf("--di cui %s e' una directory che ci piace!\n", dd->d_name);
								
								char path1[DIM_STRING];
								getcwd(path1, DIM_STRING);
								strcat(path1, "/");
								strcat(path1, nome_dir);
								chdir(path1);

								if((dir2 = opendir(dd->d_name))==NULL){
									printf("\nErrore apertura directory 2° livello\n", dir2);
									char error[]="\nSERVER: Errore apertura directory 2° livello\n";
									write(connfd, error , sizeof(error));
								}else{
									printf("\nHo aperto %s la dir di 2° livello, ora cerco file!\n", dd->d_name);

									while ((dd2 = readdir(dir2)) != NULL){
										//printf("dentro a dir2: trovato %s\t", dd->d_name);
										if(dd2->d_type == DT_REG){
											printf("\n|%d|\n",strlen(dd2-> d_name));
											write(connfd, dd2-> d_name , strlen(dd2-> d_name));
											char space=' ';
											write(connfd, &space, sizeof(char));
											printf("dir2: trovato il file %s, inviato al client!\n", dd2-> d_name);
											count++;
										}
									}
									printf("--fine while ho trovato %d file dentro alla directory di sec liv--\n", count);
									closedir(dir2);
								}
							}
						}
						printf("Ho trovato %s file dentro la dir di 2° livello!!\n", count);
						closedir(dir);
						chdir(dirServer);
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
			printf("\n\n---> Server: ricevuta richiesta di eliminazione occorrenza parola\n");
			len=sizeof(struct sockaddr_in);
			if (recvfrom(udpSd, &line, sizeof(line), 0, (struct sockaddr *)&cliaddr, &len)<0){
				perror("Errore: recvfrom"); 
				continue;
			}
			//sistemo i due dati:
			//printf("prima di sistemare, al server arriva:%s", line);
			i=0;
			while(line[i]!=sep){	//sep = '?'
				nomeFile[i]=line[i];
				i++;
			}
			nomeFile[i]='\0';
			i++; //mangio sep
			printf("Ho letto il nome del file: %s\n",nomeFile);
			j=0;
			while(line[i]!='\0'){
				parola[j++]=line[i++];
			}
			parola[j]='\0';
			printf("Ho letto parola: %s\n",parola);

			if ((fdFile=open(nomeFile, O_RDONLY))<0){
				printf("File %s inesistente\n", nomeFile);
				numEliminated=-1;
				if (sendto(udpSd, &numEliminated, sizeof(numEliminated), 0, (struct sockaddr *)&cliaddr, len)<0){
					perror("Errore: sendto (per esito negativo, problema open)");
					continue;
				}
			}else{
				printf("HO APERTO IL FILE\n");
				strcpy(temp_file, "temp.txt");
				if((temp=open(temp_file, O_CREAT | O_RDWR, 0777))<0){
					printf("Errore apertura file temp\n");
					numEliminated=-1;
					if (sendto(udpSd, &numEliminated, sizeof(numEliminated), 0, (struct sockaddr *)&cliaddr, len)<0){
						perror("Errore: sendto");
						continue;
					}
				}else{
					printf("HO APERTO TEMP\n");
					i=0;
					while(read(fdFile, &c, 1) > 0){
						//printf("%c", c);
						if(c!=' ' && c!='\n'){
							word[i++]=c;
						}
						else{ //ho letto una parola intera, ora confronto:  (strcmp ritorna -1 se la prima stringa � minore, 0 se sono uguali, 1 se maggiori)
							word[i]='\0';
							
							if(strcmp(word, parola)!=0){ //la parola va bene, scrivo su file:
								printf("\n da scrivere %s ", word);
								if(write(temp, word, strlen(word)+1)<0) //strlen ritorna il numero di caratteri escluso il terminatore \0
									printf("Errore nella scrittura di %s sul file temp", word);

							}else	numEliminated++;
							i=0;
							strcpy(word, "");
						}
					} //fine while lettura dal file
					printf("RISULTATO: le eliminazioni sono state: %d", numEliminated);

					//mando esito finale:
					if (sendto(udpSd, &numEliminated, sizeof(numEliminated), 0, (struct sockaddr *)&cliaddr, len)<0){
						perror("Errore: sendto esito finale");
						continue;
					}
					close(fdFile);
					close(temp);
					remove(nomeFile);	//int remove(const char *filename)
					rename(temp_file, nomeFile);	//int rename(const char *old_filename, const char *new_filename)
				}
			}

		} /* fine gestione richieste di conteggio */
	} /* ciclo for della select */
}

