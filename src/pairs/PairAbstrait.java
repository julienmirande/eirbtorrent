import java.io.*;
import java.net.*;
import java.lang.Math;
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
import java.util.concurrent.ArrayBlockingQueue;
import java.security.DigestException;


abstract public class PairAbstrait{

  protected String name;

  protected int DEFAULT_NB_THREADS;
  protected int MAX_NB_THREADS;
  protected int KEEP_ALIVE;
  protected int MAX_TASKS;
  protected String CONFIG_PATH;
  protected String FILE_PATH;

  protected String ipTracker;
  protected String portTracker;
  protected String portPair;
  protected Long maxMsgSize;
  protected int updatePeriod;
  protected int havePeriod;

  protected ArrayList<Fichier> fichiers;

  protected PairStats stats;

  public PairAbstrait(){}

  public String getName(){
    return this.name;
  }

  public PairStats getStats(){
    return this.stats;
  }

  public Long getMaxMsgSize(){
    return this.maxMsgSize;
  }

  public int getUpdatePeriod(){
    return this.updatePeriod;
  }

  public int getHavePeriod(){
    return this.havePeriod;
  }

  public void setPortPair(int port){
    this.portPair = Integer.toString(port);
  }

  public ArrayList<Fichier> getFichiers(){
    return this.fichiers;
  }

  /*
  * retourne l'indice du fichier correspondant à la clé key dans l'ArrayList fichiers
  */
  public int getFichierIndex(String key){
    for(int i = 0; i < fichiers.size(); i++){
      if(key.equals(fichiers.get(i).getKey())){
        return i;
      }
    }
    return -1;
  }

  public ArrayList<Fichier> getLeechFichiers(){
    ArrayList<Fichier> leech = new ArrayList<Fichier>();
    for(int i = 0; i < fichiers.size(); i++){
      // on checke si la buffermap du fichier n'est ni complète ni vide
      Fichier f = fichiers.get(i);
      int nbPieces = f.getNbPieces();
      int nbBits = countBits(f.getBufferMap());
      // si c'est le cas on ajoute le fichier à leech
      if(nbBits != nbPieces && nbBits != 0){
        leech.add(f);
      }
    }
    return leech;
  }

  /*
  * Indique si un pair dont la bufferMap est pBM a besoin ou non de pieces de la bufferMap rBM
  */
  public boolean needPieces(String pBM, String rBM, int nbPieces){
    for(int i = 0; i < nbPieces; i++){
      if(getBitStr(pBM, i) == '0' && getBitStr(rBM, i) == '1'){
        return true;
      }
    }
    return false;
  }

  /*
  * Retourne le nombre de pieces de la bufferMap rBM dont un pair possèdant la bufferMap pBM a besoin
  */
  public int nbNeedPieces(String pBM, String rBM, int nbPieces){
    int nb = 0;
    for(int i = 0; i < nbPieces; i++){
      if(getBitStr(pBM, i) == '0' && getBitStr(rBM, i) == '1'){
        nb++;
      }
    }
    return nb;
  }


  public byte[] createChecksum(String path, String filename) {
    try {
    InputStream fis =  new FileInputStream(path);

    byte[] buffer = new byte[1024];
    MessageDigest complete = MessageDigest.getInstance("MD5");
    int numRead;

    do {
      numRead = fis.read(buffer);
      if (numRead > 0) {
        complete.update(buffer, 0, numRead);
      }
    } while (numRead != -1);

    fis.close();
    buffer = filename.getBytes();
    complete.update(buffer,0,filename.length());
    return complete.digest();
  } catch (Exception e) {
    PairMain.println("GetMD5Checksum trouble");
    System.exit(0);
  }

    return new byte[1024];
}

    // see this How-to for a faster way to convert
    // a byte array to a HEX string
  public String getMD5Checksum(String path, String filename)  {
    try {
      byte[] b = createChecksum(path,filename);
      String result = "";

    for (int i=0; i < b.length; i++) {
      result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
    }
    return result;
  } catch (Exception e) {
    PairMain.println("GetMD5Checksum trouble");
  }
    return "NULL";

  }


  /*
  * Encode le message text en binaire
  */
  public String textToBinary(String text){
    StringBuilder binary = new StringBuilder();
    try{
      byte[] bytes = text.getBytes(StandardCharsets.ISO_8859_1);
      for (byte b : bytes) {
          int val = b;
          for (int i = 0; i < 8; i++) {
              binary.append((val & 128) == 0 ? 0 : 1);
              val <<= 1;
          }
      }
    }
    catch (Exception e){
      PairMain.println(e.getMessage());
    }
    return binary.toString();
  }

