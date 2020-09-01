#include <stdio.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <stdlib.h>
#include <strings.h>
#include <unistd.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <pthread.h>
#include <stdio.h>
#include <string.h>   //strlen
#include <stdlib.h>
#include <errno.h>
#include <unistd.h>   //close
#include <arpa/inet.h>    //close
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <sys/time.h> //FD_SET, FD_ISSET, FD_ZERO macros
#include "tracker_cmd.h"
char buffer[BUFFER_SIZE];
struct infos_client clients[MAX_CLIENTS];
int nb_clients,activity;
pthread_t accept_client;
struct sockaddr_in serv_addr, cli_addr;
int sockfd,valread,new_socket;
int client_sockfd[MAX_CLIENTS];
char message[BUFFER_SIZE];


void affiche(){ // Debug (affiche les fichiers de tous les clients)
  for(int ind=0;ind<MAX_CLIENTS;ind++){
    printf("client %d\n",ind);
    for(int ind1=0;ind1<clients[ind].nb_files;ind1++){
      printf("file : %s\n", clients[ind].files[ind1].filename);

    }
  }
}

void error(char *msg){
  perror(msg);
  exit(1);
}

// --------- Commande ANNOUNCE ---------

void cmd_announce(char * cmd,int id_client){
  int compt=0;
  int j=0;
  while(cmd!=NULL && strcmp(cmd, "leech") != 0){ //parse la commande [Partie fichiers complets]
    switch(compt){
      case 2: // numéro de port
        if (strlen(cmd) > PORT_LENGTH) {
          error("Port is too long");
        }
        strcpy(clients[id_client].port,cmd);
        break;

      case 4: // nom du fichier
        if (strlen(cmd) > FILENAME_LENGTH) {
          error("Filename is too long");
        }
        strcpy(clients[id_client].files[j].filename,cmd);
        break;

      case 5: // taille du fichier
        if (strlen(cmd) > FILESIZE_LENGTH) {
          error("Filesize is too long");
        }
        strcpy(clients[id_client].files[j].filesize,cmd);
        break;

      case 6: // taille des pièces du fichier
        if (strlen(cmd) > PIECE_SIZE_LENGTH) {
          error("Piece size is too long");
        }
        strcpy(clients[id_client].files[j].length_piece,cmd);
        break;

      case 7: // clé du fichier
        if (strlen(cmd) > KEY_LENGTH) {
          error("Key is too long");
        }
        strcpy(clients[id_client].files[j].key,cmd);
        compt=3;
        j++;
        break;
    }
    cmd = strtok(NULL, " []");
    compt++;
  }

  cmd = strtok(NULL, " []\n");
  while (cmd != NULL) { // [Partie fichiers en téléchargement]
    if (strlen(cmd) > KEY_LENGTH) {
      error("Key is too long");
    }
    for(int ind=0;ind<MAX_CLIENTS;ind++){
      for(int ind1=0;ind1<clients[ind].nb_files;ind1++){
        if(strcmp(cmd,clients[ind].files[ind1].key)==0){ // On récupére les informations du fichiers
          strcpy(clients[id_client].files[j].filename,clients[ind].files[ind1].filename);
          strcpy(clients[id_client].files[j].filesize,clients[ind].files[ind1].filesize);
          strcpy(clients[id_client].files[j].length_piece,clients[ind].files[ind1].length_piece);
        }
    }
  }
    strcpy(clients[id_client].files[j].key, cmd); // On stocke la clé du fichier
    cmd = strtok(NULL, " []\n");
    j++;
  }
  clients[id_client].nb_files=j; // On met à jour le nombre de fichiers

  strcpy(message,"ok\n"); // On renvoie le message
}

// --------- Commande LOOK ---------

