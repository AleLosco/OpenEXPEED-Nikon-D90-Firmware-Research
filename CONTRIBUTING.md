# Contribuire alla ricerca

Iniziare da [STATUS.md](STATUS.md) e consultare l’[indice della documentazione tecnica](docs/README.md) per trovare i risultati e le relative prove. È utile contribuire anche con una riproduzione indipendente, un caso che fallisce o una correzione di un’interpretazione.

## Campioni e risultati

Procurarsi separatamente i campioni descritti nel README. Non includere firmware, immagini decodificate, dump, HEX o database Ghidra nei contributi. Conservare gli originali fuori dal repository e aprirli soltanto in lettura; per i derivati locali usare `local/`.

Riprodurre anzitutto il report con il comando iniziale del README. Un digest diverso identifica un campione diverso: non aggirare il controllo per trattarlo come il riferimento esistente. Non aggiornare automaticamente `reports/reference.json` per far sparire una differenza; spiegare e verificare il cambiamento.

Per ogni nuovo risultato indicare campione e hash, componente, indirizzi CPU oppure offset file, comandi o script, ingressi sintetici e risultato atteso. Dichiarare i blocchi saltati, le chiamate non eseguite, i dispositivi simulati e i limiti della ricerca. Una sequenza di byte compatibile con un opcode non dimostra da sola che si tratti di codice.

## Ghidra

