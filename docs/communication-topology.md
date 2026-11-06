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

Limite del profilo FR60. La prima esecuzione senza correzioni ha scritto i dati sintetici sopra l'indirizzo di ritorno e ha tentato di eseguire 0x4d4d002a. Per ADD2, il [manuale Fujitsu](https://www.manualsdir.com/manuals/103548/fujitsu-fr-family-32-bit-microcontroller-instruction-manuel-cm71-00101-5e.html?page=98) prescrive l'estensione negativa dell'immediato a quattro bit. Il profilo usato calcola +4 a 0x121b42 e +6 a 0x121bac dove servono −12 e −10. Lo script compensa la differenza di 16 byte ai due punti di utilizzo; i sette casi si riferiscono a questa esecuzione corretta localmente. Le altre analisi che attraversano ADD2 con nibble immediato da 0 a 7 richiedono una verifica specifica.

## AB-54 · Impatto del difetto ADD2 sui percorsi emulati

Controllo locale sulle 23 prove Java della cartella ghidra che eseguono EmulatorHelper.step: 5 sul programma A, 18 sul programma B, inclusa AB-53 come controllo positivo. Copie strumentate hanno rieseguito i casi originali in progetti aperti in sola lettura. Tutti i 23 script hanno raggiunto il proprio messaggio di esito positivo. Solo D90HeaderProbe ha incontrato le codifiche interessate, a 0x121b42 e nel delay slot 0x121bac. Le sette prove di AB-53 includono la compensazione locale documentata sopra. Nei casi eseguiti dagli altri 22 script non occorre correggere i risultati per questo specifico difetto.

Metodo riproducibile: prima di ogni chiamata effettiva a step, leggere la mezza parola big endian all'indirizzo corrente, segnalando (op & 0xff80) == 0xa500. Per JMP:D e CALL:D indiretti (op >> 4 pari a 0x9f0 o 0x9f1), RET:D (0x9f20), CALL:D relativo (op >> 11 pari a 0x1b) e branch ritardati (op >> 12 pari a 0xf), controllare anche la mezza parola a PC+2. Quindi eseguire lo step originale. Le chiamate simulate o saltate non appartengono alla copertura. Nei replay Python fr_poll.py e directory_entry.py il calcolo del valore ADD2 usa già il nibble meno 16 (ispezione del sorgente).

Il listato statico di AB-49 contiene inoltre 0x108b1a: al registro R1 va aggiunto −14, mentre il profilo visualizza +2. L'istruzione si trova nell'intervallo saltato dalla prova AB-49. La base R9 e la store terminale verificate in quella prova restano distinte dal comportamento completo della routine. L'audit riguarda queste esecuzioni e questa famiglia di opcode.

## AB-55 · Origine immediata dei byte nel percorso di lettura entro un blocco

Componente B, campione AB-1. Il parser AB-53 richiede otto byte a 0x11dcd4. Dopo INT 0x40, il confronto a 0x11dcf0 seleziona due percorsi di servizio; entrambi chiamano 0x13b0b2. Questa richiama 0x1b2ddc, somma il conteggio prodotto e ripete avanzando il puntatore destinazione se lo stato è zero e il conteggio è positivo e inferiore al residuo. Il conteggio totale viene scritto a 0x13b0fa. Osservazioni statiche, con servizi OS non modellati.

0x1b2ddc divide il primo argomento in due metà da 16 bit per 0x1b1172. Sul percorso accettato usa il puntatore prodotto da questa chiamata, controlla il bit 0 del byte +2 e il bit 3 del byte +0, quindi passa la struttura a 0x1b251a. Nel ramo in cui gli indici ricavati con lo shift a 0x1b25bc–0x1b25c4 coincidono, 0x1b25e0 chiama 0x1b1988. Dopo il ritorno non negativo di 0x1b156e, il blocco a 0x1b19ec legge il puntatore a +0x80 della struttura e somma lo scostamento locale FP−8. Passa quel risultato come sorgente, il buffer del richiedente come destinazione e la lunghezza in R6 a 0x1bc8d2.

[D90ReadWindow.java](../ghidra/D90ReadWindow.java) ha eseguito il blocco da 0x1b19ec fino a 0x1b1a00, inclusa la copia originale, su d90.hex in sola lettura. Con una sorgente sintetica contenente i byte da 1 a 32, due richieste da otto byte con scostamento 0 e 3 hanno prodotto rispettivamente 1–8 e 4–11. Controllati anche i due byte adiacenti alla destinazione. Struttura, scostamento e puntatori sono predisposti: la prova verifica la copia e la sua sorgente immediata. Restano da seguire il popolamento del buffer a +0x80 e l'origine fisica dei dati. Non è stata eseguita l'intera catena dal parser.

## AB-56 · Riuso del buffer e chiamata indiretta per riempirlo

Componente B, campione AB-1. Sul percorso di AB-55 la routine 0x1b156e controlla il bit 0x02 del primo byte dell'oggetto. Se è acceso, ritorna zero. Altrimenti passa il puntatore letto a +0x80 alla catena 0x1b1524 → 0x1b8c0a. La voce selezionata è E = 0x8f6e8f6c + 0x60 × indice. Il CALL:D a 0x1b8c24 usa l'indirizzo letto da E+0x18. Nel delay slot carica R4 dalla prima parola di E. R5 contiene la posizione calcolata, R6 vale 1 e R7 punta al buffer. Un esito negativo della callback viene salvato a E+4 e tradotto in 0x80005011. Con un esito non negativo, 0x1b15b2 accende il bit 0x02 e la routine restituisce zero.

[D90BufferFill.java](../ghidra/D90BufferFill.java) ha eseguito le istruzioni da 0x1b156e fino al ritorno su d90.hex in sola lettura, con indice sintetico 1. Con flag 0x42 la callback non viene raggiunta. Con flag 0x40 riceve R4=42, R5=114, R6=1 e il puntatore buffer in R7. La posizione deriva in questo caso da 100 + ((5−2) << (11−9)) + (10 mod 4). Gli esiti sintetici 0 e 3 portano il flag a 0x42. Con −5 il flag resta 0x40, E+4 contiene −5 e il chiamante riceve 0x80005011. Tutti i quattro casi hanno superato i controlli. Il contenuto di prova è scritto dalla callback simulata: la destinazione effettiva della chiamata sulla D90 dipende dall'inizializzazione di E+0x18. L'identità del dispositivo fisico resta aperta.

## AB-57 · Installazione della funzione di lettura a +0x18

Componente B, campione AB-1. L'installatore 0x1b7ece riceve in R6 un vettore di tre puntatori. La store a 0x1b7f4a copia il secondo (vettore+4) nella voce E+0x18 di AB-56. Nel percorso analizzato le altre due destinazioni sono E+0x14 ed E+0x1c. I controlli contro zero sono sequenziali: la seconda store precede il controllo del terzo puntatore. L'indice parte da zero e il limite del ciclo è 1. L'indice 1 scelto in AB-56 per la prova aritmetica era sintetico.

Un chiamante prepara sullo stack il vettore 0x1b8366, 0x1b8376, 0x1b839a e lo passa nel delay slot della chiamata a 0x1b84ac. Il secondo indirizzo, 0x1b8376, inoltra la lettura. Aggiunge alla posizione la parola contesto+4, legge il puntatore di tabella a contesto+8 e chiama la prima funzione della tabella a 0x1b838e, passando posizione risultante, quantità e buffer. Un altro percorso, tramite 0x1b8242, riceve il vettore dal chiamante. A 0x14cdfe e 0x14ce66 la sorgente è la cella 0x8f534144. La stringa A: fornita come primo argomento e la stringa ATAM fornita a 0x13a9ee a 0x14cdc4 sono indizi statici di registrazione; non identificano da sole un dispositivo fisico.

[D90SlotInstall.java](../ghidra/D90SlotInstall.java) ha eseguito il blocco 0x1b846e–0x1b84b0 con voce 0 libera, nome e ingressi predisposti. La routine originale ha restituito zero e scritto 0x1b8376 a 0x8f6e8f84. Ripartendo a 0x1b8c0a nella stessa RAM sintetica, il dispatch ha raggiunto il target di prova 0x420000 con posizione 114, quantità 1 e buffer 0x430000. La prova conserva il contesto sullo stack del chiamante e ferma la CPU prima dell'esecuzione del target. Non stabilisce quale registrazione sia attiva quando la D90 legge l'intestazione di AB-53.

## AB-58 · Vettore restituito per ATAM

Componente B del campione AB-1. La copia di avvio da 0x287a6c a 0x8f6e94c8 (vedi [boot](boot-and-ghidra.md)) comprende a 0x29128c il descrittore destinato a 0x8f6f2ce8. La prima parola punta alla stringa ATAM a 0x1cabc4. Il percorso 0x13a8ae → 0x13a8d4 prepara il record a 0x8f533068 e registra il suo indirizzo a 0x8f53308c. La ricerca a 0x13aaaa confronta le stringhe byte per byte. In 0x13a9ee, trovato il record, la chiamata indiretta a 0x13aa3c usa record+0x10 (0x13abe8). Se restituisce zero, 0x13aa4a scrive record+0x14, cioè 0x8f53307c, nel primo output; 0x13aa50 scrive nel secondo il contesto fornito dalla callback. Al sito 0x14cdc4 gli output sono rispettivamente 0x8f534144 e 0x8f534148. La seconda parola del vettore è 0x13acb4.

[D90AtamVector.java](../ghidra/D90AtamVector.java) ha prelevato dal programma B i 28 byte della tabella iniziale, incluso il terminatore, e ha eseguito registrazione e ricerca su RAM sintetica. Ha raggiunto 0x13abe8 e simulato il suo ritorno positivo nel senso di successo (R4=0, contesto 0x400000). Controllati entrambi gli output, ha eseguito la preparazione degli argomenti di A: e l'installatore originale 0x1b7ece, con prima voce libera, verificando ritorno zero e valore 0x13acb4 a 0x8f6e8f84. Il passaggio attraverso il wrapper 0x1b81f2 e i rami OS intermedi è ricostruito staticamente come in AB-57. La prova identifica il target per questa configurazione ATAM; l'attivazione sulla fotocamera e il supporto fisico restano da verificare.

## AB-59 · Unità da 512 byte e preparazione della richiesta ATAM

Componente B del campione AB-1. A 0x13acb4 gli argomenti sono contesto, posizione, quantità e buffer. Contesto, quantità o buffer nulli portano staticamente a 0xfffffcdf; il byte nullo a contesto+0x14 porta a 0xfffffc18. Il codice limita ogni richiesta a 0x400 unità e passa a 0x19d6ec la posizione sommata alla parola contesto+0x10. Dopo un successo, gli incrementi sono la quantità appena trattata per la posizione e la stessa quantità moltiplicata per 512 per il buffer. A 0x19d706 viene confrontata la somma a 32 bit di posizione e quantità con la parola a 0x6800cc44, poi viene controllato il bit 0x40 a 0x6800cf1f. Sul percorso accettato vengono scritti quantità, posizione e puntatore rispettivamente a 0x6800cd08, 0x6800cd0c e 0x6800cd10 prima della chiamata a 0x19dc7c.

[D90AtamRequest.java](../ghidra/D90AtamRequest.java) ha eseguito quattro casi in sola lettura. Con base 100, posizione 7 e quantità 1 il limite 108 è accettato. Con quantità 1025 si osservano due chiamate con (posizione, quantità, buffer) pari a (107,1024,0x410000) e (1131,1,0x490000). Con quantità 1, limite 107 oppure bit spento, si osservano tre fallimenti rispettivamente con 0xfff20006 oppure 0xfff20001; dopo la chiamata a 0x1a03d8 la routine esterna restituisce 0xfffffcce. Il banco simula 0x19dc7c azzerando il residuo e restituendo 0x40, e simula il ritorno di 0x1a03d8. Verifica il blocco di richiesta, non il trasferimento dei dati o l'identità del supporto fisico.

## AB-60 · Configurazione del trasferimento e conteggio residuo ATAM

Sul componente B del campione AB-1, 0x19dc7c controlla il bit 0x40 a 0x6800cf1f dopo INT 0x40. Sul ramo abilitato chiama 0x19efc4. Nel percorso statico di avvio, 0x19f036 scrive tramite 0x1a011e la posizione a 0x63000008, moltiplicata per 512 quando la parola a 0x6800cc48 è zero. Poi 0x19f040 chiama 0x19afee. Questa configura la coppia di indirizzi a 0x0dc8 e 0x0dcc con il valore fisso 0x63000020 e il puntatore al buffer di AB-59, e scrive quantità × 512 a 0x0dd8. Il valore a 16 bit scritto a 0x0dc4 dipende dall'allineamento del buffer. Questo schema è compatibile con una configurazione DMA. Il listato da solo non identifica il controller né dimostra il movimento dei dati.

Sul percorso che arriva a 0x19ddb2, la lettura effettiva è da 0x63000004. La metà alta diventa il nuovo residuo a 0x6800cd08; la posizione a 0x6800cd0c avanza della quantità precedente meno il residuo. Con residuo zero e bit 0 dello stato spento, viene azzerato il byte a 0x6800cf1c. L'epilogo a 0x19de02 restituisce il byte a 0x6800cf1f. Altri rami possono arrivare all'epilogo senza questo aggiornamento.

[D90AtamTransfer.java](../ghidra/D90AtamTransfer.java) ha eseguito separatamente la configurazione da 0x19afee al ritorno e il blocco di contabilizzazione da 0x19ddb2. In quattro casi con quantità 3, buffer 0x410000 più 0, 4, 2 o 1, ha verificato 1536 byte, entrambi gli indirizzi e i controlli 0x40ad, 0x404a, 0x4049, 0x4048. Con posizione iniziale 107 e residui imposti 0, 1, 2, 3, le posizioni finali sono state 110, 109, 108, 107. Progetto in sola lettura e registri di periferica rappresentati da RAM sintetica; la prova non esegue i servizi OS intermedi né un trasferimento fisico.

## AB-61 · Due produttori dei bit richiesti dall'attesa ATAM

