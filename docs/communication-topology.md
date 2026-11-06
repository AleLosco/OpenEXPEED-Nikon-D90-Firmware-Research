Warning: truncated output (original token count: 40763)
Total output lines: 1022

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
| 0x101ce6 | Tutti i percorsi provati scrivono 1. R8 e R9 conservano i valori iniziali…28763 tokens truncated…per l'esperimento. Questa preparazione non è un modello dell'accettazione hardware, del livello assegnato alla richiesta o della cadenza; viene forzata anche quando I iniziale è zero. Non è eseguita la pcodeop d'ingresso di Ghidra. Nessuna istruzione del chiamante viene eseguita fra le invocazioni.

L'involucro e la routine 0x40a56 vengono eseguiti dalle istruzioni originali fino a RETI, poi AB-87 applica la sincronizzazione SSP dichiarata. Il test controlla a ogni ingresso il passaggio per 0x4049e e 0x40a56 e la scrittura di 0x001a a 0x4e, rappresentato in RAM: non le attribuisce effetti di riconoscimento o cancellazione di una richiesta elettrica.

Una previsione separata nel test decrementa la testa della lista fino al primo risultato non negativo e trasferisce le teste negative. Dopo ogni invocazione vengono confrontati i collegamenti bidirezionali di entrambe le liste, i campi +0x14 di tutti i nodi e i loro stati +8 (zero se trasferiti, 0x20004 altrimenti). Il contatore deve essere uguale al numero delle invocazioni fornite. Il campo nodo del servizio+0x20 conserva 0xffab con sentinella 0x6666 nei due byte seguenti. Sono inoltre confrontati i 18 registri R0–R12/AC/FP/RP/MDL/MDH prima e dopo ciascun involucro e PC=0x19e066, SP=SSP=0x81004000, PS/ILM/I iniziali.

Negli **otto scenari**, tutte le combinazioni delle quattro liste e I=0/1, il nodo prodotto dal servizio raggiunge la struttura 0x68000010 dopo **tre ingressi sintetici** del vettore 0x18. Sono 24 ingressi complessivi successivi agli otto servizi. I casi con nodi già presenti mantengono il loro stato fra gli ingressi; non si assegna alcuna unità temporale al conteggio tre.

Log finale `work/queue-interrupt-integrated.log`: otto scenari superati senza WARN, ERROR o eccezioni; 3032 istanze di passo controllate senza accessi p-code diretti a SSP/USP nei limiti della scansione AB-88. Ghidra 12.1.4, Temurin 21.0.13, programma B `-readOnly -noanalysis`. I controlli di sentinella stack ereditati da AB-89 sono effettuati prima della fase IRQ; non sono presentati come sentinelle intatte attraverso l'involucro più profondo.

La continuità verificata è **servizio → lista d'attesa → routine del vettore → lista destinazione**, con memoria e ingressi sintetici. Non viene eseguito uno scheduler o il codice del nodo trasferito. Restano sconosciuti sorgente fisica del vettore 0x18, livello ILM reale, frequenza, effetti delle scritture periferiche, comportamento concorrente e ripresa del task sulla fotocamera. Prossimo passo: risalire all'inizializzazione della sorgente associata al vettore e alle scritture correlate, senza assumere che si tratti già di un timer identificato.


## AB-93 · Inizializzazione e controlli software sugli indirizzi del vettore 0x18

Componente B, campione AB-1, 6 novembre 2026. [D90QueueSourceScan.java](../ghidra/D90QueueSourceScan.java) cerca nei blocchi inizializzati le costanti 0x404ba, 0x404c8 e 0x40070050. La prima ha quattro occorrenze letterali nelle candidate LDI:32 a 0x16398e, 0x163a2a, 0x165c52 e 0x166248; 0x404c8 non ha occorrenze letterali e 0x40070050 compare nella LDI:32 a 0x404cc. Questi conteggi non escludono chiamate relative o riferimenti costruiti. Non sono state stampate righe per i riferimenti già registrati a 0x404ba/0x404c8. Log `work/queue-source-scan.log`.