Le prove locali usano Ghidra 12.1.4, Temurin 21 e il linguaggio `fr60:BE:16:default` con compiler spec `fcc911`. Il linguaggio proviene da [ghidra-fr60](https://github.com/desrdev/ghidra-fr60). Il file [language-profile.sha256](ghidra/language-profile.sha256) identifica i sorgenti e le definizioni del profilo locale, escludendo il file `.sla` generato. Non sostituisce il processo d’installazione del modulo o una prova su un’altra versione.

Generare due file locali usando `ghidra/prepare.py`, con `--component A` e `--component B`. Importarli come programmi distinti con il loader Intel HEX. Per le verifiche recenti, aggiungere la cartella `ghidra/` allo Script Manager ed eseguire:

- Programma A: `D90AChannels.java`, `D90ACommands.java`, `D90AD4Receive.java`, `D90AD4Checksum.java`, `D90A84Protocol.java`.
- Programma B: D90OutputByte.java, D90BTransaction.java, D90BCallback34.java, D90BCallback34Effects.java, D90BStateGate.java, D90BMaskConsumers.java, D90BMessage0B.java, D90BHardwareSequence.java, D90AtamVector.java, D90AtamRequest.java, D90AtamTransfer.java, D90AtamWake.java, D90EventMode.java, D90AtamCommand.java, D90AtamCommandCallers.java, D90AtamStoreScan.java, D90AtamStatusStores.java, D90AtamR8Stores.java, D90AtamInterrupt.java e D90AtamCommandMask.java. Le prove più recenti richiedono anche gli HEX separati, generati in locale con ghidra/prepare.py.

Controllare sia i messaggi di successo sia l’assenza di eccezioni nel log; il solo codice d’uscita di Ghidra headless può non segnalare il fallimento di uno script. Queste sono prove di blocchi originali con memoria e ingressi preparati. Non eseguono l’intera fotocamera e non sostituiscono una convalida hardware.

La licenza MIT riguarda i contributi originali. Conservare attribuzioni e provenienza dei materiali esterni; gli strumenti e i firmware dei rispettivi autori non diventano MIT perché utilizzati nella ricerca.

Per verificare un’importazione nuova in modalità headless, creare una directory locale dedicata ai progetti temporanei ed eseguire `analyzeHeadless` dalla propria installazione di Ghidra. Esempio per A, dalla radice del repository:

```sh
mkdir -p local/ghidra-check
python3 ghidra/prepare.py /percorso/D90_0101.bin --component A > local/D90-A.hex
/percorso/ghidra/support/analyzeHeadless local/ghidra-check D90CheckA \
  -import local/D90-A.hex -processor fr60:BE:16:default -cspec fcc911 \
  -noanalysis -scriptPath "$PWD/ghidra" \
  -postScript D90AChannels.java -postScript D90ACommands.java \
  -postScript D90AD4Receive.java -postScript D90AD4Checksum.java \
  -postScript D90A84Protocol.java -deleteProject
```

Usare un nome di progetto temporaneo non esistente. `-deleteProject` elimina il progetto temporaneo al termine; non usarlo sul proprio database di ricerca. Per B generare l’HEX con `--component B`, scegliere un altro nome di progetto e sostituire gli script con `D90OutputByte.java`, `D90BTransaction.java` e `D90BCallback34.java`, `D90BCallback34Effects.java`, `D90BStateGate.java`, `D90BMaskConsumers.java`, `D90BMessage0B.java`, `D90BHardwareSequence.java`. Ghidra e il profilo FR60 devono essere già installati e Java configurato secondo la propria installazione.

## Prove locali successive alla rettifica AB-70

Sul programma B, gli script `D90StatusReset.java`, `D90StatusCallerPrefix.java`, `D90StatusSetup.java`, `D90SetupWait.java` e `D90SetupConfig.java` riproducono rispettivamente le prove AB-72–AB-76. L'ultimo si ferma prima del servizio OS a 0x19e064. Le modifiche sintetiche del contatore e dello stato periferico sono esplicitate nelle prove AB-75–AB-76; non rappresentano osservazioni hardware. Le scansioni e i listati statici associati sono collegati nelle rispettive sezioni di [communication-topology.md](docs/communication-topology.md). Per il programma locale esistente usare `-process nome-programma -readOnly -noanalysis`, con `-scriptPath` alla directory `ghidra/` e `-postScript` allo script scelto. Controllare sempre il log oltre al codice d'uscita.

La prova AB-78 si riproduce con `D90QueueUnlink.java` sul programma B. `D90QueueCore.java` stampa finestre statiche di supporto; non è una prova di esecuzione del servizio OS.

Per AB-79 eseguire `D90QueueLastNode.java` sul programma B: otto casi con nodo e sentinella sintetici, senza modellare effetti hardware del byte 0x44.

Per AB-80 eseguire `D90Service85Queue.java`: quattro scenari attraversano il servizio -85 dal dispatcher al ritorno locale a 0x406c6, con frame e liste sintetici. Il ripristino del task non è incluso.

Per AB-81 eseguire `D90OsReturn.java`: quattro casi ripristinano registri da un frame sintetico e si fermano prima di RETI. `D90SchedulerContext.java` stampa le finestre statiche di supporto.

Per AB-82 eseguire `D90InterruptFrameContext.java` e `D90InterruptFrame.java`: quattro casi collegano prefisso ed epilogo tramite il frame software generato dal firmware. Il test salta esplicitamente dispatcher e servizio; INT e RETI non sono eseguiti.

Per AB-83 eseguire `D90InterruptSupport.java` sul programma B. La sonda INT deve produrre il diagnostico atteso `_interrupt_prelude` non implementata, seguito da quattro casi RETI superati e dal riepilogo finale. Il log contiene intenzionalmente errore e stack trace della sonda: il solo codice d’uscita headless o una ricerca della parola ERROR non bastano a interpretarlo.

Per AB-84 eseguire `D90IlmModel.java`: caratterizza il modello installato con una STILM originale e tre RETI su frame sintetici. I risultati attesi documentano il limite a quattro bit; non attestano fedeltà hardware e dovranno essere rivalutati dopo un cambio del modello CPU.

Per AB-85, dalla radice del workspace che contiene `work/ghidra`, preparare e compilare il linguaggio isolato (la destinazione deve essere nuova):

```sh
python3 outputs/d90-re/tools/build_fr60_ilm.py work/ghidra/Ghidra/Processors/FR60/data/languages work/fr60-ilm5
JAVA_HOME="$PWD/work/jdk/Contents/Home" work/ghidra/support/sleigh work/fr60-ilm5/fr60.slaspec
```

Eseguire poi `D90IlmCorrection.java` sul programma B aperto in `-readOnly -noanalysis`, passando come argomento allo script il percorso assoluto di `work/fr60-ilm5/fr60.ldefs`. Deve riportare 64 sequenze per ciascun modello e 128 totali. Adattare i percorsi alla propria installazione; il builder verifica tutti e cinque gli input contro `ghidra/language-profile.sha256` prima di creare la destinazione, e registra `inputs.sha256` e `generated.sha256` nella copia isolata. Dopo la compilazione verificare anche `fr60.sla`: i digest della ricostruzione di riferimento sono in `ghidra/language-profile-ilm5.sha256` (Ghidra 12.1.4). Da quella directory di output, `shasum -a 256 -c /percorso/assoluto/d90-re/ghidra/language-profile-ilm5.sha256` verifica tutti e sette gli artefatti; un compilatore differente può richiedere una nuova verifica, non un aggiornamento automatico del riferimento. Il linguaggio resta sperimentale e separato: non sostituire i file dell'installazione Ghidra. Le API interne di caricamento sono verificate solo su Ghidra 12.1.4.

Per AB-86 eseguire `D90Int40Adapter.java` con lo stesso argomento assoluto `fr60.ldefs` di AB-85. Deve riportare 20 casi positivi e sei rifiuti attesi (incluso PC dispari). L’ingresso INT è sostituito esplicitamente dall’adattatore Java; RETI è eseguita dal linguaggio sperimentale. SSP resta separato da SP dopo il ritorno: questa limitazione è verificata, non corretta silenziosamente. Non è una prova del gestore firmware.

Per AB-87 eseguire `D90SupervisorStack.java` con l’argomento assoluto `fr60.ldefs` di AB-85 e `D90Int40Adapter.java` nello stesso scriptPath. Il riepilogo deve riportare 20 coppie di INT consecutivi, 40 sincronizzazioni SSP esplicite e tre rifiuti attesi. La sincronizzazione appartiene al banco di prova: non modifica il modello installato e non costituisce un alias SP/SSP generale.

Per AB-88 eseguire `D90FirmwareContext.java` sul programma B in sola lettura, con l’argomento assoluto `fr60.ldefs` di AB-85; mantenere `D90SupervisorStack.java` e `D90Int40Adapter.java` nello stesso scriptPath. Attesi 32 casi e 1480 istanze di passo controllate. Le finestre originali vengono copiate in un programma transitorio; INT e SSP sono adattati esplicitamente e il servizio viene saltato. Nessuna istruzione al PC di ritorno è eseguita.

Per AB-89 eseguire `D90Service85Integrated.java` sul programma B, con il linguaggio isolato e le dipendenze Java di AB-88. Attesi otto scenari integrati e 1024 istanze di passo controllate. Non viene saltato il servizio -85; restano espliciti l’adattatore INT e la sincronizzazione SSP dopo RETI. Il ritorno con periferiche in RAM non dimostra scadenza dell’attesa o scheduling reale.

Per AB-90 eseguire `D90QueueConsumers.java`, `D90QueueExpiryContext.java` e `D90QueueAdvance.java` sul programma B originale in sola lettura. L’ultimo deve riportare tre cicli di vita su sei invocazioni: 0→−1, 1→0→−1 e 2→1→0→−1. Il PC è reimpostato dal test a ogni invocazione; non è modellata una sorgente temporale. Non occorre il linguaggio sperimentale AB-85 per questi script.

Per AB-91 eseguire `D90QueueCallerContext.java` e `D90QueueMultiple.java` con `D90QueueAdvance.java` nello stesso scriptPath, sul programma B originale in sola lettura. Sono attesi sei scenari completi della routine 0x40a56. Le liste secondarie sono vuote; il contatore parte da 0x00001234:0xffffffff per verificare il riporto. Non viene eseguito l’involucro d’interrupt né modellata una cadenza temporale.

Per AB-92 eseguire `D90QueueVector.java` sul programma B e `D90QueueInterruptIntegrated.java` con il linguaggio isolato e le dipendenze di AB-89. Attesi otto scenari, tre ingressi sintetici del vettore 0x18 per scenario e 3032 istanze di passo controllate. Le liste passano dal servizio agli ingressi successivi senza reinizializzazione. Gli ingressi sono forzati dal test anche con I iniziale zero: non dimostrano accettazione hardware, frequenza o esecuzione del task trasferito.

Per AB-93 eseguire `D90QueueSourceScan.java`, `D90QueueSourceContext.java` e `D90QueueSourceControl.java` sul programma B originale in sola lettura, con `D90QueueAdvance.java` nello scriptPath. Attesi sei casi nell’ultimo script. Il prefisso d’avvio si ferma prima della chiamata a 0x100936; gli indirizzi periferici sono RAM sintetica e non attestano l’attivazione del vettore.

Per AB-94 eseguire `D90SourceHelperContext.java` e `D90SourceHelper.java` sul programma B originale in sola lettura, con `D90QueueAdvance.java` nello scriptPath. Attesi due casi da 463 passi fino a 0x10094e, prima di INT. I primi due byte 0x5a5a nel blocco R4 provengono dal riempimento sintetico dello stack; non sono una costante firmware né una prova autonoma di un bug.

Per AB-95 eseguire `D90Service83Context.java` e `D90Service83.java` sul programma B originale in sola lettura, con `D90QueueAdvance.java` nello scriptPath. Attese 16 combinazioni fino a 0x406c6: il blocco è sintetico e la prova non esegue INT o l’epilogo. Il valore d’errore R12=0xdf è osservato prima della successiva estensione con segno.

Per AB-96 eseguire `D90SourceServiceIntegrated.java` sul programma B in sola lettura, con l’argomento assoluto `fr60.ldefs` di AB-85 e gli script AB-86/87 nello scriptPath. Attesi otto casi fino a 0x100950 e 4208 istanze di passo controllate. Il banco di prova sincronizza esplicitamente SSP prima di INT e dopo RETI; i byte del blocco sono quelli costruiti dal prefisso. Le chiamate successive d’inizializzazione non sono eseguite.

Per AB-97 eseguire `D90SourceTailContext.java` e `D90SourceInitializationReturn.java`; il secondo richiede il linguaggio isolato e le dipendenze di AB-96. Attesi otto ritorni a 0x40740 e 5272 istanze di passo controllate. Le periferiche sono RAM; il ritorno riguarda una chiamata d’inizializzazione, non l’avvio completo. Al confine finale SP=0x81004000 ma SSP resta 0x81003fec nel modello, come esplicitamente verificato.


Per AB-98 eseguire `D90InitializationVectorBoundary.java` sul programma B con `-readOnly -noanalysis`, passando il percorso assoluto del linguaggio isolato `fr60.ldefs` di AB-85. Nello stesso scriptPath devono essere presenti `D90SourceInitializationReturn.java`, `D90SupervisorStack.java` e `D90Int40Adapter.java`. Attesi otto casi, 5336 passi e il riepilogo AB-98. Il test eredita le verifiche AB-97 e si ferma prima della chiamata a 0x4049e; il frame d'ingresso del vettore 0x18 è sintetico. Non è un test di accettazione hardware dell'interrupt.


Per AB-99 usare `D90InitializationQueuePreconditions.java` con le opzioni e il linguaggio di AB-98, lasciando tutti gli script della catena AB-97/98 e AB-86/87 nello scriptPath. Attesi otto casi, 5336 passi e riepilogo AB-99. I pattern delle strutture OS sono iniettati prima dell'inizializzazione e confrontati al ritorno e al confine CALL; la routine della coda non viene eseguita. Consultare il log anche quando il processo termina con codice zero.


Per AB-100 eseguire `D90ListInitializationCandidates.java` e `D90ListInitialization.java` sul programma B con `-readOnly -noanalysis`. Il secondo richiede `D90QueueAdvance.java` nello scriptPath e non richiede argomenti o linguaggio sperimentale. Attesi dato originale 0x5c400=32 e due casi superati: il ciclo costruisce 33 sentinelle. La scansione produce candidati grezzi, non una copertura completa dei riferimenti. I due ingressi delle routine nella prova sono imposti dal banco.


Per AB-101 eseguire `D90OsInitializationContext.java` e `D90OsRecordConstructor.java` sul programma B con `-readOnly -noanalysis`, senza argomenti. Il secondo richiede `D90QueueAdvance.java` nello scriptPath; attesi 78 casi (39 descrittori originali per due pattern). Non esegue il chiamante 0x408be o i rami di inserimento. Non richiede il linguaggio ILM sperimentale.


Per AB-102 eseguire `D90OsInitializationRecords.java` sul programma B con `-readOnly -noanalysis`, senza argomenti e con `D90QueueAdvance.java` nello scriptPath. Attesi due scenari, ciascuno con 39 costruzioni, due inserimenti, 38 blocchi ausiliari e bitmap 0x80000000. Il test usa i descrittori originali e le routine dei costruttori, ma imposta RAM, bitmap iniziale e transizioni fra i tre ingressi; non richiede il linguaggio ILM sperimentale.


Per AB-103 eseguire `D90OsInitializationRemaining.java` sul programma B con `-readOnly -noanalysis`, senza argomenti. Richiede `D90OsInitializationRecords.java` e `D90QueueAdvance.java` nello scriptPath. Attesi due scenari e 14 ritorni dalle sette routine aggiuntive; i confronti coprono la regione OS di 4096 byte, i contesti preparati e il byte 0x44. Le transizioni PC/RP fra routine restano sintetiche; non è ancora un'esecuzione continua del blocco d'avvio.


Per AB-104 eseguire `D90OsStartupContinuous.java` sul programma B con `-readOnly -noanalysis`, senza argomenti e con `D90QueueAdvance.java` nello scriptPath. Attesi due scenari da 12125 passi e riepilogo AB-104. Il percorso continuo si ferma prima di 0x40738; i registri e la RAM iniziali sono sintetici, ma non si effettuano scritture del banco durante i passi. Non richiede il modello ILM sperimentale.


Per AB-105 eseguire `D90OsPeripheralInitialization.java` sul programma B con `-readOnly -noanalysis`, passando il percorso assoluto di `fr60.ldefs` isolato di AB-85. Tenere `D90SourceInitializationReturn.java`, `D90SupervisorStack.java` e `D90Int40Adapter.java` nello scriptPath. Attesi otto scenari, 102272 istanze di passo controllate e riepilogo AB-105. Il prefisso OS e la chiamata periferica condividono memoria; INT e SSP restano adattamenti espliciti. Consultare il log oltre al codice di uscita. Non è un avvio completo né un test hardware.


Per AB-106 eseguire `D90OsInitializedInterrupt.java` con le opzioni e il linguaggio isolato di AB-105, mantenendo nello scriptPath tutta la catena AB-105/97/87/86. Attesi otto scenari e 102672 istanze di passo controllate. Il vettore 0x18 è forzato sullo stato costruito dal firmware, senza ricostruire le liste; la prova termina al ritorno a 0x40740.


Per AB-107 eseguire `D90StartupDispatchContext.java` sul programma B in sola lettura, poi `D90StartupDispatchBoundary.java` con opzioni, linguaggio isolato e dipendenze AB-106/105/97/87/86. Attesi otto scenari, 102744 passi e arresto prima di 0x407ce. USP è inizializzato sinteticamente prima dell'esecuzione; il cambio di stack utente non viene eseguito.


Per AB-108 eseguire `D90UserStackModel.java` sul programma B con `-readOnly -noanalysis`, passando il percorso assoluto `fr60.ldefs` di AB-85 e mantenendo `D90SupervisorStack.java`/`D90Int40Adapter.java` nello scriptPath. Attesi otto casi sui due linguaggi (40 istruzioni totali). Il successo caratterizza la separazione SP/USP/SSP osservata nel modello; non certifica la fedeltà hardware.


Per AB-109 eseguire `D90StackSwitchAdapter.java` sul programma B con le opzioni e dipendenze AB-108, passando il percorso assoluto del linguaggio ILM isolato. Attesi otto sequenze adattate, otto prove dei siti e 48 rifiuti di precondizione, sui due profili. Il test è isolato e non esegue RETI verso un contesto utente.


Per AB-110 eseguire `D90StartupContextRestore.java` sul programma B con `-readOnly -noanalysis` e il percorso assoluto `fr60.ldefs` di AB-85. Richiede la catena AB-107/106/105/97/87/86 e `D90StackSwitchAdapter.java` nello scriptPath. Attesi otto scenari, frame PC=0x407ee/PS=0x001f0030 e arresto a 0x407ea prima di RETI. Il contatore di 102816 passi ordinari esclude MOV USP e i quattro cambi CCR adattati per scenario.


Per AB-111 eseguire `D90UserRetiModel.java` sul programma B con `-readOnly -noanalysis`, argomento assoluto `fr60.ldefs` isolato e dipendenze AB-87/86 nello scriptPath. Attesi 16 casi sui due profili; ciascuno esegue soltanto RETI originale a 0x407ea con frame sintetico. La destinazione non viene eseguita.


Per AB-112 eseguire `D90UserRetiAdapter.java` con `-readOnly -noanalysis`, percorso assoluto del linguaggio ILM isolato e dipendenze AB-87/86 nello scriptPath. Attesi otto ritorni validi e undici rifiuti senza mutazioni dei registri/memoria confrontati. Il test usa frame sintetici e non esegue la destinazione del ritorno.


Per AB-113 eseguire `D90StartupUserLoop.java` sul programma B con `-readOnly -noanalysis`, percorso assoluto del linguaggio ILM AB-85 e tutta la catena di script AB-110, più `D90UserRetiAdapter.java`, nello scriptPath. Attesi otto ingressi nel contesto utente, 128 passi BRA e 102944 passi ordinari controllati. Non è un test di reset completo, temporizzazione hardware o passaggio a un altro task.


Per AB-114 eseguire `D90DispatchSelectionContext.java` sul programma B con `-readOnly -noanalysis` senza argomenti; eseguire poi `D90DispatchReadyState.java` con linguaggio e dipendenze AB-113. Attese voce vettore 0x3f→0x40d44 e otto ispezioni dello stato. Il nuovo gestore e SRCH1 non vengono eseguiti; la prova conferma soltanto i dati disponibili al confine finale AB-113.


Per AB-115 creare una directory isolata fuori dall'installazione Ghidra e copiare, dopo verifica dei digest contro `ghidra/language-profile.sha256`, `fr60.ldefs`, `fr60.sinc`, `fr60.pspec`, `fr60.cspec`, `fr60.dwarf`, `fr81.slaspec`, `fr81ext.sinc`. Compilare con `support/sleigh /percorso/isolato/fr81.slaspec`, usando JAVA_HOME e JAVA_TOOL_OPTIONS con `-Duser.home` nel workspace come per headless. Il manifesto `ghidra/language-profile-fr81-probe.sha256` identifica gli artefatti della prova. Eseguire `D90Srch1Probe.java` sul programma B `-readOnly -noanalysis`, passando il percorso assoluto del `fr60.ldefs` isolato e mantenendo `D90Int40Adapter.java` nello scriptPath. Nel presente ambiente sono attesi due casi zero superati e poi il riepilogo **INCONCLUSIVE** per userop `clz` non implementata: non interpretare exit code zero come successo della griglia. Il compilatore segnala sei costruttori NOP; vedere AB-115 per i limiti.


Per AB-116 eseguire `D90Srch1Adapter.java` sul programma B con `-readOnly -noanalysis`, percorso assoluto `fr60.ldefs` del profilo ILM AB-85 e `D90Int40Adapter.java` nello scriptPath. Attesi 76 casi validi e quattro rifiuti. È una verifica Java dell'adattatore sullo stato emulativo, senza `c.step()` e senza esecuzione nativa di SRCH1. Non usare il `fr60.ldefs` della copia FR81 AB-115.


Per AB-117 eseguire `D90DispatchCandidate.java` sul programma B con `-readOnly -noanalysis`, percorso assoluto del linguaggio ILM AB-85, dipendenze AB-114 e `D90Srch1Adapter.java` nello scriptPath. Attesi otto casi e 103096 passi ordinari controllati, con arresto a 0x40e62. PC=0x40e2a è imposto dal banco dopo AB-114: il prefisso d'interrupt è saltato e il suo frame non è presente. Non usare questo test come prova del cambio completo di contesto.

Per AB-118 eseguire `D90DispatchPrefix.java` con gli stessi argomenti e lo stesso programma B in sola lettura di AB-117. Il riepilogo atteso è “eight synthetic vector prefixes reached bitmap entry with checked stacks”.

Per AB-119 eseguire `D90DispatchSave.java` con programma B in sola lettura e argomenti di AB-118, includendo `D90Srch1Adapter.java` nello scriptPath. Cercare nel log “eight outgoing context saves passed at 0x40e7a”.

Per AB-120 eseguire `D90DispatchLoad.java` con programma B, opzioni e profilo ILM di AB-119. Verificare nel log “eight candidate stack loads passed at 0x40e90”.

Per AB-121 eseguire `D90DispatchFrame.java` con programma B, opzioni e profilo ILM di AB-120. Verificare nel log “eight supervisor frames passed at 0x40ea6”.

Per AB-122 eseguire `D90DispatchRegisters.java` con programma B, opzioni e profilo ILM di AB-121. Il banco aggiunge sette parole sintetiche allo stack candidato. Verificare nel log “eight register restores and current-control stores passed at 0x40eb4”.

Per AB-123 eseguire `D90DispatchReturn.java` con programma B, opzioni e profilo ILM di AB-122. Controllare il riepilogo “eight dispatcher returns reached 0x1000ac before candidate execution”. Le tre parole aggiunte a 0x8f991248 sono input sintetici.

Per AB-124 eseguire `D90CandidateEntry.java` sul programma B con `-readOnly -noanalysis`, senza argomenti. Il log elenca descrittore, blocco, 32 byte e 12 istruzioni. La verifica è statica sul database.

Per AB-125 eseguire `D90CandidateSource.java /percorso/D90_0101.bin` sul programma B con `-readOnly -noanalysis`. Lo script verifica il digest prima del confronto integrale dei due intervalli programmati B. Atteso “sample digest and both complete programmed B spans verified”. Il file originale viene letto tramite Files.readAllBytes.

Per AB-126 eseguire `D90CandidatePrologue.java` con programma B e opzioni di AB-123, passando il percorso assoluto del profilo ILM AB-85. Verificare il riepilogo “eight candidate prologues reached the first CALL at 0x1000d0”.

Per AB-127 eseguire `D90CandidateEndmMap.java` con programma B e argomenti di AB-126. Controllare gli intervalli nel log e il riepilogo “eight pre-call map inspections passed”.

Per AB-128 eseguire `D90CandidateEndm.java` con programma B, opzioni e profilo ILM di AB-126. Il banco aggiunge codice/dati ENDM e sorgenti sintetiche. Atteso “24 ENDM copies and published pointers passed across three source choices”. Il limite locale è 200000 passi per tratto delimitato.

Per AB-129 eseguire `D90SecondCallContext.java` sul programma B con `-readOnly -noanalysis`, senza argomenti. Il listato deve fermarsi alla RET a 0x185278, con riepilogo “bounded static listing reached return”.

Per AB-130 eseguire `D90CandidateSystemFix.java` con programma B e argomenti di AB-128. Atteso “16 SYSTEMFIXADJUST copies and ENDM preservation checks passed”. Sono ereditate anche le 24 copie ENDM di AB-128.

Per AB-131 eseguire `D90ThirdCallContext.java` sul programma B con `-readOnly -noanalysis`, senza argomenti. Il listato, limitato a 320 istruzioni, deve raggiungere la RET a 0x13a3aa e il riepilogo “bounded static listing reached return”.

Per AB-132 eseguire `D90ThirdCallHelper.java` sul programma B con `-readOnly -noanalysis`. Confrontare le 26 righe con i record big-endian IHH del campione AB-1 a offset 0x20aabe. Il listato termina con RET:D a 0x13a466 e il ripristino di R10 nel delay slot.

Per AB-133 eseguire `D90ThirdCallRecords.java` con programma B e argomenti di AB-130. Atteso “32 parser cases passed for both selectors with and without publication”. Le invocazioni dell'helper e i record delle due sorgenti sono sintetici.

Per AB-134 eseguire `D90ThirdCallCopies.java` con programma B e argomenti di AB-130. Atteso “eight original double-parser and copy loops passed at 0x13a278”.

Per AB-135 eseguire `D90ThirdCallFlags.java` con programma B e argomenti di AB-134. Atteso “32 flag-controlled branches passed before helper execution”. La prova impone i due flag dopo le copie e si arresta prima dell'esecuzione dell'helper.

Per AB-136 eseguire `D90Type2HelperContext.java` senza argomenti per il listato, poi `D90Type2Defaults.java` con programma B e argomenti di AB-134. Atteso “16 type-2 branch cases and 32 helper calls passed”.

Per AB-137 eseguire `D90Type3HelperContext.java` senza argomenti, poi `D90Type3Defaults.java` con programma B e argomenti di AB-136. Atteso “16 type-3 cases and 32 helper calls passed”.

Per AB-138 eseguire `D90MissingBlockClear.java` con programma B e argomenti di AB-137. Atteso “64 flag-9/10/11 combinations passed at 0x13a336”. I flag e i dati azzerati sono predisposti dal banco.

Per AB-139 eseguire `D90MissingArrayClear.java` con programma B e argomenti di AB-138. Atteso “104 nine-record clear cases passed at 0x13a364”. Le tredici maschere sono elencate nello script.

Per AB-140 eseguire `D90Type23Context.java` senza argomenti e `D90Type23Copy.java` con programma B e argomenti di AB-139. Atteso “16 flag-23 initializations and full 624-byte copies passed”.

Per AB-141 eseguire `D90Type24Context.java` senza argomenti e `D90Type24Return.java` con programma B e argomenti di AB-140. Atteso “16 flag-24 branches and third-call returns passed at 0x1000e2”. Il banco ripristina SP e R9 prima di ogni variante.

Per AB-142 eseguire `D90ThirdCallIntegrated.java` sul programma B con opzioni e profilo ILM di AB-130. Verificare il riepilogo “eight continuous third calls passed with 26 checked blocks and mirror” e l'assenza di avvisi di memoria non inizializzata.

Per AB-143 eseguire `D90FourthCallContext.java` e `D90FourthParserContext.java` sul programma B con `-readOnly -noanalysis`, senza argomenti. I listati raggiungono rispettivamente RET a 0x13b892 e RET:D a 0x13b93e.

Per AB-144 eseguire `D90FourthCallIntegrated.java` con programma B e argomenti di AB-142. Atteso “eight complete fourth calls passed at 0x1000ea”.

Per AB-145 eseguire `D90FifthCallContext.java`, `D90FifthInitContext.java` e `D90FifthListContext.java` sul programma B con `-readOnly -noanalysis`, senza argomenti. I listati sono statici e non eseguono INT.

Per AB-146 eseguire `D90FifthLists.java` con programma B e argomenti di AB-144. Atteso “eight fifth-call list preparations passed before INT 0x40”. Lo stop è prima dell'INT a 0x112014.

Per AB-147 eseguire `D90FifthServiceEntry.java` con programma B e argomenti di AB-146. Atteso “eight modeled user INT entries dispatched to 0x414da”. Il frame INT è predisposto esplicitamente sullo stack supervisore.

Per AB-148 eseguire `D90Service23Context.java` sul programma B esistente con `-readOnly -noanalysis` e lo scriptPath `ghidra/`. Il log atteso contiene l'analisi limitata di 0x414da, i blocchi 0x41534/0x415a2/0x415aa/0x41852 e il record 28 della tabella a 0x5c40c. Lo script legge il database locale senza modificarlo e non esegue il servizio. Consultare AB-148 per accessi, ipotesi e limiti.

Per AB-149 eseguire `D90Service23Return.java` con programma B, opzioni e argomenti di AB-147. Atteso “four eligible and four ineligible service returns verified”.

Per AB-150 eseguire `D90Service23UserReturn.java` con programma B, opzioni e argomenti di AB-149. Atteso “eight user returns at 0x112016 with results 0 or -63”.

Per AB-151 eseguire `D90FifthCallReturn.java` con programma B, opzioni e argomenti di AB-150. Atteso “eight fifth-call returns at 0x1000f2 preserve 0 or -63”, seguito dalle due istruzioni statiche LDI/CALL.

Per AB-152 eseguire `D90SixthCallContext.java` sul programma B con `-readOnly -noanalysis`, poi `D90SixthServiceRequest.java` con le opzioni e il profilo di AB-151. Atteso “eight sixth calls reached INT with service -23 and arguments 8/0”.

Per AB-153 eseguire `D90SixthServiceReturn.java` sul programma B con opzioni e profilo di AB-152. Atteso “eight second service returns inserted record 8 in list 4”.

Per AB-154 eseguire `D90SixthCallReturn.java` con programma B e argomenti di AB-153. Atteso “eight sixth-call returns reached 0x1000fa with result zero”.

Per AB-155 eseguire `D90PreparedTaskEntries.java` sul programma B in sola lettura con -noanalysis e senza argomenti, poi `D90SeventhCallEntry.java` con le opzioni e il profilo di AB-154. Attesi tre prefissi e “eight entries at 0x19d89c retain current control at head of list 1”.

Per AB-156 usare `D90CandidateSchedulingSites.java` sul programma B con -readOnly -noanalysis, poi `D90CurrentListChange.java` con opzioni e profilo AB-155. Atteso “eight isolated service -27 returns moved current control from list 1 to 7”.

Per AB-157 eseguire `D90Record8Dispatch.java` con programma B e argomenti di AB-156. Atteso “eight selections restored record 8 and reached its first call”.

Per AB-158 eseguire `D90Record8FirstCallee.java` sul programma B in sola lettura senza argomenti, poi `D90Record8TableInit.java` con le opzioni e il profilo di AB-157. Atteso “eight table initializations reached service -48 with 71 indexed rows”.

Per AB-159 usare `D90Record8Service48.java` oppure `D90Record8HardwareBranch.java` con opzioni e profilo AB-158. Il secondo comprende la prova del servizio e deve riportare “16 controlled peripheral branches passed after the original service return”.

Per AB-160 eseguire `D90Record8Modes.java` sul programma B con opzioni e profilo AB-159. Atteso “16 mode preparations and service -47 returns verified”.
