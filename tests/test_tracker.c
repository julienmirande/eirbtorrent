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
#include <assert.h>
#include "tracker_cmd.h"




char buffer[BUFFER_SIZE];
struct infos_client clients[MAX_CLIENTS];
int nb_clients,activity;
pthread_t accept_client;

int sockfd,valread,new_socket;
int client_sockfd[MAX_CLIENTS];
struct sockaddr_in serv_addr, cli_addr;
fd_set readfds;
char message[BUFFER_SIZE];




// --------- Test Commande ANNOUNCE ---------

void test_announce(){
  printf("\033[33m---------------- Test commande ANNOUNCE ----------------\n");
  printf("\033[0m\033[1m------- announce classique -------\033[33m\n");
  bzero(message,BUFFER_SIZE); // on réinitialise le buffer
  char buffer[BUFFER_SIZE]="announce listen 2222 seed [file_a.dat 2097152 1024 8905e92afeb80fc7722ec89eb0bf0966 file_b.dat 3145728 1536 330a57722ec8b0bf09669a2b35f88e9e]\n";
  char * filename="file_a.dat";
  char * filename1="file_b.dat";

  printf("\033[1mMessage reçue : \033[0m%s",buffer);
  char * cmd = strtok(buffer, " \n");
  cmd_announce(cmd,0);
  printf("\033[33m\033[1mMessage envoyé: \033[0m%s",message);
  assert(strcmp(message,"ok\n")==0);
  int nb_files=0;
  for(int ind1=0;ind1<clients[0].nb_files;ind1++){
    if(strcmp(filename,clients[0].files[ind1].filename)==0) // On récupére les informations du fichiers
      nb_files++;
    else if(strcmp(filename1,clients[0].files[ind1].filename)==0) // On récupére les informations du fichiers
      nb_files++;
  }
  if(nb_files==2)
    printf("\033[32m-----> Test validé\n");
  else
    printf("\033[31m-----> Test invalidé\n");

}
void test_announce1(){
  printf("\033[0m\033[1m------- announce (fichier en téléchargement) -------\033[33m\n");
  bzero(message,BUFFER_SIZE); // on réinitialise le buffer
  char buffer[BUFFER_SIZE]="announce listen 2222 seed [file_a.dat 2097152 1024 8905e92afeb80fc7722ec89eb0bf0966 file_b.dat 3145728 1536 330a57722ec8b0bf09669a2b35f88e9e] leech [ 8905e92afeb80fc7722ec89eb0bf09661 ]\n";
  char * filename="file_a.dat";
  char * filename1="file_b.dat";
  char * key = "8905e92afeb80fc7722ec89eb0bf09661";
  printf("\033[1mMessage reçue : \033[0m%s",buffer);
  char * cmd = strtok(buffer, " \n");
  cmd_announce(cmd,0);
  printf("\033[33m\033[1mMessage envoyé: \033[0m%s",message);
  assert(strcmp(message,"ok\n")==0);
  int nb_files=0;
  for(int ind1=0;ind1<clients[0].nb_files;ind1++){
    if(strcmp(filename,clients[0].files[ind1].filename)==0) // On récupére les informations du fichiers
      nb_files++;
    else if(strcmp(filename1,clients[0].files[ind1].filename)==0) // On récupére les informations du fichiers
      nb_files++;
    else if(strcmp(key,clients[0].files[ind1].key)==0) // On récupére les informations du fichiers
      nb_files++;
  }
  if(nb_files==3)
    printf("\033[32m-----> Test validé\n");
  else
    printf("\033[31m-----> Test invalidé\n");

}


// --------- Test Commande LOOK ---------

void test_cmd_look(char * buffer,char * test,char * res){
  printf("\033[0m\033[1m------- look %s -------\n",test);

  printf("\033[33m\033[1mMessage reçue : \033[0m%s",buffer);
  char *cmd = strtok(buffer, " \n");
  cmd_look(cmd);
  printf("\033[33m\033[1mMessage envoyé: \033[0m%s",message);
  if(strcmp(message,res)==0)
    printf("\033[32m-----> Test %s validé\n",test);
  else
    printf("\033[31m-----> Test %s invalidé\n",test);
}

