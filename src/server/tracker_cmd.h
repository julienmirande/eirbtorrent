#ifndef TRACKER_H
#define TRACKER_H
#define BUFFER_SIZE 1024
#define MAX_CLIENTS 3
#define MAX_FILES 10
#define PORT_LENGTH 20
#define FILENAME_LENGTH 30
#define FILESIZE_LENGTH 10
#define PIECE_SIZE_LENGTH 10
#define KEY_LENGTH 40

char buffer[BUFFER_SIZE];

// --------- LES STRUCTURES ---------

struct file{ // stockage d'un file
  char filename[FILENAME_LENGTH];
  char filesize[FILESIZE_LENGTH];
  char length_piece[PIECE_SIZE_LENGTH];
  char key[KEY_LENGTH];
};

struct infos_client{ // stockage des infos clients
  char port[PORT_LENGTH];
  struct file files[MAX_FILES];
  int nb_files;
};

void error(char *msg);

void affiche();


// --------- Commande ANNOUNCE ---------

void cmd_announce(char * cmd,int id_client);


// --------- Commande LOOK ---------

void cmd_look(char * cmd);

// --------- Commande GETFILE ---------


void cmd_getfile(char * cmd);

// --------- Commande UPDATE ---------

void cmd_update(char * cmd,int id_client);

#endif
