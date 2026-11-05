# Stato del progetto

Aggiornato il 5 novembre 2026. Questo file riassume il risultato attuale; il README presenta il progetto, mentre i documenti tecnici conservano evidenze, ipotesi e revisioni delle interpretazioni.

Il progetto è un laboratorio di ricerca con analizzatore riproducibile e prove emulative di singole routine. Non contiene una patch funzionale validata sulla fotocamera né un emulatore completo della D90.

| Area | Risultato verificato | Cosa manca |
|---|---|---|
| Campioni originali | Hash, dimensioni, struttura A/B e controlli di integrità | Estensione ad altre versioni; i controlli sono legati a questi campioni |
| Dati L | Decodifica di 307 record da 128 byte e controlli del contenitore | Significato dei campi ottici e del suffisso; non è dimostrata esecuzione di codice in L |
| CPU e memoria | Mappe di lavoro A/B, vettori, codice FR e importazione Ghidra | Identificazione completa dei chip e delle periferiche; gestione dell’estensione SRCH1 non coperta dal profilo FR60 |
| Avvio e aggiornamento | Percorsi di avvio B, controlli dell’aggiornamento, routine flash e prove isolate dei rami di errore | Avvio completo e comportamento temporale sull’hardware |
| Parametri e comunicazioni | Blocco ENDM, parametro +0x150 e scambio B del descrittore 0x46; diversi trasporti A verificati | Destinatario fisico di B, collegamento fra i componenti, significato fotografico del parametro |
| Emulazione | RAM e periferiche sintetiche sufficienti per routine e scambi delimitati | CPU/periferiche nel loro insieme, interrupt e temporizzazione realistici, sistema operativo e fotocamera avviati |
| Patch | Evidenze e infrastruttura per progettare esperimenti futuri | Una modifica utile identificata, precondizioni, prova prima/dopo e strategia di recupero |

## Avanzamento stimato

Le percentuali sono stime di copertura rispetto agli obiettivi di ciascuna area. Non misurano la quota di istruzioni del firmware decodificate né la maturità di un prodotto. La stima complessiva, circa **40%**, pesa soprattutto ciò che manca per comprendere il comportamento della fotocamera e costruire un emulatore utile.

| Area | Stima | Base della stima |
|---|---:|---|
| Formati, campioni e analizzatore riproducibile | 85% | Strutture A/B e L, hash e parser verificati per i campioni di riferimento; altre versioni e significato completo dei dati L aperti |
| Avvio, aggiornamento e flash | 60% | Diversi percorsi statici ed esperimenti delimitati; avvio completo, temporizzazione e hardware non coperti |
| Comunicazioni e routine selezionate | 50% | Trasporti A esaminati e catena B ATAM seguita da selettore e vettore fino al servizio OS -85 con frame sintetico; coda OS, destinatario, trigger elettrico e modello completo mancanti |
| Emulazione della fotocamera | 15% | Esecuzione isolata di blocchi con RAM sintetica; CPU, sistema operativo e periferiche integrate non modellati |
| Patch sperimentali validate | 5% | Nessuna patch utile verificata sulla fotocamera; mancano candidato, confronto prima/dopo e procedura di recupero |

Le stime sono indicative e possono cambiare quando si amplia o si restringe il perimetro. L’analisi AB-62–AB-77 è software statica e a istruzioni isolate. Le prove sui registri della periferica usano RAM sintetica e non identificano ancora la sorgente fisica dell’interrupt. La revisione indipendente del 4 novembre non ha rilevato problemi confermati nel perimetro AB-62–AB-69; non ha rieseguito Ghidra.

## Blocco di ricerca attuale

L’ipotesi che un ricevitore A sulla porta numericamente uguale a quella di B fosse il suo destinatario non è stata confermata. Abbiamo identificato un parser di navigazione, un protocollo a comandi di due byte e altri trasporti con controlli differenti. Questo restringe la ricerca, ma non dimostra che A non comunichi con B.

