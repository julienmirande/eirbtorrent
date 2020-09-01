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

public class UploadStats{

  private String filename;
  private String key;
  private int nbOctetsFournis = 0;
  private int nbPiecesFournies = 0;

  public UploadStats(String filename, String key, int nof, int npf){
    this.filename = filename;
    this.key = key;
    this.nbOctetsFournis = nof;
    this.nbPiecesFournies = npf;
  }

  public void displayBilan(){
    PairMain.println("\n>>>> Bilan de contribution à la connexion entrante :");
    PairMain.println(">> Fichier demandé           : " + filename);
    PairMain.println(">> Clé                       : " + key + ")");
    PairMain.println(">> Nombre de pièces fournies : " + nbPiecesFournies + " pièces");
    PairMain.println(">> Nombre d'octets fournis   : " + nbOctetsFournis + " octets");
  }

  public String getKey(){
    return this.key;
  }

  // filename + key
  public String getFile(){
    return this.filename + ":" + this.key;
  }

  // nbOctetsFournis
  public int getNbOctetsFournis(){
    return this.nbOctetsFournis;
  }

  // nbPiecesFournies
  public int getNbPiecesFournies(){
    return this.nbPiecesFournies;
  }


}
