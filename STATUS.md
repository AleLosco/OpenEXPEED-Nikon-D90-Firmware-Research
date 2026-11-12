# Stato del progetto

Aggiornato il 12 novembre 2026. Questo file riassume il risultato attuale; il README presenta il progetto, mentre i documenti tecnici conservano evidenze, ipotesi e revisioni delle interpretazioni.

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

Le percentuali sono stime di copertura rispetto agli obiettivi di ciascuna area. Non misurano la quota di istruzioni del firmware decodificate né la maturità di un prodotto. La stima complessiva, circa **45%**, pesa soprattutto ciò che manca per comprendere il comportamento della fotocamera e costruire un emulatore utile.

| Area | Stima | Base della stima |
|---|---:|---|
| Formati, campioni e analizzatore riproducibile | 85% | Strutture A/B e L, hash e parser verificati per i campioni di riferimento; altre versioni e significato completo dei dati L aperti |
| Avvio, aggiornamento e flash | 65% | Prefisso OS e inizializzazione periferica collegati fino a 0x40740 (AB-105), oltre ai percorsi di aggiornamento/flash; reset completo e hardware non coperti |
| Comunicazioni e routine selezionate | 55% | Trasporti A, catena ATAM, servizi e operazioni di coda delimitate; costruzione delle liste OS verificata. Destinatario fisico, trigger e scheduler restano aperti |
| Emulazione della fotocamera | 20% | Segmenti OS/periferica collegati sulla stessa memoria; INT/SSP espliciti e RAM sintetica. Mancano reset completo, scheduler, periferiche e tempi realistici |
| Patch sperimentali validate | 5% | Nessuna patch utile verificata sulla fotocamera; mancano candidato, confronto prima/dopo e procedura di recupero |

Le stime sono indicative e possono cambiare quando si amplia o si restringe il perimetro. Le prove fino ad AB-160 comprendono analisi statica, routine isolate e segmenti collegati con confini sintetici espliciti. Il percorso per R4=28/R5=0 comprende il ritorno al chiamante utente del servizio OS -23 in RAM sintetica. La stima complessiva non è una media aritmetica delle aree. Le prove sui registri della periferica usano RAM sintetica e non identificano ancora la sorgente fisica dell’interrupt. Le revisioni indipendenti del 4 e 11 novembre hanno segnalato rilievi di bassa severità poi corretti; i loro limiti sono registrati nelle sezioni tecniche.

