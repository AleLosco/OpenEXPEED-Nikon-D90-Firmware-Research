Warning: truncated output (original token count: 56042)
Total output lines: 1367

# Destinatario del trasporto B: confronto e inventario

Analisi del 3 novembre 2026, campione D90_0101.bin identificato dagli hash del report. Nessuna prova sulla fotocamera. Gli originali sono stati soltanto letti.

## Risultato

Il destinatario fisico del trasporto B sulla porta 0x64 resta sconosciuto. Il confronto con Nikon Hacker non permette di chiamarlo né firmware A né ponte del sensore EI155. Offre invece un controesempio concreto all’uso del numero del canale per identificare un dispositivo fra modelli diversi.

## Confronto esterno riproducibile

Tutte le fonti seguenti sono fissate alla revisione `89ad7354b6d721260b3e999061fefc51f3580ee0` di `simeonpilgrim/nikon-firmware-tools`. Sono documentazione e implementazioni della comunità, non schemi Nikon verificati su questa fotocamera.

- [EmulationFramework.java](https://github.com/simeonpilgrim/nikon-firmware-tools/blob/89ad7354b6d721260b3e999061fefc51f3580ee0/Java/Emulator/src/main/com/nikonhacker/emu/EmulationFramework.java): costruisce un pannello D5100, un sensore IMX071 e due CPU FR/TX. Collega FR CSIO0 a EI155 e FR CSIO5 al canale TX HSC0. Il collegamento EI155 usa la selezione sul pin P15 e un segnale verso l’interrupt esterno 0x10. Questi sono collegamenti del modello implementato; non sono una mappa della D90.
- [ExpeedIoListener.java](https://github.com/simeonpilgrim/nikon-firmware-tools/blob/89ad7354b6d721260b3e999061fefc51f3580ee0/Java/Emulator/src/main/com/nikonhacker/emu/memory/listener/fr/ExpeedIoListener.java): il modello colloca i registri del canale seriale 0 a 0x60–0x67, con dati a 0x64 e passo di 16 byte fra canali. La corrispondenza degli indirizzi con B è utile, ma non dimostra la corrispondenza del cablaggio.
- [Ei155.java](https://github.com/simeonpilgrim/nikon-firmware-tools/blob/89ad7354b6d721260b3e999061fefc51f3580ee0/Java/Emulator/src/main/com/nikonhacker/emu/peripherials/serialInterface/sensorBridge/Ei155.java): l’implementazione accetta un identificatore 0x45, seguito da un indirizzo e da tre byte di valore. Non implementa lo scambio con indice, lunghezza e somma totale 255 di AB-34. Un primo byte 0x46 non seleziona il suo ramo supportato. Questo esclude l’equivalenza diretta con quell’implementazione, non la presenza di un ponte del sensore diverso sulla D90.
- [arch/d90.yaml](https://github.com/simeonpilgrim/nikon-firmware-tools/blob/89ad7354b6d721260b3e999061fefc51f3580ee0/arch/d90.yaml): distingue le schede CM e DG, collegate da un connettore a 39 pin, e una scheda sensore collegata a DG. Le attribuzioni dei processori ad A e B hanno punti interrogativi nella fonte. Manca l’associazione fra singoli segnali del connettore e i registri studiati.
- [camera_matrix.csv](https://github.com/simeonpilgrim/nikon-firmware-tools/blob/89ad7354b6d721260b3e999061fefc51f3580ee0/wiki/cameras/camera_matrix.csv): l’intestazione colloca MB91F637 nella colonna `proc_b` della D90 e lascia `proc_a` vuota. È un’attribuzione esterna da verificare, non un’identificazione acquisita dal nostro campione. Non usare il solo nome per assegnare un manuale hardware a uno dei due componenti.

Non è stato eseguito l’emulatore esterno. Il confronto riguarda il sorgente e non una cattura dei segnali della D90.

## Inventario direttamente dal campione

Il costruttore B a 0x10ff08 confronta il byte iniziale del peer con 0x47 a 0x10ff3c. Il blocco a 0x10ff86 carica la base 0x1c3fa4 e moltiplica l’indice per 16. Le 71 voci 0x00–0x46 occupano l’intervallo [0x1c3fa4, 0x1c4414). I 16 byte successivi precedono la tabella degli stati a 0x1c4424 e non sono inclusi nell’inventario. La voce zero è inclusa nell’inventario ma non è per questo un comando valido: zero nel messaggio iniziale seleziona un percorso differente. Il confronto sul peer non prova un controllo universale su tutti gli indici richiesti localmente.

Ogni record contiene quattro parole a 32 bit big endian: puntatore dati, campo dimensione, tipo e callback. Il nuovo campo `b_transaction.descriptors` del report conserva questi metadati per tutte le voci 0x00–0x46, senza includere i contenuti dei buffer.

| Riscontro | Conteggio |
|---|---:|
| Tipo 0 | 41 |
| Tipo 1 | 25 |
| Tipo 2 | 5 |
| Indirizzi callback distinti | 41 |
| Voci con campo dimensione zero | 5 |

Le voci con dimensione zero sono 0x00, 0x0f, 0x1d, 0x3f e 0x44. Non vengono scartate: 0x1d ha callback 0x111216, diversa dal semplice ritorno a 0x11046c. La dimensione massima memorizzata è 255. Una dimensione nel descrittore non equivale automaticamente alla lunghezza di ogni messaggio trasmesso: il costruttore distingue i tipi e per il tipo 0 imposta a zero il conteggio TX in questo percorso.

## Prossima pista nel codice

La voce 0x33 ha buffer 0x8f52edf6, dimensione 3 e tipo 1. La voce 0x34 ha buffer 0x8f52edf5, dimensione 1, tipo 0 e callback 0x11151c. La lettura statica di questa callback mostra confronti fra i due bit bassi dei due buffer e chiamate alla routine di notifica 0x110412 con R4=0x33. Le chiamate esterne a 0x1011b0 e 0x10a806 e l’evento finale richiedono ulteriore analisi.

È una pista concreta per studiare uno scambio correlato fra due voci, più informativa del solo byte 0x46. Non è ancora una prova emulativa, una spiegazione del significato fotografico o l’identificazione del peer. Il prossimo esperimento deve isolare questi rami e documentare i punti di arresto prima delle chiamate non comprese. Modello consigliato: Astra Medio.

## Verifiche di questa fase

L’analizzatore verifica gli hash dei due campioni, le istruzioni che definiscono limite/base/passo e produce l’inventario. La rigenerazione conserva tutti i precedenti risultati del report: cambiano soltanto i quattro nuovi campi relativi alla tabella. La decodifica Ghidra ha verificato i blocchi citati; non sono state aggiunte esecuzioni emulative del protocollo. Nessuna pubblicazione remota.

## AB-39: verifica delimitata della callback 0x34

`ghidra/D90BCallback34.java` esegue le istruzioni originali da 0x11151c fino al primo ingresso in una funzione esterna, oppure fino all’INT a 0x1115b8 senza eseguirlo. La tabella seguente usa soltanto i due bit bassi del primo byte del buffer 0x33 (`a`, indirizzo 0x8f52edf6) e del byte 0x34 (`b`, indirizzo 0x8f52edf5).

| a & 3 | b & 3 = 0 | b & 3 = 1 | b & 3 = 2 | b & 3 = 3 |
|---|---|---|---|---|
| 0 | Evento | Aggiorna a, poi 0x1011b0 | Evento | Notifica 0x33 |
| 1 | Aggiorna a, poi 0x1011b0 | Evento | Evento | Evento |
| 2 | 0x10a806 | 0x10a806 | 0x10a806 | Evento |
| 3 | 0x10a806 | 0x10a806 | 0x10a806 | Evento |

Nel caso a&3=0, b&3=1, il nuovo byte è `(a & 0xfc) | 1`. Nel caso a&3=1, b&3=0, BANDL produce `a & 0xf8`: cancella anche il bit 2, non soltanto i due bit usati nel confronto. La notifica raggiunge 0x110412 con R4=0x33. Nei percorsi che arrivano direttamente all’evento, il codice imposta il bit 0x20 a 0x8f530e1e e prepara R4=0x0c, R5=0x800 prima dell’INT.

Sono state provate tutte le 16 combinazioni dei due bit bassi, ciascuna con quattro configurazioni dei bit alti per ogni buffer (0x00, 0x54, 0xa8, 0xfc): 256 casi. I punti di arresto sono stati 32 volte 0x1011b0, 16 volte 0x110412, 96 volte 0x10a806 e 112 volte 0x1115b8. I controlli includono il buffer b invariato, i due byte successivi di a invariati, i registri della notifica/evento e il flag inizializzato a 0x85. Il test controlla inoltre il puntatore della callback nella voce 0x34.

RAM, stack e ingressi sono sintetici. La prova non copre tutti i possibili byte (256 combinazioni su 65.536), il trasporto precedente, l’esecuzione delle funzioni esterne, il ritorno finale o gli effetti fotografici. Nei percorsi fermati su una chiamata non si conclude che l’evento successivo non avverrà: il codice dopo quella chiamata non è eseguito. Nessuna funzione esterna viene simulata con un ritorno inventato.

Il confronto delle istruzioni con altri moduli GitHub è descritto in [github-projects.md](related-projects.md). Il prossimo passo è studiare 0x1011b0 e 0x10a806 e trovare dove vengono inizializzati i due buffer; modello consigliato: Astra Medio.

Le 256 prove sono passate sia sul programma di lavoro sia dopo una nuova importazione B in un progetto Ghidra temporaneo, senza WARN, ERROR o eccezioni nei log. È una riproduzione sullo stesso host, non una convalida indipendente su un’altra macchina.

## AB-40: effetti delle due chiamate e controllo dello stato

La funzione 0x10a806 contiene un caricamento del puntatore 0x8f52e35c, un BORH con immediato 8 e un ritorno. Quindi imposta il bit 0x80 di quel byte, preservando gli altri. `D90BCallback34Effects.java` estende le prove AB-39 eseguendo questa funzione originale e il suo ritorno nella callback, senza sostituirla con un modello. Nei 96 casi che la raggiungono, il byte preparato a 0x25 diventa 0xa5 e il percorso prosegue fino all’evento a 0x1115b8. Negli altri casi quel byte resta 0x25.

La funzione 0x1011b0 prepara invece un INT 0x40 a 0x1011b8 con R4=0x15 e R12=-53. Il seguito statico chiama 0x1011ce, poi prepara un secondo INT con R4=0x15 e R12=-55 e restituisce il risultato conservato. La struttura è compatibile con una protezione tramite servizi del sistema operativo, ma qui non sono stati identificati formalmente gli oggetti o verificati gli effetti dei servizi. Il test si ferma prima del primo INT, senza presumere che riesca.

La prova estesa conserva i 256 ingressi di AB-39: 32 arresti al primo INT della funzione 0x1011b0, 16 all’ingresso della notifica 0x110412, 96 all’evento dopo il ritorno reale da 0x10a806 e 112 direttamente all’evento. Controlla anche buffer, registri e flag. Non esegue il sistema operativo né l’intera transazione seriale.

### Funzione interna 0x1011ce

`D90BStateGate.java` entra direttamente in questa funzione con RAM sintetica e si arresta prima delle chiamate 0x106ea2 o 0x106e14. Questo ingresso isolato non dimostra che il wrapper abbia acquisito alcuna risorsa.

- Se la mezza parola a 0x8f549e1e è diversa da zero, prepara R4=0x800 per 0x106ea2 senza modificare il byte della voce 0x33 nel tratto provato.
- Se quella mezza parola è zero, i bit 0–1 della voce 0x33 sono zero e il bit 0x80 a 0x8f52e348 è spento, cancella il bit 0x04 della voce 0x33, azzera il byte a 0x8f52e0ec e raggiunge 0x106e14 con R4=0xfffff7ff.
- Un altro ramo confronta le parole a 0x8f87156c e 0x8f871570 e il valore a 0x8f871538. Quando le prime sono uguali e il terzo vale 2, prepara la stessa cancellazione e la chiamata 0x106e14.
- Nel percorso con parole diverse, se i bit 0–1 della voce 0x33 valgono 1 imposta anche il bit 0x04; raggiunge poi 0x106ea2 con R4=0x800. Per gli altri valori bassi, il byte resta invariato prima della chiamata.

Le 32 prove combinano quattro scenari, tutti i quattro valori dei bit 0–1 e bit 0x04 inizialmente spento/acceso. Mantengono a zero il byte a 0x8f534191 per evitare un ulteriore ramo con interrogazione del sistema operativo a 0x101254: quel ramo non è coperto. La prova osserva la preparazione delle maschere; non assegna ancora un significato alle funzioni che le ricevono né ai bit dello stato.

### Preparazione dei buffer: risultato parziale

La ricerca di riferimenti ha individuato un aggiornamento a 0x10b48a: legge i bit 0–1 della voce 0x34 e scrive nei bit 0–1 della voce 0x33 zero se entrambi sono zero, altrimenti uno, preservando gli altri bit. Chiama poi 0x1011b0. Questo blocco è stato letto staticamente, non eseguito nei nuovi test.

Più avanti, il codice a 0x10b4c4 scorre dieci elementi con passo quattro, selezionati dal byte a offset +2 di ciascun elemento. Accumula prodotti di mezze parole con segno e pesi decrescenti da 10 a 1, accumulando anche i pesi. Passa le due somme a 0x1bbf12; dopo il ritorno salva i due byte bassi del risultato nella voce 0x33, prima il byte alto e poi quello basso, e notifica l’indice 0x33. La funzione aritmetica e i casi senza elementi selezionati restano da verificare. Non si conclude che questo valore rappresenti esposizione, temperatura o un’altra grandezza fotografica.

Gli indirizzi dei buffer compaiono anche in record a tre parole: indice, puntatore e dimensione (0x1d2cc8 per la voce 0x33 e 0x1d2cd4 per 0x34). Questi record non contengono di per sé i valori iniziali. La ricerca non ha ancora identificato l’inizializzazione completa all’avvio: non bisogna confonderla con gli aggiornamenti durante il funzionamento.

### Riproducibilità e seguito

Eseguire i due nuovi script sul programma B importato come indicato in CONTRIBUTING. Le prove sono state riprodotte in un nuovo progetto Ghidra sullo stesso host. Il report strutturale precedente resta invariato. Gli originali sono stati solo letti.

Prossimo passo: seguire le funzioni 0x106ea2/0x106e14 e chi consuma il bit 0x80 a 0x8f52e35c; quindi verificare l’origine degli elementi preparati da 0x10b4c4. L’inizializzazione all’avvio e il destinatario fisico restano aperti. Modello consigliato: Astra Medio.

## AB-41: maschere, registro hardware e lettori del bit 0x80

Le funzioni 0x106e14 e 0x106ea2 ricevono in R4 una maschera. Dopo la preparazione di un servizio OS con oggetto 0x16, lavorano sulla stessa parola a 0x8f52e484. Il primo percorso applica AND, il secondo OR. Con i parametri osservati in AB-40, cancellano oppure impostano il bit 0x800. La parola a 0x8f52e480 viene letta come seconda maschera per decidere il seguito; il nome funzionale di queste parole non è ancora stabilito.

Indicando con S il nuovo valore a 0x8f52e484 e con M quello a 0x8f52e480:

| Percorso | S & M | Effetto su 0x50000106 | Seguito verificato fino all’ingresso |
|---|---|---|---|
| AND | zero | Cancella bit 0x02 | 0x106f14 con R4=0 |
| AND | diverso da zero | Nessuna scrittura in questo tratto | 0x106f48 |
| OR | zero | Nessuna scrittura in questo tratto | INT a 0x106ed8 con R4=0x16, R12=-55 |
| OR | diverso da zero | Imposta bit 0x02 | 0x106f48 |

È importante la differenza fra cancellare un bit e non scriverlo: il percorso OR con intersezione zero conserva il valore precedente del registro. Il registro è lo stesso indirizzo già incontrato nel trasporto B, ma qui il bit è 0x02. Questa coincidenza non identifica una linea fisica o il destinatario del trasporto.

`D90BMaskConsumers.java` verifica 80 casi: due percorsi, cinque valori iniziali della parola (0, 0x800, 0x100, 0x900, 0xffffffff), quattro seconde maschere (0, 0x800, 0x100, 0x900) e due valori del registro (0x55, 0x57). Gli ingressi includono altri bit attivi per verificare che vengano preservati. Il test parte **dopo** l’INT di acquisizione, a 0x106e28 oppure 0x106eb6, con R5 e R6 preparati. Non simula acquisizione/rilascio OS, concorrenza o effetti elettrici del registro. Si arresta prima delle funzioni successive o del secondo INT.

### Legame statico con la voce 0x0b

La funzione 0x106f14 confronta R4 con il bit 0x04 del byte a 0x8f52f1bb. Se deve cambiarlo, lo aggiorna e chiama 0x110412 con R4=0x0b; altrimenti ritorna. Quel byte è interno al buffer della voce 0x0b già inventariata. La funzione 0x106f48 chiama questo aggiornamento con zero, esamina anche il bit 0x800 della parola di stato e, in altri rami, può richiederlo con uno. Questi seguiti sono stati letti staticamente, non eseguiti dalle 80 prove. Non viene quindi dichiarata una trasmissione completa della voce 0x0b o una funzione fotografica identificata.

### Due lettori verificati del bit a 0x8f52e35c

Nella funzione che inizia a 0x10a810, R8 viene caricato con 0x8f52e35c. Sono stati selezionati due blocchi che leggono il bit 0x80 impostato da 0x10a806:

- A 0x10aec6, bit spento: prepara R4=5 e R5=0x2000 e raggiunge l’INT a 0x10aed4. Bit acceso: salta quel tratto e raggiunge 0x10aee2.
- A 0x10b062, bit spento: prosegue a 0x10b0d4. Bit acceso e bit 0x40 spento: chiama 0x108624. Entrambi accesi: salta a 0x10b3ce.

Otto prove usano i byte 0x25, 0x65, 0xa5 e 0xe5 nei due blocchi, preparando R8 esplicitamente e fermandosi ai punti indicati. Controllano il ramo, la conservazione del byte e gli argomenti dell’evento quando preparato. Non eseguono la funzione intera e non dimostrano che tutti questi stati siano raggiungibili sulla fotocamera. Gli effetti di 0x108624 e del blocco a 0x10b3ce restano aperti.

La scansione dei riferimenti al puntatore ha prodotto molti altri candidati: la copertura dei consumatori non è completa. Le due prove chiariscono conseguenze locali del bit, senza chiamarlo arbitrariamente avvio, arresto o errore di una funzione fotografica.

### Verifica e prossima domanda

Gli 80 casi sulle maschere e gli otto sui lettori sono passati nel progetto di lavoro e dopo una nuova importazione B sullo stesso host, senza errori o avvisi di memoria non inizializzata nei log. Il report strutturale e gli hash di riferimento restano invariati.

La prossima fase deve verificare 0x106f14/0x106f48 fino alla notifica 0x0b e seguire 0x108624/0x10b3ce per capire quale operazione viene selezionata dal bit 0x80. Inizializzazione completa, significato fotografico e cablaggio restano irrisolti. Modello consigliato: Astra Medio.

## AB-42: notifica 0x0b e sequenza di scritture hardware

### Aggiornamento e notifica

`D90BMessage0B.java` verifica la funzione originale 0x106f14 per tutti i 256 valori del byte a 0x8f52f1bb, con richiesta R4=0 oppure R4=1. Se il bit 0x04 ha già il valore richiesto, la funzione ritorna senza notificare. Altrimenti aggiorna soltanto quel bit e raggiunge 0x110412 con R4=0x0b. I 512 casi controllano anche che gli altri quattro byte del buffer di cinque byte a 0x8f52f1ba restino invariati. Il test non chiama la routine di notifica, quindi non dimostra l’accodamento o la trasmissione fisica del messaggio.

Altri 96 casi entrano nella funzione 0x106f48, eseguendo anche le chiamate reali a 0x106f14 fino alla prima notifica oppure al ritorno. La funzione richiede anzitutto di spegnere il bit. Se questo è già spento, può poi richiederne l’accensione quando:

- il bit 0x800 della parola a 0x8f52e484 è spento;
- il byte a 0x8f52e4ab è diverso da zero;
- la parola vale esattamente 1, oppure vale esattamente 0x41 e almeno uno dei bit 0x04/0x08 a 0x8f52f1aa è acceso.

Le prove combinano le parole 0, 1, 0x41, 0x800, 0x801, 0x841, il byte di abilitazione 0/1, i valori 0/4/8/12 dell’altro ingresso e il bit iniziale spento/acceso. Se il bit iniziale è acceso, l’arresto avviene sulla prima notifica di spegnimento: non si presume il successivo ritorno dalla notifica, né si dichiara che non possa seguire un’accensione. Il termine «abilitazione» descrive qui il ruolo del byte nel ramo, non una funzione fotografica identificata.

### Funzione 0x108624

È una funzione senza chiamate esterne che scrive nelle periferiche mappate in memoria. La tabella ROM a 0x1bf33c contiene le quattro basi 0x4000d200, 0x4000d300, 0x4000d400 e 0x4000d500. Il test usa la tabella originale e ne verifica i valori.

| Destinazione | Ampiezza | Valore finale |
|---|---:|---:|
| Ciascuna delle quattro basi, offset +0x10 | 32 bit | 0 |
| Ciascuna delle quattro basi, offset +0 | 16 bit | 0x4000 |
| Ciascuna delle quattro basi, offset +0x40 | 16 bit | 0x0800 |
| 0x4000d800 e 0x4000d900 | 16 bit | 0x4000 |
| 0x4000d840 e 0x4000d940 | 16 bit | 0 |
| 0x4000c010 | 16 bit | 0xc000 |
| 0x4000c210 | 16 bit | 0x8100 |
| 0x4000cc80 | 16 bit | 0 |
| 0x40000010 | 16 bit | 0x2200 |

`D90BHardwareSequence.java` esegue la funzione fino al ritorno reale, incluso lo store nello slot ritardato del RET. Due prove partono da memoria riempita rispettivamente con 0xa5 e 0x5a. Verificano i valori finali e la conservazione degli altri byte nelle finestre controllate (0x44 byte per ciascuna delle sei basi e quattro byte per ciascuno degli ultimi quattro indirizzi). Non registrano ogni scrittura fuori da tali finestre e non simulano gli effetti dei dispositivi: i registri sono rappresentati da semplice memoria. Il risultato prova quali valori produce il codice, non che sensore, motori o circuiti fotografici assumano uno stato sicuro o noto.

### Percorso 0x10b3ce

Quattro prove delimitate verificano il suo ingresso con byte a 0x8f52e360 pari a 0x25, 0x65, 0xa5 o 0xe5. Quando il bit 0x40 è acceso, cancella sia 0x40 sia 0x80 e raggiunge 0x1077ca con R4=0x15. Quando 0x40 è spento, conserva il byte e raggiunge 0x1a1e40. Le prove si fermano prima di quelle chiamate; 0x8f52e360 è quattro byte dopo il flag studiato in AB-41, non lo stesso byte.

Il seguito statico contiene una cancellazione del bit 0x04 a 0x8f52f1bd e una notifica 0x0b, in un ramo successivo a un servizio OS. Più avanti compare anche una chiamata a 0x108624. Pertanto i due percorsi individuati in AB-41 possono raggiungere la stessa sequenza hardware in momenti differenti; non sono stati identificati come due operazioni fotografiche indipendenti. Il percorso completo di 0x10b3ce non è stato emulato.

### Limiti e seguito

Una ricerca mirata del testo `4000d200` nel repository Nikon Hacker non ha restituito risultati. È un esito limitato a quella query, non prova che il progetto non documenti il dispositivo con un altro nome o indirizzo simbolico. Nessuna attribuzione hardware è stata importata da altri modelli.

Le nuove prove sono passate sia nel progetto di lavoro sia dopo una nuova importazione B sullo stesso host, senza ERROR, WARN o eccezioni nei log. Non si tratta di una verifica indipendente su un’altra macchina. La prossima domanda è il ruolo dei registri 0x4000d2xx–0x4000d5xx e degli ingressi che consentono l’accensione del bit del messaggio 0x0b. Servono riscontri sulla periferica e sui chiamanti, non ulteriori combinazioni dello stesso test. Modello consigliato: Astra Medio.


## Ricognizione successiva: ingressi del controllore 0x0b

Lo script `ghidra/D90InputReferences.java`, eseguito sul programma B già importato con `-readOnly -noanalysis`, cerca operandi scalari uguali a 0x8f52e4ab, 0x8f52f1aa e alle quattro basi 0x4000d200–0x4000d500 (passo 0x100). La ricerca riguarda esclusivamente le istruzioni già presenti nel listing: non è un inventario completo del firmware e non trova automaticamente indirizzamenti indiretti o basi memorizzate in tabelle.

Sono emersi cinque caricamenti immediati: 0x1011e8, 0x106f4a, 0x10af9a e 0x10b57e caricano 0x8f52f1aa; 0x106f6c carica 0x8f52e4ab. Nei contesti stampati, 0x106f72 legge il secondo byte con LDUB. I test BTSTH a 0x10afa0 e 0x10b584 esaminano il nibble alto del primo byte con immediato 4; non vanno confusi con i bit bassi 0x04/0x08 del controllore già studiato. Nessuno di questi soli caricamenti identifica un produttore del dato o una funzione fotografica.

Nessun operando scalare corrispondente alle quattro basi hardware è emerso in questa scansione del listing. AB-42 documenta già quelle basi in una tabella ROM: l’esito non nega pertanto gli accessi hardware. Ghidra non associa i cinque punti a funzioni nel database corrente (`getFunctionContaining` restituisce null). Non si deducono da ciò confini di funzione.

Il prossimo passo resta trovare le scritture agli ingressi, includendo accessi tramite base e spiazzamento, prima di attribuire significati. Questa ricognizione è statica e non aggiunge casi emulativi né identifica la periferica.

## AB-43 · Due produttori del byte di abilitazione

Sul campione B di AB-1 (indirizzi CPU), due STB a 0x101cc4 e 0x101d06 scrivono 1 a 0x8f52e4ab. Il controllore di AB-42 legge questo stesso indirizzo a 0x106f72. Chi esegue una delle due scritture soddisfa quindi una delle precondizioni del controllore.

| Ingresso del blocco | Comportamento osservato | Arresto e copertura |
|---|---|---|
| 0x101ca2 | Scrive 1 quando il nibble basso di [0x8f52f1e9] è zero, R4 vale zero e R8 è diverso da zero. Negli altri casi provati conserva 0x33. | 0x101cc6 oppure 0x101d84. 36 combinazioni di byte 00/f0/01/11, R4 pari a 0/1/ffffffff e R8 pari a 0/1/80000000 (esadecimale). |
| 0x101ce6 | Tutti i percorsi provati scrivono 1. R8 e R9 conservano i valori iniziali quando il bit 0x20 di [0x8f52f212] è acceso e il bit 0x40 di [0x8f52f1bb] è spento. Altrimenti entrambi diventano zero. | 0x101d08. Otto combinazioni dei due bit e del flag iniziale 0/255. |

Per riprodurre, eseguire [D90EnableGate.java](../ghidra/D90EnableGate.java) sul programma B importato come in [CONTRIBUTING.md](../CONTRIBUTING.md). Lo script controlla anche i due byte adiacenti e l'indirizzo di arresto. Il progetto locale in sola lettura ha riportato 36 e 8 casi superati. Gli ingressi diretti con RAM e registri sintetici delimitano la prova ai due blocchi. Restano da stabilire l'origine dei valori di R4/R8 all'ingresso, quando la fotocamera percorre questi rami e chi azzera il flag.

## AB-44 · Provenienza di R8 e percorsi che producono R4 prima di AB-43

Indirizzi CPU del componente B, campione AB-1. Nel percorso da 0x101b88, il bit 0x80 del byte 0x8f52e348 seleziona la parola caricata in R8. Con il bit spento si legge 0x8f87156c, con il bit acceso 0x8f871570. Il listato contiene un decremento condizionale a 0x101bc4 e uno shift aritmetico a destra di un posto a 0x101bd0 controllato dal bit 0x80 a 0x8f52f1de. Il decremento richiede il bit 0x10 a 0x8f52f1bb, R8 positivo e il risultato non nullo della chiamata a 0x105b0a. Più avanti il test dello stesso byte a 0x101c62 devia dal primo ramo di AB-43 quando il bit 0x10 è acceso. La presenza di chiamate intermedie impedisce di assumere senza verifica che i due test leggano lo stesso stato.

A 0x101c92 il codice chiama 0x14e27e. Questa prepara due argomenti dai byte 0x8f52f1d9 (sette bit bassi, con rimappatura opzionale tramite tabella 0x1c715c) e 0x8f52f1da (tre bit alti). Il callee 0x14e2f2 invoca INT 0x40 a 0x14e310. Dopo il servizio può produrre zero oppure passare i due argomenti a 0x14e34c e usare il suo risultato. Tornati al chiamante, con R10 maggiore di 1 secondo il confronto signed a 0x101c94, R4 passa inoltre alla routine di divisione unsigned 0x1bc31c con R5=R10. Questa contiene DIV0U, DIV1 ripetuta e il trasferimento di MDL in R4 nel delay slot del ritorno. Il valore confrontato in AB-43 dipende quindi anche da questo passaggio.

[D90GateOrigins.java](../ghidra/D90GateOrigins.java) ha superato 40 casi nel progetto B in sola lettura. La prova entra a 0x14e316 con esito del servizio e parola locale sintetici. Il codice raggiunge 0x14e34c con gli argomenti attesi quando lo stato è zero, la parola locale ha il bit 0x04, la parola a 0x8f5340dc è zero ed entrambi i bit 0x04/0x08 a 0x8f52f1aa sono spenti. Negli altri casi provati raggiunge 0x14e344 con R4 zero. Il bit 0x40 da solo è compatibile con la chiamata. I cinque valori provati per il byte sono 0, 4, 8, 12 e 64. Il servizio OS, il corpo di 0x14e34c e la divisione restano fuori dalla prova. La provenienza iniziale di R8 e il percorso dopo il ritorno sono osservazioni statiche. Resta da identificare chi aggiorna le due parole sorgente e quale evento fisico rappresentino.

## AB-45 · Inizializzazione e aggiornamento delle parole lette da R8

Nel componente B del campione AB-1, la funzione CPU 0x1012ce prepara le due parole di AB-44. A 0x101358 legge il byte alla base 0x8f52f231 più 0x14. I due shift isolano il bit 0x20. L'indice 0/1 seleziona nella tabella ROM 0x1bec94 rispettivamente 5 o 3. La store a 0x101370 scrive il valore a 0x8f871570. La funzione conserva lo stesso valore sullo stack, lo usa come limite del ciclo a 0x1013b6 e a 0x101428 lo scrive a 0x8f87156c. Gli elementi preparati nel ciclo distano 0x3204 byte.

[D90WordSetup.java](../ghidra/D90WordSetup.java) esegue le istruzioni dal prologo fino al ritorno, incluse le chiamate interne, nel progetto B in sola lettura con stack e RAM sintetici. I quattro ingressi esadecimali 00, 20, df, ff a 0x8f52f245 hanno prodotto rispettivamente 5, 3, 5, 3 iterazioni e lo stesso numero in entrambe le parole a 32 bit. La verifica controlla le due destinazioni preimpostate a ffffffff e il raggiungimento del ritorno. Non simula l'avvio della fotocamera.

Nel disassemblato successivo, 0x101454 può decrementare la parola 0x8f87156c (store nei delay slot a 0x101482 e 0x1014a8) sui due rami che chiamano 0x105b5c. Le routine 0x1014b2 e 0x1014e2 la incrementano a 0x1014d8 e 0x101508 dopo la chiamata a 0x105b16. Questo distingue un conteggio che può variare dal valore comune di inizializzazione. Le quattro prove coprono la preparazione, mentre incrementi e decrementi sono riscontri statici. Non è stata dimostrata l'immutabilità della seconda parola. Resta da seguire chi richiede o restituisce questi elementi e quale risorsa rappresentino.

## AB-46 · Prelievo di un elemento e scelta della lista di restituzione

Nel componente B del campione AB-1, il chiamante a 0x102102 preleva un elemento con 0x101454. A partire dallo stato prodotto da AB-45, riceve in R4 il puntatore 0x8f8715bc. Salva il puntatore in R8, scrive 3 nel byte +0x4f e collega l'elemento alla lista con testa 0x8f871594 mediante 0x105b16. La parola a 0x8f87156c scende da 5 a 4 oppure da 3 a 2.

Il blocco di restituzione a 0x102ebe legge una mezza parola a +0x4a dell'elemento in R8. Se vale zero, chiama 0x1014e2, imposta +0x4f a 0 e inserisce nella lista 0x8f8715b4. Con un valore diverso da zero, chiama 0x1014b2, imposta +0x4f a 1 e inserisce nella lista 0x8f8715ac. Entrambe le routine incrementano il conteggio disponibile.

[D90ElementCycle.java](../ghidra/D90ElementCycle.java) ha verificato sei combinazioni su Ghidra in sola lettura. Ha eseguito l'inizializzazione completa con selettore 0 o 32, poi il blocco 0x1020fc fino a 0x102120, controllando identità dell'elemento, collegamenti della lista, byte di stato e conteggio. Per provare la restituzione nella stessa RAM sintetica, lo script invoca esplicitamente la routine di scollegamento 0x105b36, prepara il campo +0x4a con 0, 1 o 256 ed esegue 0x102ebe fino a 0x102eda. La verifica copre i due estremi del percorso. Il passaggio naturale fra questi blocchi, l'origine del campo +0x4a e l'uso fotografico restano aperti.

## AB-47 · Azzeramento e aggiornamento per maschera del campo +0x4a

Analisi statica Ghidra sul programma B del campione AB-1, indirizzi CPU. Una ricerca a passi di due byte nell'intervallo [0x1012ce, 0x103200) delle istruzioni LDI:8 con immediato 0x4a ha individuato sette candidati. Il contesto a 0x102bc0 e 0x102c18 carica zero in R0. Le store a 0x102bcc e 0x102c24 scrivono quindi zero nella mezza parola a R5+0x4a. Entrambi i blocchi azzerano anche la parola a +0x2c e la mezza parola a +0x48.

Nel blocco 0x10305c–0x10306e, il codice legge la mezza parola a R8+0x4a e calcola (valore & ~R6) sui 16 bit bassi. La store nel delay slot a 0x10306e aggiorna il campo per entrambi gli esiti del branch. Se rimangono bit impostati il salto raggiunge 0x1030a0. Se il risultato è zero, il listato prosegue con la chiamata a 0x105b36, l'azzeramento del byte +0x4f e la preparazione dell'inserimento nella lista 0x8f8715b4. Si tratta di un percorso distinto dal test di AB-46 a 0x102ebe.

Per riprodurre, ispezionare i tre blocchi nel programma B importato secondo CONTRIBUTING.md. La ricerca per immediato non copre indirizzi precalcolati o copie di strutture. L'osservazione statica documenta operazioni sui bit del campo. Restano aperti chi li imposta, che cosa rappresentino e se R5 e R8 puntino allo stesso elemento durante una specifica esecuzione.

## AB-48 · Due rimozioni consecutive della maschera sullo stesso elemento

Sul programma B del campione AB-1, [D90MaskRelease.java](../ghidra/D90MaskRelease.java) esegue due volte il blocco da 0x10305c sullo stesso elemento sintetico (R8=0x8f8715bc). La prima esecuzione parte con +0x4a=3 e R6=1, scrive 2 e raggiunge 0x1030a0. La seconda riparte dal valore 2 lasciato dalla prima, con R6=2, scrive 0 e raggiunge 0x103070. Lo script controlla entrambi i byte e l'indirizzo raggiunto. Eseguito su d90.hex in sola lettura, si ferma prima delle chiamate di gestione delle liste.

Ricerca statica complementare nel primo intervallo di 64 KiB di codice mappato a partire da 0x100000: 25 mezze parole allineate soddisfano (word & 0xfff0)==0xc4a0, codifica candidata per LDI:8 con immediato 0x4a. Nel contesto delle store a 0x10718e e 0x107418, R0 vale zero. La maschera calcolata prima di 0x107412 viene scritta a +0x48. Un'altra store, a 0x108e06, scrive invece a R9+0x4a la metà bassa di R4 dopo la chiamata a 0x1bbf12. L'identità della struttura in R9 rispetto al pool di AB-46 resta da verificare. La ricerca copre una sola codifica e non stabilisce quale istruzione attivi i bit nell'elemento reale.

## AB-49 · Destinazione della scrittura candidata a 0x108e06

Componente B, campione AB-1. La routine da 0x108970 carica in R9 la costante 0x8f52ebd8 a 0x10897c. Nel listato del corpo chiamante fino a 0x108e06 non compare una successiva assegnazione diretta a R9. Le store da 0x108cdc popolano più campi rispetto a questa base. Con tale base, +0x4a corrisponde a 0x8f52ec22. Il primo elemento osservato in AB-46 ha invece base 0x8f8715bc e campo a 0x8f871606.

[D90CandidateAddress.java](../ghidra/D90CandidateAddress.java) ha eseguito sul programma B in sola lettura le istruzioni 0x10897c–0x108988 e poi il blocco da 0x108df6 fino alla store inclusa, con la chiamata reale a 0x1bbf12. Con la mezza parola sintetica 12 a 0x8f52e48a, la store ha scritto 5 a 0x8f52ec22. La sentinella a 0x8f871606 è rimasta invariata. La prova conserva R9 fra i due blocchi e salta le chiamate e i servizi INT 0x40 intermedi. Il risultato circoscrive la destinazione diretta per questa base. Restano aperti eventuali trasferimenti successivi e l'origine dei bit del pool.

## AB-50 · Scrittura del valore 1 nel campo +0x4a di un elemento del pool

Componente B del campione AB-1, indirizzi CPU. A 0x102bb2 il firmware legge il collegamento a 0x8f8715b4+4. Se il puntatore è diverso dalla testa, alla prima iterazione 0x102bbe lo salva a 0x8f52e0d4 e 0x102bcc azzera la mezza parola a +0x4a. La routine da 0x118d5a legge il puntatore dalla stessa cella a 0x118d66. A 0x118d94 scrive 1 nell'intera mezza parola a R8+0x4a. È un'assegnazione: sovrascrive anche eventuali altri bit.

[D90CheckoutField.java](../ghidra/D90CheckoutField.java) ha verificato quattro combinazioni su d90.hex in sola lettura con selettore di inizializzazione 0 o 32 e campo sintetico iniziale 0 o 0x1234. Dopo AB-45, il blocco 0x102b92–0x102c52 con richiesta 1 registra il puntatore 0x8f8715bc. Lo script imposta il campo iniziale, esegue il chiamante da 0x10702c fino a 0x10707a e verifica il valore dopo ogni passo e il puntatore restituito in R8. Quindi entra a 0x118d60 senza cambiare il puntatore globale: controlla base e campo prima della store, e il valore 1 subito dopo. I byte +0xad e +0x105 sono inizializzati a zero per le operazioni del blocco.

I salti fra i tre ingressi sono predisposti dallo script. È dimostrata una scrittura capace di produrre la transizione 0→1 sullo stesso elemento in RAM sintetica. Il chiamante effettivo di 0x118d5a, l'ordine temporale sulla D90 e l'eventuale presenza di scritture precedenti restano da stabilire.

## AB-51 · Condizioni locali della chiamata a 0x118d5a

Nel componente B del campione AB-1, il chiamante carica 0x118d5a in R12 a 0x13018e e chiama tramite R12 a 0x130194. Il delay slot passa R8 in R4. R5 contiene il risultato della chiamata precedente a 0x103d46. Partendo da 0x1300f2, il bit 0x01 del byte a FP−0x11 devia prima del test del bit 0x02. Per raggiungere il ramo da 0x130164 occorre che 0x01 sia spento, 0x02 acceso e R9 contenga il bit 0x02. Il ramo chiama 0x11e0c6 con R4=FP−0x24 e R5=0. Se il risultato è zero si passa a 0x130176. Se è diverso da zero si passa a 0x130182 e, dopo il ritorno di 0x103d46, alla chiamata di AB-50.

[D90SetterCallerGate.java](../ghidra/D90SetterCallerGate.java) ha verificato 20 combinazioni nel progetto B in sola lettura: byte locale 0, 1, 2, 3 o 6, R9 pari a 0 o 2, risultato sintetico di 0x11e0c6 pari a 0 o 1. Nelle due combinazioni favorevoli (byte 2 o 6, R9=2, risultato 1) la chiamata ha eseguito la store originale e il campo è passato da 0 a 1. Negli altri casi lo script si ferma al punto di deviazione e verifica che il campo sia ancora zero. La cella globale e i registri sono predisposti; i due helper restituiscono valori simulati. Restano aperte le condizioni a monte dell'ingresso di prova, la provenienza del byte locale e il significato del risultato di 0x11e0c6 sulla D90.

## AB-52 · Come 0x11e0c6 costruisce il risultato usato da AB-51

Componente B del campione AB-1, indirizzi CPU. Il chiamante a 0x13016e passa l'indirizzo FP−0x24 e zero. La routine 0x11e0c6 chiama 0x11ec92 con il primo argomento e l'indirizzo di una parola locale di uscita. All'interno di questa preparazione il confronto signed a 0x11ece8–0x11ecea riguarda il risultato di 0x13b05a. Sul ramo non negativo, le store a 0x11ecee e 0x11ecf2 copiano quel valore a 0x8f6f2c50 e nella parola di uscita, e R6 viene impostato a 1. Sul ramo negativo la cella globale riceve −1 e R6 viene impostato a 0. Dopo INT 0x40 a 0x11ed1e, la preparazione restituisce R6.

Se la preparazione restituisce zero, 0x11e0c6 restituisce zero. Altrimenti passa a 0x1204a4 il valore della parola di uscita, la prima parola del descrittore e il secondo argomento originario. Salva il risultato, chiama 0x11eeba con la parola di uscita e restituisce il risultato salvato. Nel listato di 0x1204a4 il percorso fallisce già se 0x121b36 restituisce zero (0x1204c8–0x1204d4).

[D90CallerHelper.java](../ghidra/D90CallerHelper.java) ha eseguito in sola lettura sei casi dall'ingresso locale 0x130164 fino a 0x130176 o 0x130182. Con −1 da 0x13b05a il chiamante raggiunge 0x130176 senza invocare 0x1204a4. Con 0 o 7 il codice passa quel valore, 42 e 0 nei tre registri argomento. Un risultato simulato di 0 da 0x1204a4 porta a 0x130176, un risultato di 3 a 0x130182. Il ritorno simulato 99 da 0x11eeba non cambia la decisione. Sono originali le istruzioni del chiamante, di 0x11e0c6 e del corpo principale di 0x11ec92. Le chiamate interne elencate nello script hanno ritorni sintetici e le due INT 0x40 sono saltate conservando i registri. La prova termina prima della scrittura di AB-50. Restano da spiegare l'origine dei risultati interni e l'effetto dei servizi OS sulla D90.

## AB-53 · Controllo di un'intestazione di otto byte

Sul componente B del campione AB-1, 0x1204a4 passa una struttura locale a 0x121b36. Questa passa la prima parola a 0x11dcd4 e richiede otto byte. Un esito diverso da zero o un conteggio diverso da otto porta al ritorno zero. A 0x121b64–0x121ba2 i primi due byte selezionano MM (big endian) oppure II (little endian) e due funzioni di decodifica salvate a +8 e +12 della struttura. La prima chiamata indiretta legge i byte 2–3 come intero di 16 bit: deve valere 42. La seconda legge i byte 4–7 come intero di 32 bit e lo restituisce. Il chiamante prosegue a 0x1204d6 solo con un valore diverso da zero. La sequenza è compatibile con un'intestazione TIFF classica; l'origine concreta del flusso resta aperta.

[D90HeaderProbe.java](../ghidra/D90HeaderProbe.java) ha verificato sette casi con lettura simulata. MM e II hanno restituito entrambi 0x01020304 con gli stessi valori codificati nei rispettivi ordini. Stato di errore, conteggio 7, marcatore misto, 42 in ordine errato e ultimo campo zero hanno portato al ramo di arresto del chiamante.

Limite del profilo FR60. La prima esecuzione senza correzioni ha scritto i dati sintetici sopra l'indirizzo di ritorno e ha tentato di eseguire 0x4d4d002a. Per ADD2, il [manuale Fujitsu](https://www.manualsdir.com/manuals/103548/fujitsu-fr-family-32-bit-microcontroller-instruction-manuel-cm71-00101-5e.html?page=98) prescrive l'estensione negativa dell'immediato a quattro bit. Il profilo usato calcola +4 a 0x121b42 e +6 a 0x121bac dove servono −12 e −10. Lo script compensa la differenza di 16 byte ai due punti di utilizzo; i sette casi si riferiscono a questa esecuzione corretta localmen…36042 tokens truncated…stack di lavoro `[0x81003f00,0x81004100)` con 0x5a oppure 0xa5. SP iniziale è 0x81004000; i registri iniziali usati dal test sono azzerati. I 40 byte del contesto esterno a 0x8f991204 sono inizializzati a zero dal banco; il contesto a 0x68000ed0 ricade nella regione OS riempita col pattern e viene azzerato dal percorso originale. Il marker 0x6800cf20 parte da 0x11223344 e i byte periferici 0x44/0x46f partono dal pattern. Questi byte di periferica sono RAM sintetica.

A differenza delle prove AB-102/103, il banco non azzera separatamente la bitmap e non reimposta PC/RP fra le routine. La sequenza originale 0x407f0 azzera `[0x68000000,0x68000ec0)`, `[0x8000,0x8400)` e `[0x68000ec0,0x68000f20)` prima delle costruzioni; le ulteriori coppie di limiti uguali nel disassemblato non richiedono accessi alle regioni vuote. La prova controlla il risultato finale dell'intera catena; l'attribuzione dell'azzeramento deriva anche dal listato originale già conservato in AB-100.

Un'immagine attesa indipendente dallo stato finale dell'emulatore è costruita dalle tabelle originali e dalle regole dei campi verificate in AB-100–103: 33 sentinelle, tre sentinelle aggiuntive, 39 record, 38 blocchi ausiliari, due inserimenti, 49 oggetti con campo halfword, 22 con campo word e i due contesti preparati. Le intestazioni e i limiti delle destinazioni sono verificati prima dell'esecuzione. Dopo il punto di arresto si confrontano tutti i **3872 byte** di `[0x68000000,0x68000f20)` e i 40 byte di ciascun contesto. La bitmap finale inclusa nel confronto è 0x80000000; il contatore a 0x68000110/114 è zero.

Si verificano inoltre i 1024 byte azzerati di `[0x8000,0x8400)`, le guardie di quattro byte ai due estremi, i 224 byte `[0x68000f20,0x68001000)` ancora uguali al pattern, SP ripristinato a 0x81004000, marker 0x6800cf20=1 e byte 0x44=1, 0x46f=0x10. Il confronto non copre tutta la memoria indirizzabile o tutte le scritture intermedie; i contesti preparati non vengono eseguiti.

Log locale `work/os-startup-continuous.log`: **due scenari superati, 12125 passi ciascuno (24250 totali)**, senza WARN/ERROR/eccezioni. Ghidra 12.1.4, programma B `-readOnly -noanalysis`, linguaggio installato; non occorre l'adattatore INT o il linguaggio ILM sperimentale perché il test si ferma prima del percorso AB-97. I passi sono conteggi di istruzioni eseguite dall'emulatore, non cicli o tempi fisici.

Il risultato collega in continuità il prefisso OS e sostituisce le transizioni imposte dei test AB-102/103 con le CALL originali. Non dimostra l'avvio completo o la compatibilità generale del modello CPU. Prossimo passo: collegare questo stato prodotto dal firmware alla chiamata a 0x40738 già studiata in AB-97, conservando espliciti il modello ILM e i confini INT/SSP necessari a quel seguito; poi valutare il vettore 0x18 sulla stessa memoria.


## AB-105 · Collegamento continuo fra inizializzazione OS e periferica

Componente B, campione AB-1, 9 novembre 2026. [D90OsPeripheralInitialization.java](../ghidra/D90OsPeripheralInitialization.java) estende AB-97 iniziando da 0x40704 e prosegue attraverso le CALL originali del prefisso AB-104 fino a **prima dell'istruzione a 0x40740**. Non ricostruisce le liste fra i due segmenti. Il modello ILM sperimentale a cinque bit, l'adattatore INT #0x40 e le sincronizzazioni esplicite SSP di AB-86/87 restano necessari; non si tratta di esecuzione interamente nativa del modello CPU.

Otto scenari combinano seed periferico 0/0xff, pattern stack/OS 0x5a/0xa5 e ILM 0/31. Prima dell'esecuzione il banco riempie `[0x68000000,0x68001000)` e `[0x8000,0x8400)` col pattern, azzera 40 byte a 0x8f991204 e imposta 0x6800cf20=0x11223344. RAM periferica, stack e registri iniziali sono sintetici secondo AB-97. Le finestre originali aggiuntive sono `[0x40704,0x40738)`, `[0x407f0,0x40a56)`, `[0x41534,0x415a2)`, `[0x4186e,0x418a0)` e `[0x5c400,0x5cce4)`.

A 0x40738 si verificano visita dei dodici inizializzatori di AB-104, marker 1, bitmap 0x80000000, tutte le 33 liste e le tre sentinelle aggiuntive, i due nodi inseriti 0x680001ec/0x680007c4 con campo +8 zero e contatore 0x68000110/114 zero. Si salvano quindi i **3872 byte** di `[0x68000000,0x68000f20)` e i 40 byte del contesto esterno. Questa prova verifica collegamenti e conservazione: non ricostruisce autonomamente tutti i campi attesi come AB-104.

Segue il percorso AB-97: preparazione del blocco del servizio -83, INT adattato, dispatcher/servizio, RETI e coda d'inizializzazione fino a 0x40740. Restano attive le verifiche ereditate su blocco, registri e scritture periferiche. Il contatore prima del servizio è ora zero, prodotto dal prefisso OS, anziché il valore sentinella del test isolato. Alla fine l'intera regione salvata e il contesto esterno devono essere identici; il marker resta 1. SP=0x81004000, mentre SSP rimane separato a 0x81003fec dopo il normale recupero dello stack. Il confronto dei valori finali non esclude scritture intermedie né copre tutta la memoria.

La classe base [D90SourceInitializationReturn.java](../ghidra/D90SourceInitializationReturn.java) aggiunge hook per ingresso, limite di passi e contatore iniziale; i valori predefiniti conservano il test AB-97. AB-105 imposta ingresso 0x40704 e limite 30000 per tratto. I precedenti test invariati non sono stati rieseguiti in questo passaggio.

Log locale `work/os-peripheral-initialization.log`: **otto scenari superati, 102272 istanze di passo controllate** per accessi diretti SSP/USP, nessun WARN/ERROR/eccezione. Il conteggio comprende il prefisso OS (8 × 12125) e i 5272 passi del percorso AB-97; esclude INT adattato e RETI gestito separatamente. Non misura cicli hardware. Ambiente Ghidra 12.1.4, Java 21.0.13, programma B `-readOnly -noanalysis`, programma sperimentale transitorio, profilo ILM di AB-85 con manifesto `ghidra/language-profile-ilm5.sha256`. Il riepilogo ereditato AB-97 nel log è seguito da quello AB-105; il totale comprende entrambi i segmenti.

Non sono eseguiti reset completo, scheduler o ingresso del vettore 0x18; periferiche e stato iniziale restano sintetici. Non è una nuova validazione indipendente del binario originale. Prossimo obiettivo: verificare il vettore 0x18 sulla memoria prodotta da questa catena, distinguendo ingresso forzato, effetti sulle liste e accettazione hardware ancora non modellata.


## AB-106 · Vettore 0x18 sullo stato OS inizializzato

Componente B, campione AB-1, 9 novembre 2026. [D90OsInitializedInterrupt.java](../ghidra/D90OsInitializedInterrupt.java) estende AB-105 conservando tutte le sue verifiche. Dopo la catena originale da 0x40704 a prima di 0x40740, introduce un singolo ingresso esterno **forzato** al vettore 0x18, verificato a 0xdff9c→0x4048a. Non reinizializza liste o contesti.

Gli otto scenari ereditano seed periferico 0/0xff, pattern stack/OS 0x5a/0xa5 e ILM 0/31. Il banco salva PC/PS a SP−8/SP−4, imposta SP e SSP a SP−8, azzera I/S e trasferisce PC al gestore. Questo costruisce sinteticamente il frame e forza l'ingresso anche quando le condizioni hardware di accettazione non sono modellate. Esegue il wrapper e la routine chiamata fino a prima di RETI a 0x404aa; `returnSupervisor` esegue il ritorno con la sincronizzazione SSP esplicita di AB-87. Le finestre originali aggiunte sono `[0x4048a,0x404ae)`, `[0x404fe,0x40500)`, `[0x40a56,0x40bec)` e `[0xdff9c,0xdffa0)`.

Il confronto finale dei 3872 byte `[0x68000000,0x68000f20)` ammette soltanto il contatore basso 0x68000114 da zero a uno; la parola alta 0x68000110 resta zero. Tutte le liste, i record e il contesto interno compresi nella regione mantengono i valori precedenti. Si confrontano inoltre i 40 byte del contesto esterno a 0x8f991204, 18 registri generali/speciali salvati dal banco, PC=0x40740, SP=SSP=0x81004000 e PS originario. La halfword periferica sintetica a 0x4e risulta 0x001a. Non si confrontano tutte le altre regioni periferiche o tutte le scritture intermedie.

Log `work/os-initialized-interrupt.log`: otto scenari superati, **102672 istanze di passo controllate**, ossia 400 oltre AB-105 (50 per ingresso), senza WARN/ERROR/eccezioni. INT adattato e RETI trattato separatamente non sono inclusi nel conteggio. Ghidra 12.1.4, programma B `-readOnly -noanalysis`, linguaggio ILM isolato AB-85. Le righe ereditate che menzionano SSP=0x81003fec descrivono il confine AB-97 prima dell'hook: al termine di AB-106 SSP è invece sincronizzato a 0x81004000 e verificato esplicitamente.

Le liste d'attesa iniziali sono vuote: questa prova non verifica un nuovo trasferimento di nodi in attesa, lo scheduler o l'esecuzione dei contesti. Non determina periferica, cadenza, accettazione hardware o avvio completo. Prossima indagine: seguire le istruzioni da 0x40740 per identificare il passaggio al primo contesto e le sue precondizioni, prima di estendere l'emulazione allo scheduler.


## AB-107 · Selezione iniziale e confine dello stack utente

Componente B, campione AB-1, 9 novembre 2026. [D90StartupDispatchBoundary.java](../ghidra/D90StartupDispatchBoundary.java) estende AB-106: dopo il ritorno dell'interrupt forzato a 0x40740 esegue nove istruzioni originali, fermandosi **prima di MOV R0,USP a 0x407ce**. Il banco non modifica memoria o registri fra questi nove passi.

Il percorso scrive 2 nel marker 0x6800cf20 (0x40740–0x4074c), salta a 0x407bc, carica il descrittore 0x5c838 e ne legge la destinazione 0x680007c4 in FP. A 0x407ca salva FP in 0x68000000; a 0x407cc legge il campo +0x18 del nodo, ottenendo R0=0x68000ed0. Si tratta del record 38 e del contesto costruiti nelle prove AB-102–105. La selezione in questo segmento deriva direttamente dal descrittore costante, senza una ricerca nella bitmap. Non viene attribuito un nome funzionale al task.

Otto scenari ereditano inizializzazione, ILM sperimentale e interrupt sintetico AB-106. Si confrontano tutti i 3872 byte `[0x68000000,0x68000f20)`, ammettendo soltanto la parola iniziale aggiornata a 0x680007c4. Si verificano marker 2, FP/R0, SP invariato, S=0 e USP invariato. USP è inizializzato dal banco a 0x81234560 prima di tutta l'esecuzione; non è un valore firmware. Le finestre aggiunte sono `[0x40740,0x40750)` e `[0x407bc,0x407f0)`.

Il supporto statico [D90StartupDispatchContext.java](../ghidra/D90StartupDispatchContext.java), log `work/startup-dispatch-context.log`, mostra nel seguito MOV R0,USP; ORCCR #0x20; due letture da SP; ANDCCR #0xdf; due push; nuovo ORCCR #0x20; ripristini RP/MDL/MDH e LDM0/LDM1; ANDCCR #0xdf e RETI a 0x407ea. Sono istruzioni disassemblate, **non eseguite da AB-107**. Il cambio di stack richiede una verifica specifica del modello SP/USP/SSP: il controllo ereditato sulle istruzioni eseguite non consente accessi dedicati SSP/USP. La scansione lineare aggiuntiva a 0x40500 si arresta sul dato non decodificato a 0x4051a; non prova che tutto l'intervallo sia codice raggiungibile.

Log finale `work/startup-dispatch-boundary-final.log`: otto scenari superati, **102744 passi controllati** (72 oltre AB-106), nessun WARN/ERROR/eccezione. Ghidra 12.1.4, programma B in sola lettura, modello ILM AB-85. Il primo log `work/startup-dispatch-boundary.log` riportava otto avvisi per la lettura diagnostica di USP non inizializzato: la correzione inizializza il registro nel banco prima dell'esecuzione, senza modificare il firmware. I riepiloghi ereditati AB-97/105/106 descrivono i rispettivi confini precedenti; il PC finale di questa prova è 0x407ce.

Restano da verificare il cambio di stack, il frame consumato da RETI e il codice del primo contesto. La presenza del vettore forzato AB-106 resta una precondizione della prova, non una ricostruzione della sequenza temporale hardware. Prossimo passo: verificare in isolamento la semantica del cambio S e l'uso di USP/SSP prima di estendere il percorso oltre 0x407ce.


## AB-108 · Caratterizzazione del cambio S e dei registri di stack

Componente B, campione AB-1, 10 novembre 2026. [D90UserStackModel.java](../ghidra/D90UserStackModel.java) caratterizza in isolamento le cinque istruzioni originali `[0x407ce,0x407d8)`: MOV R0,USP; ORCCR #0x20; LD @SP+,R0; LD @SP+,R1; ANDCCR #0xdf. Non estende l'esecuzione continua AB-107 e non implementa un adattatore.

Otto casi combinano due linguaggi (installato e copia ILM a cinque bit AB-85), input ILM 0/31 e parole sintetiche 0x11223344/0x55667788. SP=SSP=0x81001000, USP iniziale=0x81002080 e R0=0x81002000. Alla posizione SP il banco scrive il pattern e il suo complemento; alla destinazione futura USP scrive 0x12345678/0x23456789. Questi valori distinguono quale memoria viene effettivamente letta. Tutti gli altri flag usati vengono inizializzati; le prove non validano il valore ILM 31 del profilo installato, il cui limite è già documentato in AB-84.

Risultati verificati dopo ogni tratto:

- MOV aggiorna USP a 0x81002000, lasciando SP a 0x81001000.
- ORCCR imposta S=1, lasciando SP invariato.
- I due pop leggono il pattern e il complemento dalla regione indicata dallo SP precedente, non le parole alla destinazione USP.
- ANDCCR ripristina S=0; PC=0x407d8, SP=0x81001008, SSP=0x81001000 e USP=0x81002000.
- Tutti i 12288 byte della memoria sintetica `[0x81000000,0x81003000)` sono invariati.

La lettura locale dei sorgenti del profilo corrobora l'osservazione: `fr60.slaspec` assegna SSP/USP a registri dedicati e SP a un registro distinto; ORCCR e ANDCCR in `fr60.sinc` aggiornano i flag senza trasferimenti fra questi registri. Questo risultato riguarda il modello Ghidra, non il comportamento fisico della CPU. La correzione ILM AB-85 non introduce un alias SP/USP/SSP.

Log `work/user-stack-model.log`: **otto casi, 40 istruzioni eseguite**, senza WARN/ERROR/eccezioni, Ghidra 12.1.4. Il programma B è aperto con `-readOnly -noanalysis`; le istruzioni vengono copiate in programmi transitori con stack sintetici. Nessuna nuova lettura indipendente del firmware originale. Le prove precedenti in solo supervisore restano circoscritte ai loro adattamenti dichiarati; questa verifica spiega perché non possono essere generalizzati al cambio di stack utente.

Prossimo passo: progettare e verificare un adattamento esplicito e delimitato del cambio di stack, con controlli sulle precondizioni e sul frame, prima di continuare AB-107 fino a RETI. Rimangono aperti ripristino completo, prima esecuzione del contesto e scheduler.


## AB-109 · Adattatore circoscritto del cambio di stack

Componente B, campione AB-1, 10 novembre 2026. [D90StackSwitchAdapter.java](../ghidra/D90StackSwitchAdapter.java) introduce `switchStack` per i soli quattro siti originali: ORCCR #0x20 a 0x407d0/0x407dc e ANDCCR #0xdf a 0x407d6/0x407e8. Non modifica il linguaggio CPU e non implementa un alias globale.

Prima di eseguire l'istruzione richiede assenza di delay slot (informazione fornita dal chiamante), PC pari e sito ammesso, S iniziale opposto alla destinazione, opcode esatto 0x9320/0x83df, SP e stack destinazione allineati a quattro byte. Dopo il passo originale verifica PC+2, S atteso e SP invariato come caratterizzato in AB-108. Scrive quindi esplicitamente il vecchio SP nel registro dedicato sorgente (SSP oppure USP) e carica SP dal registro dedicato destinazione. Accetta che il registro sorgente sia rimasto indietro rispetto a SP dopo pop/push ordinari; non verifica mappatura o validità generale della memoria destinazione. Un fallimento durante il passo o nei controlli successivi arresta la prova senza garanzia di rollback; i rifiuti di precondizione avvengono invece prima delle scritture.

Le otto sequenze isolate riutilizzano gli input AB-108 sui due profili, ILM iniziale 0/31 e due pattern della memoria supervisore. Eseguono le cinque istruzioni da 0x407ce, adattando i due cambi S. MOV carica USP=0x81002000; i pop leggono 0x12345678 e 0x23456789 dallo stack utente sintetico. Al ritorno a 0x407d8: S=0, SP=SSP=0x81001000 e USP=0x81002008. I 12288 byte della memoria sintetica restano invariati. Gli input ILM non costituiscono una nuova verifica della fedeltà ILM del profilo installato.

Ulteriori otto prove (quattro siti × due profili) verificano il salvataggio dello SP corrente 0x81001100 nel registro dedicato sorgente inizialmente diverso e il caricamento dello stack destinazione. **48 rifiuti** coprono, per ogni sito/profilo: PC dispari, S iniziale errato, SP non allineato, destinazione non allineata, opcode alterato nell'emulatore e indicazione di delay slot. I rifiuti conservano PC, SP, SSP, USP, PS, S, I, N, Z, V, C, T, D0, D1, ILM, R0, R1 e tutta la memoria sintetica di stack. Non si afferma un confronto di ogni registro della CPU.

Log `work/stack-switch-adapter.log`: otto sequenze, otto prove dei siti e 48 rifiuti superati, senza WARN/ERROR/eccezioni; Ghidra 12.1.4, programma B `-readOnly -noanalysis`, programmi transitori, profilo installato e profilo ILM AB-85. Le istruzioni originali sono copiate da `[0x407ce,0x407ea)`; gli opcode alterati dei casi negativi restano nelle singole istanze emulative.

Questa verifica dimostra il comportamento del banco con la convenzione esplicita di stack selezionato da S. Non è validazione hardware, non esegue il contesto firmware reale, non adatta RETI verso S=1 e non prova interruzioni annidate. Prossimo passo: integrare l'adattatore nel tratto AB-107, verificare il trasferimento delle prime due parole e il ripristino dei registri fino a prima di RETI, mantenendo distinti tutti gli effetti del banco.


## AB-110 · Ripristino del contesto costruito fino a prima di RETI

Componente B, campione AB-1, 10 novembre 2026. [D90StartupContextRestore.java](../ghidra/D90StartupContextRestore.java) estende AB-107 dopo tutte le sue verifiche. Continua da 0x407ce fino a **prima di RETI a 0x407ea**, sullo stato costruito dal firmware e conservato attraverso AB-105/106. Esegue MOV R0,USP originale separatamente dal controllo che vieta accessi dedicati SSP/USP nei passi ordinari; verifica la decodifica e il risultato USP=0x68000ed0. Applica poi l'adattatore esplicito AB-109 ai quattro siti 0x407d0/0x407d6/0x407dc/0x407e8.

Il test salva gli 80 byte del contesto `[0x68000ed0,0x68000f20)` prima del ripristino. Le due prime parole vengono lette con pop e trasferite sullo stack supervisore dalle istruzioni originali a 0x407d8/0x407da. Il frame finale a 0x81003ff8 contiene **PC=0x407ee e PS=0x001f0030**. I 18 registri sono confrontati con le parole successive del contesto, nell'ordine RP, MDL, MDH, R0–R12, AC, FP; vengono recuperati dalle istruzioni originali LD/LDM. Questa prova confronta il ripristino con lo stato costruito, senza derivare nuovamente ogni valore dalle tabelle come AB-104.

Al confine finale: PC=0x407ea, S=0, SP=SSP=0x81003ff8, USP=0x68000f20. Si confrontano integralmente i 3872 byte della regione OS `[0x68000000,0x68000f20)` e i 40 byte del contesto esterno a 0x8f991204 con lo stato precedente al ripristino; restano invariati. Il frame PC/PS è verificato dopo i push e alla fine, insieme alle guardie di quattro byte a 0x81003ff4 e 0x81004000. Non è una traccia di tutte le scritture né un controllo di tutta la memoria.

Log `work/startup-context-restore.log`: **otto scenari superati**, senza WARN/ERROR/eccezioni, Ghidra 12.1.4, programma B `-readOnly -noanalysis`, linguaggio isolato ILM AB-85. Il contatore ereditato riporta 102816 passi ordinari controllati, 72 oltre AB-107. Sono inoltre eseguite separatamente cinque istruzioni per scenario (MOV USP e quattro istruzioni CCR adattate), escluse da quel contatore, come già INT/RETI dei segmenti precedenti. Le righe ereditate AB-97/105/106/107 descrivono i rispettivi punti intermedi; il riepilogo AB-110 e le asserzioni definiscono il confine finale effettivo.

Il valore PS nel frame include S=1 e I=1 con ILM=31 nel profilo sperimentale. Non viene consumato: `returnSupervisor` di AB-87 rifiuta intenzionalmente questo stato utente e non è stato generalizzato. Il listato AB-107 mostra BRA 0x407ee a 0x407ee, compatibile con un contesto iniziale di attesa, ma AB-110 non esegue quella destinazione né dimostra scheduling, accettazione di interrupt o avvio completo. Prossimo passo: caratterizzare RETI verso S=1 e verificarne l'adattamento di stack prima di raggiungere la destinazione del contesto.


## AB-111 · RETI verso S=1 nei due profili CPU

Componente B, campione AB-1, 10 novembre 2026. [D90UserRetiModel.java](../ghidra/D90UserRetiModel.java) copia i due byte originali 0x9730 da 0x407ea e li esegue in programmi transitori, senza adattatore. Non prosegue la catena AB-110: frame e stack sono sintetici.

Sedici casi combinano profilo installato/copia ILM AB-85, USP 0x81002000/0x81002100 e PS 0x00000030, 0x001f0030, 0x00100020, 0x000f073f. Tutti i frame richiedono S=1; comprendono I=0/1 e combinazioni di flag. Ingresso S=0, SP=SSP=0x81000ff8. Il frame su SP contiene PC=0x407ee e il PS del caso; alla destinazione USP sono collocati PC=0x80000100 e PS=0, così da distinguere la provenienza dei dati. I 18 registri R0–R12, AC, FP, RP, MDL, MDH ricevono sentinelle diverse.

Dopo una sola RETI, entrambi i profili portano PC a 0x407ee e PS al valore del frame su SP. Vengono verificati C/V/Z/N/I/S/T/D0/D1 e ILM: quest'ultimo è limitato a quattro bit nel profilo installato e a cinque nella copia sperimentale, coerentemente con AB-84/85. I 18 registri sentinella restano invariati. **SP=0x81001000, SSP=0x81000ff8 e USP resta al valore iniziale**: il modello consuma otto byte dal vecchio SP ma non seleziona USP come nuovo SP né sincronizza SSP. Tutti i 12288 byte della memoria sintetica sono confrontati e restano invariati, incluse le guardie adiacenti al frame.

La lettura locale della semantica RETI in `fr60.sinc` mostra due letture da SP con incrementi, `unpackProgramStatus()` e ritorno al PC recuperato. È coerente con le osservazioni; non viene modificata. La destinazione 0x407ee non è eseguita né mappata come codice in questi test: il controllo riguarda soltanto il risultato di RETI.

Log `work/user-reti-model.log`: **16 casi superati, 16 istruzioni eseguite**, senza WARN/ERROR/eccezioni; Ghidra 12.1.4, programma B `-readOnly -noanalysis`. Nessuna nuova validazione indipendente dei binari originali o della CPU hardware. Nessuna generalizzazione dell'adattatore supervisore AB-87, che continua a rifiutare S=1. Prossimo passo: adattare esplicitamente il solo confine RETI utente, validando frame e puntatori prima dell'esecuzione e verificando la conservazione dello stato sui rifiuti; poi integrarlo nel contesto AB-110.


## AB-112 · Adattatore del confine RETI utente

Componente B, campione AB-1, 10 novembre 2026. [D90UserRetiAdapter.java](../ghidra/D90UserRetiAdapter.java) introduce `returnUser` per il solo sito 0x407ea. Il metodo esegue RETI originale e verifica gli effetti caratterizzati in AB-111, quindi aggiorna esplicitamente SSP al vecchio SP+8 e SP al valore USP. Non modifica il firmware o il linguaggio, né generalizza `returnSupervisor` AB-87.

Le precondizioni richiedono sito esatto, assenza di delay slot dichiarata dal chiamante, S=0, SP=SSP, allineamento a quattro byte di SP/USP, SP≤0xfffffff7, opcode 0x9730, destinazione del frame non nulla e pari, e bit S=1 nel PS salvato. Dopo il passo originale controlla PC/PS/S/ILM e che SP sia avanzato di otto byte, con SSP e USP ancora invariati; applica poi le due scritture del banco. Non verifica mappatura/eseguibilità della destinazione o validità generale dello stack utente. I rifiuti delle precondizioni precedono le scritture; un errore durante il passo o nei controlli successivi non garantisce rollback. Il metodo è verificato soltanto sul profilo ILM a cinque bit; il controllo ILM dopo il passo non è un controllo preventivo dell'identità del linguaggio.

Otto casi positivi riprendono i due valori USP e i quattro PS di AB-111, sul solo profilo `fr60:BE:16:d90-ilm5`. Il frame sintetico è a 0x81000ff8, con PC=0x407ee; dopo il ritorno SP=USP (0x81002000 o 0x81002100), SSP=0x81001000, S=1. Sono verificati PS, C/V/Z/N/I/S/T/D0/D1 e ILM, conservazione dei 18 registri sentinella e di tutti i 12288 byte della memoria sintetica di stack. La destinazione non viene eseguita.

Undici casi negativi coprono PC dispari, sito pari diverso, S iniziale errato, SP/SSP incoerenti, SP non allineato, USP non allineato, opcode alterato nell'istanza emulativa, PC di ritorno dispari, PS senza bit S, incremento SP oltre il limite e indicazione di delay slot. Dopo il rifiuto sono confrontati i 33 registri esplicitamente elencati dal test (PC/SP/SSP/USP, PS e flag, 18 registri generali/speciali) e l'intera memoria sintetica di stack. Non si afferma un confronto di ogni registro del processore o di tutta la memoria indirizzabile.

Log `work/user-reti-adapter.log`: **otto ritorni validi e undici rifiuti superati**, senza WARN/ERROR/eccezioni; Ghidra 12.1.4, programma B `-readOnly -noanalysis`, programmi transitori e linguaggio isolato AB-85. Le sole otto istruzioni eseguite sono RETI nei casi validi. È una verifica della convenzione adottata dal banco, non una validazione hardware o del contesto reale AB-110. Prossimo passo: applicare l'adattatore al frame prodotto da AB-110, verificare l'arrivo a 0x407ee e distinguere il ciclo iniziale d'attesa dall'esecuzione dei task e dallo scheduler ancora aperti.


## AB-113 · Ingresso nel contesto utente e ciclo iniziale

Componente B, campione AB-1, 10 novembre 2026. [D90StartupUserLoop.java](../ghidra/D90StartupUserLoop.java) estende AB-110 dopo tutte le verifiche del ripristino e applica `returnUser` AB-112 al frame prodotto dalle istruzioni firmware. Non reinietta il contenuto del frame né i registri del contesto. Il percorso ereditato comprende l'ingresso forzato AB-106 prima della selezione iniziale: non ricostruisce la temporizzazione reale di avvio.

Prima di RETI verifica PC=0x407ea e frame su 0x81003ff8/0x81003ffc pari a 0x407ee/0x001f0030. Dopo l'istruzione originale e le correzioni esplicite SSP/SP di AB-112 controlla PC=0x407ee, PS=0x001f0030, S=1, I=1, ILM=31, SP=USP=0x68000f20 e SSP=0x81004000. I 18 registri recuperati in AB-110 conservano i loro valori.

Per ciascuno degli otto scenari vengono poi eseguite **16 istruzioni BRA 0x407ee**, verificandone la decodifica e controllando a ogni passo i 18 registri generali/speciali più PC, PS, S, I, ILM, SP, SSP, USP, C, V, Z, N, T, D0 e D1. I valori restano invariati. Il confronto finale con l'immagine salvata prima di RETI copre `[0x68000000,0x68001000)`, i 40 byte a 0x8f991204, `[0x81003f80,0x81004080)`, `[0,0x1000)` e i quattro byte del marker 0x6800cf20. Non copre tutta la memoria indirizzabile o scritture intermedie fuori da queste regioni.

Log `work/startup-user-loop.log`: **otto ingressi nel contesto utente e 128 passi del ciclo superati**, senza WARN/ERROR/eccezioni; Ghidra 12.1.4, programma B `-readOnly -noanalysis`, profilo ILM isolato AB-85. I passi ordinari controllati per accessi dedicati SSP/USP sono 102944, 128 oltre AB-110. RETI utente viene eseguita separatamente dall'adattatore e non è inclusa in quel contatore, come gli altri confini espliciti già documentati. I riepiloghi ereditati che dichiarano RETI non eseguita descrivono soltanto i confini AB-107/110; il riepilogo finale AB-113 identifica il risultato completo di questo script.

La prova collega il segmento d'inizializzazione da 0x40704 al primo contesto selezionato, usando RAM sintetica, INT/SSP espliciti, un interrupt esterno forzato e quattro cambi di stack adattati. Non parte dal reset, non modella periferiche o accettazione hardware degli interrupt e non esegue uno scheduler che passi a un altro task. I 16 passi sono un limite del test, non un intervallo temporale fisico. Il ciclo è compatibile con una funzione di attesa iniziale; il nome e il ruolo completo del task non sono dimostrati.

Prossimo obiettivo: ricostruire il percorso che può selezionare un altro contesto dopo un interrupt, iniziando dai wrapper e dalle decisioni di scheduling già presenti nei listati. L'ingresso interrupt mentre S=1 richiede ancora un trattamento distinto dagli ingressi supervisore verificati finora; non viene forzato da questa prova.


## AB-114 · Candidato pronto e percorso del vettore 0x3f

Componente B, campione AB-1, 10 novembre 2026. Questa verifica collega la pista statica già documentata in [AB-27](boot-and-ghidra.md#ab-27) allo stato prodotto dalla catena AB-113; non presenta quella pista come una nuova scoperta. [D90DispatchSelectionContext.java](../ghidra/D90DispatchSelectionContext.java) legge la tabella vettori e disassembla `[0x40d44,0x40ef0)` senza modificare il programma. La voce 0xdff00 contiene 0x40d44: con TBR=0xdfc00 e formula `TBR + 0x3fc − 4*n` corrisponde al vettore **0x3f**, ossia indice 192 contando le parole dal primo indirizzo della tabella. Questo identifica una voce software, non la sorgente fisica dell'evento.

Il prefisso 0x40d44 azzera I, imposta S e salva R12/AC/FP. A 0x40d52 scambia con zero il byte 0x68000130; se il valore letto è zero, il ramo a 0x40d56 porta a 0x40e2a. Quest'ultimo carica la bitmap 0x68000004 e incontra a 0x40e32 la parola **0x97dd**, già identificata in AB-27 come SRCH1 AC nella definizione estesa. Lo script controlla i byte e segnala il limite FR60, senza eseguire o sostituire quell'istruzione. Non prova gli altri rami del prefisso.

Dopo la ricerca, il listato aggiunge 4 a R12 e moltiplica per otto il risultato in AC prima di leggere la testa della lista. Per il risultato di ricerca zero descritto in AB-27, l'indirizzo letto è 0x68000008. Il controllo del byte 0x68000131 (0x40e42–0x40e46) precede il confronto del candidato con il puntatore corrente 0x68000000 a 0x40e5c–0x40e60. Il ramo di differenza salva registri e frame, scrive SP nel campo +0x18 del controllo precedente a 0x40e78, carica il contesto del candidato a 0x40e8a e aggiorna il puntatore corrente a 0x40eb2. Queste sono evidenze statiche: nessuno di questi passi è eseguito dalla nuova prova.

[D90DispatchReadyState.java](../ghidra/D90DispatchReadyState.java) estende AB-113 con sole letture e asserzioni sullo stato finale, senza introdurre un nuovo interrupt o alterare PC. In otto scenari verifica:

- PC=0x407ee e S=1, controllo corrente 0x680007c4, bitmap 0x80000000;
- sentinella 0x68000008/0x6800000c collegata in entrambe le direzioni al candidato 0x680001ec, e collegamenti inversi del candidato;
- campo candidato +0x18 (0x68000204)=0x8f991204, frame con PC=0x1000ac e PS=0x001f0030;
- byte 0x68000130 e 0x68000131 entrambi zero;
- voce vettore 0xdff00→0x40d44 letta dal programma originale.

Il candidato distinto è quindi presente nello stato emulato; la scelta effettiva rimane condizionata all'ingresso nel gestore, alla semantica di SRCH1 e al mantenimento delle precondizioni lungo il percorso. Non viene calcolato artificialmente il risultato di SRCH1 nell'emulatore e non si dichiara il task avviato.

Log `work/dispatch-selection-context.log` e `work/dispatch-ready-state.log`: nessun WARN/ERROR/eccezione; otto ispezioni dello stato superate con la catena AB-113 e i suoi 102944 passi ordinari. Le nuove asserzioni non aggiungono passi firmware. Ghidra 12.1.4, programma B `-readOnly -noanalysis`, linguaggio ILM isolato per la catena emulativa. Prossimo obiettivo: caratterizzare separatamente l'istruzione SRCH1 usando le risorse locali disponibili e verificare ingressi/flag; ingresso interrupt da S=1, cambio effettivo di contesto e trigger hardware restano aperti.


## AB-115 · SRCH1 decodificata ma bloccata da clz

Componente B, campione AB-1, 10 novembre 2026. [D90Srch1Probe.java](../ghidra/D90Srch1Probe.java) prova isolatamente la parola originale 0x97dd da 0x40e32 nel linguaggio `fr81:BE:16:default`. Il progetto B conserva il proprio profilo: il test crea un programma transitorio. Non è stata eseguita la selezione del contesto AB-114.

Il pacchetto locale contiene i sorgenti FR81 ma non conteneva `fr81.sla`. Sono stati verificati contro `ghidra/language-profile.sha256` e copiati fuori dall'installazione sette file: fr60.ldefs, fr60.sinc, fr60.pspec, fr60.cspec, fr60.dwarf, fr81.slaspec, fr81ext.sinc. La copia locale `work/fr81-probe-ab115` è stata compilata con lo Sleigh di Ghidra 12.1.4 e Java 21.0.13, senza modifiche ai sorgenti. [language-profile-fr81-probe.sha256](../ghidra/language-profile-fr81-probe.sha256) registra gli input e l'artefatto compilato usato. Non è stata applicata la correzione ILM AB-85 a FR81 e la disposizione dei registri differisce da FR60.

Nel sorgente `fr81ext.sinc`, SRCH1 salva l'operando temporaneamente, imposta il risultato a 32 e termina subito se l'operando è zero; altrimenti richiama `clz`. La decodifica della parola originale produce SRCH1 AC. **Il ramo non nullo non è eseguibile nel banco attuale:** EmulatorHelper segnala `Unimplemented CALLOTHER pcodeop (clz)` al primo ingresso non nullo, 0x00000001.

La griglia prevista comprende 38 ingressi (zero, 32 bit singoli e cinque pattern multipli), ciascuno con flag iniziali zero/uno. Il primo tentativo iniziava dal valore 1 e si arrestava immediatamente (`work/srch1-probe.log`). Il diagnostico finale riordina gli ingressi per separare il ramo zero: **due casi zero completati**, AC=32 e PC=0x40e34, conservando i 31 registri elencati nel sorgente e gli otto byte di codice/guardie. Al primo nonzero si arresta e dichiara la griglia inconcludente. Non si afferma il successo dei restanti 74 casi, né la conservazione dello stato dopo l'errore: il modello può avere già modificato AC prima della userop fallita.

Log finale `work/srch1-probe-diagnostic.log`: due casi positivi e un errore emulativo clz esplicitamente riconosciuto, seguito da riepilogo INCONCLUSIVE. Il processo può terminare con codice zero perché lo script riconosce il blocco: occorre leggere il riepilogo. La compilazione finale (`work/fr81-probe-build-final.log`) termina con codice zero ma avvisa della presenza di sei costruttori NOP; non è una validazione completa del profilo. Il primo avvio del compilatore (`work/fr81-probe-build.log`) era fallito per la directory utente Ghidra non scrivibile, prima della compilazione; risolto usando `-Duser.home` nella directory locale già disponibile.

Il risultato precisa il limite segnalato in AB-27/related-projects: la presenza della semantica SRCH1 nel sorgente e la sua compilazione non garantiscono che le operazioni richiamate siano implementate nell'emulatore. Prossimo passo: definire e verificare una soluzione circoscritta per il conteggio degli zeri iniziali, evitando di assumere la compatibilità generale FR81 o di integrare un risultato non verificato nella catena AB-113.


## AB-116 · Adattatore esplicito SRCH1 AC

Componente B, campione AB-1, 10 novembre 2026. [D90Srch1Adapter.java](../ghidra/D90Srch1Adapter.java) introduce `searchOne` per il solo sito 0x40e32 e la parola 0x97dd. Il metodo verifica sito, byte originali e assenza di delay slot dichiarata dal chiamante, poi conta gli zeri iniziali dei 32 bit di AC mediante una maschera che scorre dal bit 31 verso il bit 0. Scrive soltanto AC e PC=0x40e34. Per zero restituisce 32; per bit 31 impostato restituisce zero. **Non esegue un passo emulativo dell'istruzione** e non implementa la userop clz nel motore Ghidra.

La semantica adottata deriva dalla definizione locale FR81 esaminata in AB-115 e viene confrontata con `Integer.numberOfLeadingZeros` del runtime Java, indipendente dal ciclo a maschera dell'adattatore. La prova usa il profilo FR60 ILM isolato AB-85; non richiede di passare al profilo FR81 né modifica il linguaggio installato. Le due prove native zero di AB-115 restano l'unico confronto emulativo nativo riuscito per SRCH1; i casi nonzero non costituiscono una validazione hardware.

**76 casi positivi** combinano 38 ingressi (zero, 32 bit singoli, 0xffffffff, 0x7fffffff, 0xaaaaaaaa, 0x55555555, 0x80000001) con due configurazioni di flag, zero/uno. Verificati risultato AC, avanzamento PC di due byte, conservazione dei 31 registri elencati nel test (inclusi PS, ILM e flag) e degli otto byte di codice/guardie. ILM iniziale è 0 o 31. L'adattatore restituisce zero sulla bitmap 0x80000000 di AB-114, ma non esegue ancora le istruzioni successive di selezione della lista.

Quattro casi negativi verificano sito dispari con opcode presente, sito pari diverso con opcode presente, opcode errato al sito ammesso e indicazione di delay slot. I rifiuti avvengono prima delle scritture e conservano i 33 registri controllati (inclusi PC/AC) e gli otto byte di memoria. Non è un controllo della pipeline né di tutte le codifiche SRCH1: la variante con registro diverso da AC e altri siti non sono supportati.

Log `work/srch1-adapter.log`: 76 casi validi e quattro rifiuti superati, senza WARN/ERROR/eccezioni; Ghidra 12.1.4, programma B `-readOnly -noanalysis`, programma transitorio. Sono chiamate dell'adattatore, **zero istruzioni firmware eseguite nativamente in questa prova**. Il fallimento nativo clz e la griglia inconcludente AB-115 restano validi e non vengono sostituiti da una dichiarazione di compatibilità FR81. Prossimo passo: collegare il caricamento originale della bitmap, l'adattatore e il confronto del candidato con il contesto corrente, fermandosi prima del salvataggio/cambio di contesto; il vero ingresso interrupt da S=1 resta distinto e aperto.


## AB-117 · Selezione delimitata del candidato sullo stato inizializzato

Componente B, campione AB-1, 10 novembre 2026. [D90DispatchCandidate.java](../ghidra/D90DispatchCandidate.java) estende AB-114 e mantiene lo stato prodotto dall'avvio e dal ciclo utente AB-113. Il banco imposta **PC=0x40e2a** senza introdurre un frame di interrupt: questo è un ingresso sintetico nel tratto di selezione, non il seguito naturale del ciclo 0x407ee né una prova del vettore 0x3f.

Sono aggiunte le finestre originali `[0x40e2a,0x40e64)` e `[0x404fa,0x404fc)`. Le istruzioni 0x40e2a/0x40e30 caricano R12=0x68000004 e AC=0x80000000 dalla memoria preparata. A 0x40e32 viene chiamato l'adattatore SRCH1 AB-116, che produce AC=0 e PC=0x40e34. Le istruzioni successive leggono il candidato 0x680001ec dalla prima lista e, con i byte di controllo dello stato AB-114, attraversano il controllo a 0x40e42–0x40e46, la chiamata al hook 0x404fa (verificato RET) e il confronto con il controllo corrente. Il BEQ a 0x40e60 non è preso: la prova si ferma **prima di STM1 a 0x40e62**, con AC=0x680001ec, FP=0x680007c4, R12=0x68000000 e Z=0.

Otto scenari verificano SP=USP=0x68000f20, SSP=0x81004000 e RP conservati, con S=1. I 4096 byte della regione OS `[0x68000000,0x68001000)` sono confrontati con lo stato iniziale ammettendo soltanto la parola a 0x68000f1c scritta dall'originale ST RP,@-SP a 0x40e50. Il pop recupera RP/SP ma non cancella quella parola: non si dichiara tutta la memoria OS invariata. Liste, bitmap e controllo corrente restano conservati nel confronto. Separatamente si verificano i 40 byte del contesto candidato a 0x8f991204 e i 256 byte dello stack supervisore `[0x81003f80,0x81004080)`.

Log `work/dispatch-candidate.log`: **otto selezioni delimitate superate**, senza WARN/ERROR/eccezioni; 103096 passi ordinari controllati, 152 oltre AB-114 (19 per scenario). Le otto sostituzioni SRCH1 non sono istruzioni eseguite nativamente e non entrano nel conteggio. Ghidra 12.1.4, programma B `-readOnly -noanalysis`, profilo ILM isolato. I riepiloghi ereditati descrivono confini precedenti; il riepilogo AB-117 riguarda la nuova selezione.

Questa prova conferma la scelta del candidato **date le precondizioni imposte**. Il prefisso 0x40d44–0x40e2a, il salvataggio iniziale R12/AC/FP e il frame PC/PS dell'interrupt non sono eseguiti o costruiti: proseguire direttamente al cambio di contesto da questo stato non verificherebbe il salvataggio reale del contesto interrotto. Prossimo obiettivo: ingresso sintetico esplicito da S=1 nel vettore 0x3f e prefisso del gestore fino al caricamento della bitmap, con controllo del frame e dei registri prima di ricollegare la selezione. Trigger, accettazione e tempi hardware restano aperti.