Componente B, campione AB-1. Dopo il ritorno non nullo di 0x19efc4, la INT 0x40 a 0x19dcfc riceve R12=−170, R4=FP−4, R5=7, R6=0x30000000, R7=0 e AC=3000. Nel codice successivo il branch a 0x19dd06 decide sul ritorno in R12. Con zero si eseguono ulteriori controlli, fra cui la maschera 0x073e0000 sulla parola a 0x6800cd04. Con ritorno non nullo il firmware imposta il bit 0 dello stato e scrive 0x54 a 0x6800cf1b. Il solo ritorno dall'attesa non stabilisce che tutti i dati siano arrivati; il conteggio residuo è trattato in AB-60.

Due percorsi raggiungono il servizio −48 con R4=7. Il gestore 0x19b15a, puntato dalla parola a 0xdff0c e terminante con RETI, passa R5=0x10000000 a 0x19b184 quando (halfword[0xdc2] & 0x3f00) == 0x0100. L'altro percorso in 0x19a71e, dopo la scrittura di 2 a 0x63000030, passa R5=0x20000000 a 0x19a780 se il byte a 0x6800cf1e è 0x12, 0x19 o 0x22. La corrispondenza di identificativo e maschere collega i due produttori alla richiesta di attesa. AB-62 verifica la condizione OS per lo sblocco. L'ordine reale resta aperto.

[D90AtamWake.java](../ghidra/D90AtamWake.java) ha verificato in sola lettura gli argomenti a 0x19dcfc e nove percorsi dei produttori. Ingressi sintetici per 0xdc2: 0, 0x100, 0x8101, 0x300. Per 0x6800cf1e: 0x11, 0x12, 0x19, 0x22, 0x23. Lo script controlla arrivo al servizio e registri nei casi positivi e il salto oltre il servizio negli altri. La prova si ferma prima delle INT; non misura tempi, non riproduce un interrupt fisico e non assegna unità temporali a 3000.

## AB-62 · L'attesa ATAM richiede entrambi i bit

Componente B, campione AB-1. Con TBR iniziale 0xdfc00 la voce a 0xdfefc punta a 0x4067a. Il dispatcher a 0x406b6 usa una tabella di scostamenti a 16 bit con segno, relativa a 0x40672. Le voci a 0x4051e (servizio −170) e 0x40612 (−48) conducono rispettivamente a 0x41bfc e 0x41a5a.

A 0x41c46 il servizio di attesa legge i flag a oggetto+8. Con R7=0, come nella richiesta ATAM di AB-61, la condizione è `(~flags & mask) == 0`. Per la maschera 0x30000000, un solo bit lascia l'attesa pendente. Se la condizione è già soddisfatta il ramo porta a 0x41c98. In caso contrario il percorso a 0x41c60 salva maschera e modalità nel record del task. Con modalità 2 basta invece un'intersezione non vuota.

Il servizio −48 accumula i bit con OR a 0x41a6e–0x41a72 e verifica la condizione del task in attesa nel percorso a 0x41a8a. In assenza di azzeramenti intermedi i due bit possono quindi arrivare in chiamate distinte, in entrambi gli ordini. Questo stabilisce la condizione sui flag. Restano da determinare la sequenza sulla fotocamera e l'esito del trasferimento fisico.

[D90EventMode.java](../ghidra/D90EventMode.java) verifica le due destinazioni del dispatcher, poi confronta il percorso di attesa e quello di notifica con cinque valori dei flag (zero, ciascun bit isolato, entrambi, entrambi più il bit basso) e due modalità (0 e 2), per 20 verifiche. Prova separatamente l'accumulo nei due ordini. I blocchi sono eseguiti con stato sintetico, senza simulare un ciclo completo dello scheduler.

## AB-63 · Origine del selettore della seconda notifica ATAM

Componente B del campione AB-1. La routine a 0x19f75e riceve una parola in R4. A 0x19f762–0x19f772 ne estrae i bit 29–24 e salva il valore a sei bit nel byte a 0x6800cf1e, lo stesso letto dal produttore di AB-61. Il blocco a 0x19dfa2–0x19dfbc può anche azzerare questo byte. Per i selettori 0x12 e 0x19 la routine combina con OR la parola originale con 6. Per tutti gli altri, incluso 0x22, usa la parola originale. La chiamata a 0x19f798 scrive il risultato a 0x6300000c tramite 0x1a011e.

| Indirizzo della chiamata | Parola passata | Selettore salvato | Parola scritta |
| --- | --- | --- | --- |
| 0x19f050 (percorso di AB-60) | 0x123a0037 | 0x12 | 0x123a0037 |
| 0x19f158 | 0x193a0027 | 0x19 | 0x193a0027 |

In queste due parole i bit della maschera 6 sono già accesi. [D90AtamCommand.java](../ghidra/D90AtamCommand.java) esegue i due siti di chiamata fino al ritorno e verifica entrambe le scritture. Esegue poi il blocco con tutti i 64 selettori, usando parole sintetiche con i bit 31–30 accesi e quelli della maschera 6 spenti. Per ciascun caso riparte da 0x19a75c sulla stessa memoria e verifica il ramo di AB-61 fino alla notifica o al suo salto. Solo 0x12, 0x19 e 0x22 raggiungono la notifica con maschera 0x20000000. Questo collega il campo di comando alla decisione software del gestore. Il caso 0x22 era sintetico nella prova iniziale. AB-67 identifica e verifica un chiamante che lo produce. L'evento fisico che avvia il gestore resta da stabilire.

## AB-64 · Tre chiamanti del percorso ATAM a comando variabile

Componente B del campione AB-1. La routine a 0x19f274 conserva R4 in R9 e R5 in R11. Dopo il controllo a 0x19f6fc, il percorso accettato scrive R11 a 0x63000008. La chiamata a 0x19f2ae passa R9 a 0x19f75e tramite il delay slot. Nei tre chiamanti individuati con un riferimento letterale a 0x19f274, il valore variabile è il secondo argomento. La parola di comando è immediata in ciascun sito.

| Chiamata | Comando | Selettore | Secondo argomento (aritmetica a 32 bit) |
| --- | --- | --- | --- |
| 0x1a0da0 | 0x201a0000 | 0x20 | R6 << 9 |
| 0x1a0de6 | 0x211a0000 | 0x21 | (word[R8] + word[R8+4]) << 9 |
| 0x1a0e2c | 0x261b0000 | 0x26 | (word[R8] + word[R8+4]) << 9 |

[D90AtamCommandCallers.java](../ghidra/D90AtamCommandCallers.java) esegue ogni percorso dalla preparazione degli argomenti, con R6=3 e parole sintetiche 5 e 2. Con il registro a 0x63000024 pari a zero, raggiunge 0x19f2b2 e verifica argomento, comando scritto a 0x6300000c e byte a 0x6800cf1e. Con il registro pari a uno raggiunge il primo tentativo di ripetizione a 0x19f2c8, con i tre valori sentinella intatti. Sono sei casi con periferica rappresentata da RAM. I tre siti escludono 0x22 lungo questi percorsi. La ricerca per indirizzo letterale non esclude chiamate indirette calcolate né altre scritture al byte selettore.

## AB-65 · Ricerca circoscritta di scritture sovrapposte al selettore

Componente B, campione AB-1. [D90AtamStoreScan.java](../ghidra/D90AtamStoreScan.java) cerca, a indirizzi pari nell'intervallo [0x198000, 0x1a4000), le codifiche LDI:32 con immediato in [0x6800cc00, 0x6800d000). Da ciascuna propaga gli indirizzi costanti per un massimo di 32 istruzioni consecutive. Considera copie, estensioni senza segno, somme, sottrazioni, AND e OR. Per ogni STORE p-code con destinazione risolta verifica se l'intero intervallo scritto comprende 0x6800cf1e, anche quando la scrittura comincia prima del selettore.

Sono emersi 234 punti di partenza e 83 siti di scrittura con almeno una destinazione risolta. Solo 0x19dfbc (azzeramento) e 0x19f772 (campo del comando) raggiungono il byte in queste finestre. Entrambi sono controlli positivi già documentati in AB-63. Fra i 42 siti con destinazione sconosciuta in almeno una finestra, tre si risolvono in un'altra finestra e 39 restano indeterminati con questo metodo. Lo script ne stampa indirizzo e istruzione. Per esempio a 0x19dd90 la finestra non conosce R9, ma nel percorso di errore di AB-61 la routine carica R9 con 0x6800cf1b a 0x19dc82 e scrive 0x54.

La scansione non dimostra raggiungibilità dei punti di partenza, si ferma ai cambi di flusso e non segue i puntatori caricati dalla memoria, passati dal chiamante o costruiti da basi fuori intervallo. Il risultato circoscrive la ricerca: nessuna nuova scrittura confermata di 0x22; restano aperti i percorsi esclusi da questi limiti.

## AB-66 · Destinazione delle sei scritture tramite R10

Componente B, campione AB-1. I sei siti segnalati in AB-65 appartengono a due routine. Quella a 0x19e1ca carica 0x6800cf1f in R10 a 0x19e1d6; quella a 0x19e2f8 carica lo stesso indirizzo a 0x19e2fe. Nel listato dei due corpi, fra questi carichi e i rispettivi siti, non compaiono altre assegnazioni esplicite a R10. Ciascun blocco legge il byte con LDUB, imposta il bit 0 con OR 1 e lo riscrive tramite R10. Una seconda STB scrive un valore immediato a 0x6800cf1b.

| Inizio routine | STB tramite R10 | Seconda STB | Valore a 0x6800cf1b |
| --- | --- | --- | --- |
| 0x19e1ca | 0x19e210 | 0x19e214 | 0x10 |
| 0x19e1ca | 0x19e276 | 0x19e27a | 0x52 |
| 0x19e1ca | 0x19e2da | 0x19e2de | 0x50 |
| 0x19e2f8 | 0x19e35c | 0x19e360 | 0x10 |
| 0x19e2f8 | 0x19e4a4 | 0x19e4a8 | 0x50 |
| 0x19e2f8 | 0x19e5a0 | 0x19e5a4 | 0x52 |

[D90AtamStatusStores.java](../ghidra/D90AtamStatusStores.java) esegue l'inizializzatore R10 originale e poi le sette istruzioni del blocco selezionato, su memoria sintetica. Per ciascun sito prova gli stati iniziali 0x00, 0x80 e 0xa5. Tutti i 18 casi verificano il valore immediato, il risultato stato OR 1 e la conservazione dei tre byte intermedi, incluso il selettore a 0x6800cf1e. La prova salta i rami e le chiamate intermedie. Verifica l'effetto dei blocchi con il puntatore inizializzato dalla routine; non dimostra la raggiungibilità di ogni ramo sulla fotocamera né la conservazione di R10 attraverso ogni chiamata.

## AB-67 · Un comando del firmware produce il selettore 0x22

Componente B, campione AB-1. Le due scritture tramite R8 segnalate da AB-65 impostano il bit 0 dello stato a 0x6800cf1f. La routine a 0x1a0a44 inizializza R8 a 0x1a0a6a; il ramo a 0x1a0ac8 porta alla STB a 0x1a0b42 se il ritorno dell'attesa è diverso da zero. La routine a 0x1a0b94 inizializza R8 a 0x1a0ba2; il ramo a 0x1a0bd8 porta alla STB a 0x1a0c34 se il ritorno della chiamata a 0x1a08ca è diverso da uno. [D90AtamR8Stores.java](../ghidra/D90AtamR8Stores.java) esegue ciascun inizializzatore e riparte dal confronto con ritorno sintetico 0 o 1. Con stati iniziali 0, 0x80 e 0xa5 verifica entrambi i rami e il byte selettore adiacente, per 12 casi.

Il produttore di 0x22 si trova prima dell'attesa. A 0x1a0a94 la prima routine chiama 0x1a07ae. Qui 0x223a0010 è l'argomento del controllo a 0x19f6fc; sul percorso accettato, a 0x1a0818 viene caricata una parola distinta, 0x223a0011. La chiamata indiretta a 0x1a0824 usa la destinazione immediata 0x19f75e e salva 0x22 a 0x6800cf1e. A confronto, nel percorso della seconda routine, la chiamata a 0x1a0922 usa la stessa destinazione con 0x233a0000 e salva 0x23.

[D90AtamCommand.java](../ghidra/D90AtamCommand.java) esegue ciascun sito dal caricamento della parola fino al ritorno della routine di scrittura. Verifica il byte e la parola completa a 0x6300000c; poi, sulla stessa memoria, avvia separatamente il predicato a 0x19a75c. Con 0x22 raggiunge 0x19a780 con maschera 0x20000000, con 0x23 raggiunge il ramo che salta la notifica. La prova copre ora quattro siti reali nel binario e 64 selettori sintetici. Il collegamento fra il chiamante esterno e il sito di comando è statico. Restano aperte l'origine dell'evento che avvia il gestore e la sequenza osservabile sulla fotocamera.

## AB-68 · Vettore di interrupt e selezione della notifica ATAM

Componente B, campione AB-1. Con TBR iniziale 0xdfc00 (AB-62), la voce del vettore 0x2c a 0xdff4c contiene 0x42de8. Questo wrapper salva i registri, chiama 0x19aee0 a 0x42df8 e termina con RETI a 0x42e04. Il dispatcher legge nell'ordine le parole a 0x63000038 e 0x63000030, poi calcola il loro AND a 0x19af10. Esamina in ordine le maschere 0x10, 0x20 e 0x02. Se nell'intersezione è presente 0x02 e mancano entrambe le precedenti, a 0x19af8a chiama 0x19a71e. Ogni ramo selezionato salta gli altri in questa invocazione.

Il gestore spegne 0x02 nella parola a 0x63000038, lo aggiunge con OR alla parola software a 0x6800cd00 e scrive 2 a 0x63000030. Legge quindi il selettore e raggiunge la notifica 0x20000000 per 0x12, 0x19 o 0x22 (AB-61, AB-67). Il dispatcher ha inoltre una propria notifica 0x00800000 a 0x19afe6, distinta da quella condizionata dal selettore.

[D90AtamInterrupt.java](../ghidra/D90AtamInterrupt.java) legge la voce originale e parte dal suo indirizzo di destinazione. Nove casi eseguono senza salti imposti il wrapper, il dispatcher e i rami scelti fino alla prima INT di notifica. Verificano ingresso nel gestore, identificativo 7, servizio −48, maschera e tre parole di memoria. Con 0x02 in entrambi i registri sono stati provati quattro selettori; con 0x02 in uno solo il gestore viene saltato. Con 0x32 in entrambi prevale 0x10; con 0x22 in entrambi prevale 0x20; con 0x32 nel primo e 2 nel secondo prevale 2. Le parole di periferica sono RAM sintetica: si verificano i valori letti e scritti dal firmware. L'origine elettrica dell'interrupt, l'effetto hardware della scrittura di 2 e i tempi restano da determinare.