[D90QueueSourceContext.java](../ghidra/D90QueueSourceContext.java) controlla i candidati e mostra chiamate a 0x404ba tramite R12 a 0x163994 (con delay slot ADD R9,R8), 0x163a30, 0x165c58 e 0x16624e. Una scansione allineata ogni due byte in [0x40000,0x42000) non trova CALL dirette verso 0x404ba o 0x404c8; non equivale a un grafo completo dei chiamanti e può includere decodifica di dati. Conserva inoltre le finestre [0x40738,0x40740), [0x4046a,0x4048a) e quelle dei quattro candidati sopra, come definite nello script. Log statico finale incluso in `work/queue-source-control.log`.

Nel percorso d'avvio già contestualizzato in AB-24–AB-29, 0x40738 carica 0x4046a in R12 e 0x4073e chiama R12. La routine salva RP, scrive byte 0x12 a 0x448, halfword 0x9e34 a 0x48 e halfword 0x1b a 0x4e; poi a 0x40484 chiama la funzione 0x100936. Il codice successivo recupera RP e ritorna, ma la prova di questo passaggio si ferma prima di tale chiamata. L'indirizzo 0x4e coincide con quello scritto dal gestore del vettore 0x18 (AB-92); questo collega i percorsi tramite lo stesso indirizzo, non identifica da solo il circuito o una frequenza.

[D90QueueSourceControl.java](../ghidra/D90QueueSourceControl.java) esegue sei casi in RAM sintetica sul programma B originale:

| Percorso | Input e verifiche |
| --- | --- |
| 0x404ba, quattro casi fino a RET | Halfword 0x4e iniziale 0x0000→0x001b; 0x0002→0x0002; 0xaaaa→0xaaaa; 0xffff→0xffff |
| 0x404c8 fino a RET | Halfword 0x4e=0; halfword 0x40070050=0x4000; byte 0x482 scritto e riletto come 0x23 |
| 0x40738 fino a 0x40484 | Le tre scritture d'avvio sopra, AC=0x100936, SP=0x81003ffc e parola di ritorno sullo stack=0x40740 |

La funzione a 0x404ba esamina il bit 1 del byte 0x4f, cioè il byte basso della halfword big endian a 0x4e: se spento scrive 0x001b alla halfword, se acceso ritorna senza sostituirla. I quattro casi coprono i due rami, non tutti i valori iniziali. Per 0x404c8 la parola a 0x40070050 parte da 0x55556666 e termina 0x40006666, verificando che i due byte successivi alla halfword restino 0x6666. Non viene attribuito alcun significato elettrico alle scritture. Il test non chiama i quattro chiamanti applicativi della funzione 0x404ba, ma esegue direttamente la routine.

Log `work/queue-source-control.log`: sei casi superati senza WARN, ERROR o eccezioni; Ghidra 12.1.4, Temurin 21.0.13, programma B `-readOnly -noanalysis`. Le due prove isolate fino al ritorno usano RP sentinella 0x80000000 e non eseguono codice a tale indirizzo. Il prefisso d'avvio usa limite 30 passi e le routine isolate limite 50. Non servono adattatori INT o SSP.

Il risultato verificato è il collegamento software fra l'avvio e scritture agli stessi indirizzi impiegati dal gestore della coda. La natura di 0x48/0x4e/0x448/0x482 e 0x40070050 non è identificata come periferica Nikon specifica; non si attribuiscono nomi di bit o durata al valore 0x9e34. La ricerca esterna svolta non ha fornito un riscontro primario sufficiente a cambiare questo limite. Non è eseguita l'inizializzazione completa né provata l'attivazione del vettore. Prossimo passo: analizzare la chiamata a 0x100936 e ricostruire le precondizioni del percorso d'avvio prima di cercare una cadenza fisica.


## AB-94 · Prefisso continuo di 0x100936 fino al servizio -83

