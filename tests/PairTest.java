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

public class PairTest{

  public static final String RESET = "\u001B[0m";
  public static final String GREEN = "\033[1;32m";
  public static final String RED = "\033[1;31m";
  public static final String YELLOW = "\033[1;33m";
  public static final String PURPLE = "\033[1;35m";

  public PairTest(){}

  public static void testNeedPieces(){
    int nbTests = 0;
    int nbTestsPassed = 0;

    PairAbstrait pair = new Pair1();
    String pbm;
    String rbm;
    int nbPieces;

    pbm = "00000000";
    rbm = "10000000";
    nbPieces = 8;
    assert pair.needPieces(pbm, rbm, nbPieces) == true;
    nbTests++;
    nbTestsPassed++;

    pbm = "111111111";
    rbm = "000000000";
    assert pair.needPieces(pbm, rbm, nbPieces) == false;
    nbTests++;
    nbTestsPassed++;

    pbm = "111111110";
    rbm = "111111111";
    assert pair.needPieces(pbm, rbm, nbPieces) == true;
    nbTests++;
    nbTestsPassed++;

    pbm = "111111110";
    rbm = "111111110";
    assert pair.needPieces(pbm, rbm, nbPieces) == false;
    nbTests++;
    nbTestsPassed++;

    System.out.println("TestNeedPieces      : " + nbTestsPassed + "/" + nbTests
                      + ((nbTestsPassed == nbTests) ? "   --> " + GREEN + "OK" + RESET
                                                    : "   --> " + RED + "NOK" + RESET));
  }

  public static void testAnnounce() {
    int nbTests = 0;
    int nbTestsPassed = 0;

    PairAbstrait pair = new Pair1();
    String message;
    String cmdLine[];
    // TO DO
  }

  public static void testLook() {
    int nbTests = 0;
    int nbTestsPassed = 0;

    PairAbstrait pair = new Pair1();
    String message;
    String cmdLine[];

    message = "look";
    cmdLine = message.split(" ");
    assert pair.look(cmdLine).equals("ERROR");
    nbTests++;
    nbTestsPassed++;

    message = "look fichier1_a.txt 11 12 13";
    cmdLine = message.split(" ");
    assert pair.look(cmdLine).equals("ERROR");
    nbTests++;
    nbTestsPassed++;

    message = "look fichier1_a.txt test";
    cmdLine = message.split(" ");
    assert pair.look(cmdLine).equals("ERROR");
    nbTests++;
    nbTestsPassed++;

    message = "look fichier1_a.txt > test";
    cmdLine = message.split(" ");
    assert pair.look(cmdLine).equals("ERROR");
    nbTests++;
    nbTestsPassed++;

    message = "look fichier1_a.txt < test";
    cmdLine = message.split(" ");
    assert pair.look(cmdLine).equals("ERROR");
    nbTests++;
    nbTestsPassed++;

    message = "look fichier1_a.txt test test";
    cmdLine = message.split(" ");
    assert pair.look(cmdLine).equals("ERROR");
    nbTests++;
    nbTestsPassed++;

    message = "look all";
    cmdLine = message.split(" ");
    assert pair.look(cmdLine).equals("look [ all ]");
    nbTests++;
    nbTestsPassed++;

    message = "look yluck";
    cmdLine = message.split(" ");
    assert pair.look(cmdLine).equals("look [ filename=\"yluck\" ]");
    nbTests++;
    nbTestsPassed++;

    message = "look fichier1_a.txt 12";
    cmdLine = message.split(" ");
    assert pair.look(cmdLine).equals("look [ filename=\"fichier1_a.txt\" filesize>\"12\" ]");
    nbTests++;
    nbTestsPassed++;

    message = "look fichier1_a.txt > 12";
    cmdLine = message.split(" ");
    assert pair.look(cmdLine).equals("look [ filename=\"fichier1_a.txt\" filesize>\"12\" ]");
    nbTests++;
    nbTestsPassed++;

    message = "look fichier1_a.txt < 12";
    cmdLine = message.split(" ");
    assert pair.look(cmdLine).equals("look [ filename=\"fichier1_a.txt\" filesize<\"12\" ]");
    nbTests++;
    nbTestsPassed++;

    message = "look fichier1_a.txt 12 14";
    cmdLine = message.split(" ");
    assert pair.look(cmdLine).equals("look [ filename=\"fichier1_a.txt\" \"12\"<filesize<\"14\" ]");
    nbTests++;
    nbTestsPassed++;

    System.out.println("TestLook            : " + nbTestsPassed + "/" + nbTests
                        + ((nbTestsPassed == nbTests) ? " --> " + GREEN + "OK" + RESET
                                                      : " --> " + RED + "NOK" + RESET));
  }

