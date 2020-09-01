import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Date;
import java.util.Locale;
import java.time.LocalDateTime;
import java.lang.Thread;
import java.text.NumberFormat;
import java.security.DigestException;

public class PairConnect implements Runnable{

   private Socket connexion = null;
   private PrintWriter writer = null;
   private BufferedReader reader = null;

   PairAbstrait client; // client qui lance la connexion
   private boolean statsMode;
   private int verbMode;
   private boolean tracker; // indique s'il s'agit d'une connexion au tracker
   private boolean have;
   private String key = null; // clé du fichier que l'on souhaite télécharger (tracker = false)
   private int prio = 0; // ordre de priorité de la connexion (tracker = false)

   public PairConnect(String host, int port, PairAbstrait client, boolean statsMode,
                        int verbMode, boolean tracker, boolean have, String key, int prio){
      try {
         this.connexion = new Socket(host, port);
         this.client = client;
         this.statsMode = statsMode;
         this.verbMode = verbMode;
         this.tracker = tracker;
         this.have = have;
         this.key = key;
         this.prio = prio;
      } catch (UnknownHostException e) {
         e.printStackTrace();
      } catch (IOException e) {
         PairMain.println("Le serveur du pair distant est fermé");
      }
   }

   public PairAbstrait getPairAbstrait() {
      return client;
   }

   public PrintWriter getWriter() {
      return writer;
   }

   public BufferedReader getReader() {
      return reader;
   }


   public void executerUpdate(boolean periodic) throws Exception{
     /*------- $<Update -------*/
      String message = client.update();
      PairMain.println("\n$< " + message);
      writer.println(message);

      /*------- $>ok -------*/
      String response = reader.readLine();
      if (response == null) {
         response = "Pas de réponse du tracker";
       }
       PairMain.println("$> " + response);
       if(periodic){
         PairMain.print("$< ");
       }
   }

   public void executerHave() throws Exception{

     ArrayList<Fichier> fichiers = client.getLeechFichiers();
     ArrayList<String> pairs = new ArrayList<String>();

     // pour chaque fichier du client, on envoie have à tous les autres pairs
     for(int i = 0; i < fichiers.size(); i++){

       Fichier f = fichiers.get(i);
       int k = f.getFirstFreeIndex();

       for(int j = 0; j < k; j++){
         String pair = f.getIndexPairsIP(j) + ":" + f.getIndexPairsPort(j);
         if(!pairs.contains(pair)){
           pair += ":" + f.getKey();
           pairs.add(pair);
         }
       }
     }

     for(int p = 0; p < pairs.size(); p++){
       String peer[] = pairs.get(p).split(":");
       String ip = peer[0];
       String port = peer[1];
       String key = peer[2];

       PairMain.scheduledExecutor.submit(new PairConnect(ip, Integer.parseInt(port), client, statsMode, verbMode, false, true, key, 0));
       Thread.sleep(500);
     }
   }