## AB-69 · Abilitazione del bit 0x02 prima del comando 0x223a0011

Componente B, campione AB-1. Nel percorso accettato di 0x1a07ae, la chiamata a 0x1a0816 passa 0x01700022 alla routine 0x19f7a2. Questa combina il valore con OR prima con la parola a 0x63000034, poi con quella a 0x63000038. È il secondo aggiornamento a predisporre il bit 0x02 nella maschera letta dal dispatcher di AB-68. Il comando viene caricato soltanto al ritorno, a 0x1a0818, e inviato tramite 0x19f75e (AB-67).

| Punto raggiunto nel programma B | Operazione appena completata | Valore a 0x63000038 partendo da zero |
| --- | --- | --- |
| 0x1a07e6 | OR con 0x007f0021 | 0x007f0021 |
| 0x1a07f6 | AND con 0xffffffdf | 0x007f0001 |
| 0x1a080a | Ritorno dalla preparazione del trasferimento a 0x19afee | 0x007f0001 |
| 0x1a0818 | OR con 0x01700022 | 0x017f0023 |
| 0x1a0826 | Comando 0x223a0011 scritto a 0x6300000c | 0x017f0023 |

[D90AtamCommandMask.java](../ghidra/D90AtamCommandMask.java) esegue in continuità dalla prima istruzione di 0x1a07ae, con registri di periferica e richiesta di un'unità in RAM sintetica. Nei due casi accettati la maschera iniziale è 0 oppure 0x80000002: nel secondo il bit 0x02 era già presente e viene conservato. La prova verifica anche la parola a 0x63000034 dopo le due OR, il comando ancora assente a 0x1a0818 e il selettore 0x22 al ritorno. Con bit 0 di 0x63000024 impostato, il primo tentativo raggiunge 0x1a08aa conservando le due parole di controllo e le sentinelle di comando e selettore. La prova stabilisce quando il firmware predispone la maschera. L'evento che fa comparire 0x02 nella parola a 0x63000030 resta da identificare.

## AB-70 · Il gestore registra 0x02 dopo l'ingresso nell'interrupt

**Rettifica del 5 novembre 2026.** Il titolo originale è mantenuto per conservare il collegamento esistente. La conclusione precedente secondo cui stato iniziale zero permetterebbe l'ingresso in 0x19a71e è ritirata: contraddice AB-68 e gli esiti dello script. Il testo storico in fondo a questa sezione è conservato soltanto come registro della correzione.

La riesecuzione di `D90AtamInterrupt.java` sul programma B locale, con Ghidra 12.1.4 e Temurin 21.0.13, passa nove casi, senza WARN, ERROR o eccezioni nel log esaminato. Il conteggio ora deriva dai casi completati e ogni caso stampa ingressi, ingresso nel gestore, punto di arresto, R5 e stato finale. Le asserzioni e gli ingressi dei nove casi non sono stati cambiati.

| Maschera iniziale | Stato iniziale | Selettore | Ingresso in 0x19a71e | Arresto prima della INT | R5 | Stato finale |
| --- | --- | --- | --- | --- | --- | --- |
| 0x02 | 0x00 | 0x22 | No | 0x19afe6 | 0x00800000 | 0x00 |
| 0x02 | 0x02 | 0x22 | Sì | 0x19a780 | 0x20000000 | 0x02 |

Il primo caso mantiene anche la maschera a 2 e la parola software a 0x6800cd00 al valore iniziale 0x800. Il secondo porta la maschera a zero e la parola software a 0x802. Il dispatcher seleziona il ramo mediante l'intersezione fra maschera e stato: la scrittura successiva di 2 da parte del gestore non spiega l'origine del bit letto prima della selezione.

Il log locale è `work/atam-interrupt-ab70-correction.log` nella workspace di ricerca; gli ingressi e le asserzioni riproducibili sono nello script collegato sopra. Il progetto è stato aperto con `-readOnly -noanalysis`. È una riesecuzione sullo stesso host con MMIO in RAM sintetica, non una prova sulla fotocamera. L'inizio al wrapper è imposto dall'esperimento: non riproduce la generazione hardware dell'interrupt. L'effetto fisico della scrittura e l'origine del bit restano sconosciuti. La revisione indipendente del 4 novembre copriva AB-62–AB-69, non AB-70.

### Testo storico ritirato

Le due formulazioni seguenti sono errate e conservate per tracciabilità; non costituiscono risultati verificati.

> Componente B, campione AB-1. La catena AB-68 distingue la maschera richiesta a 0x63000038 dalla parola di stato a 0x63000030. Nel percorso che seleziona 0x19a71e, l'ingresso nell'interrupt avviene prima che il gestore scriva 2 nello stato. La prova al vettore 0x2c mostra che, con maschera 2, stato iniziale zero e selettore 0x22, il codice arriva alla notifica −48 con R5=0x20000000, dopo aver scritto lo stato 2. Quindi il valore 2 osservato in 0x63000030 alla fine di questo percorso è un effetto del gestore e non è necessario che fosse già presente per entrare nel gestore.
>
> [D90AtamInterrupt.java](../ghidra/D90AtamInterrupt.java) è stato esteso a dieci casi. Il nuovo caso parte dal vettore originale con maschera 2, stato zero e selettore 0x22; verifica l'ingresso, la notifica e la transizione di stato. È una prova su RAM sintetica. AB-69 documenta la predisposizione del bit 2 nella maschera richiesta prima del comando. AB-70 non individua quale evento della periferica causi l'interrupt né come il bit 2 venga impostato nel registro di stato prima di eventuali altri ingressi.

## AB-71 · Candidati di accesso allo stato tramite le funzioni MMIO

Analisi statica del 5 novembre 2026, componente B del campione AB-1 (SHA-256 `623906f285c656a7c6e4f3cecd26bc5e2b6da38ba07f0cc76a235f7e524814af`). [D90StatusCandidates.java](../ghidra/D90StatusCandidates.java) cerca nell'intervallo [0x198000, 0x1a4000) caricamenti `LDI:32 #0x63000000,R4` su indirizzi pari e stampa finestre di massimo 12 istruzioni contenenti il testo `#0x30,`. Si ferma al primo trasferimento di controllo e mostra anche il delay slot. Il filtro è euristico: i risultati sono candidati disassemblati, non una prova della raggiungibilità dei blocchi o una ricerca completa delle letture e scritture.

Nelle 23 finestre stampate, la lettura delle istruzioni mostra il calcolo R4=0x63000030 e chiamate indirette tramite R12: 21 alla funzione 0x1a011e e due alla funzione 0x1a011a. La prima contiene `RET:D` a 0x1a011e e `ST R5,@R4` nel delay slot a 0x1a0120; la seconda contiene `RET:D` a 0x1a011a e `LD @R4,R4` nel delay slot a 0x1a011c. I valori nella tabella sono quelli preparati localmente dal codice, compresi i delay slot delle chiamate.

| Inizio finestra | Chiamata | Operazione | Valore R5 scritto |
| --- | --- | --- | --- |
| 0x19a704 | 0x19a714 | Scrittura | 0x1 |
| 0x19a748 | 0x19a758 | Scrittura | 0x2 |
| 0x19a7ae | 0x19a7be | Scrittura | 0x4 |
| 0x19a854 | 0x19a86e | Scrittura | 0x30000 |
| 0x19a8cc | 0x19a8e0 | Scrittura | 0x10000 |
| 0x19a93e | 0x19a952 | Scrittura | 0x20000 |
| 0x19a954 | 0x19a968 | Scrittura | 0x20000 |
| 0x19a9c6 | 0x19a9da | Scrittura | 0x40000 |
| 0x19aa38 | 0x19aa4c | Scrittura | 0x80000 |
| 0x19aaac | 0x19aac2 | Scrittura | 0x100000 |
| 0x19ab24 | 0x19ab3a | Scrittura | 0x200000 |
| 0x19ab9c | 0x19abb2 | Scrittura | 0x400000 |
| 0x19abfc | 0x19ac12 | Scrittura | 0x800000 |
| 0x19ac5c | 0x19ac72 | Scrittura | 0x1000000 |
| 0x19acd4 | 0x19acea | Scrittura | 0x10000000 |
| 0x19ad34 | 0x19ad4a | Scrittura | 0x20000000 |
| 0x19ad94 | 0x19adaa | Scrittura | 0x40000000 |
| 0x19adf4 | 0x19ae0a | Scrittura | 0x80000000 |
| 0x19aefe | 0x19af0e | Lettura | — |
| 0x19af32 | 0x19af42 | Scrittura | 0x10 |
| 0x19af68 | 0x19af78 | Scrittura | 0x20 |
| 0x19d8c6 | 0x19d8dc | Scrittura | 0xf1ff01ff |
| 0x19da8a | 0x19da9a | Lettura | — |

La chiamata a 0x19d8dc prepara `0xf1ff01ff`, comprendente il bit 0x02. Questo è un ulteriore sito software che scrive una parola contenente quel bit, oltre al ramo del gestore a 0x19a758. Non è dimostrato che la scrittura accenda il bit letto: la semantica della periferica potrebbe differire da quella della RAM sintetica. Non si attribuisce qui al registro una semantica di impostazione o cancellazione dei bit.

Esecuzione: Ghidra 12.1.4, progetto B esistente aperto con `-readOnly -noanalysis`, script passato con `-postScript D90StatusCandidates.java`; log locale `work/status-candidates.log`. Nessun WARN, ERROR o eccezione nel log esaminato. Non sono state eseguite nuove prove emulative in questa fase. Il metodo non segue basi in altri registri, chiamate precedenti, percorsi attraverso salti o riferimenti fuori dall'intervallo. Una sequenza decodificabile può essere dato anziché codice: le finestre nuove richiedono conferma del contesto e dei chiamanti. Prossimo passo: risalire al contesto di 0x19d8c6 e al consumo della lettura a 0x19da9a. L'origine fisica dell'interrupt resta aperta.

## AB-72 · La scrittura ampia dello stato segue la cancellazione delle maschere

Componente B, campione AB-1, analisi del 5 novembre 2026. La scrittura a 0x19d8dc di AB-71 appartiene alla routine 0x19d89c–0x19d91e. Prima della scrittura, a 0x19d8c4 viene chiamata 0x19f7da con R4=0xf1ff01ff. Questa routine complementa l'argomento e applica AND con 0x0e00fe00 alle parole 0x63000034 e 0x63000038, tramite 0x1a0122. Quindi cancella anche il bit 0x02 nella maschera del dispatcher, prima di scrivere 0xf1ff01ff a 0x63000030. Il seguito applica AND 0xffff00ff e OR 1 alla parola 0x6300002c, azzera i byte 0x6800cf16 e 0x6800cf15 e ritorna. All'inizio, se il bit 1 della mezza parola 0x5000000a è acceso, lo cancella.

[D90StatusReset.java](../ghidra/D90StatusReset.java) entra a 0x19d89c e segue tutte le chiamate originali fino al ritorno a una sentinella sintetica 0x80000000, con limite di 200 passi. Due casi inizializzano le quattro parole MMIO rispettivamente a zero e 0xffffffff e la mezza parola a zero e 0xffff. Entrambi verificano il ritorno, le due maschere, lo stato, la parola 0x6300002c, la mezza parola e i due byte inizializzati a 0x55/0x66. Non vengono sostituite funzioni con ritorni simulati.

| Ingresso delle parole MMIO | 0x63000034 e 0x63000038 finali | 0x63000030 finale | 0x6300002c finale |
| --- | --- | --- | --- |
| 0x00000000 | 0x00000000 | 0xf1ff01ff | 0x00000001 |
| 0xffffffff | 0x0e00fe00 | 0xf1ff01ff | 0xffff00ff |

La lettura a 0x19da9a appartiene invece al corpo che inizia a 0x19d9dc. Il listato mostra l'invio di 0x0d1a0000 tramite 0x19f75e a 0x19da4e, R9 inizializzato a 1000 e un ciclo contenente servizi OS. Il valore letto viene conservato in R8 a 0x19da9c. Il confronto a 0x19da82–0x19da88 usa R8 & 0x000f0001; R8 parte da zero prima del ciclo, quindi questo confronto precede la lettura corrente e usa il valore conservato dall'iterazione precedente. Il seguito controlla anche il byte 0x6800cf1a e il contatore R9. I rami a 0x19dace e 0x19dad6 distinguono i bit 16–19 e il bit 0. Questi controlli non selezionano il bit 0x02. Non è stata eseguita emulativamente questa seconda routine né i suoi servizi OS; 1000 è un valore di contatore, non una durata verificata.

[D90StatusContext.java](../ghidra/D90StatusContext.java) conserva le finestre disassemblate [0x19d89c, 0x19db40) e [0x19f7da, 0x19f81c). Log locali: `work/status-context.log` e `work/status-reset.log`. Ghidra 12.1.4, Temurin 21.0.13, programma locale B aperto con `-readOnly -noanalysis`; nessun WARN, ERROR o eccezione nei log finali. La prima preparazione della prova usava il nome di registro non supportato R13: è stata corretta usando gli alias AC/FP del profilo prima dell'esecuzione riuscita.

La sequenza è compatibile con una disabilitazione/ripristino, ma questa denominazione è un'ipotesi, non il nome recuperato della funzione. RAM sintetica non modella eventuali effetti di cancellazione alla scrittura dello stato. Non sono identificati i chiamanti che determinano quando 0x19d89c viene eseguita sulla fotocamera né la sorgente fisica del bit 0x02. Prossimo passo: risalire ai chiamanti della routine e confrontare la sequenza con l'abilitazione di AB-69.

## AB-73 · Due chiamanti eseguono la sequenza prima di 0x19db40

Componente B, campione AB-1, 5 novembre 2026. [D90StatusCallers.java](../ghidra/D90StatusCallers.java) interroga i riferimenti presenti nel database e cerca caricamenti immediati a 32 bit di 0x19d89c nell'intervallo [0x198000, 0x1a4000), su indirizzi pari. In questa esecuzione non sono stati stampati riferimenti del database; la scansione trova due candidati, confermati dal disassemblaggio locale come chiamate tramite R12. Non è un inventario completo: chiamate dirette senza riferimento già registrato, puntatori costruiti diversamente, tabelle e codice fuori dall'intervallo possono sfuggire.