  public static void testStockLookList() {
    int nbTests = 0;
    int nbTestsPassed = 0;

    PairAbstrait pair = new Pair1();
    ArrayList<Fichier> fichiers = pair.getFichiers();
    int len = fichiers.size();
    String message;
    String cmdLine[];

    message = "list [ ]";
    cmdLine = message.split(" ");
    pair.stockLookList(cmdLine);
    assert pair.getFichiers().equals(fichiers);
    nbTests++;
    nbTestsPassed++;

    message = "list [ fichier2_b.txt 33708 1024 49df6efb6fb60249d7e5cff823483d53 ]";
    cmdLine = message.split(" ");
    pair.stockLookList(cmdLine);
    assert pair.getFichiers().size() == len + 1;
    nbTests++;
    nbTestsPassed++;

    message = "list [ fichier2_b.txt 33708 1024 49df6efb6fb60249d7e5cff823483d53 ]";
    cmdLine = message.split(" ");
    fichiers = pair.getFichiers();
    pair.stockLookList(cmdLine);
    assert pair.getFichiers().equals(fichiers);
    nbTests++;
    nbTestsPassed++;

    System.out.println("TestStockLookList   : " + nbTestsPassed + "/" + nbTests
                        + ((nbTestsPassed == nbTests) ? "   --> " + GREEN + "OK" + RESET
                                                      : "   --> " + RED + "NOK" + RESET));
  }

  public static void testGetfile(){
    int nbTests = 0;
    int nbTestsPassed = 0;

    PairAbstrait pair = new Pair1();
    String message;
    String cmdLine[];

    message = "getfile";
    cmdLine = message.split(" ");
    assert pair.getfile(cmdLine).equals("ERROR1");
    nbTests++;
    nbTestsPassed++;

    message = "getfile key key";
    cmdLine = message.split(" ");
    assert pair.getfile(cmdLine).equals("ERROR1");
    nbTests++;
    nbTestsPassed++;

    message = "getfile key";
    cmdLine = message.split(" ");
    assert pair.getfile(cmdLine).equals("ERROR2");
    nbTests++;
    nbTestsPassed++;

    String key = pair.getFichiers().get(0).getKey();
    message = "getfile " + key + " ";
    cmdLine = message.split(" ");
    assert pair.getfile(cmdLine).equals(message);
    nbTests++;
    nbTestsPassed++;

    System.out.println("TestGetfile         : " + nbTestsPassed + "/" + nbTests
                        + ((nbTestsPassed == nbTests) ? "   --> " + GREEN + "OK" + RESET
                                                      : "   --> " + RED + "NOK" + RESET));
  }

  public static void testStockPeers() {
    int nbTests = 0;
    int nbTestsPassed = 0;
    //TO DO
  }

  public static void testGetBitStr() {
    int nbTests = 0;
    int nbTestsPassed = 0;

    PairAbstrait pair = new Pair1();
    String bm;

    bm = "00000000";
    for(int i = 0; i < bm.length(); i++){
      assert pair.getBitStr(bm, i) == '0';
    }
    nbTests++;
    nbTestsPassed++;

    bm = "11111111";
    for(int i = 0; i < bm.length(); i++){
      assert pair.getBitStr(bm, i) == '1';
    }
    nbTests++;
    nbTestsPassed++;

    bm = "11101111";
    assert pair.getBitStr(bm, 4) == '0';
    nbTests++;
    nbTestsPassed++;

    bm = "01101011";
    assert pair.getBitStr(bm, 1) == '1';
    nbTests++;
    nbTestsPassed++;

    System.out.println("TestGetBitStr       : " + nbTestsPassed + "/" + nbTests
                        + ((nbTestsPassed == nbTests) ? "   --> " + GREEN + "OK" + RESET
                                                      : "   --> " + RED + "NOK" + RESET));
  }

  public static void testCountBits() {
    int nbTests = 0;
    int nbTestsPassed = 0;

    PairAbstrait pair = new Pair1();
    String bm;

    bm = "00000000";
    assert pair.countBits(bm) == 0;
    nbTests++;
    nbTestsPassed++;

    bm = "11111111";
    assert pair.countBits(bm) == 8;
    nbTests++;
    nbTestsPassed++;

    bm = "11101101";
    assert pair.countBits(bm) == 6;
    nbTests++;
    nbTestsPassed++;

    bm = "00000011";
    assert pair.countBits(bm) == 2;
    nbTests++;
    nbTestsPassed++;

    System.out.println("TestCountBits       : " + nbTestsPassed + "/" + nbTests
                        + ((nbTestsPassed == nbTests) ? "   --> " + GREEN + "OK" + RESET
                                                      : "   --> " + RED + "NOK" + RESET));
  }

