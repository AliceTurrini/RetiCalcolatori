#include <stdio.h>
#include <rpc/rpc.h>
#include <dirent.h>
#include <unistd.h>
#include <fcntl.h>
#include "scan.h"
#define DIM 50

void main(int argc, char *argv[]){
    CLIENT *cl; //gestore di trasporto
    char *server;
    char service;
    
    //par file
    char *nome_file;
    Count *ris_f;

    //par dir
    Directory dir;
    int *ris_d;

    if (argc < 2) {
        fprintf(stderr, "uso:\t%s nomeServer\n", argv[0]);
        exit(1);
    }
    server = argv[1];
    cl = clnt_create(server, SCANPROG, SCANVERS, "udp");
    if (cl == NULL){ 
        clnt_pcreateerror(server);
        exit(1);
    }
    nome_file=(char*)malloc(DIM);
    dir.nome=(char*)malloc(DIM);

    printf("Inserire il nome del servizio: 'F' (file) o 'D' (directory)\n"); 
    while((service=getchar())!=EOF){
        if(service!='F' && service!='D'){
            printf("Carattere non valido\n");
            printf("Inserire il nome del servizio: 'F' (file) o 'D' (directory), oppure EOF per terminare\n");
            continue;
        }
        if(service=='F'){           //RICHIESTO FILE
              // consumo il fine linea
              char fine_linea;
            gets(&fine_linea);  // usato come buffer da scartare
            printf("Inserire il nome del file da esaminare: \n");
            gets(nome_file);
            ris_f=file_scan_1(&nome_file, cl);

            if (ris_f == NULL) { // controllo errore RPC
                fprintf(stderr, "%s\t: %s fallisce la rpc scan_file\n", argv[0],server);
                clnt_perror(cl, server);
                exit(1);
            }
            //Codice errore in caso di insuccesso: chars, words, lines = 0;
            if (ris_f->chars == 0) { //controllo errore risultato
                fprintf(stderr, "%s\t: %s errore nel file remoto %s\n", argv[0], server, nome_file);
                clnt_perror(cl, server);
            } else {
                printf("file_scan avvenuta con successo!\n");
                printf("Il file remoto %s contiene:\t%d caratteri,\t%d parole,\t%d linee\n", nome_file, ris_f->chars, ris_f->words, ris_f->lines);
            }
        } else {                 //RICHIESTA DIRECTORY
            printf("Inserire il nome della directory da esaminare\n");
                          // consumo il fine linea
              char fine_linea; char nomedir[50];
            gets(&fine_linea);  // usato come buffer da scartare
            gets(dir.nome);
            printf("Inserire la soglia\n");
            scanf("%d", &dir.soglia);
            gets(&fine_linea);  // usato come buffer da scartare
            ris_d=dir_scan_1(&dir,cl);

            if (ris_d == NULL) { // controllo errore RPC
                fprintf(stderr, "%s\t: %s fallisce la rpc scan_dir\n", argv[0],server);
                clnt_perror(cl, server);
                exit(1);
            }
            if (*ris_d == -1) { //controllo errore risultato
                fprintf(stderr, "%s\t: %s errore nella dir remota %s con soglia %d\n", argv[0], server,dir.nome,dir.soglia);
                clnt_perror(cl, server);
            } else{
                printf("dir_scan avvenuta con successo!\n");
                printf("La dir remota %s contiene\t%d file con più di %d caratteri\n", dir.nome, *ris_d, dir.soglia);
            }
        }
        printf("Inserire il nome del servizio: 'F' (file) o 'D' (directory), oppure EOF per terminare\n"); 
    }
    free(nome_file); 
    clnt_destroy(cl);
    printf("Termino...\n"); exit(0);
} // main