   public void run(){

    // s'il s'agit d'une connexion au tracker on affiche le shell client
    if (tracker) {
     PairMain.println("Connexion tracker établie sur le port "+ client.portTracker + "...\n");
     PairMain.println("Date de début : " + LocalDateTime.now());
     PairMain.println("------------- SHELL "+ client.getName() + " -------------\n");

     try{
       // writer : sert à écrire dans l'entrée d'écriture du socket connecté au tracker
       // afin de lui envoyer des messages
       writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(connexion.getOutputStream())), true);
       // reader  : sert à lire dans la sortie de lecture du socket connecté au tracker
       // afin de récupérer les messages qu'il a envoyé
       reader = new BufferedReader(new InputStreamReader(connexion.getInputStream()));

       // announce automatique au lancement du client
       String message = client.announce();
       PairMain.println("$< " + message);
       writer.println(message);

       String response = reader.readLine();
       if (response == null) {
         response = "Pas de réponse du tracker";
       }
       PairMain.println("$> " + response);

       // on attend que l'utilisateur saisisse une commande dans le shell
       while (true) {

          Scanner sc = new Scanner(System.in);
          PairMain.print("$< ");
          String nl = sc.nextLine();
          String str[] = nl.split(" ");
          String rsp[];

          // ANNOUNCE
          // traitement commande announce
          if (str[0].equals("announce")) {
            // Génération de la commande à envoyer au bon format
            message = client.announce();
            // Envoi du message coté tracker
            PairMain.println("$< " + message);
            writer.println(message);

            // traitement réponse announce : ok
            response = reader.readLine();
            if (response == null) {
              response = "Pas de réponse du tracker";
            }
            PairMain.println("$> " + response);
          }

          // LOOK
          // traitement commande look
          else if (str[0].equals("look")) {
            message = client.look(str);
            if (message.equals("ERROR")) {
              PairMain.println("Usage: \n"
              + "   - look filename\n"
              + "   - look filename size (default > size)\n"
              + "   - look filename < size\n"
              + "   - look filename > size\n"
              + "   - look filename lower upper\n"
              + "   - look all");
              continue;
            }
            PairMain.println("$< " + message);
            writer.println(message);


            // traitement réponse look : list
            response = reader.readLine();
            if (response == null) {
              response = "Pas de réponse du tracker";
            }
            PairMain.println("$> " + response);

            rsp = response.split(" ");
            if (rsp[0].equals("list")){
              client.stockLookList(rsp);
            }
          }

          // GETFILE
          // traitement commande getfile
          else if (str[0].equals("getfile")) {
            message = client.getfile(str);
            if (message.equals("ERROR1")) {
              PairMain.println("Usage: getfile key");
              continue;
            }
            else if (message.equals("ERROR2")){
              PairMain.println("Le pair ne connaît pas cette clé... "
                                + "Veuillez saisir une clé renvoyée par le tracker suite à la commande look.");
              continue;
            }

            writer.println(message);


            // traitement réponse getfile : peers
            response = reader.readLine();
            if (response == null) {
              response = "Pas de réponse du tracker";
            }
            PairMain.println("$> " + response);

            rsp = response.split(" ");
            if (rsp[0].equals("peers")){
              client.stockPeers(rsp);

              String key = rsp[1];
              // on récupère la bufferMap du pair pour le fichier identifié par key
              Fichier f = client.getFichiers().get(client.getFichierIndex(key));
              int nbPieces = f.getNbPieces();
              int nbBits = client.countBits(f.getBufferMap());
              long fileSize = f.getLength();

              // on récupère tous les pairs qui possèdent le fichier (réponse du tracker)
              String peers[] = Arrays.copyOfRange(rsp, 3, rsp.length - 1);
              ArrayList<DownloadStats> peersStats = new ArrayList<DownloadStats>();

              // création d'un DownloadStats pour chaque pair différent du pair courant
              for(int p = 0; p < peers.length; p++){
                // on récupère l'adresse ip et le port de chaque pair
                String peer[] = peers[p].split(":");
                String ip = peer[0];
                String port = peer[1];
                // on vérifie qu'il ne s'agit pas du pair courant
                if(!port.equals(client.portPair)){
                  peersStats.add(new DownloadStats(key, f.getFilename(), ip, Integer.parseInt(port),
                                                    nbPieces, fileSize));
                }
                else if(peers.length == 1){
                  PairMain.println("Ce pair est le seul à posséder le fichier.");
                }
              }

              int nbConnexions = 0; // nombre de connexions effectuées pour le téléchargement du fichier
              long nbTotOctetsDL = 0; // nombre total d'octets téléchargés
              long rest = fileSize; // reste en octets à télécharger
              long pieceSize = f.getPieceSize(); // taille des pièces du fichier
              int previousNbBits = nbBits;
              long t0, t1, t2, t4;

              int orderPrio; // ordre de priorité pour les connexions en parallèles
              int first = 1; // indique s'il s'agit de la première connexion effectuée
              int dl = 0; // indique si on est rentré dans le while ou non

              t0 = new Date().getTime();
              // on télécharge des pièces tant qu'on a pas téléchargé le fichier entièrement
              while(nbBits != nbPieces){
                orderPrio = 0;
                dl = 1;

                for(int i = 0; i < peersStats.size(); i++){
                  DownloadStats stats = peersStats.get(i);

                  // on récupère la bufferMap du pair auquel on souhaite se connecter
                  int remoteIndex = f.getPairIndex(stats.getIp(), Integer.toString(stats.getPort()));
                  int remoteBM = client.countBits(f.getIndexPairsBM(remoteIndex));

                  // on lance la connexion si le pair distant possède au moins une pièce dont on a besoin
                  // ou si la bufferMap reçue est vide (cas de la première connexion car aucun have n'a encore été fait)
                  if(client.needPieces(f.getBufferMap(), f.getIndexPairsBM(remoteIndex), nbPieces)
                      || (remoteBM == 0)){

                    // n'est affiché qu'à la première connexion
                    if(first == 1){
                      PairMain.println("\n>>>> Début du téléchargement du fichier " + f.getFilename() + "...");
                    }

                    t1 = new Date().getTime();

                    // On lance la connnexion avec le pair distant
                    PairMain.scheduledExecutor.submit(new PairConnect(stats.getIp(),
                                    stats.getPort(), client, statsMode, verbMode, false, false, key, 0));

                    t2 = new Date().getTime();

                    nbConnexions++;

                    if(statsMode || verbMode != 0){
                      orderPrio = 0;
                      Thread.sleep(2000); // sleep 2s
                    }
                    else{
                      orderPrio++;
                      Thread.sleep(100); // sleep 0.1s
                    }

                    // on recalcule le nombre de pièces possédées par le pair
                    nbBits = client.countBits(f.getBufferMap());
                    int nbPiecesDL = nbBits - previousNbBits;
                    long nbOctetsDL = 0;
                    previousNbBits = nbBits;

                    if(rest >= nbPiecesDL * pieceSize){
                      nbOctetsDL = nbPiecesDL * pieceSize;
                    }
                    else{
                      nbOctetsDL = rest;
                    }
                    rest -= nbOctetsDL;
                    nbTotOctetsDL += nbOctetsDL;

                    // on met à jour les statistiques de téléchargement de la connexion
                    stats.increaseNbTotPiecesDL(nbPiecesDL);
                    stats.increaseNbTotOctetsDL((int)nbOctetsDL);
                    stats.increaseTotTimeDL((float)((t2 - t1)/1000.0));
                    stats.increaseNbConnexions(1);

                    // si le mode stats est activé, on affiche un rapport de connexion
                    if(statsMode){
                      stats.displayStats(nbBits, nbTotOctetsDL, first);
                    }

                    // on lance update après la première connexion pour informer
                    // le tracker du début du téléchargement
                    if(first == 1){
                      first = 0;
                      executerUpdate(false);
                    }

                  } // fin du if de lancement d'une connexion : on passe à la connexion suivante

                } // fin de la boucle for : on a lancé toutes les connexions possibles pour ce tour

              } // fin du while : toutes les pièces ont été téléchargées

              // si on est rentré dans le while : le pair a téléchargé le fichier entièrement
              if(dl == 1){
                t4 = new Date().getTime();

                PairMain.println("\n>>>> Le fichier " + f.getFilename() + " a été téléchargé intégralement");

                // on ajoute les DownloadStats aux statistiques du client qui a téléchargé le fichier
                for(int i = 0; i < peersStats.size(); i++){
                  client.getStats().addDownloadStats(peersStats.get(i));
                }

                // si le mode stats est activé on affiche le bilan du téléchargement
                if(statsMode){
                  NumberFormat nf = NumberFormat.getNumberInstance(Locale.FRENCH);
                  nf.setMinimumFractionDigits(2);
                  nf.setMaximumFractionDigits(2);

                  float timeDL = (float)((t4 - t0)/1000.0);
                  float debit = (float) (nbTotOctetsDL * 1.0 / timeDL);

                  PairMain.println("\n>>>>>>>>>>>>>>>>>>>>>> BILAN DU TÉLÉCHARGEMENT <<<<<<<<<<<<<<<<<<<<<<\n");
                  PairMain.println(">> Durée du téléchargement         : " + timeDL + "s");
                  PairMain.println(">> Nombre d'octets téléchargés     : " + nbTotOctetsDL + "/" + fileSize
                  + " (" + nbTotOctetsDL * 1.0 /fileSize * 100 + "%)");
                  PairMain.println(">> Nombre de pieces téléchargées   : " + nbBits + "/" + nbPieces
                  + " (" + nbBits * 1.0 /nbPieces * 100 + "%)");
                  PairMain.println(">> Nombre de connexions effectuées : " + nbConnexions);
                  PairMain.println(">> Débit du téléchargement         : " + nf.format(debit) + " octets/s");

                  // on affiche le bilan de contribution de chaque pair à qui on a téléchargé des pièces
                  for(int i = 0; i < peersStats.size(); i++){
                    peersStats.get(i).displayBilan();
                  }

                  PairMain.println("\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>+<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n");
                }

                // on lance update pour informer le tracker de la fin du téléchargement
                executerUpdate(false);
              }

              // si on est pas rentré dans le while cela signifie que le pair possèdait
              // déjà le fichier entièrement
              else{
                PairMain.println("Ce pair possède déjà le fichier entièrement !");
              }
            }

          } // fin du traitement de la réponse à getfile (peers)

          // UPDATE
          // traitement commande update + réponse : ok
          else if (str[0].equals("update")) {
            executerUpdate(false);
          }

          // HAVE
          // traitement commande have + réponse : have
          else if (str[0].equals("have")) {
            executerHave();
          }

          // EXIT
          // traitement commande exit
          else if (str[0].equals("exit")) {
            writer.println("exit");
            PairMain.println("-------- END ----------");
            reader.close();
            writer.close();
            connexion.close();
            break;
          }

          // OPEN
          // traitement commande open
          else if(str[0].equals("open")){
            PairMain.scheduledExecutor.submit(new PairServer(client, verbMode, statsMode));
          }

          // STATS
          // traitement commande stats
          else if(str[0].equals("stats")){
            if(str.length > 2){
              PairMain.println("Usage : stats [download | upload]");
            }
            else if(str.length == 1){
              client.getStats().displayStatsDL();
              client.getStats().displayStatsUL();
            }
            else if(str[1].equals("download")){
              client.getStats().displayStatsDL();
            }
            else if (str[1].equals("upload")){
              client.getStats().displayStatsUL();
            }
            else{
              PairMain.println("Usage : stats [download | upload]");
            }
          }

          else {
            continue;
          }
        }
      } catch (NumberFormatException e) {
        e.printStackTrace();
        PairMain.println("NumberFormatException: " +e.getCause() + e.getLocalizedMessage()+ e.getMessage() + e.getStackTrace()[0].getLineNumber() );
     }catch (Exception e) {
      PairMain.println("Exception: " + e.getMessage() + e.getStackTrace()[0].getLineNumber());
     }/*
      catch (Exception e) {
         PairMain.println("L'exécution du thread client ne s'est pas bien passée : " +e.getCause() + e.getLocalizedMessage()+ e.getMessage() + e.getStackTrace()[0].getLineNumber());
      }*/

  }

  // --------- CONNEXION PAIR-PAIR ------------ //
  // si tracker = false, il s'agit d'une connexion pair-à-pair
  else {

    // on récupère les coordonnées de connexion du pair distant (serveur)
    InetSocketAddress remote = (InetSocketAddress)connexion.getRemoteSocketAddress();
    String connexionIP = remote.getAddress().getHostAddress();
    int connexionPort = connexion.getPort();

    if(verbMode != 0){
      PairMain.println("\n>>>>>> Connexion établie avec le pair écoutant sur le port " + connexionPort + "...\n");
    }

    try {
      String message = "";
      String response = "";
      String rsp[];

      writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(connexion.getOutputStream())), true);
      reader = new BufferedReader(new InputStreamReader(connexion.getInputStream()));

      // si have = true, il s'agit d'une connexion périodique dédiée à un have
      if(have){
        /*------- $<Have -------*/
        // on informe le pair distant de notre bufferMap pour le fichier identifié par key
        message = client.cmdHave(this.key);
        if(verbMode != 0){
          PairMain.println("$< " + message);
        }
        writer.println(message);

        /*------- $>Have -------*/
        response = reader.readLine();
        if (response == null) {
          response = "Pas de réponse du pair distant / message = " + message ;
        }
        if(verbMode != 0){
          PairMain.println("$> " + response + "\n");
        }
        rsp = response.split(" ");
        // on stocker la bufferMap renvoyée par le pair distant (serveur)
        client.stockPairBM(connexionIP, Integer.toString(connexionPort), rsp);

        /*------- $<Close -----*/
        message = "close";
        writer.println(message);

        response = reader.readLine();
        if (response == null) {
          response = "Pas de réponse du pair distant / message = " + message ;
        }
        if(verbMode != 0){
          PairMain.println(response + "\n");
        }
      }

      // si have = false, il s'agit d'une connexion dans le cadre d'un téléchargement de pièces
      else{

        /*------- $<Interested -------*/
        // on informe le pair distant de notre intérêt pour le fichier identifié par la clé key
        message = client.cmdInterested(this.key);
        if(verbMode != 0){
          PairMain.println("$< " + message);
        }
        writer.println(message);

        /*------- $>Have -------*/
        response = reader.readLine();
        if (response == null) {
          response = "Pas de réponse du pair distant / message = " + message ;
        }
        if(verbMode != 0){
          PairMain.println("$> " + response + "\n");
        }
        rsp = response.split(" ");
        // on stocke la bufferMap renvoyée par le pair distant (serveur)
        client.stockPairBM(connexionIP, Integer.toString(connexionPort), rsp);


        /*------- $<Getpieces -------*/
        // on demande les pièces dont nous avons besoin par rapport à la bufferMap renvoyée
        message = client.cmdGetpieces(rsp, this.prio);
        if(verbMode != 0){
          PairMain.println("$< " + message);
        }
        writer.println(message);

        /*------- $>Data -------*/
        response = reader.readLine();
        if (response == null) {
          response = "Pas de réponse du pair distant / message = " + message ;
        }
        rsp = response.split(" ");
        // on stocke les pièces renvoyées par le pair distant
        client.stockData(rsp);

        switch(verbMode){
          case 0:
            break;
          case 1:
            // si verbMode = 1 (-v), on affiche la réponse data sans le codage binaire des pièces
            // data $key [ $index1:%piece1% ... ]
            response = client.lightRspGetpieces(response.split(" "));
            PairMain.println("$> " + response + "\n");
            break;
          case 2:
            // si verbMode = 2 (-vf), on affiche la réponse data avec le codage binaire des pièces
            // data $key [ $index1:1010101110011... ... ]
            PairMain.println("$> " + response + "\n");
            break;
          default:
            break;
        }

        /*------- $<Close -----*/
        message = "close";
        writer.println(message);

        response = reader.readLine();
        if (response == null) {
          response = "No answer from peer";
        }
        if(verbMode != 0){
          PairMain.println(response + "\n");
        }
      }
    } catch (Exception e) {
        //e.printStackTrace();
        connexion = null;
    }
  }
}

}