Componente B, campione AB-1, 6 novembre 2026. [D90SourceHelperContext.java](../ghidra/D90SourceHelperContext.java) conserva le finestre [0x100936,0x10095e), [0x10095e,0x100cec), [0x100cec,0x100d24). La funzione 0x100936 salva RP, crea un frame con ENTER 0xc e chiama nell'ordine 0x10095e, 0x100cec e 0x100d18. Prepara poi R4=FP−8, selettore R12=-83 e raggiunge INT #0x40 a **0x10094e**. Il seguito statico contiene chiamate a 0x100d24, 0x100d3e e 0x100f00, poi LEAVE e ritorno: queste operazioni successive non sono eseguite nella prova. Log statico `work/source-helper-context.log`.

[D90SourceHelper.java](../ghidra/D90SourceHelper.java), con gli helper di AB-90, entra da 0x40738 e attraversa senza sostituire chiamate 0x4046a→0x100936 e i suoi primi tre callee, fermandosi prima di INT. Prova due stati di RAM sintetica: byte iniziali tutti zero oppure tutti 0xff negli intervalli [0,0x1000) e [0x6b000000,0x6b000100). Lo stack [0x81003f00,0x81004000) è inizializzato a 0x5a. Registri generali, speciali e flag usati sono inizializzati a zero, salvo SP=0x81004000 e RP=0x12345678. Dopo l'avvio non vengono iniettati dati. Ogni caso raggiunge il confine in **463 passi**, entro il limite di 1000.

Il test verifica il passaggio per 0x100936/0x10095e/0x100cec/0x100d18 e i seguenti risultati selezionati; non pretende una specifica completa di ogni scrittura della lunga routine 0x10095e:

| Indirizzo/campo | Risultato verificato |
| --- | --- |
| halfword 0x48; halfword 0x4e; byte 0x448 | 0x9e34; 0x001b; 0x12 conservati dopo i callee |
| byte 0x40; 0xa0; 0x41; 0xa1 | 0; 0; 0x41; 4 |
| parola 0x6b000008 | 0xdfffffff |
| parola 0x6b000000 | valore iniziale AND 0xfeffffff |
| parola 0x6b000018 | valore iniziale OR 0x08000000 |
| parola 0x130 | 0xffffffff |
| halfword 0x13e; 0x4f8; 0x4fe | 0x0a13; 0x007e; 0x0873 |
| byte 0x7ef | 0x14 |
| R12; R4; SP al confine | -83; FP−8; 0x81003fec |

La funzione 0x10095e esegue inoltre una lunga sequenza statica di letture/modifiche/scritture nel banco a partire da 0x440 e ai registri bassi; la finestra nel relativo script conserva le istruzioni senza attribuire nomi di periferica. Il test controlla solo gli effetti selezionati in tabella. Non sono inferiti frequenze, livelli interrupt effettivi o effetti elettrici dai valori osservati in RAM.

Il blocco di otto byte puntato da R4 vale **5a5a000000000000** in entrambe le prove. Le istruzioni a 0x100946/0x100948 scrivono zero alla halfword FP−6 e alla parola FP−4. I primi due byte a FP−8 conservano la sentinella dello stack predisposta dal test: **0x5a5a non è un valore firmware recuperato o una costante Nikon**. Prima di chiamare il blocco un descrittore completamente inizializzato occorre studiare come il servizio -83 lo interpreta e l'origine dei byte non scritti in questo percorso. Non si conclude da questa sola osservazione che esista un bug nel firmware.

Log `work/source-helper.log`: due casi superati senza WARN, ERROR o eccezioni. Ghidra 12.1.4, Temurin 21.0.13, programma B originale in `-readOnly -noanalysis`. Il modello sperimentale AB-85 non è necessario perché INT non viene eseguita; non si applicano adattatori INT/SSP. I 463 passi non sono una misura temporale. Non viene completata la funzione 0x100936 né l'inizializzazione della fotocamera. Prossimo passo: identificare il ramo del servizio -83 e i campi effettivamente letti dal blocco R4, poi valutare la prosecuzione oltre INT. Sorgente hardware del vettore 0x18 e frequenza restano aperte.