  /*
  * Décode la chaîne binaire bin pour retrouver le message correspondant
  */
  public String binaryToText(String bin){
      StringBuilder sb = new StringBuilder();
      char[] chars = bin.toCharArray();

      //for each character
      for (int j = 0; j < chars.length; j+=8) {
          int idx = 0;
          int sum = 0;
          //for each bit in reverse
          for (int i = 7; i>= 0; i--) {
            if(i + j < chars.length){
              if (chars[i+j] == '1') {
                  sum += 1 << idx;
              }
              idx++;
            }
          }
          sb.append(Character.toChars(sum));
      }
      return sb.toString();
  }


  /* ------------------ PROTOCOLE PAIR-TRACKER -------------------------*/

  /*
  * Commande announce : le pair annonce sa présence au Tracker ainsi que la
  * liste des fichiers qu'il possède
  *
  * return : "announce listen $port seed [ $filename1 $length1 $piecesize1 $key1 $filename2 ... ]
  *                              leech [ ... ]"
  */
  public String announce(){

    // commande qui sera générée avec les arguments
    String cmd = "announce listen " + portPair;
    String seed = " seed [ ";
    String leech = "] leech [ ";

    String filename = "";
    String length = "";
    String pieceLength = "";
    String key = "";

    /* Ajoute chaque fichier du répertoire avec sa taille,
    la taille de chaque partie et sa clé à la partie seed ou leech de la ligne
    de commande en fonction de la BufferMap du fichier possédée */
    for (int i = 0; i < fichiers.size(); i++) {
      Fichier f = fichiers.get(i);

      Long fSize = f.getLength();
      Long pSize = f.getPieceSize();
      int nbPieces = f.getNbPieces();
      int nbBits = countBits(f.getBufferMap());

      // le fichier n'est annoncé dans seed que si le pair le possède entièrement (buffermap = 111...)
      if(nbBits == nbPieces){
        filename = f.getFilename();
        length = Long.toString(fSize);
        pieceLength = Long.toString(pSize);
        key = f.getKey();

        seed = seed.concat(filename + " "
        + length + " "
        + pieceLength + " "
        + key + " ");
      }

      // le fichier n'est annoncé dans leech que si le pair en possède au moins une partie
      else if(nbBits != 0){
        key = f.getKey();
        leech = leech.concat(key + " ");
      }
    }

    // construction de la chaine pour envoi
    cmd = cmd.concat(seed + leech + "]");
    return cmd;
  }

  /*
  * Indique si la chaîne de caractères str correspond à un nombre
  */
  public boolean isNumeric(String str)
  {
    for (char c : str.toCharArray())
    {
      if (!Character.isDigit(c)) return false;
    }
    return true;
  }

  /*
  * Commande look : le pair demande au Tracker la liste des fichiers du réseau
  * qui vérifient les critères spécifiés en ligne de commande
  *
  * return : "look [ filename="$filename" criterion2 ... ]"
  */
  public String look(String cmdLine[]){

    if (cmdLine.length < 2 || cmdLine.length > 4) {
      return "ERROR";
    }

    String cmd = "look [ ";

    // critère all
    if(cmdLine[1].equals("all")){
      cmd += "all ]";
      return cmd;
    }

    else{
      // le critère filename doit être spécifié en première position
      cmd += "filename=" + "\"" + cmdLine[1] + "\"";

      // critère filesize (> par défaut)
      if(cmdLine.length == 3){
        if(isNumeric(cmdLine[2]))
          cmd += " filesize>\"" + cmdLine[2] + "\"";
        else
          return "ERROR";
      }

      else if(cmdLine.length == 4){

        // critères filesize> ou filesize<
        if(cmdLine[2].equals(">") || cmdLine[2].equals("<")){
          if(isNumeric(cmdLine[3]))
            cmd += " filesize" + cmdLine[2] + "\"" + cmdLine[3] + "\"";
          else
            return "ERROR";
        }

        // critère <filesize<
        else{
          if(isNumeric(cmdLine[2]) && isNumeric(cmdLine[3]))
            cmd += " \"" + cmdLine[2] + "\"<filesize<\"" + cmdLine[3] + "\"";
          else
            return "ERROR";
        }
      }
    }

    cmd += " ]";
    return cmd;
  }

