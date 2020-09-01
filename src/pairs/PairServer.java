import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;


public class PairServer implements Runnable{

   //On initialise des valeurs par défaut
   private int port;
   private String host;
   private ServerSocket server = null;
   private boolean isRunning = true;
   private int maxClient = 100;
   private int timeOut = 2000000; // 2000s avant expiration du serveur
   PairProcess cp = null;
   private PairAbstrait client;
   private int verbMode;
   private boolean statsMode;

   public PairServer(){
      try {
         this.server = new ServerSocket(this.port, this.maxClient, InetAddress.getByName(host));
      } catch (UnknownHostException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public PairServer(PairAbstrait client, int verbMode, boolean statsMode){
      this.host = client.ipTracker;
      this.port = Integer.parseInt(client.portPair);
      this.client = client;
      this.verbMode = verbMode;
      this.statsMode = statsMode;
      try {
         server = new ServerSocket(this.port, this.maxClient, InetAddress.getByName(this.host));
      } catch (UnknownHostException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public void run(){
      System.out.println("Démarrage du serveur du pair en background sur le port " + port+ "...\n");
      while(true){
         try {
               // Au bout de timeOut temps d'inactivité en ms le serveur se ferme
               //server.setSoTimeout(timeOut);
               //System.out.println(server.getSoTimeout());
               //On attend une connexion d'un client
               Socket sockClient = server.accept();

               //Une fois reçue, on la traite dans un thread séparé
               if(verbMode != 0){
                 PairMain.println("\n>>>>>> Nouvelle connexion entrante sur le port " + sockClient.getPort() +"...");
               }
               cp = new PairProcess(sockClient, this.client, verbMode, statsMode);
               PairMain.scheduledExecutor.submit(cp);
            } catch (IOException e) {
               cp.close();
               break;
            }
         }

         try {
            System.err.println("\n>>>>>> Fermeture du serveur pour inactivité...");
            server.close();

         } catch (IOException e) {
            e.printStackTrace();
            server = null;
         }
   }

   public void close(){
      isRunning = false;
   }

   public void shutdown() throws IOException{
      server.setSoTimeout(2000);
   }

}
