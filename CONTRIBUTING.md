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