| Ingresso del chiamante | Chiamata a 0x19d89c | Chiamata successiva a 0x19db40 | Lettura dello stato software |
| --- | --- | --- | --- |
| 0x19d4ac | 0x19d4bc | 0x19d4c4 | 0x19d4c6 |
| 0x19d52c | 0x19d53c | 0x19d544 | 0x19d546 |

Entrambi i prefissi salvano R8 e RP, caricano R8=0x6800cf1f ed eseguono le due chiamate senza un ramo condizionale intermedio. Il seguito statico legge il byte a quell'indirizzo: se il bit 0 è acceso restituisce 0xfff20009; altrimenti, se il bit 6 è spento, restituisce 0xfff20003; altrimenti restituisce zero. Non viene assegnato un significato fotografico ai codici. Il valore restituito da 0x19db40 non è direttamente usato da questi confronti, che leggono lo stato software.

[D90StatusCallerPrefix.java](../ghidra/D90StatusCallerPrefix.java) esegue ciascun chiamante dal suo ingresso, attraversa la routine originale 0x19d89c e si ferma prima della prima istruzione di 0x19db40. Due configurazioni MMIO per chiamante (zero e tutti i bit accesi) danno quattro casi riusciti. Vengono controllati gli effetti di AB-72, R8=0x6800cf1f e il byte di stato software inizializzato a 0xa5, invariato fino a quel confine. Non si simula il ritorno della seconda funzione e non si eseguono le decisioni finali dei chiamanti. Il limite è 200 passi per caso, RAM e registri sono sintetici.

Il listato di 0x19db40 mostra prima le chiamate a 0x19df9c e 0x19e020, poi la lettura di 0x63000024 e il controllo della maschera 0x00040000 a 0x19db68–0x19db6e. Il corpo successivo contiene altre chiamate e servizi OS. È il prossimo confine da studiare; la sequenza non basta ancora a identificare il dispositivo o il momento in cui viene invocata nella fotocamera.

[D90StatusCallerContext.java](../ghidra/D90StatusCallerContext.java) stampa le finestre statiche [0x19d450, 0x19d574) e [0x19db40, 0x19dc00); i prefissi dei due chiamanti iniziano ai confini identificati sopra. Log locali: `work/status-callers.log`, `work/status-caller-context.log`, `work/status-caller-prefix.log`. Ghidra 12.1.4 e Temurin 21.0.13, progetto B aperto con `-readOnly -noanalysis`; nessun WARN, ERROR o eccezione nei log finali. La verifica riguarda i due ingressi isolati e non dimostra che siano gli unici chiamanti o che il percorso venga eseguito all'avvio.

## AB-74 · Azzeramento software e controllo del bit 18

Componente B, campione AB-1, 5 novembre 2026. La prima chiamata di 0x19db40, a 0x19db52 verso 0x19df9c, azzera dieci parole a 32 bit: 0x6800cd10, 0x6800cd08, 0x6800cd0c, 0x6800cd04, 0x6800cc50, 0x6800cc44, 0x6800cd00, 0x6800cc40, 0x6800cc4c e 0x6800cc48. Azzera inoltre sette byte: 0x6800cf1f, 0x6800cf1e, 0x6800cf1d, 0x6800cf1b, 0x6800cf1c, 0x6800cf17 e 0x6800cf14. Il selettore 0x6800cf1e di AB-63 viene quindi azzerato in questo percorso prima della seconda chiamata.

[D90StatusSetup.java](../ghidra/D90StatusSetup.java) entra a 0x19db40 ed esegue il prefisso fino all'ingresso di 0x19e020, senza eseguire quest'ultima. Due casi riempiono [0x6800cc40, 0x6800cf20) con 0x55 oppure 0xaa: il confronto dell'intero intervallo verifica le scritture elencate e la conservazione degli altri byte al suo interno. Non si afferma l'assenza di scritture fuori dall'intervallo, come quelle dello stack.

La seconda chiamata a 0x19db54 entra in 0x19e020. Il listato mostra una chiamata a 0x19e0fa, un controllo del nibble basso della mezza parola 0x50000004 e ulteriori chiamate a 0x1a0322 (nel ramo diverso da 1), 0x1a0414, 0x1a0156 e 0x1a03d8. A 0x19e064 prepara INT 0x40 con R4=2 e R12=-85. Nel seguito compaiono la lettura di 0x63000024 con maschera 0x10000 e chiamate a 0x19d864 per la decisione del ciclo. Questi servizi e l'intero corpo non sono stati eseguiti in questa fase: non viene simulato un ritorno riuscito per raggiungere il controllo seguente.

Una seconda prova entra separatamente a 0x19db56, dopo quella chiamata, ed esegue la lettura originale tramite 0x1a011a e la selezione del ramo. Il bit controllato è 18 (0x00040000), distinto dal bit 16 usato nel corpo precedente.

| Parola sintetica a 0x63000024 | Confine raggiunto, prima di eseguire la funzione | Effetto sul byte a 0x63000028 |
| --- | --- | --- |
| 0 | 0x1a03d8 con R4=0 | 0xa4 conservato |
| 0x00040000 | 0x19e68e | 0xa4 diventa 0xa5 |
| 0x00080000 | 0x1a03d8 con R4=0 | 0xa4 conservato |
| 0x000c0000 | 0x19e68e | 0xa4 diventa 0xa5 |

Il ramo con bit 18 acceso esegue BORL 1 a 0x19db94: l'operazione riguarda il byte all'indirizzo 0x63000028. I tre byte successivi inizializzati a 0x55, 0x66, 0x77 restano invariati. Il registro letto resta invariato in tutti e quattro i casi. Il ramo con bit spento chiama 0x1a03d8 prima di scrivere il codice 0x10 e aggiornare lo stato: la prova si ferma prima della chiamata e non attribuisce a quel seguito un esito emulativo.

Le due prove del prefisso e le quattro prove isolate del controllo passano. [D90StatusSetupContext.java](../ghidra/D90StatusSetupContext.java) conserva le finestre statiche consultate; log locali `work/status-setup-context.log` e `work/status-setup.log`. Ghidra 12.1.4, Temurin 21.0.13, progetto B in `-readOnly -noanalysis`, nessun WARN, ERROR o eccezione nei log finali. Tutti gli ingressi sono sintetici; i limiti sono 150 passi per il prefisso e 50 per il controllo. Non si identifica il segnale fisico corrispondente al bit 18 né si collega temporalmente la prova isolata al completamento reale di 0x19e020. Prossimo passo: esaminare 0x19e0fa e le dipendenze della preparazione 0x19e020.

## AB-75 · Due uscite dalla prima attesa della preparazione

Componente B, campione AB-1, 5 novembre 2026. La funzione 0x19e020 chiama 0x19e0fa a 0x19e026. Quest'ultima imposta con OR il bit 0x100 della parola a 0x63000100 (chiamata a 0x19e116), poi chiama 0x19d856, che richiama 0x404ae. Il corpo a 0x404ae carica in R4 la parola a 0x68000114 e ritorna: in questo tratto non richiede un servizio OS. Il valore iniziale viene conservato in R8; una variabile locale è impostata a 2.

Prima di leggere il bit, a 0x19e152 viene chiamata 0x19d864 con R4=1000, R5=valore iniziale e R6=puntatore alla variabile locale. Il corpo di AB-72 confronta la differenza fra il valore corrente letto tramite 0x404ae e quello iniziale con la soglia: sotto soglia restituisce zero; a soglia raggiunta decrementa la variabile se non è zero e restituisce zero; quando è già zero restituisce uno. La natura e l'unità del valore a 0x68000114 restano da identificare, quindi 1000 non viene convertito in millisecondi.

Se la funzione restituisce zero, 0x19e128–0x19e142 legge 0x63000100 e ritorna quando il bit 0x100 è spento, altrimenti ripete il controllo. Un risultato diverso da zero da 0x19d864 porta invece direttamente all'epilogo. Il ritorno a 0x19e028 non garantisce quindi che il bit sia stato cancellato. La parola periferica non viene nuovamente controllata dal chiamante prima della lettura successiva della mezza parola 0x50000004.

[D90SetupWait.java](../ghidra/D90SetupWait.java) parte dall'ingresso 0x19e020 e attraversa la prima chiamata fino al ritorno a 0x19e028, dove si ferma. Inizializza 0x63000100 a 0x80000000 e 0x68000114 a zero. Non sostituisce funzioni o risultati di ritorno: modifica esplicitamente la RAM sintetica in due punti dichiarati, per rappresentare due scenari distinti.

| Scenario sintetico | Intervento dello script | Chiamate a 0x19d864 | Passaggi per la lettura a 0x19e128 | Stato finale a 0x63000100 |
| --- | --- | --- | --- | --- |
| Bit cancellato | Prima della prima lettura a 0x19e128, scrive 0x80000000 nello stato | 1 | 1 | 0x80000000 |
| Contatore avanzato | Al primo ingresso in 0x19e144, scrive 1000 a 0x68000114 e lo mantiene | 3 | 2 | 0x80000100 |

Entrambi i casi raggiungono 0x19e028 entro il limite di 500 passi. La prova verifica la precedente impostazione del bit, il punto finale, i conteggi e le parole finali. Il secondo scenario esercita due decrementi della variabile locale e una terza chiamata che permette l'uscita con il bit ancora acceso. Non è una simulazione temporale della fotocamera, non dimostra chi aggiorni 0x68000114 e non prova che la periferica cancelli autonomamente il bit. Non sono coperti tutti i valori del contatore, il suo riporto o il resto di 0x19e020.

[D90SetupDependencies.java](../ghidra/D90SetupDependencies.java) conserva le finestre statiche delle dipendenze. Log locali: `work/setup-dependencies.log` e `work/setup-wait.log`. Esecuzioni con Ghidra 12.1.4 e Temurin 21.0.13, progetto B in `-readOnly -noanalysis`; nessun WARN, ERROR o eccezione nei log finali. Prossimo passo: proseguire da 0x19e028 attraverso le configurazioni fino al primo servizio OS a 0x19e064, conservando la distinzione fra i due esiti della prima attesa.

## AB-76 · Preparazione continua fino al primo servizio OS

Componente B, campione AB-1, 5 novembre 2026. [D90SetupConfig.java](../ghidra/D90SetupConfig.java) estende la sequenza di AB-75: entra a 0x19e020, esegue tutte le chiamate originali del prefisso e si ferma prima di INT 0x40 a 0x19e064. Non esegue il servizio né simula il suo ritorno. I sei casi combinano i due scenari della prima attesa di AB-75 con tre mezze parole iniziali a 0x50000004: 0xab01, 0xab04 e 0xab00.

La decisione a 0x19e036 salta 0x1a0322 quando il nibble basso vale 1. Negli altri due casi provati chiama 0x1a0322 con R4=0, che imposta il nibble a 1 preservando i dodici bit alti. La successiva 0x1a0414 applica OR 0xf00 alla parola 0x63000028, poi 0x1a0156 con R4=0 cancella il bit 0x02 della stessa parola.

La chiamata a 0x1a03d8 con R4=1 attraversa 0x1a0382, che usa di nuovo 0x19d856 e 0x19d864. Qui il registro sintetico 0x63000024 vale zero: dopo il primo controllo della soglia, la lettura dei due bit bassi permette l'uscita dal ciclo. Il seguito cancella il bit 0x04 nella parola a 0x6300002c. Al ritorno, il ramo R4 originale=1 di 0x1a03d8 imposta il bit 0x08 del byte 0x50000105. Il byte software 0x6800cf1f inizializzato a 0xa5 rimane invariato in questo ramo.

| Elemento | Ingresso sintetico | Valore al confine 0x19e064 |
| --- | --- | --- |
| Mezza parola 0x50000004 | 0xab01, 0xab04 oppure 0xab00 | 0xab01 |
| Parola 0x63000028 | 0xaaaaaaaa | 0xaaaaafa8 |
| Parola 0x6300002c | 0xffffffff | 0xfffffffb |
| Parola 0x63000024 | 0 | 0 |
| Byte 0x50000105 | 0x52 | 0x5a |
| Byte 0x6800cf1f | 0xa5 | 0xa5 |
| R4 / R12 | Registri preparati dal firmware | 2 / -85 |

Le asserzioni conservano anche quelle di AB-75: impostazione e stato finale del bit 0x100 a 0x63000100, valore finale a 0x68000114 e conteggi della prima attesa. Si verificano inoltre zero/una chiamata a 0x1a0322 secondo il nibble e un passaggio per la lettura a 0x1a0398. Il limite è 800 passi per caso. Nel caso di cancellazione del bit lo script modifica la RAM prima della lettura a 0x19e128; nel caso di avanzamento scrive 1000 a 0x68000114 al primo passaggio in 0x19e144. Questi interventi restano ingressi sintetici espliciti. Non vi sono ulteriori interventi durante la seconda attesa: il valore del contatore rimane quello raggiunto e il registro 0x63000024 è inizializzato a zero.

Entrambi gli esiti della prima attesa raggiungono il medesimo confine OS con le configurazioni in tabella; nel caso di avanzamento il bit della prima attesa resta acceso. Non si conclude che i due scenari siano equivalenti sull'hardware. Non sono coperti gli altri valori del nibble, la seconda attesa con bit bassi accesi, l'esecuzione del servizio -85 o il resto della preparazione.

Log locale `work/setup-config.log`: sei casi passati, nessun WARN, ERROR o eccezione. Ambiente Ghidra 12.1.4, Temurin 21.0.13, programma B esistente aperto con `-readOnly -noanalysis`. RAM e periferiche sono sintetiche, senza modello temporale della fotocamera. Prossimo passo: identificare il servizio -85 e i suoi vincoli prima di estendere l'esecuzione oltre 0x19e064.

## AB-77 · Destinazione e precondizioni iniziali del servizio -85

Componente B, campione AB-1, 5 novembre 2026. Applicando il dispatcher di AB-62 al selettore -85, la voce è a 0x40672 + 2×(-85) = 0x405c8. La mezza parola con segno vale 0x08ba e seleziona 0x40f2c. [D90DelayService.java](../ghidra/D90DelayService.java) legge la voce e stampa la finestra statica [0x40f2c, 0x40fcc). Il nome del file non identifica formalmente il servizio: una funzione di attesa/ritardo è un'interpretazione da approfondire, non un simbolo recuperato.