  /*
  * Stocke le fichier renvoyé par le tracker suite à la commande look
  */
  public void stockLookList(String msg[]){

    if(msg[2].equals("]")){
      return;
    }

    String filename = msg[2];
    Long length = Long.valueOf(msg[3]);
    Long pieceLength = Long.valueOf(msg[4]);
    String key = msg[5];

    // on vérifie si le fichier est déjà présent dans la liste de fichiers connus par le pair
    int index = getFichierIndex(key);

    // si le fichier n'est pas déjà présent, on l'ajoute à la liste
    if(index == -1){

      // Crée la BufferMap du fichier
      // (ici que des 0 car le pair ne possède pas encore le fichier)
      Long nbPieces = length / pieceLength + 1;
      StringBuffer bm = new StringBuffer();
      for(int n = 0; n < nbPieces; n++){
        bm.append("0");
      }

      Fichier f = new Fichier(filename, length, key, bm.toString());
      fichiers.add(f);
    }
  }

  /*
  * Commande getfile : le pair demande au Tracker les informations de connexion
  * des pairs qui possèdent le fichier correspondant à la clé spécifiéel
  *
  * return : "getfile $key"
  */
  public String getfile(String cmdLine[]){
    if(cmdLine.length != 2){
      return "ERROR1";
    }
    else if (getFichierIndex(cmdLine[1]) == -1){
      return "ERROR2";
    }
    String cmd = cmdLine[0] +" " + cmdLine[1] + " ";
    return cmd;
  }

  /*
  * Stocke les informations de connexion aux peers envoyés par le tracker
  * suite à la commande getfile
  */
  public void stockPeers(String msg[]){

    String key = msg[1];
    String peers[] = Arrays.copyOfRange(msg, 3, msg.length - 1);

    // on vérifie si le fichier est déjà présent dans la liste de fichiers connus par le pair
    int index = getFichierIndex(key);
    // si le fichier est déjà présent (normalement oui):
    if(index != -1){

      // on récupère tous les pairs qui possèdent le fichier (réponse du tracker)
      for(int i = 0; i < peers.length; i++){

        // on récupère l'adresse ip et le port de chaque pair
        String peer[] = peers[i].split(":");
        String ip = peer[0];
        String port = peer[1];

        // on vérifie qu'il ne s'agit pas du pair courant
        if(!port.equals(this.portPair)){

          Fichier f = fichiers.get(index);
          int pairIndex = f.getPairIndex(ip, port);
          // on vérifie que le pair n'est pas déjà dans la liste des pairs possédant le fichier
          if(pairIndex == -1){

            // on récupère le premier indice disponible dans le tableau de pairs du fichier
            pairIndex = f.getFirstFreeIndex();
            // s'il reste de la place on ajoute le pair à la liste
            if(pairIndex != -1){
              f.addPairToFile(pairIndex, ip, port);
            }
          }
        }
      }
    }
  }

  /*
  * Commande update : le pair indique au Tracker la liste des clés correspondant
  * au fichiers qu'il possède
  *
  * return : "update seed [ $key1 key2 ... ] leech [ key1 ... ]"
  */
  public String update(){

    // commande qui sera générée avec les arguments
    String cmd = "update";
    String seed = " seed [ ";
    String leech = "] leech [ ";

    String key = "";

    /* Ajoute la clé de chaque fichier du répertoire à la partie seed ou leech de la ligne
    de commande en fonction de la BufferMap du fichier possédée */
    for (int i = 0; i < fichiers.size(); i++) {
      Fichier f = fichiers.get(i);

      Long fSize = f.getLength();
      Long pSize = f.getPieceSize();
      int nbPieces = f.getNbPieces();
      int nbBits = countBits(f.getBufferMap());

      // le fichier n'est annoncé dans seed que si le pair le possède entièrement (buffermap = 111... = 2^n -1)
      if(nbBits == nbPieces){
        key = f.getKey();
        seed = seed.concat(key + " ");
      }

      // le fichier n'est annoncé dans leech que si le pair en possède au moins une partie
      else if(nbBits != 0){
        key = f.getKey();
        leech = leech.concat(key + " ");
      }
    }

    // construction de la chaine pour envoi
    cmd = cmd.concat(seed + leech + "]");
    return cmd;
  }


  /* ---------------------- PROTOCOLE PAIR-PAIR ----------------------------*/

  /*
  * Commande interested : le pair indique son intérêt pour le fichier
  * correspondant à la clé passée en paramètre
  *
  * return : "interested $key"
  */
  public String cmdInterested(String key){
    String cmd = "interested " + key;
    return cmd;
  }

