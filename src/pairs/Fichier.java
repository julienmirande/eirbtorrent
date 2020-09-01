import java.io.*;
import java.net.*;
import java.lang.Math;
import java.util.Arrays;
import java.util.ArrayList;
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
import java.util.concurrent.ArrayBlockingQueue;

public class Fichier {

  protected String CONFIG_PATH;

  protected String filename;
  protected long length;
  protected long pieceSize;
  protected String key;

  protected int maxPeers;
  protected String bufferMap;
  protected String pairsIP[];
  protected String pairsPort[];
  protected String pairsBM[];

  protected String data[];

  public Fichier(){}

  public Fichier(String fn, long l, String k, String bm){

    this.filename = fn;
    this.length = l;
    this.key = k;
    this.bufferMap = bm;

    this.CONFIG_PATH = "./../src/pairs/";

    File fc = new File(this.CONFIG_PATH);

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
        // Extraction du nombre max de pairs pour un fichier
        if (st.startsWith("max-peers-file")) {
          this.maxPeers = Integer.parseInt(st.split("\\=")[1].trim());
        }
        // Extraction de la taille des pièces des fichiers
        if (st.startsWith("piece-size")) {
          this.pieceSize = Long.valueOf(Integer.parseInt(st.split("\\=")[1].trim()));
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }

    this.pairsIP = new String[this.maxPeers];
    this.pairsPort = new String[this.maxPeers];
    this.pairsBM = new String[this.maxPeers];

    this.data = new String[getNbPieces()];

  }

  /*
  * Retourne le nombre de pièces qui composent le fichier
  */
  public int getNbPieces(){
    return (int)(this.length / this.pieceSize + 1);
  }

  /*
  * retourne l'indice du pair possédant l'adresse ip et le port spécifiés
  */
  public int getPairIndex(String ip, String port){
    for(int i = 0; i < pairsIP.length; i++){
      if(ip.equals(pairsIP[i]) && port.equals(pairsPort[i])){
        return i;
      }
    }
    return -1;
  }

  /*
  * retourne le premier indice disponible dans les tableaux de pairs
  */
  public int getFirstFreeIndex(){
    for(int i = 0; i < pairsIP.length; i++){
      if(pairsIP[i] == null){
        return i;
      }
    }
    return -1;
  }

  /*
  * ajoute un pair caractérisé par l'ip et le port spécifiés
  * à la liste des pairs possédant le fichier
  */
  public void addPairToFile(int index, String ip, String port){
    this.setIndexPairsIP(index, ip);
    this.setIndexPairsPort(index, port);

    // Crée la BufferMap du fichier pour le pair
    // (ici que des 0 car on ne connait pas encore la bm du pair qu'on ajoute)
    int nbPieces = getNbPieces();
    StringBuffer bm = new StringBuffer();
    for(int n = 0; n < nbPieces; n++){
      bm.append("0");
    }
    this.setIndexPairsBM(index, bm.toString());
  }

  // ---------- Setters et Getters ------------------------- //

  // filename
  public void setFilename(String fn){
    this.filename = fn;
  }

  public String getFilename(){
    return this.filename;
  }

  // length
  public void setLength(long l){
    this.length = l;
  }

  public long getLength(){
    return this.length;
  }

  // pieceSize
  public void setPieceSize(long ps){
    this.pieceSize = ps;
  }

  public long getPieceSize(){
    return this.pieceSize;
  }

  // key
  public void setKey(String k){
    this.key = k;
  }

  public String getKey(){
    return this.key;
  }

  // bufferMap
  public void setBufferMap(String bm){
    this.bufferMap = bm;
  }

  public String getBufferMap(){
    return this.bufferMap;
  }

  // pairsIP
  public void setPairsIP(String pip[]){
    this.pairsIP = pip;
  }

  public void setIndexPairsIP(int i, String ip){
    this.pairsIP[i] = ip;
  }

  public String[] getPairsIP(){
    return this.pairsIP;
  }

  public String getIndexPairsIP(int i){
    return this.pairsIP[i];
  }

  // pairsPort
  public void setPairsPort(String pp[]){
    this.pairsPort = pp;
  }

  public void setIndexPairsPort(int i, String p){
    this.pairsPort[i] = p;
  }

  public String[] getPairsPort(){
    return this.pairsPort;
  }

  public String getIndexPairsPort(int i){
    return this.pairsPort[i];
  }

  // pairsBM
  public void setPairsBM(String bm[]){
    this.pairsBM = bm;
  }

  public void setIndexPairsBM(int i, String bm){
    this.pairsBM[i] = bm;
  }

  public String[] getPairsBM(){
    return this.pairsBM;
  }

  public String getIndexPairsBM(int i){
    return this.pairsBM[i];
  }

  // data
  public void setData(String d[]){
    this.data = d;
  }

  public void setIndexData(int i, String d){
    this.data[i] = d;
  }

  public String[] getData(){
    return this.data;
  }

  public String getIndexData(int i){
    return this.data[i];
  }

}