Il corpo salva RP e confronta il byte a FP+0x4d con 0x1f. Se differisce, salta a 0x41000. Se coincide, distingue l'argomento R4: negativo porta a 0x40f82, zero a 0x40f7c, positivo legge la parola a FP+0x30 in R12 e sostituisce FP con il puntatore letto a 0x68000000, poi chiama 0x40bec. Il ramo positivo è quello pertinente all'argomento 2 di AB-76. La natura precisa del byte salvato, del campo +0x30 e del puntatore globale non è stabilita dalla sola prova.

Il seguito statico del ramo positivo scrive campi della struttura puntata, imposta la parola +8 a 0x00020004, chiama 0x418a0 e aggiorna puntatori a offset +0 e +4. Questa struttura è compatibile con inserimento in una coda del sistema operativo, ma la sua gestione completa, il risveglio e il ritorno al chiamante restano da verificare. Gli epiloghi locali di zero e negativo caricano rispettivamente 0 e 0xdf in R12 nel delay slot; qui non vengono interpretati come codici OS formali.

[D90Service85.java](../ghidra/D90Service85.java) verifica la mezza parola e parte dal dispatcher a 0x406b6 con R12=-85. Usa un frame sintetico a 0x81001000, stack a 0x81004000, parola +0x30=0x1234 e puntatore globale 0x68000000=0x81002000. Registra il passaggio per 0x40f2c e si ferma ai confini seguenti, entro 60 passi:

| Byte frame+0x4d | R4 | Confine raggiunto, prima dell'istruzione |
| --- | --- | --- |
| 0x1f | 2 | 0x40bec |
| 0x1f | 0 | 0x40f7c |
| 0x1f | -1 | 0x40f82 |
| 0 | 2 | 0x41000 |

Nel caso positivo verifica anche FP=0x81002000 e R12=0x1234. Non esegue 0x40bec, gli epiloghi o il ramo a 0x41000; non simula il ritorno di alcuna funzione. Il frame viene preparato direttamente, quindi la prova non copre INT 0x40, la costruzione del frame dal processore o il prefisso del gestore prima di 0x406b6. Non collega ancora in continuità AB-76 alla gestione del servizio.

Log locali `work/delay-service.log` e `work/service85.log`. Ghidra 12.1.4, Temurin 21.0.13, programma B esistente in `-readOnly -noanalysis`. I quattro casi finali passano senza WARN, ERROR o eccezioni. Una prima esecuzione segnalava letture non inizializzate dei registri S e I in ANDCCR: lo script finale li inizializza esplicitamente a zero. Rimangono da studiare 0x40bec e 0x418a0, la costruzione reale del frame e il meccanismo che permette la ripresa della preparazione. Non è verificata alcuna durata temporale.

## AB-78 · Rimozione di un nodo dalla lista bidirezionale

Componente B, campione AB-1, 6 novembre 2026. Il servizio -85 (AB-77) chiama 0x40bec con la struttura corrente in FP. Il servizio chiama 0x40bec a 0x40f48 e, dopo il ritorno, chiama separatamente 0x418a0 a 0x40f58. Quest'ultima legge i puntatori del nodo corrente agli offset +0 e +4, aggiorna il puntatore successivo del precedente e il puntatore precedente del successivo, quindi ritorna con AC=1. Se i due vicini coincidono, entra invece in un ramo che legge il valore a nodo+0xc e aggiorna la maschera globale a 0x68000004. Il ramo speciale non è coperto da AB-78.

[D90QueueUnlink.java](../ghidra/D90QueueUnlink.java) esegue le istruzioni originali da 0x418a0 fino al ritorno, in due liste circolari sintetiche contenenti tre e quattro nodi. Rimuove rispettivamente il nodo centrale e quello d'indice 2. In entrambi verifica i due collegamenti fra vicini, i puntatori conservati nel nodo rimosso e AC=1. Tutti gli indirizzi dei nodi sono in RAM sintetica. La prova non stabilisce il significato delle strutture nel sistema operativo Nikon, non esegue 0x40bec e non modella scheduler, interrupt o maschera globale.

[D90QueueCore.java](../ghidra/D90QueueCore.java) conserva il disassemblato delle finestre [0x40bec, 0x40d20), [0x418a0, 0x41960) e [0x41852, 0x418a0). Sono finestre statiche, non un disassemblaggio completo di ogni coda o di tutto il servizio. Log locale di prova: `work/queue-unlink.log`; log del listato: `work/queue-core.log`. Ambiente Ghidra 12.1.4, Temurin 21.0.13, programma B aperto con `-readOnly -noanalysis`; la prova dinamica passa senza WARN, ERROR o eccezioni nel log.

L'operazione osservata è compatibile con la rimozione di un elemento da una lista bidirezionale, ma non basta a dare un nome o uno scopo alla lista. Rimangono da verificare il ramo a vicini coincidenti, il chiamante e le precondizioni complete di 0x40bec, l'inserimento successivo nel corpo del servizio -85 e la ripresa del servizio. Nessuna durata o condizione hardware è stata osservata.

## AB-79 · Rimozione dell'ultimo nodo e aggiornamento della maschera

Componente B, campione AB-1, 6 novembre 2026. [D90QueueLastNode.java](../ghidra/D90QueueLastNode.java) verifica 0x418a0 con un nodo sintetico a 0x81001000 collegato in entrambe le direzioni a una sentinella a 0x81001100. La sentinella punta inizialmente al nodo in entrambe le direzioni. Dopo la rimozione, entrambi i puntatori della sentinella puntano alla sentinella stessa; i due puntatori del nodo rimosso restano invariati. Non si prova una chiamata su una lista già vuota.

Quando i vicini coincidono, dopo le scritture a 0x418a6 e 0x418a8 il ramo legge la mezza parola senza segno a nodo+0x0c. Per gli indici provati 1, 2, 16 e 32 il codice cancella dalla parola globale 0x68000004 il bit `0x80000000 >>> (indice - 1)`. Sono stati provati due valori iniziali della parola globale, zero e 0xffffffff, per otto casi totali. Partendo da zero il risultato resta zero.

| Indice sintetico | Maschera globale iniziale | Maschera globale finale |
| --- | --- | --- |
| 1 | 0xffffffff | 0x7fffffff |
| 2 | 0xffffffff | 0xbfffffff |
| 16 | 0xffffffff | 0xfffeffff |
| 32 | 0xffffffff | 0xfffffffe |

Ogni caso esegue fino al ritorno a una sentinella PC 0x80000000, con limite di 60 passi. Verifica i quattro puntatori, la maschera globale, FP ripristinato al nodo e AC=1. Verifica anche la scrittura del byte 1 a 0x44, inizializzato a 0x55: in questa prova l'indirizzo è rappresentato in RAM, quindi non si attribuisce alla scrittura alcun effetto reale di scheduling o interrupt.

Log locale `work/queue-last-node.log`: otto casi passati, senza WARN, ERROR o eccezioni. Ghidra 12.1.4, Temurin 21.0.13, progetto B in `-readOnly -noanalysis`. La prova non copre tutti gli indici, valori fuori dall'intervallo 1–32, liste corrotte, concorrenza, scheduler completo o riattivazione del task. L'interpretazione del campo +0x0c come priorità resta da dimostrare.

**Correzione del testo AB-78:** la precedente frase che attribuiva a 0x40bec una chiamata a 0x418a0 era errata. Le due chiamate sono nel servizio -85, rispettivamente a 0x40f48 e 0x40f58. È stata corretta anche la dicitura «inserimento tramite 0x418a0»: la routine esegue la rimozione; le scritture di inserimento successive sono nel corpo del servizio, dopo il ritorno. La correzione non cambia gli esiti delle due prove AB-78.

Prossimo passo: verificare 0x40bec con una lista sintetica coerente e seguire l'inserimento successivo nel servizio -85. Il ritorno dal servizio alla preparazione resta aperto.

## AB-80 · Esecuzione del servizio -85 fino al ritorno al dispatcher

Componente B, campione AB-1, 6 novembre 2026. [D90Service85Queue.java](../ghidra/D90Service85Queue.java) entra a 0x406b6 con R12=-85 e R4=2, attraversa 0x40f2c, 0x40bec e 0x418a0 senza sostituire chiamate, e si ferma al ritorno a 0x406c6 prima di eseguirne l'istruzione. La prova verifica la voce 0x405c8=0x08ba. Il frame sintetico a 0x81001000 contiene byte +0x4d=0x1f e parola +0x30=0x1234; il puntatore a 0x68000000 indica il nodo 0x81002000.

Il nodo parte come unico elemento di una lista con sentinella 0x81002100 e campo +0x0c=2. La seconda lista ha sentinella 0x68000118. Per ciascun caso sono inizializzati tutti i collegamenti di entrambe le liste e i valori a nodo+0x14 dei nodi già presenti. Nessuna scrittura sintetica viene eseguita dopo l'avvio.

0x40bec scorre la seconda lista sottraendo dal valore richiesto i campi +0x14 già incontrati. Se il residuo è inferiore al campo successivo, restituisce il nodo successivo in R5, il residuo in R8 e il campo successivo ridotto in R1; se raggiunge la sentinella, restituisce questa in R5. Il corpo del servizio salva il residuo nel nodo corrente, lo rimuove dalla prima lista tramite 0x418a0 e lo inserisce prima del nodo restituito. Quando esiste un successore, ne aggiorna anche il campo +0x14. Le seguenti combinazioni sono verificate con richiesta 2:

| Campi +0x14 iniziali della seconda lista | Posizione del nuovo nodo (indice da zero) | Campi +0x14 finali, nell'ordine della lista |
| --- | --- | --- |
| Lista vuota | 0 | [2] |
| [5] | 0 | [2, 3] |
| [1, 5] | 1 | [1, 1, 4] |
| [1] | 1 | [1, 1] |

In tutti i casi la prova controlla entrambi i collegamenti di ogni nodo della lista risultante, tutti i campi +0x14, la prima sentinella tornata a puntare a sé stessa, la parola nodo+8=0x00020004 e la mezza parola nodo+0x20=0x1234. I due byte successivi a quest'ultima conservano la sentinella 0x6666. Controlla inoltre la maschera globale 0x68000004, da 0xffffffff a 0xbfffffff (AB-79), il byte 0x68000132 da 0xa5 a 0xb5, il byte 0x44 impostato a 1 e R12=0 al ritorno. Il limite è 250 passi per scenario.

I campi relativi sono compatibili con una coda ordinata per intervalli, ma non è ancora dimostrata la loro unità temporale o la gestione della scadenza. Gli scenari non coprono tutti i valori, l'uguaglianza fra richiesta e campo, il riporto aritmetico, liste corrotte, modifiche concorrenti o interrupt. In particolare il codice di 0x40bec può ripartire dalla testa in base al bit 0x10 a 0x68000132; gli esperimenti non iniettano variazioni concorrenti di quel bit.

Log locale `work/service85-queue.log`: quattro casi passati, nessun WARN, ERROR o eccezione. Ghidra 12.1.4, Temurin 21.0.13, progetto B in `-readOnly -noanalysis`. Frame, liste e registri periferici sono sintetici. Non è eseguito INT 0x40 né costruito il frame dal processore; non si attraversa il seguito del dispatcher, che chiama 0x404f8 prima di ripristinare il contesto. Il ritorno locale a 0x406c6 non dimostra che il task abbia terminato l'attesa o sia tornato alla preparazione ATAM. Prossimo passo: analizzare 0x404f8 e il seguito del dispatcher, individuando le condizioni necessarie alla ripresa.

## AB-81 · Il seguito del dispatcher ripristina i registri fino a RETI

Componente B, campione AB-1, 6 novembre 2026. La chiamata a 0x406ce verso 0x404f8 raggiunge una sola istruzione RET nel programma esaminato. Non si identifica quindi quella funzione come scheduler. Il seguito a 0x406d0 estende con segno il byte basso di R12 e ripristina dal frame salvato R0–R11, AC, FP, RP, MDL e MDH. La parola salvata fra R11 e AC viene letta temporaneamente in FP e spostata a sinistra: se il bit iniziale 31 è spento, il codice scrive 0x10 al byte 0x46f e lo rilegge; se acceso salta queste operazioni. Termina a RETI, indirizzo 0x406f4.

[D90OsReturn.java](../ghidra/D90OsReturn.java) entra separatamente a 0x406c6 con SP=0x81004000 e 18 parole sintetiche sullo stack. Le parole di indice 0–11 contengono i valori distinti 0x11000000+indice; la parola 12 è zero oppure 0x80000000; le parole 13–17 contengono i corrispondenti valori distinti. Verifica il ripristino dei 17 registri sopra elencati, SP avanzato di 72 byte, il passaggio per 0x404f8 e l'arresto prima di RETI. Non prepara né consuma il frame aggiuntivo necessario a RETI.

I quattro casi combinano i due valori della parola 12 con R12 iniziale zero o 0xdf. Al confine il risultato è rispettivamente zero o -33, per l'estensione del byte con segno; non si assegna un nome OS al codice. Il byte 0x46f, inizializzato a 0x55, diventa 0x10 con parola 12 zero e resta 0x55 con parola 12=0x80000000. Il limite è 50 passi per caso. Gli effetti periferici di 0x46f sono rappresentati esclusivamente in RAM sintetica.

[D90SchedulerContext.java](../ghidra/D90SchedulerContext.java) stampa solo le finestre [0x404f8, 0x404fa) e [0x406c6, 0x406f6), evitando di trattare le tabelle e i dati adiacenti come codice. Log finale `work/os-return.log`: quattro casi passati, senza WARN, ERROR o eccezioni; Ghidra 12.1.4 e Temurin 21.0.13, programma B in `-readOnly -noanalysis`. Una prima esecuzione aveva segnalato flag N/Z/V/C non inizializzati in ANDCCR; lo script finale li inizializza esplicitamente a zero, oltre a S e I.

È una prova separata dell'epilogo, non una continuazione con frame reale delle prove AB-80. Non dimostra la ripresa del task o della preparazione ATAM, la gestione di un interrupt pendente o l'esecuzione dello scheduler. Non è dimostrata l'assenza di modifiche runtime al codice. Prossimo passo: ricostruire la generazione e il consumo del frame dell'interrupt e il percorso associato alla scrittura del byte 1 a 0x44 (AB-79), senza presumere che tale scrittura abbia in RAM sintetica gli effetti della periferica reale.