  /*
  * Réponse aux commandes interested et have : le pair répond en envoyant une BufferMap
  * concernant les parties du fichier correspondant à la clé spécifiée
  *
  * return : "have $key $buffermap"
  */
  public String rspInterestedHave(String cmdLine[]){
    String key = cmdLine[1];
    int index = getFichierIndex(key);
    Fichier f = fichiers.get(index);
    String rsp = "have " + key + " " + f.getBufferMap();
    return rsp;
  }


  /*
  * Indique la valeur du bit en position i dans la bufferMap bm
  */
  public int getBit(Long bm, int i){
    return (int)((bm >> i) & 1);
  }

  public int getBitStr(String bm, int i){
    return bm.charAt(bm.length() - i -1);
  }

  /*
  * Compte le nombre de bits positionnés à 1 dans le bufferMap bm
  * (e.g. le nombre de pièces possédées par le pair)
  */
  public int countBits(String bm) {
    int cpt = 0;
    for (int i=0; i<bm.length(); i++) {
      if (bm.charAt(i) == '1'){
        cpt ++;
      }
    }
    return cpt;
  }

  public int[] getIndexesFromBM(String key, String bufferMap, int prio){

    int bmSize = bufferMap.length();
    int receivedBM = countBits(bufferMap);

    // on récupère la bufferMap du pair pour le fichier identifié par key
    Fichier f = fichiers.get(getFichierIndex(key));
    int nbPiecesPBM = f.getNbPieces();
    int nbBitsPBM = countBits(f.getBufferMap());

    // si la bufferMap reçue est vide ou si le pair possède déjà toutes les pièces du fichier,
    // on retourne un tableau d'un élément à -1
    if(receivedBM == 0 || nbPiecesPBM == nbBitsPBM){
      int err[] = {-1};
      return err;
    }

    // sinon, si le pair a besoin d'au moins une pièce du fichier
    else{

      // on crée un tableau d'indices de taille min(maxPieces, bmSize)
      Long maxPieces = this.maxMsgSize / f.getPieceSize();
      int nbPieces = Math.min(maxPieces.intValue(), bmSize);
      int[] indexes = new int[nbPieces];
      for(int k = 0; k < nbPieces; k++){
        indexes[k] = -1;
      }

      // le pair ignore les offset premiers indices en fonction de sa priorité
      // (prio = 0 => commence dès le début / prio = 1 => commence à partir de 1*nbPieces indices ...)
      int offset = prio * nbPieces;

      // on remplit indexes avec les indices des bits à 1 dans la bufferMap reçue si le
      // pair ne possède pas déjà ces pièces

      // compteur de pieces ajoutées
      int c = 0;
      // on parcours la bufferMap de gauche à droite
      for(int i = bmSize - 1; i > -1; i--){
        // si on n'a pas encore atteint le nombre max de pieces
        if(c < nbPieces){
          // pour la position i, si le bit de la bm du pair est à 0 et le bit de
          // la bm reçue est à 1, on ajoute l'indice de la pièce dans indexes
          if(getBitStr(f.getBufferMap(), i) == '0' && getBitStr(bufferMap, i) == '1'){

            // on ignore les offset premiers indices
            if(offset > 0){
              offset--;
            }
            else{
              indexes[c] = bmSize - i - 1;
              c++;
            }
          }
        }
        // si on a atteint le nombre max de pieces, inutile de parcourir le reste
        // de la bufferMap
        else{
          break;
        }
      }
      return indexes;
    }
  }


  /*
  * Commande getPieces : le pair demande le téléchargement des pièces du fichier
  * suite à la réponse have
  *
  * return : "getpieces $Key [ $Index1 $Index2 ... ]"
  */
  public String cmdGetpieces(String cmdLine[], int prio) {
    String key = cmdLine[1];
    String bm = cmdLine[2];
    String cmd = "getpieces " + key + " [ ";
    int indexes[] = getIndexesFromBM(key, bm, prio);

    // si le premier indice est -1 cela signifie que le pair n'a besoin
    // d'aucune pièce du fichier
    if(indexes[0] == -1){
      cmd += "]";
      return cmd;
    }

    for(int i = 0; i < indexes.length; i++){
      if(indexes[i] != -1){
        cmd += indexes[i] + " ";
      }
    }
    cmd += "]";
    return cmd;
  }