Il confronto con l’emulatore Nikon Hacker non identifica il destinatario del trasporto D90. Abbiamo un inventario delle 71 voci B e prove delimitate del collegamento fra le voci 0x33/0x34, le maschere di stato e la notifica 0x0b. Quest’ultima viene richiesta quando cambia un bit del suo buffer; sono verificate anche le condizioni del controllore fino alla prima notifica o al ritorno.

La funzione 0x108624 è ora eseguita isolatamente fino al ritorno: scrive valori precisi in sei blocchi hardware e altri registri. Le verifiche usano memoria sintetica, quindi non identificano gli effetti fisici di quelle scritture. La prossima fase deve identificare la periferica e risalire agli ingressi che abilitano il bit del messaggio 0x0b. L’inizializzazione completa all’avvio, il significato fotografico e il destinatario fisico restano aperti.

Le scoperte ATAM AB-62–AB-77 seguono l’attesa OS, la produzione del selettore 0x22, il vettore 0x2c, gli accessi al registro di stato 0x63000030 e l’ingresso nel servizio OS -85. La revisione del 5 novembre ha inoltre corretto AB-70: con stato iniziale zero il gestore 0x19a71e non viene selezionato. La sorgente fisica dell’interrupt resta sconosciuta. Fonti, indirizzi e limiti sono in [communication-topology.md](docs/communication-topology.md); la valutazione dei riferimenti esterni è in [related-projects.md](docs/related-projects.md). Prossima indagine: capire la funzione 0x40bec e la coda OS, quindi seguire il ritorno al codice chiamante. Modello consigliato: Astra Medio.

## Pubblicazione

Il repository pubblico su GitHub presenta il progetto come alpha di ricerca. Gli aggiornamenti successivi includono nuove scoperte e strumenti, mantenendo firmware, immagini estratte e database Ghidra fuori da Git.

Criteri di mantenimento della pubblicazione:

- Campioni separati dal codice e mantenuti in sola lettura. Firmware, immagini estratte e database Ghidra esclusi da Git.
- Report riproducibile da una copia pulita dei sorgenti.
- Istruzioni di importazione e prove Ghidra, con identificazione del profilo linguaggio utilizzato.
- Distinzione esplicita fra fatti, esperimenti con ingressi sintetici, ipotesi e copertura mancante.
- Guida ai contributi e licenza dei contributi originali.

Una riproduzione su una seconda macchina resta utile come verifica indipendente. Non è stata eseguita e non va confusa con una nuova importazione sullo stesso host. Le prove numerose non misurano la percentuale di firmware compresa: molti casi variano gli ingressi dello stesso blocco di poche istruzioni.

Descrizione impostata nel campo About di GitHub: “Independent Nikon D90 firmware research. Progress ~40%: package analysis 85%, update and flash 60%, communications 50%, emulation 15%, validated patches 5%. No firmware included.” Argomenti: nikon, firmware, reverse-engineering, ghidra, camera.

## Verifica di preparazione del 3 novembre 2026

- Il report completo dell’analizzatore coincide byte per byte con `reports/reference.json`, anche da un nuovo checkout locale privo dei derivati ignorati.
- Le cinque prove recenti di A e le due di B elencate in CONTRIBUTING sono passate sia sui programmi di lavoro sia dopo nuove importazioni in progetti Ghidra temporanei. Gli script aggiornati inizializzano esplicitamente anche i registri salvati dai prologhi e il registro di periferica usato dalla selezione A. Nei log delle nuove importazioni finali non risultano avvisi di letture non inizializzate o errori.
- Tutti i blob della cronologia raggiungibile esaminata sono testo UTF-8 senza byte NUL. Il controllo dei nomi non ha trovato firmware, HEX o database Ghidra nella cronologia. Si tratta di un controllo degli artefatti, non di una certificazione legale o di una revisione indipendente di ogni riga.
- Le esclusioni Git comprendono ora anche HEX e progetti Ghidra. La documentazione indica come riprodurre le prove e conserva i digest del profilo linguaggio.

Valutazione: il repository è pubblicato come alpha di ricerca. Gli aggiornamenti AB-62–AB-77, la rettifica AB-70, la revisione indipendente del 4 novembre, l’indice aggiornato e la stima di avanzamento vengono pubblicati con questo aggiornamento sul ramo main. Una verifica su un’altra macchina rafforzerebbe la riproducibilità, ma non richiede prima di risolvere il collegamento A–B o l’emulazione completa.