  public static void testCmdGetpieces() {
    int nbTests = 0;
    int nbTestsPassed = 0;

    PairAbstrait pair = new Pair1();
    String message;
    String cmdLine[];
    String res;
    String resExpect;
    pair.stockLookList("list [ fichier2_b.txt 33708 1024 49df6efb6fb60249d7e5cff823483d53 ]".split(" "));

    message = "have 49df6efb6fb60249d7e5cff823483d53 000000000000000000000000000000000";
    cmdLine = message.split(" ");
    res = pair.cmdGetpieces(cmdLine, 0);
    resExpect = "getpieces 49df6efb6fb60249d7e5cff823483d53 [ ]";
    assert res.equals(resExpect);
    nbTests++;
    nbTestsPassed++;

    message = "have 49df6efb6fb60249d7e5cff823483d53 111111111111111111111111111111111";
    cmdLine = message.split(" ");
    res = pair.cmdGetpieces(cmdLine, 0);
    resExpect = "getpieces 49df6efb6fb60249d7e5cff823483d53 [ 0 1 2 3 ]";
    assert res.equals(resExpect);
    nbTests++;
    nbTestsPassed++;

    message = "have 49df6efb6fb60249d7e5cff823483d53 111111111111111111111111111111111";
    cmdLine = message.split(" ");
    res = pair.cmdGetpieces(cmdLine, 1);
    resExpect = "getpieces 49df6efb6fb60249d7e5cff823483d53 [ 4 5 6 7 ]";
    assert res.equals(resExpect);
    nbTests++;
    nbTestsPassed++;

    message = "have 49df6efb6fb60249d7e5cff823483d53 111111111111111111111111111111111";
    cmdLine = message.split(" ");
    res = pair.cmdGetpieces(cmdLine, 2);
    resExpect = "getpieces 49df6efb6fb60249d7e5cff823483d53 [ 8 9 10 11 ]";
    assert res.equals(resExpect);
    nbTests++;
    nbTestsPassed++;

    message = "have 49df6efb6fb60249d7e5cff823483d53 000000100000001100000000010000000";
    cmdLine = message.split(" ");
    res = pair.cmdGetpieces(cmdLine, 0);
    resExpect = "getpieces 49df6efb6fb60249d7e5cff823483d53 [ 6 14 15 25 ]";
    assert res.equals(resExpect);
    nbTests++;
    nbTestsPassed++;

    message = "have 49df6efb6fb60249d7e5cff823483d53 001000101000001100000000010000001";
    cmdLine = message.split(" ");
    res = pair.cmdGetpieces(cmdLine, 1);
    resExpect = "getpieces 49df6efb6fb60249d7e5cff823483d53 [ 15 25 32 ]";
    assert res.equals(resExpect);
    nbTests++;
    nbTestsPassed++;

    message = "have 49df6efb6fb60249d7e5cff823483d53 000000100000000000000000000000000";
    cmdLine = message.split(" ");
    res = pair.cmdGetpieces(cmdLine, 0);
    resExpect = "getpieces 49df6efb6fb60249d7e5cff823483d53 [ 6 ]";
    assert res.equals(resExpect);
    nbTests++;
    nbTestsPassed++;

    message = "have 49df6efb6fb60249d7e5cff823483d53 000000100000000000000000000000000";
    cmdLine = message.split(" ");
    res = pair.cmdGetpieces(cmdLine, 1);
    resExpect = "getpieces 49df6efb6fb60249d7e5cff823483d53 [ ]";
    assert res.equals(resExpect);
    nbTests++;
    nbTestsPassed++;

    pair.stockData("data 49df6efb6fb60249d7e5cff823483d53 [ 0:11111111 1:11111111 2:11111111 3:11111111 ]".split(" "));

    message = "have 49df6efb6fb60249d7e5cff823483d53 111100000000000000000000000000000";
    cmdLine = message.split(" ");
    res = pair.cmdGetpieces(cmdLine, 0);
    resExpect = "getpieces 49df6efb6fb60249d7e5cff823483d53 [ ]";
    assert res.equals(resExpect);
    nbTests++;
    nbTestsPassed++;

    message = "have 49df6efb6fb60249d7e5cff823483d53 111111100000000000000000000000000";
    cmdLine = message.split(" ");
    res = pair.cmdGetpieces(cmdLine, 0);
    resExpect = "getpieces 49df6efb6fb60249d7e5cff823483d53 [ 4 5 6 ]";
    assert res.equals(resExpect);
    nbTests++;
    nbTestsPassed++;

    System.out.println("TestCmdGetpieces    : " + nbTestsPassed + "/" + nbTests
                        + ((nbTestsPassed == nbTests) ? " --> " + GREEN + "OK" + RESET
                                                      : " --> " + RED + "NOK" + RESET));
  }