## AB-95 · Il servizio -83 imposta il contatore e ignora i primi due byte

Componente B, campione AB-1, 6 novembre 2026. [D90Service83Context.java](../ghidra/D90Service83Context.java) verifica il calcolo della voce `0x40672 + 2×(-83) = 0x405cc`: la halfword con segno vale 0x087e e conduce a **0x40ef0**. Conserva le finestre [0x40ef0,0x40f0e) e [0x40f84,0x40f88), circoscritte al corpo osservato e al ritorno d'errore. Il listato finale è incluso in `work/service83.log`.

A 0x40ef0 il servizio copia R4 in FP, legge una halfword senza segno a FP+2 in R5 e una parola a FP+4 in R6. Non legge FP+0/FP+1 nel percorso disassemblato. Sposta R5 in R7, esegue LSR2 0x10 e controlla carry. Se il bit 15 del campo alto è acceso raggiunge 0x40f84, che ritorna caricando R12=0xdf nel delay slot. Negli altri casi scrive R6 alla parola 0x68000114 e R5 alla parola 0x68000110, fra ANDCCR 0xef e ORCCR 0x10, poi ritorna con R12=0. La descrizione della funzione come impostazione del contatore è supportata da queste scritture; non è un nome OS recuperato.

[D90Service83.java](../ghidra/D90Service83.java), con gli helper di AB-90, parte dal dispatcher a 0x406b6 con selettore -83 e R4=0x81001000. Attraversa 0x40ef0 e si ferma al ritorno locale 0x406c6, prima dell'epilogo. Il blocco sintetico ha due byte iniziali 0 oppure 0xffff, campo alto +2 scelto fra 0,0x7fff,0x8000,0xffff e campo basso +4 zero oppure 0x89abcdef: **16 combinazioni**.

| Campo alto +2 | Esito provato | Parole contatore finali |
| --- | --- | --- |
| 0 oppure 0x7fff | R12=0 | 0x68000110=campo alto, 0x68000114=campo basso |
| 0x8000 oppure 0xffff | R12=0xdf al ritorno locale | Conservate 0x11112222 e 0x33334444 predisposte dal test |

Ogni caso controlla la voce di tabella, il passaggio nel servizio, SP finale=0x81004000 e il blocco di input interamente invariato. Le varianti dei primi due byte non cambiano l'esito. In questa prova l'epilogo non estende con segno R12: 0xdf è il valore locale osservato, non un codice già trasformato in -33 dal dispatcher. Il limite è 60 passi per scenario.

Questo risolve la domanda delimitata di AB-94: la sentinella 0x5a5a nei primi due byte del blocco di avvio non viene letta dal corpo di questo servizio. Gli altri sei byte, che il prefisso scrive a zero, sono quelli usati per impostare il contatore. Il dato non dimostra un bug e non giustifica attribuire ai byte ignorati un significato non osservato. Non si afferma che gli stessi byte siano ignorati da ogni altra routine firmware.

Log finale `work/service83.log`: 16 casi superati senza WARN, ERROR o eccezioni; Ghidra 12.1.4, Temurin 21.0.13, programma B originale `-readOnly -noanalysis`. Non viene eseguita INT, non si prepara il frame CPU completo e non si prosegue oltre 0x406c6. È una prova separata da AB-94, con blocco e contatore predisposti direttamente. Restano da verificare il passaggio continuo dall'avvio attraverso INT/servizio/RETI, i callee successivi di 0x100936 e l'identità fisica dei registri configurati. Prossimo passo: collegare il prefisso AB-94 al servizio e al suo ritorno mediante gli adattamenti espliciti già verificati, prima di attraversare i callee successivi.


## AB-96 · Continuità dall'inizializzazione al servizio -83 e ritorno RETI