## Rettifica locale del 5 novembre 2026

La riesecuzione di `D90AtamInterrupt.java` corregge AB-70: con maschera 2, stato zero e selettore 0x22 il gestore 0x19a71e viene saltato, lo stato resta zero e la prima notifica ha R5=0x00800000. La precedente descrizione della notifica 0x20000000 con stato zero è ritirata. I casi sono nove; il messaggio di successo ora li conta e ne registra gli esiti. AB-68 è coerente con questa verifica. La correzione era locale al momento della nota; è inclusa in questo aggiornamento. Le percentuali restano stime indicative invariate; questa rettifica non aggiunge copertura hardware.

AB-71 aggiunge un inventario statico circoscritto di 23 finestre candidate di accesso allo stato: 21 scritture e due letture tramite helper. La scrittura a 0x19d8dc passa 0xf1ff01ff, che contiene il bit 0x02; il suo effetto hardware non è identificato. Prossima indagine: contesto di questa scrittura e consumo della lettura a 0x19da9a. AB-71 era locale al momento della nota; è incluso in questo aggiornamento.

AB-72 chiarisce che la scrittura ampia dello stato segue la cancellazione di bit nelle due parole di controllo, incluso 0x02. Due esecuzioni isolate della routine 0x19d89c passano fino al ritorno con RAM sintetica. Il ruolo di disabilitazione/ripristino è un’ipotesi; chiamanti e semantica hardware restano aperti. Prossimo passo: chiamanti di 0x19d89c. Risultati locali al momento della nota; inclusi in questo aggiornamento.

AB-73 identifica due chiamanti di 0x19d89c, a 0x19d4ac e 0x19d52c. Quattro prove attraversano la sequenza e si fermano all’ingresso comune di 0x19db40. Il seguito statico legge lo stato software per scegliere fra zero e due codici; non è stato eseguito. Prossima indagine: le prime due chiamate di 0x19db40 e il controllo a 0x63000024. Risultati locali al momento della nota; inclusi in questo aggiornamento.

AB-74 verifica l’azzeramento iniziale di dieci parole e sette byte, seguito dal confine a 0x19e020. Quattro ulteriori prove isolate distinguono il bit 18 di 0x63000024 e i due rami successivi. Il tratto intermedio contiene servizi OS e un ciclo non ancora eseguiti. Prossimo passo: dipendenze di 0x19e020, iniziando da 0x19e0fa. Risultati locali al momento della nota; inclusi in questo aggiornamento.

AB-75 esegue la prima attesa di 0x19e020 fino al ritorno a 0x19e028 in due scenari sintetici: cancellazione del bit e avanzamento del valore a 0x68000114. Nel secondo il bit resta acceso al ritorno, quindi il ritorno non prova il completamento hardware. Unità del contatore e periferica restano sconosciute. Prossima indagine: preparazioni da 0x19e028 al primo servizio OS a 0x19e064. Risultati locali al momento della nota; inclusi in questo aggiornamento.

AB-76 estende in continuità la preparazione da 0x19e020 al primo INT a 0x19e064: sei casi verificano configurazioni e argomenti R4=2/R12=-85, conservando i due esiti sintetici della prima attesa. La seconda attesa usa 0x63000024=0; il servizio OS non viene eseguito. Prossimo passo: identificazione del servizio -85 e delle precondizioni. Risultati locali al momento della nota; inclusi in questo aggiornamento.

AB-77 risolve il servizio -85 alla funzione 0x40f2c tramite la voce 0x405c8. Quattro prove dal dispatcher verificano il controllo del frame e i rami per argomento positivo, zero e negativo; il caso R4=2 si ferma prima di 0x40bec. Il frame è sintetico e non è stata eseguita la coda OS. Prossimo passo: 0x40bec, 0x418a0 e precondizioni della ripresa. Risultati locali al momento della nota; inclusi in questo aggiornamento.