  public static void testSetBitBufferMap() {
    int nbTests = 0;
    int nbTestsPassed = 0;

    PairAbstrait pair = new Pair1();
    String bm;
    String res;
    String resExpect;

    bm = "00000000";
    res = pair.setBitBufferMap(bm, 0);
    resExpect = "10000000";
    assert res.equals(resExpect);
    nbTests++;
    nbTestsPassed++;

    bm = "10000000";
    res = pair.setBitBufferMap(bm, 0);
    resExpect = "10000000";
    assert res.equals(resExpect);
    nbTests++;
    nbTestsPassed++;

    bm = "11111110";
    res = pair.setBitBufferMap(bm, 7);
    resExpect = "11111111";
    assert res.equals(resExpect);
    nbTests++;
    nbTestsPassed++;

    System.out.println("TestSetBitBufferMap : " + nbTestsPassed + "/" + nbTests
                        + ((nbTestsPassed == nbTests) ? "   --> " + GREEN + "OK" + RESET
                                                      : "   --> " + RED + "NOK" + RESET));
  }


  public static void testCmdHave() {
    int nbTests = 0;
    int nbTestsPassed = 0;

    PairAbstrait pair = new Pair1();
    String key;
    String message;
    String cmdLine[];
    String res;
    String resExpect;
    pair.stockLookList("list [ fichier2_b.txt 33708 1024 49df6efb6fb60249d7e5cff823483d53 ]".split(" "));

    key = "e7350600059895985d7f52744f7c08e8";
    res = pair.cmdHave(key);
    resExpect = "have e7350600059895985d7f52744f7c08e8 111";
    assert res.equals(resExpect);
    nbTests++;
    nbTestsPassed++;

    key = "49df6efb6fb60249d7e5cff823483d53";
    res = pair.cmdHave(key);
    resExpect = "have 49df6efb6fb60249d7e5cff823483d53 000000000000000000000000000000000";
    assert res.equals(resExpect);
    nbTests++;
    nbTestsPassed++;

    key = "49df6efb6fb60249d7e5cff823483d53";
    message = "data " + key + " [ 0:11111111 1:11111111 2:11111111 3:11111111 ]";
    cmdLine = message.split(" ");
    pair.stockData(cmdLine);
    res = pair.cmdHave(key);
    resExpect = "have 49df6efb6fb60249d7e5cff823483d53 111100000000000000000000000000000";
    assert res.equals(resExpect);
    nbTests++;
    nbTestsPassed++;

    key = "49df6efb6fb60249d7e5cff823483d53";
    message = "data " + key + " [ 4:11111111 5:11111111 6:11111111 7:11111111 ]";
    cmdLine = message.split(" ");
    pair.stockData(cmdLine);
    res = pair.cmdHave(key);
    resExpect = "have 49df6efb6fb60249d7e5cff823483d53 111111110000000000000000000000000";
    assert res.equals(resExpect);
    nbTests++;
    nbTestsPassed++;

    key = "49df6efb6fb60249d7e5cff823483d53";
    message = "data " + key + " [ 32:11111111 ]";
    cmdLine = message.split(" ");
    pair.stockData(cmdLine);
    res = pair.cmdHave(key);
    resExpect = "have 49df6efb6fb60249d7e5cff823483d53 111111110000000000000000000000001";
    assert res.equals(resExpect);
    nbTests++;
    nbTestsPassed++;

    System.out.println("TestCmdHave         : " + nbTestsPassed + "/" + nbTests
                        + ((nbTestsPassed == nbTests) ? "   --> " + GREEN + "OK" + RESET
                                                      : "   --> " + RED + "NOK" + RESET));
  }

  public static void testPairAbstrait(){
    System.out.println(YELLOW + "\n>> Tests fonctionnels PairAbstrait :" + RESET);
    testNeedPieces();
    testAnnounce();
    testLook();
    testStockLookList();
    testGetfile();
    testGetBitStr();
    testCountBits();
    testCmdGetpieces();
    testSetBitBufferMap();
    testCmdHave();
  }

  public static void main(String args[]){
    System.out.println(PURPLE + "\n>>>> Tests fonctionnels côté pair : " + RESET);
    testPairAbstrait();
  }


}