## AB-82 · Frame dei registri generato dal prefisso e consumato dall'epilogo

Componente B, campione AB-1, 6 novembre 2026. [D90InterruptFrameContext.java](../ghidra/D90InterruptFrameContext.java) stampa [0x404f4, 0x404f6) e [0x4067a, 0x406b6). Nel programma esaminato 0x404f4 contiene RET. Il prefisso a 0x4067a salva temporaneamente FP e RP, chiama questa funzione e recupera RP. Usa quindi FP=SP+9, equivalente allo SP iniziale+5, per invertire, esaminare e reinvertire il nibble basso del byte puntato. Se il nibble iniziale era 0xf, esegue STILM 0x1e e cancella il bit 31 di R12 tramite LSL/LSR; se era zero salta quel ramo. I due valori non esauriscono tutti i nibble possibili.

Dopo il recupero di FP, il firmware esegue ORCCR 0x10 e salva MDH, MDL, RP, il gruppo R8–R12/AC/FP e R0–R7. Il frame risultante occupa 72 byte. Rispetto al nuovo SP, le parole sono:

| Offset | Contenuto salvato |
| --- | --- |
| +0x00–+0x2c | R0–R11, nell'ordine |
| +0x30 | R12 dopo l'eventuale cancellazione del bit 31 |
| +0x34 | AC |
| +0x38 | FP |
| +0x3c | RP |
| +0x40 | MDL |
| +0x44 | MDH |

Segue la scrittura e rilettura del byte 0x1e a 0x46f; a 0x406b4 FP riceve lo SP del frame. Questo chiarisce l'origine software della parola +0x30 utilizzata dal servizio e della parola 12 esaminata nell'epilogo AB-81. Non identifica ancora il significato OS del bit 31.

[D90InterruptFrame.java](../ghidra/D90InterruptFrame.java) entra a 0x4067a con SP=0x81004000, R0–R11=0x11000000+indice e AC/FP/RP/MDL/MDH=0x22000000+indice del gruppo. Predispone otto byte a SP: parola 0x80000100 e parola `nibble << 16`, con nibble zero oppure 0xf. Sono byte forniti dal test, non un frame prodotto da INT. R12 è 0x55 oppure 0x80000055, per quattro combinazioni. Lo script inizializza flag e ILM e verifica tutte le 18 parole salvate, SP=0x81003fb8, FP=SP, gli otto byte iniziali conservati e 0x46f=0x1e.

A quel punto lo script **salta esplicitamente il dispatcher e il servizio**, impostando PC=0x406c6 e R12=0. Esegue l'epilogo originale fino a 0x406f4, prima di RETI, e verifica il recupero dei 17 registri diversi da R12, SP iniziale e gli otto byte ancora invariati. Se il R12 salvato conserva il bit 31, 0x46f resta 0x1e; negli altri casi diventa 0x10. Il limite è 100 passi per ciascuna delle due fasi. Non è una continuazione integrata di AB-80.

Log locale `work/interrupt-frame.log`: quattro casi passati senza WARN, ERROR o eccezioni; Ghidra 12.1.4, Temurin 21.0.13, programma B in `-readOnly -noanalysis`. Sono verificati produzione e consumo del frame software dei registri. Restano aperti generazione del frame da INT, semantica e consumo PC/PS tramite RETI, effetti hardware di 0x44 e 0x46f, modifiche runtime e ripresa del task. Prossimo passo: verificare il supporto emulativo di INT/RETI e il frame PC/PS prima di integrare il servizio -85.


## AB-83 · Supporto effettivo di INT e RETI nell'emulatore Ghidra

Componente B, campione AB-1, 6 novembre 2026. [D90InterruptSupport.java](../ghidra/D90InterruptSupport.java) verifica il disassemblato originale INT #0x40 a 0x19e064 e RETI a 0x406f4. Sullo stesso programma B, con EmulatorHelper senza estensioni personalizzate, un singolo passo da INT fallisce con `Unimplemented CALLOTHER pcodeop (_interrupt_prelude), PC=0019e064`. Il test richiede esplicitamente questo fallimento: un cambiamento del supporto provoca una richiesta di rivalutazione, non un successo silenzioso. Il log contiene l'errore e lo stack trace attesi della sonda; non sono un fallimento delle quattro prove RETI successive.

La sorgente locale Ghidra 12.1.4 `Ghidra/Processors/FR60/data/languages/fr60.sinc`, SHA-256 `b163bf22c9d5f260f9b2c22bde2d45eb18720968c0fce84214c48edfb792f6f0`, dichiara `_interrupt_prelude` a riga 157 e lo invoca nella definizione INT alle righe 1036–1042. La definizione RETI alle righe 1047–1055 legge la parola puntata da SP come destinazione, avanza SP di quattro, legge PS, avanza di altri quattro, decodifica PS e ritorna alla destinazione. Si descrive il modello Ghidra installato: non è una nuova validazione del manuale della CPU o dell'hardware Nikon. In particolare, il percorso successivo alla pcodeop di INT non è stato eseguito e la risoluzione della destinazione tramite vettore rimane da verificare.

Quattro prove separate iniziano direttamente dalla RETI originale, con SP=0x81004000 e otto byte sintetici: parola PC seguita da parola PS, in big endian. Incrociano PC=0x19e066 oppure 0x80000100 e PS=0 oppure 0x000f071f. Dopo un singolo passo controllano PC, SP=0x81004008, PS e conservazione degli otto byte. Verificano anche i campi decodificati dal modello installato: C/V/Z/N/I/S ai bit 0–5, T/D0/D1 ai bit 8–10 e ILM ai bit 16–19. Nei due PS scelti S resta zero; non si verifica un cambio fra stack utente e supervisore. I quattro casi passano. Nessuna istruzione alla destinazione di ritorno viene eseguita.

Log locale `work/interrupt-support.log`, Ghidra 12.1.4 e Temurin 21.0.13, programma B aperto `-readOnly -noanalysis`. I frame sono forniti dal test; INT e RETI non sono collegati fra loro né al servizio -85. Questo risultato spiega perché una simulazione continua richiede un adattatore d'ingresso interrupt esplicito e verificato, anziché presumere il supporto completo di EmulatorHelper. Non dimostra la ripresa del task, effetti di 0x44/0x46f, temporizzazione o fedeltà completa del modello FR. Prossimo passo: confrontare la semantica d'ingresso con una fonte CPU primaria e progettare la sonda del vettore e del frame, dichiarando ogni effetto introdotto dall'adattatore.


## AB-84 · Limite a quattro bit del modello ILM e progetto dell'ingresso INT