  /*
  * Réponse à la commande getpieces : le pair envoie la liste des pièces correspondant
  * aux indices demandés pour le fichier spécifié
  *
  * return : "data $Key [ $Index1:$Piece1 $Index2:$Piece2 ... ]"
  */
  public String rspGetpieces(String cmdLine[]) {
    String key = cmdLine[1];
    String msg = "data " + key + " [ ";

    String piecesIndexesStrings[] = Arrays.copyOfRange(cmdLine, 3, cmdLine.length - 1);
    int nbPieces = piecesIndexesStrings.length;
    int piecesIndexes[] = new int[nbPieces];

    for (int i = 0; i < nbPieces; i++) {
      piecesIndexes[i] = Integer.parseInt(piecesIndexesStrings[i]);
    }

    Fichier f = fichiers.get(getFichierIndex(key));
    long fileSize = f.getLength();
    long pieceSize = f.getPieceSize();
    long rest = fileSize;
    long toRead = 0;
    String filePieceString;

    for (int j = 0; j < nbPieces; j++) {

      // ajoute l'indice au message
      int index = piecesIndexes[j];
      msg += index + ":";

      // ajoute la piece codée en binaire au message
      filePieceString = f.getIndexData(index);
      String bin = textToBinary(filePieceString);
      msg += bin + " ";
    }

    msg += "]";
    return msg;
  }

  /*
  * Retourne une chaîne de caractères correspondant à la réponse data à getpieces
  * mais sans l'affichage binaire des pieces
  */
  public String lightRspGetpieces(String msg[]){
    String response = "data " + msg[1] + " [ ";
    String pieces[] = Arrays.copyOfRange(msg, 3, msg.length - 1);

    for(int i = 0; i < pieces.length; i++){

      int index = Integer.parseInt(pieces[i].split(":")[0]);
      response += index + ":%piece" + index + "% ";
    }
    response += "]";
    return response;
  }

  /*
  * Retourne une bufferMap avec le bit i positionné à 1
  */
  public String setBitBufferMap(String bm, int i){
    StringBuffer newBM = new StringBuffer(bm);
    newBM.setCharAt(i, '1');
    return newBM.toString();
  }

  /*
  * Stocke les pièces du fichier identifié par key envoyées par un pair
  */
  public void stockData(String msg[]){
    String key = msg[1];
    String pieces[] = Arrays.copyOfRange(msg, 3, msg.length - 1);

    if(pieces.length == 0){
      return;
    }

    Fichier f = fichiers.get(getFichierIndex(key));

    for(int i = 0; i < pieces.length; i++){

      String piece[] = pieces[i].split(":");
      int index = Integer.parseInt(piece[0]);
      String pieceBinaryData = piece[1];

      String pieceAsciiData = binaryToText(pieceBinaryData);
      f.setIndexData(index, pieceAsciiData);

      // modifie la bufferMap en fonction des nouvelles pièces obtenues
      f.setBufferMap(setBitBufferMap(f.getBufferMap(), index));
    }

    // on checke si la buffermap du fichier est complète
    int nbPieces = f.getNbPieces();
    int nbBits = countBits(f.getBufferMap());

    // si c'est le cas, on crée le fichier dans le répertoire FILE_PATH
    if(nbBits == nbPieces){
      try{
        FileOutputStream file = new FileOutputStream(this.FILE_PATH + f.getFilename(), true);
        for(int j = 0; j < nbPieces; j++){
          String data = f.getIndexData(j);
          file.write(data.getBytes());
        }
        file.close();
      }
      catch (Exception e) {
        PairMain.println("ici " + e.getMessage());
      }
    }
  }

  /*
  * Commande have : le pair indique l'état d'avancement du téléchargement
  *
  * return : "have $Key $BufferMap"
  */
  public String cmdHave(String key){
    int index = getFichierIndex(key);
    Fichier f = fichiers.get(index);
    String bm = f.getBufferMap();
    String cmd = "have " + key + " " +bm;
    return cmd;
  }


  /*
  * Stocke la BufferMap pour le fichier décrit par la clé key envoyée par le
  * pair correspondant à l'adresse IP et au port spécifiés
  */
  public void stockPairBM(String ip, String port, String msg[]){
    String key = msg[1];
    String bm = msg[2];
    int index = getFichierIndex(key);
    Fichier f = fichiers.get(index);
    int pairIndex = f.getPairIndex(ip, port);
    f.setIndexPairsBM(pairIndex, bm);
  }
}