void cmd_look(char * cmd){
  strcpy(message,"list [ ");
  char filename[30]; //stocke le nom de fichier demandé
  int filesize=0; //stocke la taille de fichier demandée
  int filesize1=0;
  int op_size=0; // gère l'opérateur demandé

  const char * separators = " []\"\n";

  while(cmd!=NULL){ // parse la commande et stocke les informations

    // critère all
    if(strcmp(cmd, "all") == 0){
      strcpy(filename, cmd);
    }

    // critère filename
    else if(strcmp(cmd, "filename=") == 0){
      cmd = strtok(NULL, separators);
      strcpy(filename, cmd);
    }

    // critère filesize>
    else if(strcmp(cmd, "filesize>") == 0){
      cmd = strtok(NULL, separators);
      filesize = atoi(cmd);
      op_size = 0;
    }

    // critère filesize<
    else if(strcmp(cmd, "filesize<") == 0){
      cmd = strtok(NULL,separators);
      filesize = atoi(cmd);
      op_size = 1;
    }

    // critère <filesize<
    else if(strcmp(cmd, "look") != 0 && strcmp(cmd, "\n") != 0){
      filesize = atoi(cmd);
      cmd = strtok(NULL, separators);
      if(strcmp(cmd ,"<filesize<") == 0){
        cmd = strtok(NULL, separators);
        filesize1 = atoi(cmd);
        op_size = 2;
      }
      else{
        filesize = -1;
      }
    }

    cmd = strtok(NULL, separators);
  }
  char keys[MAX_FILES][KEY_LENGTH]; //stocke les fichiers correspondants
  int j;
  int nb_files=0; //nombre de fichiers correspondants
  for(int ind=0;ind<MAX_CLIENTS;ind++){ //parcours chaque client
    for(int ind1=0;ind1<clients[ind].nb_files;ind1++){ //parcours chaque fichier du client
      if(strcmp(filename,clients[ind].files[ind1].filename)==0 //si le nom du fichier correspond
      || strlen(filename)==0
      || strcmp(filename,"all")==0){

        if(filesize==0 || (op_size==0 && atoi(clients[ind].files[ind1].filesize)>filesize) // supérieur à
        || (op_size==1 && atoi(clients[ind].files[ind1].filesize)<filesize) // inférieur à
        || (op_size==2 && atoi(clients[ind].files[ind1].filesize)>filesize
            && (atoi(clients[ind].files[ind1].filesize)<filesize1))){ // inf et supp

          for(j=0; j<nb_files;j++){ //vérifie si ce fichier est déjà renvoyé
            if(strcmp(clients[ind].files[ind1].key,keys[j])==0)
            break;
          }
          if(j>=nb_files){ //si le fichier est pas déjà renvoyé, on le renvoie
            char m[BUFFER_SIZE];
            strcpy(m,clients[ind].files[ind1].filename);
            strcat(m," ");
            strcat(m,clients[ind].files[ind1].filesize);
            strcat(m," ");
            strcat(m,clients[ind].files[ind1].length_piece);
            strcat(m," ");
            strcat(m,clients[ind].files[ind1].key);
            strcat(message,m);
            strcat(message," ");
            strcpy(keys[nb_files],clients[ind].files[ind1].key);
            nb_files++;

          }
        }
      }
    }
  }
  strcat(message,"]\n");
  if(nb_files==0)
    strcpy(message,"Aucun fichier du réseau ne vérifie ces critères\n");
}

// --------- Commande GETFILE ---------


void cmd_getfile(char * cmd){
  cmd = strtok(NULL, " \n");
  strcpy(message,"peers ");
  strcat(message,cmd);
  strcat(message," [ ");
  int nb_files=0; //nombre de fichiers correspondants
  for(int ind=0;ind<MAX_CLIENTS;ind++){ // on parcours l'ensemble des fichiers
    for(int ind1=0;ind1<clients[ind].nb_files;ind1++){
      if(strcmp(clients[ind].files[ind1].key,cmd)==0){ // si la clé correspond on l'ajoute
        strcat(message,inet_ntoa(cli_addr.sin_addr) );
        strcat(message,":");
        strcat(message,clients[ind].port);
        strcat(message," ");
        nb_files=1;
      }
    }
  }
  strcat(message,"]\n");
  if(nb_files==0)
    strcpy(message,"Cette clé ne correspond à aucun fichier du réseau\n");
  while(cmd!=NULL)
  cmd = strtok(NULL, " ");
}

// --------- Commande UPDATE ---------

void cmd_update(char * cmd,int id_client){
  cmd = strtok(NULL, " []\n");
  cmd = strtok(NULL, " []\n");

  int j=0;
  struct file files[MAX_FILES]; // on stocke les files
  while(cmd!=NULL){ // on parse la commande pour récupérer les informations
    if(strcmp(cmd, "leech") != 0){ //Fichiers complets
      if (strlen(cmd) > KEY_LENGTH) {
        error("Key is too long");
      }
      strcpy(files[j].key,cmd); // on stocke la clé
      j++;
    }else{ //Fichiers en téléchargement
      cmd = strtok(NULL, " []\n");
      while(cmd!=NULL){
        if (strlen(cmd) > KEY_LENGTH) {
          error("Key is too long");
        }
        strcpy(files[j].key,cmd); // on stocke la clé
        j++;
        cmd = strtok(NULL, " []\n");
      }
    }
    cmd = strtok(NULL, " []\n");
  }
  for(int ind=0;ind<j;ind++){ // On regarde tous les fichiers updatés
    int stocked = 0;
    for(int ind1=0;ind1<clients[id_client].nb_files;ind1++){ // on regarde tous les fichiers du client
      if(strcmp(clients[id_client].files[ind1].key,files[ind].key)==0){
        stocked = 1;
        break; // si le fichier est déjà stocké, on passe au suivant
      }
    }

    if(stocked == 0){ // si le fichier n'est pas stocké
      for(int ind2=0;ind2<MAX_CLIENTS;ind2++){ // on récupére les informations sur le fichier
        for(int ind3=0;ind3<clients[ind2].nb_files;ind3++){
          if(strcmp(files[ind].key,clients[ind2].files[ind3].key)==0){
            strcpy(clients[id_client].files[clients[id_client].nb_files].filename,clients[ind2].files[ind3].filename);
            strcpy(clients[id_client].files[clients[id_client].nb_files].filesize,clients[ind2].files[ind3].filesize);
            strcpy(clients[id_client].files[clients[id_client].nb_files].length_piece,clients[ind2].files[ind3].length_piece);
            break;
          }
        }
      }
      strcpy(clients[id_client].files[clients[id_client].nb_files].key, files[ind].key);
      clients[id_client].nb_files++; // on ajoute le fichier aux fichiers du client
    }
  }
  strcpy(message,"ok\n");
}
