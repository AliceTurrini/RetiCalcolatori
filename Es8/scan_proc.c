#include <stdio.h>
#include <rpc/rpc.h>
#include <dirent.h>
#include <unistd.h>
#include <fcntl.h>
#include "scan.h"
// struct Count{ int chars; int words; int lines; };
// struct Directory { string nome <50>; int soglia; };

Count *file_scan_1_svc (char **nome_file, struct svc_req *rp){
    static Count ris;
    int fd;
    char c;
    int nread;
    ris.chars=0; ris.words=0; ris.lines=0;

    //Codice errore in caso di insuccesso: chars, words, lines = 0;
    if ((fd=open(*nome_file, O_RDONLY)) < 0){ //apertura file
        printf("Server:\tImpossibile aprire il file %s\n", *nome_file);
        return (&ris);
    }

    while((nread=read(fd, &c, sizeof(char)))) { //lettura a carattere
        if(nread<0){
            printf("Server:\tErrore lettura da file %s\n", *nome_file);
            close(fd);
            return (&ris);
        }
        //printf("carattere: %c\n",c); //debug
        if(c==' ' && ris.chars>0) ris.words++; //incremento
        else if (c=='\n') {
            ris.words++;
            ris.lines++;
        } else ris.chars++;
    }
        printf("Nel file %s trovati:\t%d caratteri,\t%d parole,\t%d linee\n", *nome_file, ris.chars, ris.lines, ris.words);
        close(fd);
        return (&ris);

    
}

int *dir_scan_1_svc (Directory *dir, struct svc_req *rp){
    static int ris=0;
    DIR* dd;
    struct dirent * elem;
    int fd, size, nread;
    char c;

    if((dd = opendir(dir->nome))==NULL){
        printf("Server:\tImpossibile aprire la directory %s\n", dir->nome);
        ris=-1;
        return (&ris);
    }
    //ho aperto la dir

    while ((elem = readdir(dd)) != NULL){
        printf("Dentro a dir %s: trovato %s\n", dir->nome, elem->d_name);
        if(elem->d_type == DT_REG){
            if ((fd=open(elem->d_name, O_RDONLY)) < 0){//fallisce qui
                printf("Server:\tImpossibile aprire il file %s\n", elem->d_name);
                closedir(dd);
                ris=-1;
                return (&ris);
            }
            //ho aperto il file
            
            //conto num_byte (=num_char) con lseek, posso anche fare a caratteri
            size = lseek(fd, 0, SEEK_END);
            if (size==-1){
                printf("Server:\tErrore lettura da file %s\n", elem->d_name);
                close(fd); closedir(dd);
                ris=-1;
                return (&ris);
            }
            /* lseek ritorna un dato di tipo off_t, non ci sono problemi di conversione in int (testato)
            This is a data type defined in the sys/types.h header file (of fundamental type unsigned long) 
            and is used to measure the file offset in bytes from the beginning of the file. 
            It is defined as a signed, 32-bit integer, but if the programming environment enables large 
            files off_t is defined to be a signed, 64-bit integer.

            */


            //a carattere:
            // while((nread=read(fd, &c, sizeof(char)))) { //lettura a carattere
            //     if(nread<0){
            //         printf("Server:\tErrore lettura da file %s\n", elem->d_name);
            //         close(fd); closedir(dd);
            //         ris=-1;
            //         return (&ris);
            //     }
            //     size++;
            // }

            printf("Numero di byte sul file %s:\t%d\n", elem->d_name, size);
            if(size > dir->soglia) ris++;
            close(fd);            
        }
    }
    closedir(dd);
    return (&ris);
}




// int *somma_1_svc( Operandi *op, struct svc_req *rp){ 
//     static int ris;
//     printf("Operandi ricevuti: %i e %i\n", op->op1, op->op2);
//     ris = (op->op1) + (op->op2);
//     printf("Somma: %i\n", ris);
//     return (&ris);
// }

// int *moltiplicazione_1_svc(Operandi *op, struct svc_req *rp){ 
//     static int ris;
//     printf("Operandi ricevuti: %i e %i\n", op->op1, op->op2);
//     ris = (op->op1) * (op->op2);
//     printf("Moltiplicazione: %i\n", ris);
//     return (&ris);
// }

// char **echo_1_svc (char **msg, struct svc_req *rp) {
//     static char *echo_msg;
//     free(echo_msg);
//     echo_msg=(char*)malloc(strlen(*msg)+1);
//     printf("Messaggio ricevuto: %s\n", *msg);
//     strcpy(echo_msg, *msg);
//     printf("Messaggio da rispedire: %s\n", echo_msg);
//     return (&echo_msg);
// }