Componente B, campione AB-1, 6 novembre 2026. [D90SourceServiceIntegrated.java](../ghidra/D90SourceServiceIntegrated.java) collega il prefisso AB-94 al dispatcher e al servizio AB-95, senza sostituire il blocco costruito dal firmware né saltare il corpo del servizio. Usa il linguaggio isolato PS/ILM a cinque bit di AB-85, l'adattatore esplicito INT di AB-86 e il ritorno supervisore AB-87. La sorgente dei byte è il programma Ghidra locale B aperto in sola lettura, non una nuova validazione indipendente del contenitore firmware originale.

Copia in ProgramDB transitorio le finestre [0x40738,0x40740), [0x4046a,0x4048a), [0x100936,0x100d24), [0x404f4,0x404fa), [0x405cc,0x405ce), [0x4067a,0x406f6), [0x40ef0,0x40f0e) e [0xdfefc,0xdff00). Verifica la voce vettore 0xdfefc=0x4067a e la voce servizio 0x405cc=0x087e. Crea RAM sintetica bassa, banco 0x6b000000, stack e parole del contatore, mantenendo invariati installazione Ghidra e database firmware.

La matrice incrocia riempimento periferiche 0/0xff, riempimento stack 0x5a/0xa5 e ILM iniziale 0/31, per **otto casi**. Gli altri registri generali e speciali sono inizializzati a zero, con SP=SSP=0x81004000, TBR=0xdfc00, S=I=0. Parte da PC=0x40738 e attraversa 0x100936, 0x10095e, 0x100cec e 0x100d18 fino a INT 0x10094e. Verifica R12=-83, SP=R4=0x81003fec e il blocco reale di otto byte uguale a `5a5a000000000000` oppure `a5a5000000000000`. Le parole contatore iniziali 0x11112222/0x33334444 devono essere ancora intatte prima del servizio, rendendo verificabile la successiva modifica.

Poiché il modello mantiene SP e SSP separati durante le istruzioni di stack, il banco di prova esegue **SSP=SP esplicitamente al confine prima di INT**. L'adattatore costruisce poi il frame CPU e raggiunge il gestore originale. Lo script esegue prefisso, dispatcher, servizio a 0x40ef0 ed epilogo senza iniezioni nel corpo. Dopo RETI verifica e applica la sincronizzazione SSP di AB-87. Non è la pcodeop INT originale di Ghidra e non si afferma un alias SP/SSP generale.

Al ritorno verifica PC=**0x100950**, SP=SSP=0x81003fec, R12=0 e il recupero di R0–R11/AC/FP/RP/MDL/MDH, confrontati ai 17 valori effettivi prima di INT. PS viene confrontato alla parola salvata dall'adattatore, che compone lo stato dai flag del modello; ILM deve tornare al valore iniziale 0 o 31. Le due parole a 0x68000110/114 devono essere entrambe zero. L'intero blocco R4 deve restare invariato, compresi i byte iniziali provenienti dal riempimento sintetico. Non si esegue l'istruzione a 0x100950 e non si completa la funzione 0x100936.

Log finale `work/source-service-integrated.log`: otto casi superati senza WARN, ERROR o eccezioni; **4208 istanze di passo** controllate dalla scansione p-code delimitata come AB-88 senza accessi diretti a SSP/USP. Il conteggio esclude l'ingresso INT sostituito e il passo RETI gestito separatamente da AB-87; non è una misura di cicli hardware. Limite 1200 passi per fase. Ghidra 12.1.4, Temurin 21.0.13, programma B `-readOnly -noanalysis`.

La continuità dimostrata è prefisso di inizializzazione → impostazione a zero del contatore tramite servizio −83 → ritorno al punto successivo alla richiesta OS. Non è dimostrato che la periferica sia funzionante, che il vettore 0x18 sia attivato o che il contatore progredisca in tempo reale. Restano esclusi S=1, concorrenza, scheduler, comportamento elettrico e chiamate successive a 0x100d24/0x100d3e/0x100f00. Prossimo passo: analizzare questi callee successivi, estendendo il ritorno dell'inizializzazione solo con precondizioni ed effetti periferici dichiarati.


