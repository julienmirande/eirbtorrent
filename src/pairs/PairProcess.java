import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.text.DateFormat;
import java.util.Date;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.ArrayList;

public class PairProcess implements Runnable{

   private Socket sock;
   private PrintWriter writer = null;
   private BufferedReader reader = null;
   private boolean closeConnexion = false;
   private PairAbstrait client;
   private int verbMode;
   private boolean statsMode;

   public PairProcess(Socket pSock, PairAbstrait client, int verbMode, boolean statsMode){
      this.sock = pSock;
      this.client = client;
      this.verbMode = verbMode;
      this.statsMode = statsMode;
   }

   /*
   * Retourne le nombre de pièces et d'octets fournis dans le message data (réponse à getpieces)
   */
   public int[] getNbPiecesOctets(String data[]){
     int ret[] = {0, 0};
     String pieces[] = Arrays.copyOfRange(data, 3, data.length - 1);
     ret[0] = pieces.length;
     int octets = 0;
     for(int i = 0; i < pieces.length; i++){
       String bin = pieces[i].split(":")[1];
       octets += bin.length() /8;
     }
     ret[1] = octets;
     return ret;

   }

   // Pour les connexions pairs-à-pairs, le traitement des commandes est lancé dans un thread séparé
   public void run(){
      // tant que la connexion est active, on traite les demandes entrantes
      while(!sock.isClosed()){

         try {
            writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(sock.getOutputStream())), true);
            reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));

            //On attend la requête du client
            String request = reader.readLine();
            if(verbMode == 2){
              PairMain.println("Requête reçue : " + request);
            }
            String cmd[] = request.split(" ");
            InetSocketAddress remote = (InetSocketAddress)sock.getRemoteSocketAddress();
            String remoteIp = remote.getAddress().getHostAddress();
            int remotePort = remote.getPort();

            //On affiche quelques infos, pour le débuggage
            String debug = "";
            debug = "Thread : " + Thread.currentThread().getName() + ". ";
            debug += "Demande de l'adresse : " + remoteIp +".";
            debug += " Sur le port : " + remotePort + ".\n";
            debug += "\t -> Commande reçue : " + request + "\n";
            //System.err.println("\n" + debug);

            String toSend = "";

            //On traite la demande du client en fonction de la commande envoyée
            switch(cmd[0].toUpperCase()){

              // request : "interested $key"
               case "INTERESTED":
                  toSend = client.rspInterestedHave(cmd);
                  writer.println(toSend);
                  break;

              // request : "getpieces $key [ $Index1 $Index2 ... ]"
               case "GETPIECES":
                  toSend = client.rspGetpieces(cmd);
                  writer.println(toSend);

                  // on récupère les données de la réponse data pour faire des statistiques
                  String data[] = toSend.split(" ");
                  String key = data[1];
                  String filename = client.getFichiers().get(client.getFichierIndex(key)).getFilename();
                  int po[] = getNbPiecesOctets(data);
                  int nbPieces = po[0];
                  int nbOctets = po[1];

                  // on créé un UploadStats avec les statistiques de contribution du pair
                  UploadStats us = new UploadStats(filename, key, nbOctets, nbPieces);

                  if(statsMode){
                    us.displayBilan();
                  }

                  // on ajoute l'UploadStats aux statistiques du pair
                  client.getStats().addUploadStats(us);
                  break;

              // request : "have $key $buffermap"
               case "HAVE":
                  toSend = client.rspInterestedHave(cmd);
                  writer.println(toSend);
                  break;

               case "CLOSE":
                  toSend = ">>>>>> Fermeture de la connexion avec le pair...";
                  writer.println(toSend);
                  closeConnexion = true;
                  break;

               default :
                  toSend = "Commande inconnue !";
                  writer.println(toSend);
                  break;
            }

            if(closeConnexion){
              if(verbMode != 0){
               PairMain.println("\n>>>>>> Fermeture de la connexion sur le port " + sock.getPort() +"...\n");
              }
              writer.close();
              reader.close();
              sock.close();
              break;
            }
         }
         catch(SocketException e){
            System.err.println("LA CONNEXION A ETE INTERROMPUE ! ");
            break;
         }
         catch (IOException e) {
            e.printStackTrace();
         }
         catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   public void close() {
      closeConnexion = true;
   }

}
