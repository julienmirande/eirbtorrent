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

public class PairStats{

  private String name;

  private ArrayList<DownloadStats> dsList;
  private ArrayList<String> downloadedFiles;

  private ArrayList<UploadStats> usList;
  private ArrayList<String> uploadedFiles;

  private NumberFormat nfDebit;

  public PairStats(String name){
    this.name = name;

    this.dsList = new ArrayList<DownloadStats>();
    this.downloadedFiles = new ArrayList<String>();

    this.usList = new ArrayList<UploadStats>();
    this.uploadedFiles = new ArrayList<String>();

    nfDebit = NumberFormat.getNumberInstance(Locale.FRENCH);
    nfDebit.setMinimumFractionDigits(2);
    nfDebit.setMaximumFractionDigits(2);
  }


  public void addDownloadStats(DownloadStats ds){
    this.dsList.add(ds);
    if(!this.downloadedFiles.contains(ds.getFile())){
      this.downloadedFiles.add(ds.getFile());
    }
  }

  public void addUploadStats(UploadStats us){
    this.usList.add(us);
    if(!this.uploadedFiles.contains(us.getFile())){
      this.uploadedFiles.add(us.getFile());
    }
  }

  public void displayStatsDL(){

    int nbPiecesDL = 0;
    int nbOctetsDL = 0;
    int nbConnexionsDL = 0;
    float timeDL = 0;
    float debit = 0;

    PairMain.println("\n>>>>>>>>>>>>>>> Statistiques de téléchargement du " + name + " <<<<<<<<<<<<<<<");

    for(int i = 0; i < downloadedFiles.size(); i++){
      String file[] = downloadedFiles.get(i).split(":");
      String filename = file[0];
      String filekey = file[1];

      PairMain.println("\n>>>>>> Fichier téléchargé : " + filename);

      boolean first = true;

      for(int j = 0; j < dsList.size(); j++){
        DownloadStats ds = dsList.get(j);

        if(first){
          first = false;
          PairMain.println("\n>> Clé               : " + filekey);
          PairMain.println(">> Taille du fichier : " + ds.getFileSize() + " octets");
          PairMain.println(">> Nombre de pièces  : " + ds.getFileNbPieces());
        }

        String key = ds.getKey();
        if(key.equals(filekey)){
          nbPiecesDL += ds.getNbTotPiecesDL();
          nbOctetsDL += ds.getNbTotOctetsDL();
          nbConnexionsDL += ds.getNbConnexions();
          timeDL += ds.getTotTimeDL();
          ds.displayBilan();
        }
      }
    }

    if(timeDL != 0){
        debit = (float) (nbOctetsDL * 1.0 / timeDL);
    }
    PairMain.println("\n>>>>>>>>>>>> Bilan de téléchargement du " + name + " <<<<<<<<<<<<\n");
    PairMain.println(">> Nombre de fichiers téléchargés        : " + downloadedFiles.size() + " fichier(s)");
    PairMain.println(">> Nombre total d'octets téléchargés     : " + nbOctetsDL + " octets");
    PairMain.println(">> Nombre total de pièces téléchargées   : " + nbPiecesDL + " pièces");
    PairMain.println(">> Nombre total de connexions effectuées : " + nbConnexionsDL + " connexions");
    PairMain.println(">> Temps total de téléchargement         : " + nfDebit.format(timeDL) + "s");
    PairMain.println(">> Débit moyen de téléchargement         : " + nfDebit.format(debit) + " octets/s");
    PairMain.println("\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n");
  }

  public void displayStatsUL(){

    int nbPiecesUL = 0;
    int nbOctetsUL = 0;
    int nbConnexionsUL = usList.size();

    PairMain.println("\n>>>>>>>>>>>>>>> Statistiques de contribution du " + name + " <<<<<<<<<<<<<<<");

    for(int i = 0; i < uploadedFiles.size(); i++){
      String file[] = uploadedFiles.get(i).split(":");
      String filename = file[0];
      String filekey = file[1];

      int nbPiecesFileUL = 0;
      int nbOctetsFileUL = 0;
      int nbConnexionsFileUL = 0;

      PairMain.println("\n>>>>>> Fichier fourni : " + filename);
      PairMain.println("\n>> Clé                                         : " + filekey);

      for(int j = 0; j < usList.size(); j++){
        UploadStats us = usList.get(j);

        String key = us.getKey();
        if(key.equals(filekey)){
          nbPiecesFileUL += us.getNbPiecesFournies();
          nbPiecesUL += us.getNbPiecesFournies();
          nbOctetsFileUL += us.getNbOctetsFournis();
          nbOctetsUL += us.getNbOctetsFournis();
          nbConnexionsFileUL += 1;
        }
      }

      PairMain.println(">> Nombre d'octets fournis pour ce fichier     : " + nbOctetsFileUL + " octets");
      PairMain.println(">> Nombre de pièces fournies pour ce fichier   : " + nbPiecesFileUL + " pièces");
      PairMain.println(">> Nombre de connexions reçues pour ce fichier : " + nbConnexionsFileUL + " connexions");
    }

    PairMain.println("\n>>>>>>>>>>>> Bilan de contribution du " + name + " <<<<<<<<<<<<\n");
    PairMain.println(">> Nombre de fichiers fournis        : " + uploadedFiles.size() + " fichier(s)");
    PairMain.println(">> Nombre total d'octets fournis     : " + nbOctetsUL + " octets");
    PairMain.println(">> Nombre total de pièces fournies   : " + nbPiecesUL + " pièces");
    PairMain.println(">> Nombre total de connexions reçues : " + nbConnexionsUL + " connexions");
    PairMain.println("\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n");
  }

}
