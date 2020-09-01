#Procédure de lancement

## __Commandes disponibles__

### __Commandes make__

* **make**: compile le serveur et les pairs et place les exécutables dans le dossier *./out*
* **make pair**: compile les pairs
* **make tracker**: compile le tracker
* **make test-tracker**: compile les tests du tracker
* **make clean**: supprime tout le contenu de *./out*
* **make cleanfiles**: supprime tous les fichiers téléchargés par les pairs

### __Commandes dans le shell pair__

* **announce** : `announce`
* **look**     : `look filename $criterion1 $criterion2 ...`
* **getfile**  : `getfile $key`
* **update**   : `update`
* **open**     : `open`
* **stats**    : `stats [download | upload]`
* **exit**     : `exit`

## __Etapes:__

1. Compiler les fichiers avec `make` à la racine
2. Ouvrir un terminal et lancer `./tracker` depuis *./out/*
3. Ouvrir un autre terminal et lancer un pair : `java PairMain -n $noPair [-s] [-v | -vf]` depuis *./out/*  
**Options :**  
  * `-n $noPair` : indique le numéro du pair à lancer (obligatoire)
  * `-s` : active le mode statistiques
  * `-v` : active le mode verbeux allégé
  * `-vf` : active le mode verbeux complet (affichage du codage binaire des pièces)
4. Saisir une commande disponible dans le shell pair