Componente B, campione AB-1, 6 novembre 2026. Il confronto usa come fonte primaria il [manuale Fujitsu FR30 MB91F127/128, CM71-10115-1E](https://www.infineon.com/assets/row/public/documents/10/57/infineon-fr30-mb91f127-128-hardware-manual-additionaltechnicalinformation-en.pdf), sezioni 3.3.1 e 3.9.6, pagine stampate 33 e 56–58 (PDF indici 47 e 70–72). È un riferimento della famiglia FR, non l'identificazione del chip Nikon. Il manuale descrive ILM su cinque bit, PS[20:16]. Per INT salva PS e PC+2 tramite SSP, azzera I e S e legge la destinazione dal vettore; RETI richiede S=0 e consuma PC e PS dallo stack. La fonte FR60 Lite precedentemente citata ha restituito durante questa consultazione un reindirizzamento a un documento F2MC estraneo; quel contenuto non è usato come conferma.

Nel `fr60.sinc` installato, identificato dall'hash in AB-83, `packProgramStatus` e `unpackProgramStatus` usano PS[16,4], mentre STILM applica `& 0x0f`. [D90IlmModel.java](../ghidra/D90IlmModel.java) caratterizza questo comportamento senza correggerlo: esegue la STILM originale a 0x40692 e verifica ILM=0x0e dopo l'immediato 0x1e. Esegue inoltre RETI a 0x406f4 con PC sintetico 0x19e066 e PS uguale a 0x000f0000, 0x001e0000 o 0x001f0000. Le parole PS restano complete, ma il registro ILM decodificato vale rispettivamente 0x0f, 0x0e e 0x0f. Controlla anche PC e SP finale. La prova di STILM è isolata: non attraversa il gestore.

Log locale `work/ilm-model.log`: quattro casi superati senza WARN, ERROR o eccezioni. Ghidra 12.1.4, Temurin 21.0.13, programma B in `-readOnly -noanalysis`. È una conferma del comportamento del modello installato e di una differenza rispetto alla fonte FR consultata. Non dimostra da sola la semantica del chip Nikon. I successi AB-81–AB-83 rimangono validi nei rispettivi perimetri: non erano prove della fedeltà di ILM a cinque bit né della selezione di interrupt concorrenti.

### Progetto dell'adattatore, non ancora implementato

L'adattatore dovrà essere esplicito e separato dalle istruzioni originali, inizialmente limitato a INT #0x40, S=0 e stack unico, con rifiuto dei casi S=1 e delay slot. Prima di mutare lo stato dovrà verificare opcode, allineamento, TBR e destinazione letta dalla parola del vettore. Dovrà poi costruire le due parole PC/PS, aggiornare SP/SSP coerentemente e azzerare I/S prima dell'ingresso nel gestore. La composizione di PS dovrà includere tutti e cinque i bit ILM; la correzione coerente di STILM, lettura/scrittura di PS e RETI richiede una strategia distinta. Non è sufficiente aggiungere la sola `_interrupt_prelude`.

I controlli previsti comprendono valori ILM ai due lati di 15/16, frame e sentinelle intatti, mancata modifica di RP all'ingresso INT e consumo di otto byte al ritorno. Occorrerà verificare separatamente anche il salto indiretto del modello INT: la definizione SLEIGH usa l'espressione dell'indirizzo di vettore dopo la pcodeop, mentre la sonda AB-83 si arresta prima. Nessun adattatore è applicato al modello installato in AB-84. Prossima indagine: scegliere e verificare una correzione coerente del modello PS/ILM prima della prova integrata con servizio -85. Scheduler, temporizzazione, periferiche e ripresa reale restano aperti.


## AB-85 · Correzione isolata di PS/ILM e confronto sui 32 valori

6 novembre 2026. Questa prova riguarda il modello CPU Ghidra, non esegue firmware A/B e non introduce una patch Nikon. [build_fr60_ilm.py](../tools/build_fr60_ilm.py) prepara una copia locale del linguaggio, partendo esclusivamente dall'hash sorgente fr60.sinc di AB-83. Sostituisce esattamente tre espressioni: `PS[16,4] = ILM` con `PS[16,5] = ILM`, `ILM = PS[16,4]` con `ILM = PS[16,5]`, e la maschera STILM `0x0f` con `0x1f`. Copia i file ausiliari necessari e assegna l'ID sperimentale `fr60:BE:16:d90-ilm5`. Non registra il linguaggio globalmente, non sovrascrive directory esistenti e rifiuta sorgenti con hash diverso. I file Ghidra generati restano locali, fuori dal repository.

Hash SHA-256 del fr60.sinc corretto: `8ee76f11cf417fd86f723acad4e864ccf9b67f9796f7d4d4e7f2e77fd325a857`. L'hash della sorgente installata dopo la prova resta `b163bf22c9d5f260f9b2c22bde2d45eb18720968c0fce84214c48edfb792f6f0`. La compilazione con lo Sleigh della stessa Ghidra termina con successo, segnalando quattro costruttori NOP; questo avviso del compilatore non attesta un difetto delle prove ILM e non viene nascosto.

[D90IlmCorrection.java](../ghidra/D90IlmCorrection.java) carica il linguaggio isolato mediante un provider privato, usando per riflessione il costruttore non pubblico di SleighLanguageProvider. È una dipendenza dall'API interna verificata con Ghidra 12.1.4; non se ne promette compatibilità con altre versioni. Crea due ProgramDB transitori in memoria, uno col modello installato e uno col modello corretto, e li rilascia senza salvarli. Il programma firmware esistente è aperto in sola lettura e fornisce soltanto il linguaggio originale per il confronto.

Per ciascun modello esegue tutti i valori ILM 0–31 con due configurazioni degli altri campi PS, 0 e 0x70f: 64 sequenze per modello, 128 totali. Le sequenze sintetiche, a 0x80000000, sono STILM, MOV PS,R0, ST PS,@-SP, MOV R1,PS con R1=0, LD @SP+,PS, MOV PS,R2 e RETI. Gli opcode sono rispettivamente `87xx`, `1710`, `1790`, `0711`, `0790`, `1712`, `9730`. Non sono bytes estratti dal firmware. Lo stack iniziale è 0x81004000; il frame RETI fornito dal test contiene destinazione 0x80000080 e PS=`(ILM << 16) | flags`.

Le asserzioni verificano ILM dopo STILM, PS scritto sullo stack, azzeramento di ILM tramite MOV R1,PS, recupero tramite LD, PS letto in R0/R2, ritorno di SP alla base e infine PC/PS/SP/ILM dopo RETI. Il modello installato produce `ILM & 15`; quello corretto conserva ILM per tutti i 32 valori. Nei casi RETI originali il registro PS conserva comunque la parola intera anche se ILM è troncato, come già osservato in AB-84. MOV registro→PS è verificata con zero: la copertura dei valori non nulli in decodifica passa per LD e RETI. Non sono confrontati tutti i bit riservati di PS o tutte le combinazioni di flag.

Log locale `work/ilm-correction.log`: entrambi i riepiloghi da 64 sequenze e il totale da 128 sono presenti, senza WARN, ERROR o eccezioni di esecuzione. Verificati inoltre il rifiuto di una destinazione già esistente e di una sorgente non riconosciuta nel builder. Ambiente Ghidra 12.1.4, Temurin 21.0.13, progetto B `-readOnly -noanalysis`.

È una correzione sperimentale coerente dei tre punti PS/ILM individuati, non una validazione completa del processore. INT, cambio SSP/USP, S=1, arbitraggio interrupt, temporizzazione e periferiche rimangono fuori dalla prova. La sorgente installata e il database firmware restano invariati; le prove precedenti continuano a usare il modello originale. Prossimo passo: implementare l'adattatore INT limitato al caso S=0 e verificarne frame e risoluzione del vettore sul linguaggio sperimentale, prima di collegare il servizio -85.


## AB-86 · Adattatore esplicito INT 0x40 e ritorno sintetico con RETI

6 novembre 2026. [D90Int40Adapter.java](../ghidra/D90Int40Adapter.java) implementa in Java un ingresso INT limitato, sul linguaggio isolato a cinque bit di AB-85. Non implementa la pcodeop Ghidra né esegue il p-code originale di INT: sostituisce esplicitamente l'intera istruzione al confine. Il riferimento architetturale e i limiti di identificazione del chip restano quelli dichiarati in AB-84.

Il metodo `enter` richiede PC pari (controllo aggiunto nella rettifica sotto), S=0, SP=SSP allineati a quattro byte, spazio aritmetico per otto byte di frame, TBR allineato a 0x400 e opcode 0x1f40. Riceve dal chiamante un booleano che segnala il delay slot e lo rifiuta se vero: non ricostruisce autonomamente la pipeline. Legge una parola big endian a `TBR+0x2fc`, anziché usare l'indirizzo della voce come destinazione, e richiede una destinazione non nulla e pari. Non dimostra che questa sia codice valido o eseguibile e non è una validazione generica di memoria fisica.

Compone PS dai campi C/V/Z/N/I/S/T/D0/D1 e ILM a cinque bit, conservando gli altri bit del registro PS. Scrive PS a SP−4 e PC+2 a SP−8, aggiorna SP e SSP a SP−8, azzera I e S nello stato attivo e imposta PC alla parola letta dal vettore. RP resta invariato. Il codice di adattamento è software dell'esperimento, non una modifica o istruzione scoperta nel firmware.

La prova crea un ProgramDB transitorio con istruzioni e memoria sintetiche. INT #0x40 si trova a 0x80000000. TBR=0xdfc00 e la voce 0xdfefc è riempita dal test con 0x80000100 oppure 0x80000200: a entrambe le destinazioni viene collocata RETI (0x9730). Non è la voce originale Nikon, sebbene il suo indirizzo coincida. I casi incrociano ILM=0, 15, 16, 30, 31, I=0/1 e le due destinazioni, per **20 casi**. Gli altri flag sono zero.

Dopo l'adattatore verifica PC alla destinazione, SP=SSP=0x81003ff8, le due parole PC=0x80000002 e PS iniziale, I/S azzerati, PS attivo e RP=0x12345678. Esegue poi un passo RETI del modello isolato e controlla PC=0x80000002, SP=0x81004000, PS e ILM/I iniziali, RP e due sentinelle di stack a 0x81003ff4 e 0x81004000. Nessuna istruzione al PC di ritorno viene eseguita.

**Ulteriore limite osservato:** dopo RETI, SSP rimane 0x81003ff8 mentre SP vale 0x81004000. I due registri del modello sono separati; l'adattatore non nasconde questa differenza correggendola dopo il ritorno. Perciò questi casi non dimostrano un alias SP/SSP fedele, né supportano ancora una seconda entrata con SP=SSP. Questa verifica deve precedere l'integrazione del servizio e delle attese ripetute.

Cinque prove negative verificano il rifiuto di S=1, SP diverso da SSP, opcode INT #0x41, destinazione dispari e delay slot dichiarato dal chiamante. In ciascun caso controllano che PC, SP e una sentinella nel futuro frame non siano cambiati. Non si afferma la copertura di ogni input invalido o l'atomicità in presenza di errori di memoria durante le scritture.

Log locale `work/int40-adapter.log`: 20 casi positivi e cinque rifiuti attesi, senza WARN, ERROR o eccezioni non gestite. Ghidra 12.1.4, Temurin 21.0.13, programma B aperto `-readOnly -noanalysis` ma nessuna istruzione firmware eseguita. Il linguaggio installato e il database restano invariati. Mancano alias SP/SSP, S=1, interrupt annidati, periferiche, scheduler, temporizzazione e prova integrata del servizio -85. Prossimo passo: verificare e definire il mantenimento coerente di SP/SSP nel caso supervisore prima di collegare il gestore firmware.


## AB-87 · Convenzione esplicita SP/SSP ai confini del ritorno supervisore

6 novembre 2026. [D90SupervisorStack.java](../ghidra/D90SupervisorStack.java) estende lo script AB-86 e usa lo stesso linguaggio isolato PS/ILM a cinque bit. Il metodo `returnSupervisor` esegue RETI e poi aggiorna esplicitamente SSP al nuovo SP, dopo aver verificato gli effetti dell'istruzione. È una convenzione del banco di prova limitata a S=0, non una correzione generale del linguaggio né un effetto attribuito al firmware. Il limite SP/SSP osservato in AB-86 resta presente nel modello sottostante.

Prima del passo il metodo richiede S=0, SP=SSP allineati, spazio aritmetico per otto byte, opcode RETI 0x9730, S=0 nel PS di ritorno e destinazione pari. Legge PC/PS dal frame. Dopo un passo verifica PC, PS, S e SP avanzato di otto byte; solo allora scrive SSP=SP. Non simula il ritorno a S=1 o il cambio stack utente/supervisore. Non promette rollback qualora una verifica successiva al passo fallisca.

Le prove collocano due INT #0x40 consecutivi a 0x80000000 e 0x80000002, con destinazione sintetica del vettore 0xdfefc uguale a 0x80000100 oppure 0x80000200. Entrambe contengono RETI. La matrice incrocia ILM=0, 15, 16, 30, 31, I=0/1 e le due destinazioni: **20 coppie**, cioè 40 ingressi tramite adattatore e 40 RETI con sincronizzazione esplicita. Il codice INT originale del linguaggio continua a essere sostituito dal metodo `enter` di AB-86.

Non viene impostato PC fra il primo e il secondo ciclo: il primo RETI raggiunge direttamente il secondo INT. Ogni ingresso riusa il frame a 0x81003ff8, contenente il PC di ritorno corretto (0x80000002 oppure 0x80000004) e il PS iniziale. Ogni ritorno verifica SP=SSP=0x81004000, ILM e I recuperati, RP=0x12345678 e sentinelle intatte a 0x81003ff4 e 0x81004000. Il PC finale è 0x80000004; l'istruzione a quell'indirizzo non è eseguita. Gli altri flag sono inizializzati a zero.

Tre prove negative preparano rispettivamente un PS di ritorno con S=1, SP diverso da SSP e un opcode diverso da RETI. Il metodo le rifiuta prima del passo; il test verifica PC, SP, SSP e gli otto byte del frame invariati. Questa copertura non comprende ogni errore di memoria o input invalido.

Log locale `work/supervisor-stack.log`: 20 coppie e tre rifiuti attesi superati senza WARN, ERROR o eccezioni non gestite; Ghidra 12.1.4, Temurin 21.0.13. Il programma B resta aperto in sola lettura, ma le istruzioni eseguite sono sintetiche in ProgramDB transitorio. Lo script dipende da `D90Int40Adapter.java` nello stesso scriptPath e dall'argomento assoluto fr60.ldefs di AB-85.

La convenzione rende ripetibile il caso limitato di due ingressi supervisore con handler composto dalla sola RETI. Non sincronizza SP/SSP dopo ogni istruzione e non copre letture/scritture esplicite di SSP durante un handler, interrupt annidati, cambio task, periferiche o temporizzazione. Le definizioni MOV dei registri dedicati nel fr60.sinc installato (righe 965–972) leggono/scrivono direttamente il registro selezionato: prima dell'integrazione va quindi controllato se il percorso firmware usa SSP mentre SP cambia. Prossimo passo: verificare tale condizione e collegare prefisso ed epilogo originali al frame d'ingresso, mantenendo espliciti adattatore e sincronizzazione. Il servizio -85 e la ripresa del task rimangono fuori da AB-87.


## AB-88 · Prefisso ed epilogo firmware collegati al frame dell'adattatore

Componente B, campione AB-1, 6 novembre 2026. [D90FirmwareContext.java](../ghidra/D90FirmwareContext.java) collega l'ingresso esplicito INT di AB-86, il prefisso originale 0x4067a–0x406b4, l'epilogo originale 0x406c6–0x406f4 e RETI, con sincronizzazione SSP dopo il ritorno secondo AB-87. **Il dispatcher e il servizio sono saltati esplicitamente:** raggiunto 0x406b6, lo script imposta PC=0x406c6 e R12 al risultato sintetico previsto. Non è una prova integrata del servizio -85.

Il test verifica nel programma B la voce originale 0xdfefc=0x4067a e copia nel ProgramDB transitorio le finestre [0x404f4,0x404fa), [0x4067a,0x406b6), [0x406c6,0x406f6), [0x19e064,0x19e066) e [0xdfefc,0xdff00). Byte e voce del vettore provengono dal programma locale esistente, senza riestrarre o rileggere il file firmware originale autonomo. Nessun byte firmware è incorporato nello script o salvato nel repository. Il ProgramDB usa il linguaggio isolato a cinque bit di AB-85; database e linguaggio installati restano invariati.

La matrice comprende ILM=0,15,16,31, I=0/1, R12 iniziale 0x55 oppure 0x80000055 e risultato sintetico zero oppure 0xdf: **32 casi**. SP=SSP iniziale è 0x81004000; R0–R11 e AC/FP/RP/MDL/MDH hanno gli stessi valori distinti di AB-82. Gli altri flag partono da zero. L'adattatore sostituisce INT a 0x19e064, salva PC+2=0x19e066 e PS e raggiunge il gestore letto dal vettore originale. Il prefisso e le chiamate alle funzioni a 0x404f4/0x404f8 vengono poi eseguiti dal modello dalle istruzioni copiate.

Al confine 0x406b6 verifica il frame software di 72 byte a 0x81003fb0: tutte le 18 parole, SP=FP, il PC di ritorno a frame+72 e PS a frame+76. Il frame completo occupa quindi 80 byte rispetto allo SP precedente a INT. Con ILM iniziale 15 o 31, il prefisso esegue STILM 0x1e (ILM attivo=30 nel modello corretto) e cancella il bit 31 del selettore salvato; con ILM 0 o 16 non attraversa quel ramo e conserva ILM. È un'osservazione per questi quattro valori, non una caratterizzazione esaustiva degli stati CPU.

Dopo il salto esplicito del servizio, l'epilogo arriva a RETI con SP=SSP=0x81003ff8. Il metodo AB-87 esegue RETI e aggiorna esplicitamente SSP. Il test verifica PC=0x19e066, SP=SSP=0x81004000, PS/ILM/I/S iniziali, i 17 registri recuperati e R12 esteso con segno (zero oppure -33). Controlla due sentinelle a 0x81003fa4 e 0x81004000. Il byte 0x46f, rappresentato esclusivamente in RAM sintetica, termina a 0x1e se il selettore salvato conserva il bit 31, altrimenti a 0x10. Non esegue l'istruzione al PC di ritorno.

Prima di ciascun passo delle fasi prefisso/epilogo, lo script esamina il p-code disassemblato e rifiuta varnode di registro che si sovrappongano agli offset 8–15 dello spazio registri, dedicati a SSP/USP nel linguaggio utilizzato. Le **1480 istanze di passo** esaminate non contengono tali accessi diretti. La scansione è limitata ai percorsi effettivamente provati e al p-code restituito per ciascun passo, non a tutto il firmware; RETI finale è gestita separatamente da AB-87. Non esclude dipendenze indirette non rappresentate nel modello o modifiche runtime.

Log locale `work/firmware-context.log`: 32 casi superati, 1480 istanze controllate, nessun WARN, ERROR o eccezione. Ghidra 12.1.4, Temurin 21.0.13, programma B `-readOnly -noanalysis`. Restano espliciti i due adattamenti del banco di prova (INT e sincronizzazione SSP), la correzione PS/ILM e il salto del servizio. Non sono coperti S=1, interrupt annidati, periferiche reali, scheduler, temporizzazione o ripresa effettiva di un task Nikon. Prossimo passo: includere dispatcher e servizio -85 con liste sintetiche coerenti, eliminando il salto manuale da 0x406b6 a 0x406c6.


## AB-89 · Servizio -85 integrato fra ingresso adattato e ritorno RETI

Componente B, campione AB-1, 6 novembre 2026. [D90Service85Integrated.java](../ghidra/D90Service85Integrated.java) elimina il salto manuale del servizio presente in AB-88. Dopo l'adattatore INT #0x40 a 0x19e064, esegue le istruzioni originali del prefisso, del dispatcher a 0x406b6, del servizio a 0x40f2c, della ricerca a 0x40bec, della rimozione a 0x418a0 e dell'epilogo. Verifica il passaggio per questi siti e per il ritorno locale 0x406c6. Si ferma prima di RETI e poi la esegue tramite il controllo supervisore AB-87, fino a PC=0x19e066. Nessuna istruzione a tale PC viene eseguita.

Sono copiate dal programma B in un ProgramDB transitorio le finestre [0x404f4,0x404fa), [0x405c8,0x405ca), [0x4067a,0x406f6), [0x40bec,0x40c26), [0x40f2c,0x40f82), [0x418a0,0x418d4), [0x19e064,0x19e066) e [0xdfefc,0xdff00). La voce originale 0xdfefc=0x4067a viene controllata. Si usa il linguaggio isolato di AB-85, non si cambia il database originale e non si incorporano byte firmware nello script. La sorgente dei byte è il programma Ghidra locale, non una nuova lettura indipendente del contenitore Nikon.

Le precondizioni sono sintetiche: ILM=31, S=0, I=0 oppure 1, R12=-85, R4=2, SP=SSP=0x81004000, nodo corrente 0x81002000 e relativa sentinella sorgente 0x81002100. Il frame software viene prodotto dal prefisso a 0x81003fb0, non predisposto dal test. La parola PS salvata dall'adattatore genera il byte 0x1f a frame+0x4d richiesto dal ramo positivo del servizio. Il prefisso imposta ILM attivo=30 e salva R12=0x7fffffab dopo la cancellazione del bit 31; il dispatcher raggiunge comunque il servizio -85. Il test controlla tutte le 18 parole del frame al confine del dispatcher e poi continua senza mutare registri o memoria.

La lista destinazione ha sentinella 0x68000118 e nodi sintetici a 0x81006000, 0x81006100 quando necessari. Le quattro combinazioni di AB-80 sono ripetute con entrambi i valori di I, per **otto casi**:

| Campi +0x14 iniziali | Posizione inserita (indice zero) | Campi +0x14 finali |
| --- | --- | --- |
| [] | 0 | [2] |
| [5] | 0 | [2,3] |
| [1,5] | 1 | [1,1,4] |
| [1] | 1 | [1,1] |

Sono verificati i collegamenti in entrambe le direzioni di tutti i nodi risultanti, i campi relativi e la sentinella sorgente tornata a puntare a sé stessa. Sono controllati nodo+8=0x00020004, nodo+0x14 con residuo 2 oppure 1, maschera 0x68000004=0xbfffffff, byte 0x68000132=0xb5 e byte 0x44=1. **Il campo nodo+0x20 contiene ora 0xffab**, derivato dalla mezza parola bassa del selettore salvato nel frame, e i due byte seguenti conservano 0x6666: la parola verificata è 0xffab6666. Il valore 0x1234 di AB-80 era un input sintetico per la prova isolata, non una costante firmware. Il significato OS del campo resta aperto.

Al ritorno verifica R12=0, recupero dei 17 registri (R4 iniziale era 2), PC=0x19e066, SP=SSP=0x81004000 e PS/ILM/I/S iniziali. Il byte RAM 0x46f termina a 0x10. Due sentinelle a 0x81003fa4 e 0x81004000 restano intatte. Il limite è 400 passi per fase. Per le 1024 istanze di passo controllate, la scansione p-code delimitata come in AB-88 non rileva accessi diretti ai registri dedicati SSP/USP.

Log finale locale `work/service85-integrated.log`: otto casi superati senza WARN, ERROR o eccezioni. Una prima esecuzione si era fermata al delay slot 0x418d0 perché la finestra copiata terminava troppo presto; la finestra finale include l'istruzione originale del delay slot e il test è stato rieseguito. Non è stato sostituito il ritorno della funzione con un risultato sintetico.

Ambiente Ghidra 12.1.4, Temurin 21.0.13, programma B in `-readOnly -noanalysis`. Dopo l'ingresso adattato non vi sono iniezioni di stato nel corpo firmware; rimangono espliciti la sostituzione Java di INT e l'aggiornamento SSP dopo RETI. Gli indirizzi 0x44 e 0x46f sono RAM sintetica: le loro scritture non attivano interrupt o scheduling. Il ritorno osservato non dimostra che una vera attesa sia scaduta o che il task riprenda sulla fotocamera. Restano fuori i rami con altre precondizioni, S=1, concorrenza, scheduler, scadenza della coda e periferiche reali. Prossimo passo: individuare il percorso che aggiorna i campi temporali della lista e riporta i nodi in una struttura eseguibile, prima di attribuire una durata o un risveglio alla richiesta 2.


## AB-90 · Avanzamento della lista e trasferimento dopo il passaggio sotto zero

Componente B, campione AB-1, 6 novembre 2026. [D90QueueConsumers.java](../ghidra/D90QueueConsumers.java) cerca i byte big endian delle costanti 0x68000118 e 0x68000114 nei blocchi inizializzati del programma. Per la sentinella trova sette occorrenze, tutte precedute da una candidata LDI:32 al sito indicato:

| Sito candidato | Costante |
| --- | --- |
| 0x408a0 | 0x68000118 |
| 0x40a72 | 0x68000118 |
| 0x40bec | 0x68000118 |
| 0x40c2c | 0x68000118 |
| 0x40d84 | 0x68000118 |
| 0x40f5c | 0x68000118 |
| 0x413ca | 0x68000118 |
| 0x404ae | 0x68000114 |

La ricerca è letterale: non identifica tutti i riferimenti indiretti o costruiti aritmeticamente e il disassemblaggio candidato non prova da solo che il sito venga eseguito. Log `work/queue-consumers.log`. [D90QueueExpiryContext.java](../ghidra/D90QueueExpiryContext.java) conserva le finestre statiche finali [0x404ae,0x404b8), [0x404fe,0x40500), [0x40a72,0x40ae2) e [0x4186e,0x418a0); log `work/queue-expiry-context.log`. A 0x404ae la parola 0x68000114 viene letta e restituita in R4: questa sola occorrenza letterale non identifica chi la incrementi.

Il percorso a 0x40a72 carica la sentinella, legge il primo nodo e, se presente, imposta il bit 0x10 a 0x68000132. A 0x40a8a legge nodo+0x14, sottrae uno e lo riscrive nel delay slot a 0x40a90. Nei casi verificati, se il risultato è zero o positivo raggiunge 0x40ae0 senza rimuovere il nodo. Se è negativo scollega il primo nodo e applica alla parola nodo+8 la maschera 0xfffcfffb. Quando il risultato è zero chiama 0x4186e, che inserisce il nodo in una lista selezionata dal campo halfword nodo+0x0c, aggiorna la maschera a 0x68000004 e scrive il byte 1 a 0x44. L'interpretazione della destinazione come lista di task eseguibili è compatibile con queste operazioni, ma lo scheduler non è eseguito né identificato completamente.

[D90QueueAdvance.java](../ghidra/D90QueueAdvance.java) entra direttamente a 0x40a72 e si ferma prima di 0x40ae0, entro 160 passi per invocazione. Predispone un solo nodo sintetico 0x81001000 nella lista 0x68000118, nodo+8=0x00020004 e indice +0x0c=2. La lista destinazione con sentinella 0x68000010 è vuota e la maschera globale è zero. Prova tre valori iniziali di nodo+0x14, mantenendo lo stato fra invocazioni e reimpostando esplicitamente PC all'ingresso per ciascuna:

| Valore iniziale | Valori dopo invocazioni successive | Invocazione del trasferimento |
| --- | --- | --- |
| 0 | -1 | 1 |
| 1 | 0, -1 | 2 |
| 2 | 1, 0, -1 | 3 |

Sono **tre cicli di vita e sei invocazioni totali**, non sei scenari indipendenti né sei interrupt hardware. Il trasferimento verificato avviene quando il valore passa sotto zero, non quando raggiunge zero. Non se ne deduce una durata: mancano origine e frequenza delle invocazioni e convenzione temporale della coda.

Dopo ogni invocazione il test controlla entrambi i collegamenti di nodo e sentinelle, nodo+0x14, nodo+8 e maschera globale. Prima del trasferimento il nodo resta nella lista sorgente e nodo+8 resta 0x20004; dopo, la sorgente è vuota, il nodo è nella destinazione, nodo+8=0 e la maschera è 0x40000000. Controlla il passaggio per 0x4186e e per la funzione a 0x404fe (RET nel programma), la parola nodo+0x20 invariata a 0x12345555, SP ripristinato a 0x81004000 e byte 0x68000132=0xb5. I byte RAM 0x44 e 0x46f restano 0x55 prima del trasferimento e diventano rispettivamente 1 e 0x1e dopo. Il test non sostituisce le chiamate firmware; non applica adattatori INT o SSP, perché esegue un segmento isolato senza INT/RETI.

Log `work/queue-advance.log`: tre cicli di vita superati senza WARN, ERROR o eccezioni. Ghidra 12.1.4, Temurin 21.0.13, programma B originale in `-readOnly -noanalysis`. Non sono coperti lista multinodo, altri stati o indici, overflow, chiamante, sequenza completa della routine dopo 0x40ae0, concorrenza o periferiche reali. Non è ancora una continuazione nello stesso stato delle prove AB-89. Prossimo passo: risalire al chiamante di 0x40a72 e verificare il comportamento con più nodi, prima di attribuire una cadenza o una ripresa effettiva al trasferimento.


## AB-91 · Chiamata da 0x4049e, contatore e gestione di più nodi

Componente B, campione AB-1, 6 novembre 2026. [D90QueueCallerContext.java](../ghidra/D90QueueCallerContext.java) individua una sola occorrenza letterale di 0x40a56 nei blocchi inizializzati, a 0x4049a. Il disassemblato da 0x40498 mostra LDI:32 0x40a56,R12 e CALL @R12 a 0x4049e, seguite dal recupero di RP/MDL/MDH, R0–R7, R8–R12/AC/FP e RETI a 0x404aa. È un chiamante statico compatibile con un involucro d'interrupt; non identifica ancora vettore, sorgente hardware o cadenza. La ricerca dei riferimenti già presenti nel database a 0x40a56 non ha prodotto righe; quella letterale non è una ricerca esaustiva dei chiamanti indiretti.

La routine a 0x40a56 salva RP e FP, carica FP=0x68000110 e incrementa di uno la coppia di parole +0 e +4 con ADD/ADDC a 0x40a68/0x40a6a. La parola bassa è a 0x68000114, quella già letta in AB-90. Dopo le scritture a 0x40a6c/0x40a6e prosegue per caduta a 0x40a72: il segmento AB-90 è quindi parte di questa routine. Il codice esamina poi altre liste a 0x68000120 e 0x68000128 e ritorna a 0x40bea, recuperando FP/RP. Il significato fisico del contatore rimane aperto.

[D90QueueMultiple.java](../ghidra/D90QueueMultiple.java), che estende gli helper di AB-90, esegue l'intera routine da 0x40a56 al ritorno a una sentinella PC=0x80000000, entro 600 passi. Le due liste successive a 0x68000120/128 sono sintetiche e vuote. La prima lista ha sentinella 0x68000118 e fino a tre nodi a 0x81001000+0x100×indice, ciascuno con nodo+8=0x20004 e indice halfword +0x0c=2. La destinazione 0x68000010 è inizialmente vuota. Sei scenari producono:

| Campi +0x14 iniziali | Campi +0x14 finali, per nodo originale | Nodi trasferiti nella stessa invocazione |
| --- | --- | --- |
| [0,1] | [-1,0] | 1 |
| [0,0] | [-1,-1] | 2 |
| [1,1] | [0,1] | 0 |
| [0,2] | [-1,1] | 1 |
| [0,0,0] | [-1,-1,-1] | 3 |
| [] | [] | 0 |

Questi casi confermano che, dopo una rimozione, il nuovo primo nodo viene esaminato e decrementato nella stessa invocazione; quando il risultato è zero/non negativo, i nodi successivi non vengono toccati. Non si attribuiscono automaticamente i valori a millisecondi o a intervalli fisici. La destinazione conserva l'ordine dei nodi trasferiti nei casi con uguale indice provati.

Ogni scenario inizializza la coppia contatore a 0x00001234:0xffffffff e verifica il risultato 0x00001235:0x00000000. Controlla tutti i valori +0x14, gli stati dei nodi (zero se trasferiti, altrimenti 0x20004), il campo +0x20 invariato a 0x12345555 e i collegamenti nelle due direzioni di entrambe le liste. Conta gli ingressi nella funzione d'inserimento 0x4186e, verifica la maschera globale zero oppure 0x40000000 e recupero di SP=0x81004000, FP=0x12345678 e RP=0x80000000. Non esegue l'istruzione alla sentinella di ritorno.

Log locali `work/queue-multiple.log` (sei casi superati) e `work/queue-caller-context.log` (listato finale con chiamante); nessun WARN, ERROR o eccezione nei log finali. Finestre statiche conservate: [0x40498,0x404ae), [0x40a30,0x40a72), [0x40b72,0x40bec). Ghidra 12.1.4, Temurin 21.0.13, programma B originale `-readOnly -noanalysis`; nessun adattatore INT/SSP è necessario alla prova isolata della routine. La porzione iniziale della finestra 0x40a30 descrive una routine diversa che termina a 0x40a54 e non viene eseguita dal test.

Non sono coperti altri indici/stati, liste secondarie popolate, tutte le condizioni di overflow, concorrenza, involucro completo fino a RETI o hardware. La prova non prosegue nello stato AB-89 e non esegue uno scheduler. Prossimo passo: risalire dall'involucro contenente 0x4049e al vettore e alle precondizioni d'ingresso, poi collegare questa routine alla lista prodotta dal servizio -85. La cadenza fisica resta sconosciuta.


## AB-92 · Vettore 0x18 e continuità dal servizio alla lista destinazione

Componente B, campione AB-1, 6 novembre 2026. [D90QueueVector.java](../ghidra/D90QueueVector.java) legge le 256 parole della tabella con TBR=0xdfc00, selezionando destinazioni nell'intervallo [0x40400,0x404ae). Trova il vettore **0x18**, voce **0xdff9c**, destinazione **0x4048a**. L'indirizzo della voce è coerente con `TBR + 0x3fc − 4×0x18`. È una ricerca delimitata sulle destinazioni, non una classificazione di tutti i vettori. Il listato da 0x4048a mostra il salvataggio R8–R12/AC/FP, R0–R7, MDH/MDL/RP, la scrittura halfword 0x1a a 0x4e, la chiamata da 0x4049e a 0x40a56 e il recupero del contesto fino a RETI 0x404aa. Log `work/queue-vector.log`.

[D90QueueInterruptIntegrated.java](../ghidra/D90QueueInterruptIntegrated.java) estende la prova integrata AB-89, includendo inoltre finestre originali [0x4048a,0x404ae), [0x404fe,0x40500), [0x40a56,0x40bec), [0x4186e,0x418a0) e [0xdff9c,0xdffa0). Usa il linguaggio isolato di AB-85 e le convenzioni esplicite di AB-86/87. I byte sono copiati dal programma Ghidra B in un ProgramDB transitorio; il database originale resta in sola lettura. Nessuna periferica fisica viene interrogata.

Prima esegue il servizio -85 come AB-89, con ILM iniziale 31, S=0, I=0/1, richiesta 2 e le quattro liste destinazione iniziali [], [5], [1,5], [1]. I nodi già presenti hanno ora stato sintetico +8=0x20004 e indice +0x0c=2, necessari anche alla fase successiva. La struttura destinazione 0x68000010 e le liste secondarie 0x68000120/128 sono inizialmente vuote. Il contatore 0x68000110/114 parte da zero. Dopo il ritorno del servizio **non si ricostruiscono né si azzerano le liste**.

Il banco di prova prepara quindi ingressi esterni sintetici al vettore 0x18: scrive PC=0x19e066 e PS iniziale nel frame di otto byte a 0x81003ff8, imposta SP=SSP a tale indirizzo, S/I attivi a zero e PC alla destinazione letta dalla voce originale. Mantiene ILM=31 per l'esperimento. Questa preparazione non è un modello dell'accettazione hardware, del livello assegnato alla richiesta o della cadenza; viene forzata anche quando I iniziale è zero. Non è eseguita la pcodeop d'ingresso di Ghidra. Nessuna istruzione del chiamante viene eseguita fra le invocazioni.

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