## AB-97 · Ritorno completo della chiamata d'inizializzazione a 0x40740

Componente B, campione AB-1, 6 novembre 2026. [D90SourceTailContext.java](../ghidra/D90SourceTailContext.java) conserva le tre finestre [0x100d24,0x100d3e), [0x100d3e,0x100df2), [0x100f00,0x100fc4). Nel listato esaminato questi tre callee effettuano scritture e letture/modifiche/scritture su indirizzi periferici e ritornano. Nessuno dei loro percorsi lineari provati richiede una nuova INT o un'attesa. Il nome o lo scopo fisico dei blocchi non viene inferito dai soli indirizzi.

[D90SourceInitializationReturn.java](../ghidra/D90SourceInitializationReturn.java) estende la prova AB-96 oltre 0x100950: attraversa 0x100d24, 0x100d3e e 0x100f00, poi LEAVE/ritorno di 0x100936, il recupero RP di 0x4046a e ritorna a **0x40740**. Si tratta del completamento della chiamata da 0x4073e, non dell'inizializzazione completa della fotocamera. Nessuna istruzione a 0x40740 viene eseguita.

La matrice e i controlli fino al ritorno del servizio -83 restano quelli di AB-96: riempimento periferiche 0/0xff, stack 0x5a/0xa5, ILM 0/31, per otto casi, incluse le asserzioni sui 17 registri al confine 0x100950. La copia originale viene estesa fino a 0x100fc4. Il test aggiunge RAM sintetica agli intervalli [0x40000000,0x40200000), [0x50000000,0x50000100), [0x6f000000,0x6f010000), riempiti con lo stesso byte periferico. Questi intervalli sono copertura di test, non una mappa fisica validata del SoC Nikon. Non vengono iniettati risultati dei callee o saltate le loro istruzioni.

Dopo il ritorno finale controlla SP=0x81004000, RP=0x40740 e FP=0x440, valore impostato dalla routine esterna prima della chiamata. Il contatore 0x68000110/114 resta zero. Non richiede che tutti i registri generali conservino i valori all'ingresso della routine: i callee li utilizzano. Il registro separato SSP resta 0x81003fec dopo le istruzioni ordinarie che recuperano lo stack; il test **verifica e dichiara** questo residuo del modello invece di presentarlo come un alias risolto. Non viene effettuata una nuova entrata interrupt dopo tale ritorno.

Sono verificate le seguenti scritture finali selezionate, con `H` uguale a 0 oppure 0xffff secondo il riempimento iniziale:

| Indirizzi | Verifica |
| --- | --- |
| halfword 0x50000008 | H OR 0x4000 |
| halfword 0x5000000c | 0x5300; i due byte successivi conservano H |
| halfword 0x40080002 e 0x40080006 | (H AND 0xff0f) OR 0x0030 |
| halfword 0x40090000, 0x400a0000, 0x40050000, 0x4003040a, 0x4013040a, 0x40020000, 0x400f0000, 0x400b0000, 0x40040000, 0x400e0000, 0x400c0000, 0x40010000, 0x4011a000 | zero; i due byte successivi conservano H |
| parole 0x6f004008, 0x6f00c008, 0x4011c000 | zero su tutti e quattro i byte |
| halfword 0x40040800 | 0x8000 |
| halfword 0x400c0004 | 0xc000 |
| halfword 0x4000d240, 0x4000d340, 0x4000d440, 0x4000d540 | 8 |
| byte 0x4001ff00 e 0x4001ff01 | zero |

I listati nelle tre finestre conservano anche le altre istruzioni; la tabella riporta ciò che è controllato dinamicamente, non una specifica completa delle periferiche o di ogni loro bit. Nessun effetto di queste scritture viene attribuito al circuito reale.

