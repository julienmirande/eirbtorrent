import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Pair2 extends PairAbstrait {

  public Pair2(){
    this.name = "Pair 2";

    this.DEFAULT_NB_THREADS = 5;
    this.MAX_NB_THREADS = 10;
    this.KEEP_ALIVE = 30;
    this.MAX_TASKS = 50;
    this.FILE_PATH = "./../src/pairs/pair2/fichiers/";
    this.CONFIG_PATH = "./../src/pairs/pair2/";

    this.fichiers = new ArrayList<Fichier>();

    this.stats = new PairStats(name);

    File fc = new File(CONFIG_PATH);

    FilenameFilter filter = new FilenameFilter() {
      @Override
      public boolean accept(File f, String name) {
        // on ne recherche que les fichiers .ini
        return name.endsWith(".ini");
      }
    };

    try {

      // Extraction des fichiers .ini dans files
      File[] filesIni = fc.listFiles(filter);

      // On check si config.ini existe
      if (filesIni.length == 0) {
        PairMain.println("Aucun fichier de configuration n'a été trouvé. Veuillez le créer et réessayer.");
        System.exit(0);
      }

      // Lecture du fichier de config
      BufferedReader br = new BufferedReader(new FileReader(filesIni[0]));

      // On parcours le fichier de config à la recherche des éléments qui nous intéressent
      String st;
      while ((st = br.readLine()) != null) {
        // Extraction de l'ip du tracker
        if (st.startsWith("tracker-address")) {
          this.ipTracker = st.split("\\=")[1].trim();
        }
        // Extraction du port d'écoute du tracker
        else if (st.startsWith("tracker-port")) {
          this.portTracker = st.split("\\=")[1].trim();
        }
        // Extraction du port d'écoute du pair
        else if (st.startsWith("pair-port")) {
          this.portPair = st.split("\\=")[1].trim();
        }
        // Extraction de la taille max des messages
        else if (st.startsWith("max-message-size")) {
          this.maxMsgSize = Long.valueOf(Integer.parseInt(st.split("\\=")[1].trim()));
        }
        // Extraction de la période de déclenchement de update
        else if (st.startsWith("update-period")) {
          this.updatePeriod = Integer.parseInt(st.split("\\=")[1].trim());
        }
        // Extraction de la période de déclenchement de have
        else if (st.startsWith("have-period")) {
          this.havePeriod = Integer.parseInt(st.split("\\=")[1].trim());
        }
      }

      // On met les fichiers disponibles dans le tableau fichiers[]
      File f = new File(FILE_PATH);
      File[] files = f.listFiles();

      for (int i = 0; i < files.length; i++) {

        // Extrait le nom du fichier
        String filename = files[i].getName();

        Path file = Paths.get(FILE_PATH + filename);
        BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);

        // Extrait la taille totale du fichier
        long fileSize = 0;
        if (attr.isRegularFile()){
          fileSize = attr.size();
        }

        // Calcule la clé correspondant au fichier
        String key = getMD5Checksum(FILE_PATH + filename, filename);

        // Crée le fichier avec les éléments spécifiés
        Fichier fichier = new Fichier(filename, fileSize, key, "0");

        // Crée la BufferMap du fichier (ici que des 1 car les fichiers du répertoire sont entiers)
        Long pieceSize = fichier.getPieceSize();
        Long nbPieces = fileSize / pieceSize + 1;
        StringBuffer bm = new StringBuffer();
        for(int n = 0; n < nbPieces; n++){
          bm.append("1");
        }
        fichier.setBufferMap(bm.toString());

        // Crée le tableau data contenant le contenu du fichier
        long rest = fileSize;
        long toRead = 0;

        try {

          RandomAccessFile raf = new RandomAccessFile(this.FILE_PATH + filename, "r");

          for (int j = 0; j < nbPieces; j++) {

            if(rest >= pieceSize){
              toRead = pieceSize;
            }
            else {
              toRead = rest;
            }

            byte[] filePiece = new byte[(int) toRead];
            rest -= toRead;

            // positionne l'offset de lecture du fichier à la bonne position
            long offset = j * pieceSize;
            raf.seek(offset);

            // lit toRead octets du fichier et les met dans filePiece
            raf.read(filePiece);

            // ajoute la piece d'indice j au tableau data du fichier
            fichier.setIndexData(j, new String(filePiece, StandardCharsets.UTF_8));

          }

        } catch (Exception e) {
          PairMain.println("test " + e.getMessage());
        }

        // ajoute le fichier à la liste de ses fichiers
        fichiers.add(fichier);

      }

    } catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }
  }
}
