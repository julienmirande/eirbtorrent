import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.*;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

class PairMain {

  private final static Logger logger = Logger.getLogger(PairMain.class.getName());
  public static ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(5);

  public static void print(String msg) {
    System.out.print(msg);
    logger.info(msg);
  }

  public static void println(String msg) {
    print(msg);
    System.out.println();
  }


  public static void usage(){
    System.out.println("Usage : java PairMain -n noClient [-s ] [-v | -vf]");
  }

  /* ------------------------ MAIN ------------------------------------ */
  public static void main(String args[]) throws Exception {

    FileHandler handler = new FileHandler("logs.txt", true);
    handler.setFormatter(new SimpleFormatter());
    logger.setUseParentHandlers(false);
    logger.addHandler(handler);

    if (args.length < 2 || args.length > 4) {
      usage();
      System.exit(0);
    }

    PairAbstrait client;
    int noClient = 1;
    boolean statsMode = false;
    int verbMode = 0;

    // parseur de la ligne de commande
    int n = 0;
    for(int i = 0; i < args.length; i++){
      if(args[i].equals("-n")){
        if(args.length > i + 1){
          noClient = Integer.parseInt(args[i + 1]);
          i++;
          n = 1;
        }
        else{
          usage();
          System.exit(0);
        }
      }
      else if(args[i].equals("-s")){
        statsMode = true;
      }
      else if(args[i].equals("-v")){
        verbMode = 1;
      }
      else if(args[i].equals("-vf")){
        verbMode = 2;
      }
    }

    if(n != 1){
      usage();
      System.exit(0);
    }

    switch (noClient) {
      case 1:
        client = new Pair1();
        break;
      case 2:
        client = new Pair2();
        break;
      case 3:
        client = new Pair3();
        break;
      default:
        client = new Pair1();
        noClient = 1;
        break;
    }

    //--------DEMARRAGE DU CLIENT POUR LE TRACKER --------//
    final String ipTracker = client.ipTracker;
    final int portTracker = Integer.parseInt(client.portTracker);

    scheduledExecutor.setCorePoolSize(client.DEFAULT_NB_THREADS);
    scheduledExecutor.setMaximumPoolSize(client.MAX_NB_THREADS);

    PairConnect clientTracker = new PairConnect(ipTracker, portTracker, client, statsMode, verbMode, true, false, null, 0);
    PairServer server = new PairServer(client, verbMode, statsMode);

    // Un thread dédié pour le update périodique
    Runnable runUpdateTracker = () -> {
    try {
      clientTracker.executerUpdate(true);
      } catch (Exception e) {
      PairMain.println("L'exécution de l'update périodique ne s'est pas bien passée : " + e.getMessage() + e.getStackTrace()[0].getLineNumber());
      }
    };

    // Un thread dédié pour le have périodique
    Runnable runHave = () -> {
    try {
      clientTracker.executerHave();
      } catch (Exception e) {
      PairMain.println("L'exécution du have périodique ne s'est pas bien passée : " + e.getMessage() + e.getStackTrace()[0].getLineNumber());
      }
    };

    Runnable runInfos = () -> {
      PairMain.println("Empty queue: " + scheduledExecutor.getQueue().isEmpty());
      PairMain.println("Nb taches en cours: " + scheduledExecutor.getTaskCount());
      PairMain.println("Nb threads en cours: " + scheduledExecutor.getActiveCount());
      PairMain.println("Nb threads dans le pool: " + scheduledExecutor.getPoolSize());
      PairMain.println("Nb taches terminés en cours: " + scheduledExecutor.getCompletedTaskCount());
    };

    // On lance la connexion avec le tracker
    Future<Integer> future = scheduledExecutor.submit(clientTracker,1);
    scheduledExecutor.submit(server);

    // lance les threads d'update et have périodiquement
    scheduledExecutor.scheduleAtFixedRate(runUpdateTracker, client.getUpdatePeriod(),
                                                            client.getUpdatePeriod(), TimeUnit.SECONDS);
    scheduledExecutor.scheduleAtFixedRate(runHave, 0, client.getHavePeriod(), TimeUnit.SECONDS);

    while(!future.isDone()) {
    }

    server.shutdown();

    //On attent le retour en asynchrone
    try {
      // si on a le code de retour 1 du exit du tracker
      // alors on détruit les éxécutors pour terminer
      if (future.get() == 1){
        scheduledExecutor.shutdown();
        scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    }finally {
      if (!scheduledExecutor.isTerminated()) {
          System.err.println("Suppression des tâches en cours");
      }
      scheduledExecutor.shutdownNow();
  }
    // Fin du thread main

  }

}
