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
#include "thpool.h"
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
threadpool thpool;

void * func_accept(){
  socklen_t clilen = sizeof(cli_addr);
  int sd;
  while(1){
    FD_ZERO(&readfds);
    FD_SET(sockfd, &readfds);
    int max_sd = sockfd;
    int id_client; // stocke le file descriptor du nouveau client
    for ( int i = 0 ; i < MAX_CLIENTS ; i++){
      sd = client_sockfd[i];
      if(sd > 0) // si le socket est valide on l'ajoute à la read_list
        FD_SET( sd , &readfds);
      if(sd > max_sd) //récupère le numéro de file desciptor le plus élevé
        max_sd = sd;
    }
    //on attend l'activité de l'un des sockets
    activity = select( max_sd + 1 , &readfds , NULL , NULL , NULL);
    if ((activity < 0) && (errno!=EINTR))
      printf("select error");
    if (FD_ISSET(sockfd, &readfds)){
      if ((new_socket = accept(sockfd,(struct sockaddr *)&cli_addr, (socklen_t*)&clilen))<0){
        error("accept");
      }
      nb_clients++;

      for (int i = 0; i < MAX_CLIENTS; i++){
        if( client_sockfd[i] == 0 ) //ajoute le socket au tableau de sockets
        {
          client_sockfd[i] = new_socket;
          id_client=i;
          break;
        }
      }
      printf("Nouvelle connexion, Pair n°%d : socket fd = %d , @IP = %s , port = %d\n\n", id_client, new_socket, inet_ntoa(cli_addr.sin_addr) , ntohs(cli_addr.sin_port));

    }else{
      for (int i = 0; i < MAX_CLIENTS; i++){
        id_client=i; // on récupère le file descriptor

        sd = client_sockfd[i];
        if (FD_ISSET(sd, &readfds))
        {
          char buffer1[BUFFER_SIZE];
          bzero(buffer,BUFFER_SIZE); // on réinitialise le buffer

          int nb_read = read(sd,buffer,BUFFER_SIZE-1);     //lis les octets du socket newsockfd et les écrit dans le buffer
          if (nb_read < 0)
            error("ERROR reading from socket");

          if(nb_read==0){ // si on ne reçoit aucun message
            getpeername(sd , (struct sockaddr*)&cli_addr , \
            (socklen_t*)&clilen);
            printf("Pair n°%d déconnecté : @IP = %s , Port = %d \n" ,id_client, inet_ntoa(cli_addr.sin_addr) , ntohs(cli_addr.sin_port));
            close( sd ); // on ferme la connexion
            clients[id_client].nb_files=0;
            for (int i = 0; i < MAX_CLIENTS; i++){
              if( client_sockfd[i] == sd ){ // on réinitialise le stockage des file descriptor
                client_sockfd[i] = 0;
                break;
              }
            }
            break;
          }
          printf("Message reçu du pair n°%d : %s\n", id_client, buffer);
          bzero(buffer1,BUFFER_SIZE);
          strcpy(buffer1,buffer);
          char * test = strtok(buffer, " ");
          bzero(message,BUFFER_SIZE);
          while(test!=NULL){ // on teste si la commande est conforme
            if(strcmp(test,"\n") == 0){
              char * cmd = strtok(buffer1, " \n");
              if (strcmp(cmd,"announce") == 0){ // Traitement commande announce
                cmd_announce(cmd,id_client);
                break;
              }else if (strcmp(cmd,"look") == 0){//Traitement de la commande look
                cmd_look(cmd);
                break;
              }else if(strcmp(cmd,"getfile") == 0){//Traitement de la commande getfile
                cmd_getfile(cmd);
                break;
              }else if(strcmp(cmd,"update") == 0){ //Traitement de la commande update
                cmd_update(cmd,id_client);
                break;
              }
            }
            test = strtok(NULL, " []");
          }
          unsigned int n= send(new_socket, message, strlen(message), 0);
          if(n!= strlen(message))
            perror("send");
        }
      }
    }

  }
}

char *lire(char *tableau, int taille, FILE *fichier)
{
  char *retourligne;
  for(int i=0;i<2;i++){
    fgets(tableau, taille, fichier);
    retourligne= strchr(tableau, '\n');
    if (retourligne != NULL)
        retourligne = '\0';
      }
  retourligne = strtok(tableau, "\n");
  return tableau;
}
// --------- MAIN ---------
int main(){
  FILE * file=NULL;
  char * chaine = "src/server/config.ini";
  file = fopen(chaine, "r+");
  if (file == NULL){
    char * chaine = "../src/server/config.ini";
    file = fopen(chaine, "r+");
  }
  char tableau[20];
  int portno=atoi(lire(tableau, 20, file)); //récupère le numéro de port
  int opt = 1;
  nb_clients=0;
  thpool=thpool_init(4); // on initialise le pool de thread
  sockfd = socket(AF_INET, SOCK_STREAM, 0); //crée un socket et renvoie un descripteur
  if (sockfd < 0)
    error("socket failed");
  for (int i = 0; i < MAX_CLIENTS; i++){ //initialise client_socket[] à 0
    client_sockfd[i] = 0;
  }
  if( setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, (char *)&opt,sizeof(opt)) < 0 )
    error("setsockopt");
  bzero((char *) &serv_addr, sizeof(serv_addr)); //met à 0 les octets de serv_addr
  serv_addr.sin_family = AF_INET;
  serv_addr.sin_addr.s_addr = INADDR_ANY;
  serv_addr.sin_port = htons(portno); //affecte le numéro de port au socket
  // affecte l'adresse serv_addr à la socket référencée par sockfd
  if (bind(sockfd, (struct sockaddr *) &serv_addr,sizeof(serv_addr)) < 0)
    error("ERROR on binding");
  printf("Ecoute sur le port : %d \n", portno);
  // marque la socket référencée par sockfd comme une socket passive utilisée pour accepter les connexions entrantes en utilisant accept.
  // 5 correspond à la taille max de la file d'attente.
  if(listen(sockfd,MAX_CLIENTS)<0)
    error("listen failed");
  thpool_add_work(thpool,(void * )func_accept,NULL);

  thpool_wait(thpool); // on attend que tous les threads soient finis

  thpool_destroy(thpool); // on destruit le pool de thread
  fclose(file);
  return 0;
}
