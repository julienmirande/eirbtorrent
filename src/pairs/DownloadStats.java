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

public class DownloadStats{

  private String key;
  private String filename;
  private int fileNbPieces;
  private long fileSize;

  private String ip;
  private int port;

  private int nbPiecesDL = 0;
  private int nbTotPiecesDL = 0;
  private int nbOctetsDL = 0;
  private int nbTotOctetsDL = 0;
  private int nbConnexions = 0;
  private float timeDL = 0;
  private float totTimeDL = 0;

  private NumberFormat nfDebit;
  private NumberFormat nfPourcent;


  public DownloadStats(String key, String filename, String ip, int port, int fileNbPieces, long fileSize){
    this.key = key;
    this.filename = filename;
    this.ip = ip;
    this.port = port;
    this.fileNbPieces = fileNbPieces;
    this.fileSize = fileSize;

    nfPourcent = NumberFormat.getNumberInstance(Locale.FRENCH);
    nfPourcent.setMinimumFractionDigits(1);
    nfPourcent.setMaximumFractionDigits(1);

    nfDebit = NumberFormat.getNumberInstance(Locale.FRENCH);
    nfDebit.setMinimumFractionDigits(2);
    nfDebit.setMaximumFractionDigits(2);
  }

  public String displayProgress(int nbPieces, int nbTotal){
    String progress = "[";
    for(int i = 0; i < nbPieces; i++){
      progress += "=";
    }
    for(int j = nbPieces; j < nbTotal; j++){
      progress += "-";
    }
    progress += "]";
    return progress;
  }

  public void displayStats(int nbBits, long nbOctets, int first){

    float debit = 0;
    if(timeDL != 0){
      debit = (float) (nbOctetsDL * 1.0 / timeDL);
    }

    PairMain.println("\n>>>> Rapport de connexion au pair écoutant sur le port " + port);
    PairMain.println(">> Nombre de pièces téléchargées       : " + nbPiecesDL + " pièces");
    PairMain.println(">> Nombre d'octets téléchargés         : " + nbOctetsDL + " octets");
    PairMain.println(">> Durée de téléchargement             : " + nfDebit.format(timeDL) + "s");
    PairMain.println(">> Débit de la connexion               : " + nfDebit.format(debit) + " octets/s");

    PairMain.println(">> Progression du téléchargement       : " + nfPourcent.format((float)nbOctets/fileSize*100) + "% "
                        + displayProgress(nbBits, fileNbPieces));
    PairMain.println(">> Nombre total de pièces téléchargées : " + nbBits + "/" + fileNbPieces + " pièces");
    PairMain.println(">> Nombre total d'octets téléchargés   : " + nbOctets + "/" + fileSize + " octets");

    if(first == 1){
      PairMain.println("");
    }
  }

  public void displayBilan(){

    float debit = 0;
    if(totTimeDL != 0){
      debit = (float) (nbTotOctetsDL * 1.0 / totTimeDL);
    }

    PairMain.println("\n>>>> Bilan de contribution du pair écoutant sur le port " + port);
    PairMain.println(">> Nombre de connexions effectuées sur ce port : " + nbConnexions + " connexions");
    PairMain.println(">> Temps de téléchargement total               : " + nfDebit.format(totTimeDL) + "s");
    PairMain.println(">> Nombre total de pièces fournies             : " + nbTotPiecesDL + "/" + fileNbPieces
                        + " pièces (" + nfPourcent.format(nbTotPiecesDL *1.0 /fileNbPieces * 100) + "%)");
    PairMain.println(">> Nombre total d'octets fournis               : " + nbTotOctetsDL + "/" + fileSize
                        + " octets (" + nfPourcent.format(nbTotOctetsDL * 1.0 /fileSize * 100) + "%)");
    PairMain.println(">> Débit de téléchargement                     : " + nfDebit.format(debit) + " octets/s");
  }

  // GETTERS ET SETTERS

  // key
  public String getKey(){
    return this.key;
  }

  public void setKey(String key){
    this.key = key;
  }

  // fileSize
  public long getFileSize(){
    return this.fileSize;
  }

  public void setFileSize(long fileSize){
    this.fileSize = fileSize;
  }

  // fileNbPieces
  public int getFileNbPieces(){
    return this.fileNbPieces;
  }

  public void setFileNbPieces(int nb){
    this.fileNbPieces = nb;
  }

  // filename + key
  public String getFile(){
    return this.filename + ":" + this.key;
  }

  // key
  public String getIp(){
    return this.ip;
  }

  // port
  public int getPort(){
    return this.port;
  }

  public void setPort(int port){
    this.port = port;
  }

  // nbTotPiecesDL
  public int getNbTotPiecesDL(){
    return this.nbTotPiecesDL;
  }

  public void setNbTotPiecesDL(int n){
    this.nbTotPiecesDL = n;
  }

  public void increaseNbTotPiecesDL(int n){
    this.nbPiecesDL = n;
    this.nbTotPiecesDL += n;
  }

  // nbTotOctetsDL
  public int getNbTotOctetsDL(){
    return this.nbTotOctetsDL;
  }

  public void setNbTotOctetsDL(int n){
    this.nbTotOctetsDL = n;
  }

  public void increaseNbTotOctetsDL(int n){
    this.nbOctetsDL = n;
    this.nbTotOctetsDL += n;
  }

  // nbConnexions
  public int getNbConnexions(){
    return this.nbConnexions;
  }

  public void setNbConnexions(int n){
    this.nbConnexions = n;
  }

  public void increaseNbConnexions(int n){
    this.nbConnexions += n;
  }

  // timeDL
  public float getTotTimeDL(){
    return this.totTimeDL;
  }

  public void setTotTimeDL(float t){
    this.totTimeDL = t;
  }

  public void increaseTotTimeDL(float t){
    this.timeDL = t;
    this.totTimeDL += t;
  }




}