void test_look(){
  printf("\033[33m---------------- Test commande LOOK ----------------\n");
  char buffer[BUFFER_SIZE]="announce listen 2222 seed [file_a.dat 2097152 1024 8905e92afeb80fc7722ec89eb0bf0966 file_b.dat 3145728 1536 330a57722ec8b0bf09669a2b35f88e9e]\n";
  char * cmd = strtok(buffer, " \n");
  cmd_announce(cmd,0);
  bzero(message,BUFFER_SIZE); // on réinitialise le buffer
  char buffer1[BUFFER_SIZE]="look [filename=\"file_a.dat\" filesize>\"1048576\"]\n";
  test_cmd_look(buffer1,"filesize >","list [ file_a.dat 2097152 1024 8905e92afeb80fc7722ec89eb0bf0966 ]\n");
  char buffer2[BUFFER_SIZE]="look [filename=\"file_a.dat\" filesize<\"1048576\"]\n";
  test_cmd_look(buffer2,"filesize <","Aucun fichier du réseau ne vérifie ces critères\n");
  char buffer3[BUFFER_SIZE]="look [filename=\"file_a.dat\" \"1048576\"<filesize<\"2107152\"]\n";
  test_cmd_look(buffer3,"< filesize <","list [ file_a.dat 2097152 1024 8905e92afeb80fc7722ec89eb0bf0966 ]\n");
  char buffer4[BUFFER_SIZE]="look all\n";
  test_cmd_look(buffer4,"all","list [ file_a.dat 2097152 1024 8905e92afeb80fc7722ec89eb0bf0966 file_b.dat 3145728 1536 330a57722ec8b0bf09669a2b35f88e9e ]\n");
}

// --------- Test Commande GETFILE ---------

void test_getfile(){
  printf("\033[33m---------------- Test commande GETFILE ----------------\n");
  printf("\033[0m\033[1m------- getfile classique -------\033[33m\n");
  char buffer[BUFFER_SIZE]="announce listen 2222 seed [file_a.dat 2097152 1024 8905e92afeb80fc7722ec89eb0bf0966 file_b.dat 3145728 1536 330a57722ec8b0bf09669a2b35f88e9e]\n";
  char * cmd = strtok(buffer, " \n");
  cmd_announce(cmd,0);
  bzero(message,BUFFER_SIZE); // on réinitialise le buffer
  char buffer1[BUFFER_SIZE]="getfile 8905e92afeb80fc7722ec89eb0bf0966\n";
  printf("\033[1mMessage reçue : \033[0m%s",buffer1);
  cmd = strtok(buffer1, " \n");
  cmd_getfile(cmd);
  printf("\033[33m\033[1mMessage envoyé: \033[0m%s",message);
  if(strcmp(message,"peers 8905e92afeb80fc7722ec89eb0bf0966 [ 0.0.0.0:2222 ]\n")==0)
    printf("\033[32m-----> Test validé\n");
  else
    printf("\033[31m-----> Test invalidé\n");
}

void test_getfile1(){
  printf("\033[0m\033[1m------- getfile (aucune correspondance) -------\033[33m\n");
  char buffer[BUFFER_SIZE]="announce listen 2222 seed [file_a.dat 2097152 1024 8905e92afeb80fc7722ec89eb0bf0966 file_b.dat 3145728 1536 330a57722ec8b0bf09669a2b35f88e9e]\n";
  char * cmd = strtok(buffer, " \n");
  cmd_announce(cmd,0);
  bzero(message,BUFFER_SIZE); // on réinitialise le buffer
  char buffer1[BUFFER_SIZE]="getfile 8905e92afeb80fc7722ec89eb0bf09661\n";
  printf("\033[1mMessage reçue : \033[0m%s",buffer1);
  cmd = strtok(buffer1, " \n");
  cmd_getfile(cmd);
  printf("\033[33m\033[1mMessage envoyé: \033[0m%s",message);
  if(strcmp(message,"Cette clé ne correspond à aucun fichier du réseau\n")==0)
    printf("\033[32m-----> Test validé\n");
  else
    printf("\033[31m-----> Test invalidé\n");
}


// --------- Test Commande UPDATE ---------

void test_update(){
  printf("\033[33m---------------- Test commande UPDATE ----------------\n");
  bzero(message,BUFFER_SIZE); // on réinitialise le buffer
  char buffer[BUFFER_SIZE]="announce listen 2222 seed [file_a.dat 2097152 1024 8905e92afeb80fc7722ec89eb0bf0966 file_b.dat 3145728 1536 330a57722ec8b0bf09669a2b35f88e9e]\n";
  char * cmd = strtok(buffer, " \n");
  cmd_announce(cmd,0);
  bzero(message,BUFFER_SIZE); // on réinitialise le buffer
  char buffer1[BUFFER_SIZE]="update seed [] leech [8905e92afeb80fc7722ec89eb0bf09661]\n";
  printf("\033[1mMessage reçue : \033[0m%s",buffer1);
  cmd = strtok(buffer1, " \n");
  cmd_update(cmd,0);
  printf("\033[33m\033[1mMessage envoyé: \033[0m%s",message);
  char * key="8905e92afeb80fc7722ec89eb0bf09661";
  assert(strcmp(message,"ok\n")==0);
  int nb_file=0;
  for(int ind1=0;ind1<clients[0].nb_files;ind1++){
    if(strcmp(key,clients[0].files[ind1].key)==0) // On récupére les informations du fichiers
      nb_file=1;
  }
  if(nb_file==1)
    printf("\033[32m-----> Test validé\n");
  else
    printf("\033[31m-----> Test invalidé\n");

}

// --------- MAIN ---------
int main(){
  test_announce();
  test_announce1();
  test_look();
  test_getfile();
  test_getfile1();
  test_update();
  return 0;
}
