CC = gcc
CFLAGS= -Wall -Wextra -g -std=c99
DIRS=./src/server
DIRC1=./src/pairs/pair1
DIRC2=./src/pairs/pair2
DIRC3=./src/pairs/pair3
DIRC=./src/pairs
DIRT=./tests
OUT=./out
DIRARG=-I$(DIRC) -I$(DIRS)
OBJECT=$(DIRC)/PairProcess.java \
										$(DIRC)/PairConnect.java \
										$(DIRC)/PairServer.java \
										$(DIRC)/PairMain.java \
										$(DIRC)/Fichier.java \
										$(DIRC)/PairAbstrait.java \
										$(DIRC)/PairStats.java \
										$(DIRC)/DownloadStats.java \
										$(DIRC)/UploadStats.java \
										$(DIRC1)/Pair1.java \
										$(DIRC2)/Pair2.java \
										$(DIRC3)/Pair3.java \
										

all:
	@$(MAKE) -s tracker
	@$(MAKE) -s pair

pair: $(OBJECT)
	@javac -cp $(DIRC) -Xlint -d ./out/ $^
		@echo 'Compilation pairs  [OK]'

test: test-tracker test-pair

test-pair: $(OBJECT)
	@javac -Xlint -d ./out/ $^ $(DIRT)/PairTest.java
	@echo 'Compilation tests  [OK]'
	@cd $(OUT) && java -ea PairTest

$(OUT)/thpool.o: $(DIRS)/thpool.c
			$(CC) $(CFLAGS) $(DIRARG) -c $< -o $@ -lpthread

$(OUT)/tracker_cmd.o: $(DIRS)/tracker_cmd.c
		  $(CC) $(CFLAGS) $(DIRARG) -c $< -o $@ -lpthread

tracker: $(OUT)/thpool.o $(OUT)/tracker_cmd.o $(DIRS)/tracker.c
	@$(CC) $(CFLAGS) $(DIRARG) $(DIRS)/tracker.c -lpthread -o ./out/tracker $< $(OUT)/tracker_cmd.o
	@echo 'Compilation tracker [OK]'

test-tracker: tracker
	@$(CC) $(CFLAGS) $(DIRARG) $(DIRT)/test_tracker.c -lpthread -o ./out/test_tracker $(OUT)/tracker_cmd.o
	@echo 'Compilation test-tracker  [OK]'
	@$(OUT)/test_tracker

exec-serv: all
	./out/tracker 8080

cleanall: clean cleanfiles

clean:
	$(RM) out/* *[#~]

cleanfiles:
	$(RM) $(DIRC1)/fichiers/*2* $(DIRC1)/fichiers/*3* $(DIRC1)/fichiers/test.txt
	$(RM) $(DIRC2)/fichiers/*1* $(DIRC2)/fichiers/*3* $(DIRC2)/fichiers/bigFile.txt
	$(RM) $(DIRC3)/fichiers/*1* $(DIRC3)/fichiers/*2* $(DIRC3)/fichiers/bigFile.txt

.PHONY: all clean