La revisione indipendente AB-78–AB-97 ha individuato due lacune di robustezza/riproducibilità, corrette e verificate localmente: PC dispari nell’adattatore INT e verifica incompleta degli input del linguaggio sperimentale. Test e riesecuzioni mirate sono documentati nella [rettifica AB-85/AB-86](docs/communication-topology.md#rettifica-di-riproducibilità-ab-85ab-86--6-novembre-2026). Quelle correzioni non avevano modificato le stime: riguardavano il banco di prova, non nuova copertura hardware.

## Come leggere i progressi recenti

Le stime sopra sono valutazioni qualitative del perimetro di ricerca. Non derivano da un conteggio esaustivo e non permettono di stimare il tempo residuo. Il valore complessivo 45% serve come riferimento storico approssimativo. I traguardi verificabili seguenti descrivono meglio lo sviluppo attuale.

| Traguardo | Stato locale e riferimento |
|---|---|
| Estrarre e analizzare i campioni di riferimento | Riproducibile per A/B e L, con limiti sui significati dei dati |
| Collegare inizializzazione OS e contesto utente | Segmenti verificati in AB-104–123 con adattamenti espliciti |
| Seguire le chiamate d'inizializzazione del candidato | Sei chiamate completate nel percorso delimitato, con due richieste del servizio -23 e ritorno al candidato in AB-154 |
| Eseguire un contesto preparato e verificarne la funzione | AB-157 collega selezione del record 8, ripristino e prima CALL in otto scenari con ingresso sintetico nel dispatcher. Funzione del task ancora aperta |
| Far avanzare la fotocamera con periferiche e tempi modellati | Aperto |
| Validare una patch con effetto fotografico identificato | Aperto |

AB-148–153 approfondiscono un tratto dell'inizializzazione e i suoi due inserimenti in lista. La copertura delle periferiche e dei tempi resta quella precedente. Il passo strategico successivo è collegare un contesto preparato alla sua effettiva esecuzione nel banco, identificando prima ingressi e dipendenze. Gli identificativi AB registrano risultati, senza costituire unità uniformi di avanzamento.

## Priorità attuale

AB-160 verifica 16 preparazioni delle modalità software 1/3 del task 8 e il ritorno del servizio -47 con le due maschere dell'oggetto 3. I bit interessati erano già zero nello stato ereditato. La prossima priorità è identificare precondizioni e struttura degli argomenti della richiesta -170 a 0x10fdb4, quindi seguirne il percorso delimitato. [Evidenze AB-160](docs/communication-topology.md#ab-160--due-preparazioni-del-task-8-e-servizio--47)

## Contesto e progressione della ricerca

L’ipotesi che un ricevitore A sulla porta numericamente uguale a quella di B fosse il suo destinatario non è stata confermata. Abbiamo identificato un parser di navigazione, un protocollo a comandi di due byte e altri trasporti con controlli differenti. Questo restringe la ricerca, ma non dimostra che A non comunichi con B.

Il confronto con l’emulatore Nikon Hacker non identifica il destinatario del trasporto D90. Abbiamo un inventario delle 71 voci B e prove delimitate del collegamento fra le voci 0x33/0x34, le maschere di stato e la notifica 0x0b. Quest’ultima viene richiesta quando cambia un bit del suo buffer; sono verificate anche le condizioni del controllore fino alla prima notifica o al ritorno.

La funzione 0x108624 è ora eseguita isolatamente fino al ritorno: scrive valori precisi in sei blocchi hardware e altri registri. Le verifiche usano memoria sintetica, quindi non identificano gli effetti fisici di quelle scritture. La prossima fase deve identificare la periferica e risalire agli ingressi che abilitano il bit del messaggio 0x0b. L’inizializzazione completa all’avvio, il significato fotografico e il destinatario fisico restano aperti.

Le scoperte ATAM AB-62–AB-97 seguono l’attesa OS, la produzione del selettore 0x22, il vettore 0x2c, gli accessi al registro di stato 0x63000030 e l’ingresso nel servizio OS -85, con una prova delimitata della rimozione da una lista bidirezionale. La revisione del 5 novembre ha inoltre corretto AB-70: con stato iniziale zero il gestore 0x19a71e non viene selezionato. La sorgente fisica dell’interrupt resta sconosciuta. Fonti, indirizzi e limiti sono in [communication-topology.md](docs/communication-topology.md); la valutazione dei riferimenti esterni è in [related-projects.md](docs/related-projects.md). AB-82 collega il salvataggio e il ripristino dei registri con un frame software generato dalle istruzioni originali; dispatcher e servizio sono saltati esplicitamente. AB-83 conferma che INT richiede la pcodeop non implementata `_interrupt_prelude`, mentre quattro prove RETI consumano correttamente frame PC/PS sintetici secondo il modello Ghidra. AB-84 documenta inoltre il limite ILM a quattro bit del modello installato rispetto al riferimento FR a cinque bit, con quattro prove mirate e un progetto di adattatore non ancora implementato. AB-85 verifica una copia isolata del modello PS/ILM a cinque bit, con 128 sequenze sintetiche confrontate col modello originale. AB-86 prova un adattatore esplicito INT #0x40 su memoria sintetica: 20 casi di ingresso/RETI e sei rifiuti attesi dopo la rettifica AB-86. Il ritorno lascia SSP separato da SP nel modello. AB-87 verifica una sincronizzazione esplicita SSP=SP al confine RETI nel solo caso supervisore, con 20 coppie di interrupt sintetici consecutivi e tre rifiuti attesi; non implementa un alias globale. AB-88 collega prefisso/epilogo originali al frame dell’adattatore in 32 casi fino a RETI e al PC del chiamante; il servizio è saltato esplicitamente. Le 1480 istanze di passo esaminate non accedono direttamente a SSP/USP. AB-89 include dispatcher e servizio -85 in otto scenari fino al PC del chiamante, senza saltare il corpo del servizio; INT e SSP restano adattamenti espliciti e le periferiche sono RAM. AB-90 individua il decremento a 0x40a72 e verifica tre cicli di vita: il nodo viene trasferito dopo il passaggio sotto zero (valore 2: terza invocazione del banco di prova). Frequenza e scheduler restano sconosciuti. AB-91 individua la chiamata a 0x4049e e verifica sei scenari della routine completa 0x40a56: aggiornamento con riporto del contatore 0x68000110/114 e trasferimenti multinodo nella stessa invocazione. AB-92 identifica il vettore 0x18 (0xdff9c→0x4048a) e collega otto servizi a 24 ingressi sintetici successivi senza ricostruire le liste: il nodo del servizio viene trasferito dopo tre ingressi. Non sono modellati accettazione hardware o scheduler. AB-93 collega l’avvio 0x40738→0x4046a alle scritture 0x48/0x4e e verifica sei casi di prefisso/controllo con RAM sintetica. La periferica e la cadenza restano non identificate. AB-94 esegue il prefisso continuo attraverso 0x100936 e tre callee in due casi da 463 passi, fino a INT del servizio -83. Due byte del blocco R4 conservano il contenuto sintetico dello stack. AB-95 verifica 16 casi del servizio -83: legge +2/+4 del blocco e imposta il contatore; i primi due byte non vengono letti in questo percorso. AB-96 collega prefisso, servizio -83 e RETI in otto casi fino a 0x100950, con contatore azzerato e 17 registri recuperati; INT e sincronizzazioni SSP restano espliciti. AB-97 completa anche i tre callee e il ritorno della chiamata a 0x40740 in otto scenari con RAM sintetica; non è avvio completo. SSP resta separato dopo il recupero ordinario dello stack. Le priorità storiche che seguono descrivono i passaggi di allora; il riepilogo corrente è nella sezione sopra. La sorgente fisica dell’interrupt, l'identità della periferica, la cadenza, gli effetti del byte 0x44 e la ripresa del task restano aperti.

## Pubblicazione

Il repository pubblico su GitHub presenta il progetto come alpha di ricerca. Gli aggiornamenti successivi includono nuove scoperte e strumenti, mantenendo firmware, immagini estratte e database Ghidra fuori da Git.

Criteri di mantenimento della pubblicazione:

- Campioni separati dal codice e mantenuti in sola lettura. Firmware, immagini estratte e database Ghidra esclusi da Git.
- Report riproducibile da una copia pulita dei sorgenti.
- Istruzioni di importazione e prove Ghidra, con identificazione del profilo linguaggio utilizzato.
- Distinzione esplicita fra fatti, esperimenti con ingressi sintetici, ipotesi e copertura mancante.
- Guida ai contributi e licenza dei contributi originali.

Una riproduzione su una seconda macchina resta utile come verifica indipendente. Non è stata eseguita e non va confusa con una nuova importazione sullo stesso host. Le prove numerose non misurano la percentuale di firmware compresa: molti casi variano gli ingressi dello stesso blocco di poche istruzioni.

Descrizione About proposta: “Independent Nikon D90 firmware research. Progress ~45%: formats/tools 85%, update/flash 65%, communications 55%, emulation 20%, validated patches 5%. Ghidra and bounded OS/task experiments. No firmware included.” Argomenti: nikon, firmware, reverse-engineering, ghidra, camera.

## Verifica di preparazione del 3 novembre 2026

- Il report completo dell’analizzatore coincide byte per byte con `reports/reference.json`, anche da un nuovo checkout locale privo dei derivati ignorati.
- Le cinque prove recenti di A e le due di B elencate in CONTRIBUTING sono passate sia sui programmi di lavoro sia dopo nuove importazioni in progetti Ghidra temporanei. Gli script aggiornati inizializzano esplicitamente anche i registri salvati dai prologhi e il registro di periferica usato dalla selezione A. Nei log delle nuove importazioni finali non risultano avvisi di letture non inizializzate o errori.
- Tutti i blob della cronologia raggiungibile esaminata sono testo UTF-8 senza byte NUL. Il controllo dei nomi non ha trovato firmware, HEX o database Ghidra nella cronologia. Si tratta di un controllo degli artefatti, non di una certificazione legale o di una revisione indipendente di ogni riga.
- Le esclusioni Git comprendono ora anche HEX e progetti Ghidra. La documentazione indica come riprodurre le prove e conserva i digest del profilo linguaggio.

Valutazione: il repository è pubblicato come alpha di ricerca. Gli aggiornamenti fino ad AB-77 e la rettifica AB-70 sono pubblicati sul ramo main. Lo snapshot locale include AB-78–AB-97 e le correzioni della revisione. La cronologia locale contiene la pubblicazione del 6 novembre; lo stato remoto non è stato consultato il 9 novembre. Una verifica su un’altra macchina rafforzerebbe la riproducibilità, ma non richiede prima di risolvere il collegamento A–B o l’emulazione completa.

## Rettifica locale del 5 novembre 2026

La riesecuzione di `D90AtamInterrupt.java` corregge AB-70: con maschera 2, stato zero e selettore 0x22 il gestore 0x19a71e viene saltato, lo stato resta zero e la prima notifica ha R5=0x00800000. La precedente descrizione della notifica 0x20000000 con stato zero è ritirata. I casi sono nove; il messaggio di successo ora li conta e ne registra gli esiti. AB-68 è coerente con questa verifica. La correzione era locale al momento della nota; è inclusa in questo aggiornamento. Le percentuali restano stime indicative invariate; questa rettifica non aggiunge copertura hardware.

AB-71 aggiunge un inventario statico circoscritto di 23 finestre candidate di accesso allo stato: 21 scritture e due letture tramite helper. La scrittura a 0x19d8dc passa 0xf1ff01ff, che contiene il bit 0x02; il suo effetto hardware non è identificato. Prossima indagine: contesto di questa scrittura e consumo della lettura a 0x19da9a. AB-71 era locale al momento della nota; è incluso in questo aggiornamento.

AB-72 chiarisce che la scrittura ampia dello stato segue la cancellazione di bit nelle due parole di controllo, incluso 0x02. Due esecuzioni isolate della routine 0x19d89c passano fino al ritorno con RAM sintetica. Il ruolo di disabilitazione/ripristino è un’ipotesi; chiamanti e semantica hardware restano aperti. Prossimo passo: chiamanti di 0x19d89c. Risultati locali al momento della nota; inclusi in questo aggiornamento.

AB-73 identifica due chiamanti di 0x19d89c, a 0x19d4ac e 0x19d52c. Quattro prove attraversano la sequenza e si fermano all’ingresso comune di 0x19db40. Il seguito statico legge lo stato software per scegliere fra zero e due codici; non è stato eseguito. Prossima indagine: le prime due chiamate di 0x19db40 e il controllo a 0x63000024. Risultati locali al momento della nota; inclusi in questo aggiornamento.

AB-74 verifica l’azzeramento iniziale di dieci parole e sette byte, seguito dal confine a 0x19e020. Quattro ulteriori prove isolate distinguono il bit 18 di 0x63000024 e i due rami successivi. Il tratto intermedio contiene servizi OS e un ciclo non ancora eseguiti. Prossimo passo: dipendenze di 0x19e020, iniziando da 0x19e0fa. Risultati locali al momento della nota; inclusi in questo aggiornamento.

AB-75 esegue la prima attesa di 0x19e020 fino al ritorno a 0x19e028 in due scenari sintetici: cancellazione del bit e avanzamento del valore a 0x68000114. Nel secondo il bit resta acceso al ritorno, quindi il ritorno non prova il completamento hardware. Unità del contatore e periferica restano sconosciute. Prossima indagine: preparazioni da 0x19e028 al primo servizio OS a 0x19e064. Risultati locali al momento della nota; inclusi in questo aggiornamento.

AB-76 estende in continuità la preparazione da 0x19e020 al primo INT a 0x19e064: sei casi verificano configurazioni e argomenti R4=2/R12=-85, conservando i due esiti sintetici della prima attesa. La seconda attesa usa 0x63000024=0; il servizio OS non viene eseguito. Prossimo passo: identificazione del servizio -85 e delle precondizioni. Risultati locali al momento della nota; inclusi in questo aggiornamento.

AB-77 risolve il servizio -85 alla funzione 0x40f2c tramite la voce 0x405c8. Quattro prove dal dispatcher verificano il controllo del frame e i rami per argomento positivo, zero e negativo; il caso R4=2 si ferma prima di 0x40bec. Il frame è sintetico e non è stata eseguita la coda OS. Prossimo passo: 0x40bec, 0x418a0 e precondizioni della ripresa. Risultati locali al momento della nota; inclusi in questo aggiornamento.

AB-78 esegue 0x418a0 su due liste circolari sintetiche e verifica la rimozione dei nodi centrali e i collegamenti dei vicini. Il ramo a vicini coincidenti e la coda OS completa restano aperti; la semantica della lista è compatibile con una lista bidirezionale, senza nome OS verificato. Risultati locali al momento della nota; inclusi nello snapshot di pubblicazione del 6 novembre registrato nella cronologia locale.

AB-79 verifica otto casi di rimozione dell’ultimo nodo davanti a una sentinella: la lista diventa vuota e viene cancellato un bit della maschera globale secondo il campo +0x0c. Corretto il rapporto fra le chiamate in AB-78: il servizio -85 chiama separatamente 0x40bec e 0x418a0. Significato del campo, scheduler e ripresa restano aperti. Risultati locali al momento della nota; inclusi nello snapshot di pubblicazione del 6 novembre registrato nella cronologia locale.

AB-80 collega dispatcher, ricerca nella lista, rimozione e inserimento: quattro scenari con richiesta 2 arrivano al ritorno locale del servizio a 0x406c6 e verificano collegamenti e campi relativi. Il seguito del dispatcher e la ripresa del task restano aperti. Prossima indagine: 0x404f8. Risultati locali al momento della nota; inclusi nello snapshot di pubblicazione del 6 novembre registrato nella cronologia locale.

AB-81 identifica 0x404f8 come RET nel campione e verifica quattro esecuzioni separate dell’epilogo fino a prima di RETI: ripristino di 17 registri, estensione di R12 e ramo su una parola salvata. Frame reale, RETI e scheduling restano aperti. Risultati locali al momento della nota; inclusi nello snapshot di pubblicazione del 6 novembre registrato nella cronologia locale.


## Ripresa locale · 9 novembre 2026

Checkout iniziale pulito, con risultati fino ad AB-97 e rettifica AB-85/86. Nessuna nuova verifica del remoto. Il registro operativo resta questo file; dettagli e prove già completate restano in [communication-topology.md](docs/communication-topology.md), con riproduzione in [CONTRIBUTING.md](CONTRIBUTING.md).

- **Completato:** ritorno dell'inizializzazione a 0x40740 (AB-97), prove sintetiche del vettore 0x18 (AB-92), correzioni PC/digest (rettifica AB-85/86). Le precedenti riesecuzioni non sono state ripetute per questa organizzazione.
- **Ripresa tecnica:** confronto dei sorgenti `D90SourceInitializationReturn.java`, `D90QueueVector.java`, `D90QueueSourceControl.java` e del listato locale `work/queue-vector.log`. Il wrapper 0x4048a scrive la halfword 0x1a a 0x4e (istruzioni 0x40494/0x40496) prima della chiamata a 0x40a56. AB-93 verifica invece la preparazione 0x4e=0x1b. La corrispondenza suggerisce un ruolo nel controllo dell'evento, ma non ne identifica la semantica hardware.
- **Lacuna concreta:** AB-97 verifica il contatore azzerato al ritorno e diverse scritture finali; non asserisce esplicitamente il valore finale di 0x4e. La scrittura iniziale AB-93 non basta a garantirlo dopo tutti i callee. La continuità verso il vettore resta da eseguire.
- **Priorità 1:** estendere la prova dell'inizializzazione per osservare 0x48/0x4e al ritorno, poi entrare esplicitamente nel wrapper del vettore 0x18 sulla stessa memoria e verificare 0x4e=0x1a prima della chiamata a 0x40a56. Documentare ogni frame e sincronizzazione SSP imposti dal banco.
- **Priorità 2:** collegare contatore e liste con precondizioni esplicite, riusando AB-92; non attribuire un ingresso forzato all'accettazione hardware di un interrupt.
- **Aperti:** identità della periferica, significato di 0x44 e 0x4e, cadenza, accettazione interrupt, scheduler/ripresa task e collegamento A–B. L'ipotesi di controllo/riconoscimento dell'evento resta non confermata. Percentuali invariate.

`AGENTS.md` definisce il workflow permanente. Le esclusioni locali sono state estese a log, dump/raw, linguaggi compilati, temporanei e file `.env`. Gli HEX e i progetti sotto `local/` restano esclusi; i firmware originali sono esterni al repository. Prima di pubblicare occorre comunque esaminare i file tracciati e il contenuto dei documenti: l'esclusione per nome non certifica l'assenza di dati riservati.


### Aggiornamento tecnico AB-98 · 9 novembre 2026

Completata la priorità 1 sopra: otto scenari collegano l'inizializzazione al prefisso del vettore 0x18 sulla stessa memoria sintetica. Al ritorno 0x48=0x9e34 e 0x4e=0x1b; il wrapper porta 0x4e a 0x1a preservando i byte adiacenti. Arresto prima di CALL a 0x4049e, contatore ancora zero. Log senza avvisi/errori, 5336 passi controllati; dettagli, script e limiti in [AB-98](docs/communication-topology.md#ab-98--dallinizializzazione-al-prefisso-del-vettore-0x18).

Prossima priorità: precondizioni delle liste richieste da 0x40a56 prima di collegarne l'esecuzione al nuovo percorso. Ingresso IRQ ancora imposto dal banco, periferica e cadenza ignote; nessuna nuova evidenza hardware o variazione delle percentuali. AB-98 e le modifiche al workflow restano locali, senza commit o pubblicazione.


### Aggiornamento tecnico AB-99 · 9 novembre 2026

Verificate le precondizioni della coda sul percorso AB-97/98: otto casi con pattern 0x5a/0xa5 conservano le quattro coppie di puntatori delle sentinelle e le regioni task/bitmap/flag, prima e dopo il prefisso del vettore. Il percorso non costruisce liste vuote valide da questi ingressi. Dettagli e limiti in [AB-99](docs/communication-topology.md#ab-99--precondizioni-delle-liste-dopo-linizializzazione-delimitata); log locale `work/initialization-queue-preconditions.log`, 5336 passi, nessun avviso/errore.

Priorità aggiornata: cercare l'inizializzazione OS delle sentinelle, prima di collegare la coda al percorso d'avvio. Le liste artificiali di AB-91/92 restano precondizioni del banco. Non è dimostrato un difetto firmware né l'assenza di inizializzazione altrove. Modello consigliato: **Astra Medio**, per correlare produttori dei puntatori, flusso d'avvio e precondizioni. Nessun commit o aggiornamento remoto; percentuali invariate.


### Aggiornamento tecnico AB-100 · 9 novembre 2026

Individuati e provati i produttori delle sentinelle: 0x40886 costruisce 33 coppie da 0x68000008 a 0x68000108 (comprende 0x68000010); 0x408a0 costruisce le tre sentinelle a 0x68000118/120/128 e scrive -1 a 0x68000138. Due casi con pattern differenti verificano tutti i byte della regione, senza avvisi/errori nel log finale. Il listato mostra chiamate a questi produttori prima di 0x40738, ma non è ancora eseguito l'intero percorso. Dettagli e rettifica dell'attesa iniziale del test in [AB-100](docs/communication-topology.md#ab-100--produttori-delle-sentinelle-prima-della-chiamata-ab-97).

Prossima priorità: effetti delle chiamate 0x4071a–0x40728 sulle strutture OS, prima del collegamento continuo al vettore. Modello consigliato: **Astra Medio**, per distinguere configurazione statica, stato delle liste e raggiungibilità. Avanzamento stimato invariato; modifiche soltanto locali.


### Aggiornamento tecnico AB-101 · 9 novembre 2026

Inventariate le otto tabelle delle chiamate 0x4071a–0x40728. La prima prepara 39 descrittori; verificato isolatamente il costruttore 0x41578 su tutti i descrittori originali con due pattern di destinazione: 78 casi superati senza avvisi/errori. Collega ciascun record a sé stesso e alla propria configurazione; i rami successivi che possono inserirlo nelle liste non sono ancora eseguiti. Evidenze e distinzione statico/dinamico in [AB-101](docs/communication-topology.md#ab-101--tabelle-dinizializzazione-os-e-costruttore-dei-record).

Prossima priorità: bit dei 39 descrittori e chiamante completo 0x408be, con precondizioni delle liste documentate da AB-100. Modello consigliato: **Astra Medio**, per validare inserimento e contesto dei record. Stato hardware e percentuali invariati; nessun commit o modifica remota.


### Aggiornamento tecnico AB-102 · 9 novembre 2026

Eseguita 0x408be completa dopo i costruttori delle liste: due scenari verificano 39 record, 38 blocchi ausiliari e due inserimenti (indici di lista 1 e 33). Le altre 31 liste e le tre sentinelle aggiuntive restano vuote; bitmap finale 0x80000000. Il campo indice 33 ha richiesto la correzione del limite del test, non del firmware. [AB-102](docs/communication-topology.md#ab-102--costruzione-completa-dei-record-e-due-inserimenti-iniziali) documenta precondizioni sintetiche, campi verificati e log finale senza avvisi/errori.

Prossima priorità: routine 0x40906–0x40a30 e mantenimento delle liste dopo le rimanenti tabelle. Modello consigliato: **Astra Medio**, per il confronto fra strutture e flusso d'inizializzazione. Nessuna prova di scheduler o hardware; percentuali invariate, nessun commit o modifica remota.


### Aggiornamento tecnico AB-103 · 9 novembre 2026

Eseguite sulla memoria prodotta da AB-102 le sette routine rimanenti 0x40906–0x40a30: due scenari, 14 ritorni, 49 oggetti con campo halfword e 22 con campo word per scenario; cinque tabelle a conteggio zero. Confronto completo dei 4096 byte della regione OS dopo ogni ritorno, più contesti e byte 0x44: liste e record precedenti conservati. Log senza avvisi/errori; dettagli e perimetro in [AB-103](docs/communication-topology.md#ab-103--restanti-tabelle-os-e-conservazione-delle-liste).

Prossima priorità: collegare le CALL originali 0x40704–0x40738, includendo azzeramento 0x407f0 e preparazione 0x40878, per sostituire le transizioni fra routine imposte dal banco. Modello consigliato: **Astra Medio**, per verificare precondizioni, ordine e stato risultante del blocco continuo. Percentuali invariate; nessun commit o modifica remota.


### Aggiornamento tecnico AB-104 · 9 novembre 2026

Eseguito in continuità il blocco 0x40704–0x40738 attraverso azzeramento e CALL originali: due scenari da 12125 passi, senza ulteriori scritture del banco dopo l'avvio. Lo stato finale OS e i contesti corrispondono all'immagine attesa costruita dalle tabelle; verificati anche azzeramenti, guardie, marker e byte periferici sintetici. Nessun avviso/errore nel log. Dettagli in [AB-104](docs/communication-topology.md#ab-104--prefisso-os-continuo-attraverso-le-call-originali).

Prossima priorità: collegare lo stato OS così prodotto al percorso AB-97 da 0x40738, senza reinizializzare le liste. Servono ancora gli adattamenti espliciti ILM/INT/SSP per quel seguito. Modello consigliato: **Astra Medio**, per controllare la continuità dello stato e i limiti CPU. Nessun avvio completo dimostrato; percentuali invariate e modifiche solo locali.


### Aggiornamento tecnico AB-105 · 9 novembre 2026

Collegati prefisso OS e inizializzazione periferica da 0x40704 a prima di 0x40740, senza reiniettare le liste: otto scenari e 102272 istanze di passo controllate, log senza avvisi/errori. Conservati i 3872 byte della regione OS e il contesto esterno attraverso il seguito AB-97; restano espliciti INT, SSP e RAM periferica. Evidenze, sorgenti e limiti in [AB-105](docs/communication-topology.md#ab-105--collegamento-continuo-fra-inizializzazione-os-e-periferica), riproduzione in [CONTRIBUTING.md](CONTRIBUTING.md).

Stima complessiva rivista a circa 45% per l’integrazione locale AB-98–AB-105; dettagli nella tabella iniziale. Nessuna patch utile validata o fotocamera completamente emulata. Prossimo obiettivo: vettore 0x18 sullo stato prodotto dal firmware; identità periferica, temporizzazione e scheduler restano problemi aperti. Modello consigliato: **Astra Medio**. Nessun commit, push o modifica remota.


### Aggiornamento tecnico AB-106 · 9 novembre 2026

Otto scenari collegano AB-105 a un ingresso forzato del vettore 0x18 e al ritorno a 0x40740. Verificati contatore 0→1, conservazione della regione OS e del contesto esterno, registri e frame di ritorno; 102672 passi controllati, log senza avvisi/errori. [AB-106](docs/communication-topology.md#ab-106--vettore-0x18-sullo-stato-os-inizializzato) documenta evidenze e limiti. Liste d'attesa iniziali vuote, accettazione hardware e scheduler non modellati. Prossimo obiettivo: percorso da 0x40740 al primo contesto. Modello consigliato: Astra Medio. Stime invariate; risultato locale, non ancora pubblicato.


### Aggiornamento tecnico AB-107 · 9 novembre 2026

Verificata in otto scenari la selezione diretta del record 38: descrittore 0x5c838 → nodo 0x680007c4 → contesto 0x68000ed0. Marker 2 e puntatore corrente aggiornati; arresto prima di 0x407ce. Log finale senza avvisi/errori, 102744 passi; corretto un avviso diagnostico inizializzando USP nel banco. [AB-107](docs/communication-topology.md#ab-107--selezione-iniziale-e-confine-dello-stack-utente) distingue percorso eseguito e seguito statico. Prossima priorità: cambio S e modello SP/USP/SSP; ripristino del contesto e scheduler aperti. Astra Medio consigliato. Percentuali invariate; modifiche locali, non pubblicate.


### Aggiornamento tecnico AB-108 · 10 novembre 2026

Otto prove isolate delle cinque istruzioni da 0x407ce confermano che il modello installato e quello ILM corretto mantengono SP separato da USP/SSP anche al cambio S. I pop leggono dalla posizione SP precedente; log senza avvisi/errori. [AB-108](docs/communication-topology.md#ab-108--caratterizzazione-del-cambio-s-e-dei-registri-di-stack) registra input, confronti e limiti. Non è stato eseguito il primo contesto né modificato il modello CPU. Prossimo obiettivo: adattatore circoscritto del cambio stack e prove sulle precondizioni prima di riprendere AB-107. Modello consigliato: Astra Medio. Stime invariate; modifiche soltanto locali.


### Aggiornamento tecnico AB-109 · 10 novembre 2026

Adattatore del cambio S verificato in isolamento sui due profili: otto sequenze, otto prove dei siti e 48 rifiuti senza modifiche ai registri e alla memoria controllati. Log senza avvisi/errori. [AB-109](docs/communication-topology.md#ab-109--adattatore-circoscritto-del-cambio-di-stack) documenta convenzione, precondizioni e limiti. Non è un alias globale né un ritorno utente validato. Prossima priorità: integrare il ripristino AB-107 fino a prima di RETI e verificare il frame. Modello consigliato: Astra Medio. Percentuali invariate; lavoro solo locale.


### Aggiornamento tecnico AB-110 · 10 novembre 2026

Otto scenari collegano il contesto costruito al ripristino di 18 registri e al trasferimento del frame PC/PS sullo stack supervisore; stop prima di RETI a 0x407ea. Destinazione preparata 0x407ee, PS=0x001f0030. Regione OS, contesto esterno e guardie verificati; log senza avvisi/errori. [AB-110](docs/communication-topology.md#ab-110--ripristino-del-contesto-costruito-fino-a-prima-di-reti) distingue istruzioni originali e quattro cambi stack adattati. La destinazione è un salto su sé stesso nel listato: ruolo di attesa compatibile ma scheduler non dimostrato. Prossimo obiettivo: RETI verso S=1, ancora escluso dall'adattatore supervisore. Modello consigliato: Astra Medio. Percentuali invariate; modifiche solo locali.


### Aggiornamento tecnico AB-111 · 10 novembre 2026

Sedici prove isolate di RETI verso S=1 confermano lettura del frame su SP, ripristino PC/PS e flag secondo ciascun profilo, conservazione di 18 registri e della memoria sintetica. SP avanza di otto byte ma resta separato da USP; SSP non avanza. Log senza avvisi/errori. [AB-111](docs/communication-topology.md#ab-111--reti-verso-s1-nei-due-profili-cpu) documenta il limite del modello, non un difetto hardware. Prossimo obiettivo: adattatore del confine RETI utente e successiva integrazione in AB-110. Modello consigliato: Astra Medio. Stime invariate; risultati solo locali.


### Aggiornamento tecnico AB-112 · 10 novembre 2026

Adattatore RETI utente circoscritto a 0x407ea: otto ritorni sintetici e undici rifiuti verificati sul profilo ILM corretto, senza avvisi/errori. RETI recupera PC/PS; il banco aggiorna poi SSP e seleziona USP come SP. [AB-112](docs/communication-topology.md#ab-112--adattatore-del-confine-reti-utente) conserva precondizioni e limiti, incluso il mancato rollback dopo eventuali errori del passo. Prossimo obiettivo: integrazione con il frame AB-110 e arrivo alla destinazione 0x407ee. Modello consigliato: Astra Medio. Percentuali invariate; lavoro solo locale.


### Aggiornamento tecnico AB-113 · 10 novembre 2026

Raggiunto il contesto iniziale in modalità utente dal frame prodotto dal firmware: otto scenari, 128 passi del ciclo a 0x407ee, controlli su registri, stack e regioni di memoria senza avvisi/errori. [AB-113](docs/communication-topology.md#ab-113--ingresso-nel-contesto-utente-e-ciclo-iniziale) registra tutti gli adattamenti e l'interrupt forzato ereditato. È integrazione di un segmento d'avvio; reset completo, periferiche reali e passaggio ad altri task restano aperti. Prossimo obiettivo: percorso di selezione di un altro contesto dopo interrupt e ingresso da modalità utente. Modello consigliato: Astra Medio. Percentuali invariate; risultati solo locali, non pubblicati.


### Aggiornamento tecnico AB-114 · 10 novembre 2026

Confermata la corrispondenza della voce 0xdff00 con il vettore 0x3f e il gestore 0x40d44. Otto ispezioni dello stato prodotto da AB-113 verificano il candidato 0x680001ec, frame 0x8f991204 e ingresso 0x1000ac, distinto dal contesto di attesa; byte di controllo iniziali zero. [AB-114](docs/communication-topology.md#ab-114--candidato-pronto-e-percorso-del-vettore-0x3f) collega queste evidenze alla pista statica preesistente AB-27. Il gestore non è eseguito: SRCH1 a 0x40e32 e ingresso interrupt da S=1 restano da verificare. Prossimo obiettivo: caratterizzazione isolata di SRCH1. Modello consigliato: Astra Medio. Stime invariate; modifiche solo locali.


### Aggiornamento tecnico AB-115 · 10 novembre 2026

Compilata una copia isolata dei sorgenti FR81 verificati per hash. SRCH1 decodifica correttamente e due casi con input zero restituiscono 32 conservando lo stato controllato; il primo nonzero fallisce sulla userop clz non implementata. **Griglia inconcludente**, non 76 casi superati. [AB-115](docs/communication-topology.md#ab-115--srch1-decodificata-ma-bloccata-da-clz) conserva errori, avvisi del compilatore, manifesti e riproduzione. Prossimo obiettivo: soluzione circoscritta per clz/SRCH1 con verifica dei risultati e flag prima dell'integrazione. Modello consigliato: Astra Medio. Percentuali invariate; nessuna modifica remota.


### Aggiornamento tecnico AB-116 · 10 novembre 2026

Adattatore SRCH1 AC a 0x40e32 verificato in 76 casi contro un calcolo Java indipendente, più quattro rifiuti senza mutazioni; flag e registri controllati conservati, log senza avvisi/errori. [AB-116](docs/communication-topology.md#ab-116--adattatore-esplicito-srch1-ac) precisa che non è esecuzione nativa dell'istruzione né validazione hardware. Il blocco clz di AB-115 resta documentato. Prossimo obiettivo: prova delimitata del percorso bitmap→candidato→confronto con contesto corrente. Modello consigliato: Astra Medio. Stime invariate; modifiche solo locali.


### Aggiornamento tecnico AB-117 · 10 novembre 2026

Otto prove delimitate collegano bitmap originale, SRCH1 adattata e confronto: candidato 0x680001ec distinto dal corrente 0x680007c4, stop prima del salvataggio a 0x40e62. Verificati liste/frame candidato, SP/RP e memoria OS con l'unica parola di lavoro scritta dal push; log senza avvisi/errori, 103096 passi ordinari. [AB-117](docs/communication-topology.md#ab-117--selezione-delimitata-del-candidato-sullo-stato-inizializzato) dichiara PC imposto a 0x40e2a e prefisso/frame d'interrupt assenti. Prossimo obiettivo: ingresso da S=1 e prefisso del vettore 0x3f, prima del cambio completo. Modello consigliato: Astra Medio. Stime invariate; risultati solo locali.

### Aggiornamento tecnico AB-118 · 11 novembre 2026

Otto scenari raggiungono la lettura della bitmap passando dal prefisso originale del gestore, con frame sintetico e commutazione esplicita dello stack. [Prova e limiti AB-118](docs/communication-topology.md#ab-118--prefisso-del-vettore-0x3f-dal-ciclo-utente). Prossima priorità: collegare questo stato alla selezione AB-117 e verificare il salvataggio del contesto corrente. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-119 · 11 novembre 2026

Otto prove continue dal prefisso AB-118 verificano il frame uscente di 80 byte e il puntatore scritto nel controllo corrente. [Evidenze AB-119](docs/communication-topology.md#ab-119--salvataggio-del-contesto-uscente). Prossimo obiettivo: seguire il caricamento dello stack del candidato da 0x40e7a. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-120 · 11 novembre 2026

Otto scenari continui verificano il caricamento dello stack candidato e delle prime due parole in R0/R1. [Evidenze AB-120](docs/communication-topology.md#ab-120--caricamento-dello-stack-candidato). Prossimo obiettivo: seguire la chiamata tramite 0x404f6 e la preparazione del frame supervisore. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-121 · 11 novembre 2026

Otto prove verificano il passaggio di PC e PS del candidato attraverso la chiamata a 0x404f6 fino al frame supervisore. [Evidenze AB-121](docs/communication-topology.md#ab-121--preparazione-del-frame-supervisore-del-candidato). Prossimo obiettivo: seguire il ripristino dei registri da 0x40ea6 e verificare l'aggiornamento del puntatore al controllo corrente. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-122 · 11 novembre 2026

Otto prove verificano il caricamento di 15 registri e il passaggio del campo corrente al candidato. Sette parole dello stack sono input sintetici aggiunti per verificare l'ordine dei caricamenti. [Evidenze AB-122](docs/communication-topology.md#ab-122--ripristino-dei-registri-e-aggiornamento-del-controllo-corrente). Prossima priorità: seguire il ramo di epilogo da 0x40eb4 e identificare gli input richiesti per il rientro. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-123 · 11 novembre 2026

La RETI del gestore raggiunge il PC candidato 0x1000ac in otto scenari sul ramo zero. [Evidenze e precondizioni AB-123](docs/communication-topology.md#ab-123--ritorno-al-pc-del-candidato). Prossima priorità: verificare la mappatura dell'indirizzo di destinazione e identificare i byte originali prima di tentare la prima istruzione. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-124 · 11 novembre 2026

Il descrittore 0x5c480 collega il candidato a 0x1000ac, dove il database contiene un prologo decodificabile e chiamate successive. [Evidenze AB-124](docs/communication-topology.md#ab-124--descrittore-e-destinazione-del-candidato). Prossimo obiettivo: verificare i byte rispetto al campione originale e alla mappatura di caricamento. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-125 · 11 novembre 2026

Verificati il digest dell'originale e l'identità integrale delle due regioni programmate B nel database. Il PC candidato 0x1000ac corrisponde all'offset file 0x1400ae. [Evidenze AB-125](docs/communication-topology.md#ab-125--confronto-diretto-con-il-campione-originale). Prossimo obiettivo: eseguire il prologo candidato fino al confine prima della CALL a 0x1000d0, preservando le precondizioni esplicite. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-126 · 11 novembre 2026

Otto scenari eseguono le prime otto istruzioni del candidato dal ritorno AB-123. Verificati stack, registri salvati e destinazione della prima chiamata. [Evidenze AB-126](docs/communication-topology.md#ab-126--prime-istruzioni-del-candidato). Prossima priorità: confrontare gli accessi della routine 0x19a542 con la mappa disponibile prima di estendere la prova. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-127 · 11 novembre 2026

Il confronto della mappa al punto di chiamata identifica codice e valori interni disponibili in B e quattro regioni dati da rappresentare nel banco. [Evidenze AB-127](docs/communication-topology.md#ab-127--mappa-richiesta-dalla-prima-chiamata-endm). Prossima prova: eseguire i tre percorsi di scelta con firme controllate e verificare l'intero blocco risultante, compresi i 32 byte finali e il puntatore pubblicato. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-128 · 11 novembre 2026

Ventiquattro prove verificano le tre scelte ENDM, il risultato completo di 31520 byte, la sostituzione dei 32 byte finali e il puntatore pubblicato. [Evidenze AB-128](docs/communication-topology.md#ab-128--tre-scelte-endm-verificate-in-emulazione). Prossima priorità: identificare gli accessi della seconda chiamata, a 0x18522c, e la relazione con lo stato prodotto da ENDM. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-129 · 11 novembre 2026

La seconda routine confronta la firma SYSTEMFIXADJUST e seleziona tra sorgente 0x880000 e dati interni 0x1d610c per copiare 244 byte a 0x8f933214. [Evidenze statiche AB-129](docs/communication-topology.md#ab-129--seconda-chiamata-e-blocco-systemfixadjust). Prossima priorità: verificare entrambi i percorsi dopo AB-128, controllando il blocco ENDM già prodotto e lo stack. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-130 · 11 novembre 2026

Sedici prove verificano entrambi i percorsi SYSTEMFIXADJUST, i 244 byte copiati, le guardie e la conservazione del blocco ENDM precedente. [Evidenze AB-130](docs/communication-topology.md#ab-130--due-percorsi-systemfixadjust-in-emulazione). Prossima priorità: ricostruire gli accessi della terza chiamata a 0x13a1dc e gli input richiesti dal banco. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-131 · 11 novembre 2026

La terza chiamata gestisce due tabelle di 26 puntatori e copie guidate da descrittori, con helper e correzioni successive. [Struttura statica AB-131](docs/communication-topology.md#ab-131--struttura-della-terza-chiamata). Prossima priorità: ricostruire l'helper 0x13a3ac e le 26 voci a 0x1caabc per determinare sorgenti, destinazioni e limiti delle copie. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-132 · 11 novembre 2026

Ricostruita staticamente la preparazione delle due tabelle e confrontati tutti i 26 descrittori con il campione originale. [Evidenze AB-132](docs/communication-topology.md#ab-132--lettura-delle-due-sorgenti-e-descrittori). Prossima priorità: eseguire l'helper con record sintetici validi, lunghezza discordante, marcatore 0xaa55 e terminatore 0xffff e verificare i puntatori prodotti per entrambi i selettori. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-133

Trentadue prove verificano la raccolta dei puntatori, il rifiuto della lunghezza discordante, l'allineamento del record dispari e la pubblicazione tramite marcatore per entrambi i selettori. [Evidenze AB-133](docs/communication-topology.md#ab-133--parser-dei-record-su-sorgenti-controllate). Prossima priorità: collegare le due invocazioni originali nel chiamante e verificare la scelta delle sorgenti e le copie fino a 0x13a278. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-134

Otto scenari collegano la terza CALL, entrambi i parser e il ciclo su 26 voci. Verificate priorità modulo 65536, sorgente alternativa e segnalazione delle voci mancanti. [Evidenze AB-134](docs/communication-topology.md#ab-134--scelta-della-sorgente-e-copia-dei-record). Prossimo obiettivo: seguire i primi rami da 0x13a278 variando la presenza dei tipi 1 e 2. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-135

Trentadue prove verificano le quattro combinazioni dei flag 1/2, due modifiche nel blocco tipo 1 e il primo ingresso nell'helper 0x13a802. [Evidenze AB-135](docs/communication-topology.md#ab-135--rami-controllati-dai-flag-1-e-2). Prossima priorità: analizzare l'helper 0x13a802 e verificare il ciclo di quattro invocazioni con flag 2 impostato. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-136

Sedici prove confermano il ramo controllato dal flag 2 e le 32 copie originali di 19 byte, con confronto dell'intero blocco tipo 2. [Evidenze AB-136](docs/communication-topology.md#ab-136--quattro-copie-dei-valori-interni-per-il-tipo-2). Prossima priorità: seguire le operazioni da 0x13a2b6 e ricostruire il ramo controllato dal flag 3 e l'helper 0x13a814. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-137

Sedici prove verificano le maschere intermedie e le quattro copie di 35 byte controllate dal flag 3. [Evidenze AB-137](docs/communication-topology.md#ab-137--valori-interni-per-il-tipo-3-e-maschere-intermedie). Prossima priorità: verificare i rami dei flag 9, 10 e 11 a partire da 0x13a2f4. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-138

Sessantaquattro prove verificano gli azzeramenti controllati dai flag 9/10/11, con guardie, flag e stack confrontati. [Evidenze AB-138](docs/communication-topology.md#ab-138--azzeramenti-controllati-dai-flag-9-10-e-11). Prossima priorità: ricostruire il ciclo da 0x13a336 per i flag 12 e successivi e verificarne gli intervalli di scrittura. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-139

Centoquattro prove verificano il ciclo sui flag 12–20, gli azzeramenti di 64 byte in record distanziati di 578 byte e il confine finale del ciclo. [Evidenze AB-139](docs/communication-topology.md#ab-139--nove-azzeramenti-indicizzati-dai-flag-1220). Prossima priorità: identificare gli accessi della chiamata 0x1ad966 controllata dal flag 23 e la copia da 0x8f6e3f2c. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-140

Sedici prove verificano l'helper controllato dal flag 23 e la successiva copia di 624 byte, con confronto di sorgente, destinazione, guardie, tipo 1 e stack. [Evidenze AB-140](docs/communication-topology.md#ab-140--inizializzazione-del-tipo-23-e-copia-successiva). Prossima priorità: analizzare 0x1ae4c0, controllata dal flag 24, prima di chiudere la terza chiamata. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-141

Sedici prove verificano l'azzeramento controllato dal flag 24, la scrittura finale e il ritorno originale al candidato a 0x1000e2, con ripristino dei registri salvati. [Evidenze AB-141](docs/communication-topology.md#ab-141--flag-24-e-ritorno-della-terza-chiamata). Prossima priorità: eseguire la terza chiamata dall'ingresso al ritorno su record sintetici coerenti, evitando la reimpostazione intermedia dei flag. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-142

Otto esecuzioni continue della terza chiamata dal candidato verificano il ritorno a 0x1000e2 e tutti i 26 blocchi, i flag e la copia del tipo 23, con due configurazioni complementari di record sintetici. [Evidenze AB-142](docs/communication-topology.md#ab-142--terza-chiamata-continua). Prossima priorità: identificare destinazione e dipendenze della quarta chiamata e verificare quali dati prodotti fin qui vengono letti. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-143

Identificata la quarta chiamata 0x13b7f6 e il parser di cinque coppie puntatore/lunghezza. [Evidenze statiche AB-143](docs/communication-topology.md#ab-143--quarta-chiamata-e-parser-a-cinque-voci). Prossima priorità: verificare le basi ROM e predisporre record sintetici per i limiti del parser e la scelta delle copie. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-144

Otto esecuzioni della quarta chiamata verificano priorità, copie alternative, lunghezze zero e massima, due casi di arresto del parser e ritorno a 0x1000ea. [Evidenze AB-144](docs/communication-topology.md#ab-144--quarta-chiamata-continua-e-limiti-dei-record). Prossima priorità: identificare la quinta chiamata e verificare eventuali letture dei blocchi già preparati. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-145

La quinta chiamata 0x112008 prepara due teste di lista e collega due elementi, poi raggiunge INT 0x40 con servizio -23. [Evidenze statiche AB-145](docs/communication-topology.md#ab-145--quinta-chiamata-e-nuovo-confine-int). Prossima priorità: eseguire il prefisso delle liste fino a 0x112014, verificando i collegamenti e lo stato utente al confine INT. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-146

Otto prove verificano entrambe le teste, i due elementi collegati e lo stato al confine INT della quinta chiamata. [Evidenze AB-146](docs/communication-topology.md#ab-146--liste-della-quinta-chiamata-e-confine-utente). Prossima priorità: caratterizzare l'ingresso INT 0x40 da S=1 e la selezione del servizio -23, con frame supervisore esplicito. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-147

Otto prove raggiungono 0x414da per il servizio -23 dal confine AB-146 con ingresso INT utente sintetico e dispatcher originale. [Evidenze AB-147](docs/communication-topology.md#ab-147--ingresso-utente-modellato-e-selezione-del-servizio--23). Prossima priorità: identificare gli accessi del servizio e gli esiti per R4=28, R5=0 prima di proseguirne l'esecuzione. Stima complessiva circa 45%.

### Aggiornamento tecnico AB-148

L'analisi statica del servizio -23 identifica il descrittore 28 a 0x5c704 e il percorso che, se il controllo a D+8 lo consente, prepara un contesto e chiama l'inserimento in lista 0x4186e. L'esito è compatibile con una lista selezionata dal valore 13, ma non prova che il task venga eseguito. [Evidenze AB-148](docs/communication-topology.md#ab-148--mappa-statica-del-servizio-os--23). Prossima priorità: emulare il servizio con argomenti 28/0 su regioni sintetiche, verificando rami e scritture senza attribuire effetti fisici agli indirizzi. La stima complessiva resta circa 45%: questa fase aggiunge una mappa statica, non copertura hardware o avvio completo.