Log finale `work/source-initialization-return.log`: otto scenari completati senza WARN, ERROR o eccezioni; **5272 istanze di passo** controllate senza accessi p-code diretti a SSP/USP nel perimetro della scansione AB-88. Come in AB-96 il conteggio esclude INT sostituita e RETI gestita separatamente; non misura cicli o durata. Ghidra 12.1.4, Temurin 21.0.13, programma B `-readOnly -noanalysis`, linguaggio sperimentale e ProgramDB transitorio. Restano espliciti la sostituzione INT e le sincronizzazioni SSP ai confini già dichiarati; non si modifica il database originale o l'installazione Ghidra.

Il risultato dimostra il ritorno di una chiamata d'avvio attraverso il servizio -83 e tre callee di configurazione con RAM sintetica. Non dimostra periferiche inizializzate correttamente sull'hardware, accettazione del vettore 0x18, cadenza fisica, scheduler o avvio completo. Prossimo passo: confrontare lo stato di configurazione prodotto con gli ingressi del vettore 0x18, preservando la distinzione fra ingresso forzato dal test ed evento hardware accettato.


## Rettifica di riproducibilità AB-85/AB-86 · 6 novembre 2026

La revisione indipendente in sola lettura AB-78–AB-97 ha segnalato due lacune confermate nel sorgente: l'adattatore accettava un PC dispari contenente `1f 40`, e il builder verificava solo `fr60.sinc`, non gli altri quattro input. Non ha rieseguito prove né riletto i firmware originali. Non risulta che queste lacune abbiano alterato i casi storici, che usano PC pari e il profilo locale documentato; non costituiscono nuove conclusioni hardware.

`enter()` ora rifiuta il PC dispari prima di leggere l'opcode o scrivere frame/registri. La sesta prova negativa colloca `1f 40` a `0x80000001`. Tutti e sei i rifiuti verificano l'invarianza dei registri PC, SP, SSP, TBR, PS, ILM, I, S, C, V, Z, N, T, D0, D1, RP e dei 16 byte da `0x81003ff4` (frame e guardie). I cinque casi e il log originari di AB-86 rimangono evidenza storica; il conteggio corrente è 20 positivi e sei negativi. Non si rivendica atomicità in caso di errore durante le scritture di un ingresso valido.

Il builder verifica `fr60.sinc`, `fr60.slaspec`, `fr60.pspec`, `fr60.cspec`, `fr60.dwarf` contro il [manifesto originale](../ghidra/language-profile.sha256) prima di creare l'output. Scrive gli stessi byte letti e verificati, evitando una seconda lettura degli input. Registra i cinque digest d'ingresso e i sei artefatti generati prima della compilazione. Il [manifesto sperimentale](../ghidra/language-profile-ilm5.sha256) registra inoltre i sette artefatti realmente usati nella nuova prova, incluso `fr60.sla` compilato con Ghidra 12.1.4. I manifesti non certificano la correttezza semantica del modello.

Verifica Python: 11 test superati, di cui quattro nuovi metodi sul builder con input sintetici (cinque sottocasi di alterazione, file mancante, output interno alla sorgente, costruzione valida/digest e rifiuto di output esistente). Questi test non validano il linguaggio FR60. La copia reale `work/fr60-ilm5-review` è stata costruita dai cinque input installati verificati; la compilazione riporta il consueto avviso di quattro costruttori NOP.

Riesecuzione locale Ghidra 12.1.4 / Temurin 21.0.13, programma B in `-readOnly -noanalysis`, log `work/review-ab85-ab86-ab97-final.log`: AB-85 128 sequenze, AB-86 20 positivi e sei rifiuti, AB-97 otto scenari e 5272 passi controllati, senza WARN o ERROR nel log finale. La prima esecuzione (`work/review-ab85-ab86-ab97.log`) aveva segnalato registri non inizializzati nelle nuove letture di controllo dei casi negativi; il banco ora li inizializza esplicitamente. Restano i limiti di RAM periferica sintetica, adattamento INT/SSP esplicito e assenza di temporizzazione/hardware. Non sono state rieseguite tutte le prove AB-78–AB-97 e non è una riproduzione indipendente su un'altra macchina.
