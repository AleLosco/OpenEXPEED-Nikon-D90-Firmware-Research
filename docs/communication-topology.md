Warning: truncated output (original token count: 74822)
Total output lines: 1868

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

Sul componente B del campione AB-1, 0x19dc7c controlla il bit 0x40 a 0x6800cf1f dopo INT 0x40. Sul ramo abilitato chiama 0x19efc4. Nel percorso statico di avvio, 0x19f036 scrive tram…49822 tokens truncated…ue byte di stato agli indirizzi 0x8f53300d e 0x8f53300e per ciascuno degli otto scenari. Questi due byte sono imposti dal banco dopo le copie. Prima di ogni combinazione ripristina il blocco tipo 1 e i registri R8, R9, R10, FP, SP, RP, imponendo PC=0x13a278.

Con flag 1 uguale a zero il blocco tipo 1 è conservato. Con flag 1 uguale a uno, il byte a 0x8f53c4d4 viene mascherato con 0xcf e quello a +4 diventa (precedente & 0x83) | 0x08. Il confronto copre tutti i 160 byte. La maschera 0xcf corrisponde alla semantica BANDH del profilo in uso.

Con flag 2 uguale a zero il codice raggiunge 0x13a2b6. Con flag 2 uguale a uno la CALL originale raggiunge 0x13a802 con R4=0x8f53da71 e RP=0x13a2ac. Il banco si arresta prima di decodificare o eseguire l'helper. Gli 80 byte dello stack candidato sono conservati in ogni caso.

Log locale work/third-call-flags.log, Ghidra 12.1.4, profilo ILM AB-85. Tutte le 32 prove passano senza WARN/ERROR. La variazione dei flag è sintetica. L'effetto dell'helper e il comportamento con payload tipo 1 effettivamente assente restano da verificare.

## AB-136 · Quattro copie dei valori interni per il tipo 2

Componente B del campione AB-1. [D90Type2HelperContext.java](../ghidra/D90Type2HelperContext.java) verifica staticamente che l'helper 0x13a802 copia 19 byte dalla sorgente originale 0x1cabaf alla destinazione R4 e ritorna a 0x13a812. [D90Type2Defaults.java](../ghidra/D90Type2Defaults.java) raggiunge lo stato AB-134 e impone flag 1=0 e flag 2=0 oppure 1. Ogni variante riparte da 0x13a278 con blocco tipo 2 e registri di confine ripristinati.

Con flag 2=1, il ciclo originale invoca quattro volte l'helper. R4 vale 0x8f53da71+58×indice, per indice 0–3, e RP vale 0x13a2ac. Il risultato atteso sostituisce nel blocco tipo 2 i 19 byte agli offset 31, 89, 147 e 205 con i valori originali a 0x1cabaf. Con flag 2=0 il blocco è conservato. Entrambi i percorsi si arrestano a 0x13a2b6, prima delle successive operazioni.

Sedici casi (otto stati × due flag) e 32 invocazioni dell'helper superati. Il confronto copre tutti i 234 byte del blocco tipo 2, i 160 byte del tipo 1 e gli 80 byte dello stack. SP è conservato. Ghidra 12.1.4, profilo ILM AB-85, programma B in sola lettura, log locali work/type2-helper-context.log e work/type2-defaults.log, senza WARN/ERROR. Il flag 2 è imposto dopo il caricamento sintetico del tipo 2. La semantica dei 19 campi copiati resta aperta.

## AB-137 · Valori interni per il tipo 3 e maschere intermedie

Componente B del campione AB-1. [D90Type3HelperContext.java](../ghidra/D90Type3HelperContext.java) disassembla l'helper 0x13a814: copia 35 byte da 0x1cab8c a R4 e ritorna a 0x13a824. [D90Type3Defaults.java](../ghidra/D90Type3Defaults.java) prosegue da AB-136, imponendo flag 3=0 o 1 e PC=0x13a2b6 prima di ogni variante. Il blocco tipo 3 è un input sintetico distinguibile byte per byte.

Il tratto iniziale imposta i quattro bit alti del byte +16 del blocco tipo 1 e azzera i due bit bassi del byte +11. R10 viene avanzato di 11. Con flag 3 impostato seguono quattro CALL originali con R4=0x8f53d987+56×indice e RP=0x13a2ea. Ogni chiamata copia i 35 byte originali agli offset 23, 79, 135 e 191 del blocco tipo 3. Con flag zero il blocco tipo 3 è conservato. L'arresto è a 0x13a2f4.

Sedici casi e 32 invocazioni superati in Ghidra 12.1.4 sul profilo ILM AB-85. Il confronto copre tutti i 226 byte tipo 3, i 160 byte tipo 1 e gli 80 byte dello stack. L'ultimo byte tipo 3 occupa la cella precedentemente usata come guardia iniziale del tipo 2; la mappa aggiunta evita sovrapposizioni dei blocchi. Log locali work/type3-helper-context.log e work/type3-defaults.log senza WARN/ERROR. Il flag 3 e il payload sono imposti dal banco. Restano aperti i significati dei campi e i rami successivi.

## AB-138 · Azzeramenti controllati dai flag 9, 10 e 11

Componente B del campione AB-1. [D90MissingBlockClear.java](../ghidra/D90MissingBlockClear.java) raggiunge lo stato AB-137 e impone tutte le otto combinazioni binarie dei flag 9, 10 e 11 a 0x8f533015–0x8f533017. Ogni variante reimposta PC=0x13a2f4 e riempie con byte distinguibili tre regioni dati sintetiche, ciascuna con quattro byte di guardia su entrambi i lati.

Il flag 9 controlla l'azzeramento dei 68 byte da 0x8f9237ac. Il flag 10 controlla i 68 byte da 0x8f922768. Il flag 11 controlla i due byte a 0x8f94e116. Per ogni flag zero il corrispondente contenuto è conservato. Le guardie restano integre in ogni caso. Tutte le varianti terminano a 0x13a336, prima del ciclo successivo.

Sessantaquattro casi (otto stati AB-137 × otto combinazioni) superati. Sono confrontate interamente le tre regioni e le guardie, i 26 flag e gli 80 byte dello stack candidato. Ghidra 12.1.4, profilo ILM AB-85, programma B in sola lettura. Log locale work/missing-block-clear.log senza WARN/ERROR. I flag e le regioni azzerate sono input sintetici aggiunti al banco dopo AB-137. Restano aperti il significato dei campi e la prosecuzione dai flag 12 e successivi.

## AB-139 · Nove azzeramenti indicizzati dai flag 12–20

Componente B del campione AB-1. [D90MissingArrayClear.java](../ghidra/D90MissingArrayClear.java) continua dallo stato AB-138 a 0x13a336. Il ciclo originale esamina nove byte di stato, indici 12–20, e avanza il puntatore relativo di 0x242 (578) byte per iterazione. Per ogni flag impostato azzera i primi 64 byte del record a 0x8f94cc9c+578×indice. Con flag zero conserva il record. All'uscita a 0x13a364 R4=0x1452 e R5=21, compreso l'incremento nel delay slot finale.

Il banco impone tredici maschere sui nove flag: tutte zero, tutte uno, due alternanze e ciascuno dei nove bit singolarmente. Ogni maschera riparte da PC=0x13a336 con dati distinguibili riscritti. Per tutti gli otto scenari sono confrontati 5202 byte di record più quattro byte di guardia per lato, verificando anche le porzioni tra gli intervalli azzerati. Sono confrontati i 26 flag e gli 80 byte dello stack.

Tutte le 104 prove passano con Ghidra 12.1.4 e profilo ILM AB-85, programma B in sola lettura. Log locale work/missing-array-clear.log senza WARN/ERROR. Le 13 maschere costituiscono una selezione di casi mirati, con dati e flag sintetici. Restano da analizzare il ramo del flag 23 e l'epilogo della routine.

## AB-140 · Inizializzazione del tipo 23 e copia successiva

Componente B del campione AB-1. [D90Type23Context.java](../ghidra/D90Type23Context.java) verifica il listato 0x1ad966–0x1ad97c. L'helper azzera le prime due parole del blocco a 0x8f6e3f2c e applica BANDL #7 al byte a 0x8f53c4d5. Nel profilo usato il byte è mascherato con 0xf7.

[D90Type23Copy.java](../ghidra/D90Type23Copy.java) raggiunge AB-139, poi prova flag 23=0 e 1 imponendo PC=0x13a364 e riscrivendo sorgente, destinazione e blocco tipo 1. Con flag zero l'helper viene saltato. Con flag uno i primi otto byte della sorgente sono azzerati e la maschera è applicata. Il ciclo originale successivo copia 624 byte a parole, in ordine discendente, da 0x8f6e3f2c a 0x8f6e419c. Le due regioni sono adiacenti.

Sedici casi superati, arresto a 0x13a38c. Il confronto copre sorgente e destinazione intere più quattro byte di guardia esterna per lato (1256 byte complessivi), tutti i 160 byte tipo 1 e gli 80 byte dello stack. Ghidra 12.1.4, profilo ILM AB-85, programma B in sola lettura. Log work/type23-context.log e work/type23-copy.log senza WARN/ERROR. Le regioni dati e il flag sono sintetici. Il ramo del flag 24 e il ritorno della terza chiamata restano da verificare.

## AB-141 · Flag 24 e ritorno della terza chiamata

Componente B del campione AB-1. [D90Type24Context.java](../ghidra/D90Type24Context.java) verifica staticamente che l'helper 0x1ae4c0 azzera la mezza parola a 0x8f6e1164 nel delay slot della RET a 0x1ae4c8. [D90Type24Return.java](../ghidra/D90Type24Return.java) raggiunge AB-140 e impone flag 24=0 oppure 1, riscrivendo i dati di prova, PC=0x13a38c, SP e R9 al valore di confine.

Con flag impostato la mezza parola viene azzerata; con flag zero è conservata. In entrambi i casi l'epilogo azzera il byte a 0x8f549bee e ritorna al candidato a 0x1000e2. Il banco confronta i byte adiacenti alle due scritture, gli 80 byte dello stack e RP/R8–R11 con le cinque parole salvate sullo stack. SP avanza di 20 byte.

Sedici casi superati con Ghidra 12.1.4 e profilo ILM AB-85, programma B in sola lettura. Log work/type24-context.log e work/type24-return.log. Una prima versione del banco riusava R9 dopo il ritorno precedente, che lo aveva ripristinato al valore del candidato: il secondo caso leggeva da una base errata. La versione finale ripristina R9 prima di ogni variante e il log finale è privo di WARN/ERROR. Le varianti del flag sono ingressi sintetici dal confine AB-140. La verifica chiude l'epilogo delimitato; restano da collegare tutti i rami su un singolo insieme coerente di record.

## AB-142 · Terza chiamata continua

Componente B, campione AB-1. [D90ThirdCallIntegrated.java](../ghidra/D90ThirdCallIntegrated.java) eredita il confine AB-130 a 0x1000da. Prima della CALL il banco prepara 26 destinazioni riempite con 0xbb, la memoria di lavoro azzerata e due sorgenti sintetiche con marcatore 0xaa55 e terminatore 0xffff. In quattro scenari il primo banco contiene i 13 tipi pari e il secondo contiene solo il marcatore e il terminatore. Negli altri quattro i 13 tipi dispari sono nel secondo banco. Il payload varia per scenario, tipo e posizione. Le lunghezze e le destinazioni provengono dai descrittori originali AB-132.

La prova esegue un solo tratto dal PC ereditato a 0x1000e2. Il firmware costruisce le tabelle, determina i 26 flag, copia i record ed esegue i rami di correzione e il ritorno. Durante la chiamata tutte le scritture provengono dalle istruzioni emulate. Il confronto finale copre i 22690 byte delle 26 destinazioni, i 624 byte della copia del tipo 23, i 26 flag, SP, RP e R8–R11. I risultati attesi derivano dai payload forniti e dalle regole AB-134–141.

Ghidra 12.1.4, profilo ILM AB-85, programma B in sola lettura. Il log locale work/third-call-integrated.log riporta otto esiti positivi senza WARN/ERROR. La prova verifica due configurazioni complementari di record presenti. Il contenuto dei banchi fisici e la funzione dei singoli campi sulla fotocamera restano aperti.

## AB-143 · Quarta chiamata e parser a cinque voci

Componente B del campione AB-1. [D90FourthCallContext.java](../ghidra/D90FourthCallContext.java) verifica LDI a 0x1000e2 e CALL a 0x1000e8, con destinazione 0x13b7f6. Il confronto diretto del campione originale a offset 0x1400e4 restituisce 9f8c0013b7f6971c. La routine salva R8–R10 e RP e invoca 0x13b894 con selettore 0, poi 1 nel delay slot della seconda chiamata.

[D90FourthParserContext.java](../ghidra/D90FourthParserContext.java) ricostruisce l'helper. Usa due basi dalla tabella ROM 0x1cac80 e 0x1cac84, registra i puntatori a 0x8f5330f0, azzera 40 byte temporanei a 0x8f5330f8 e raccoglie cinque coppie puntatore/lunghezza di otto byte. Il terminatore 0xffff arresta la scansione. Per i record ordinari il codice controlla tipo inferiore a 5 e lunghezza non superiore a 0x3600. Il marcatore 0xaa55 salva il contatore a 0x8f533170+2×selettore, imposta il byte a 0x8f53326e+selettore e pubblica i 40 byte nella tabella a 0x8f533120+0x28×selettore. Il budget iniziale è 0x20000.

Il chiamante confronta la differenza dei contatori ridotta a 16 bit con 0x8000, ordinando le due tabelle. Per ciascuna delle cinque voci sceglie la prima lunghezza non nulla e copia il payload alla destinazione 0x89f958c0+0x3600×indice. Salva i 16 bit bassi della lunghezza a 0x8f533264+2×indice. La RET è a 0x13b892. I listati non mostrano letture dei puntatori ENDM o dei blocchi SYSTEMFIXADJUST. Le aree di lavoro sono distinte da quelle del parser precedente.

Ghidra 12.1.4, programma B in sola lettura, log locale work/fourth-call-context.log. Entrambi i listati raggiungono i rispettivi ritorni senza WARN/ERROR. Il risultato è statico. La natura fisica delle sorgenti e della nuova destinazione resta da identificare prima di interpretare le copie come effetti sulla fotocamera.

## AB-144 · Quarta chiamata continua e limiti dei record

Componente B del campione AB-1. [D90FourthCallIntegrated.java](../ghidra/D90FourthCallIntegrated.java) verifica le due parole ROM a 0x1cac80 e 0x1cac84, rispettivamente 0x780000 e 0x7a0000. Dopo AB-142 il banco predispone due sorgenti sintetiche e le destinazioni. Il firmware esegue la CALL dal candidato e ritorna a 0x1000ea. I record ordinari comprendono lunghezze dispari con padding, una lunghezza zero e un record tipo 4 lungo esattamente 0x3600.

Negli otto scenari le differenze modulo 65536 dei contatori sono 0, 1, 0x7fff, 0x8000, 0x8001, 0xffff, 2 e 0x8000. Quando entrambi i banchi offrono la voce 0, viene scelto il primo sotto 0x8000 e il secondo a partire dalla soglia. Le voci disponibili in un solo banco vengono copiate anche quando l'altro è preferito. Una lunghezza zero permette di usare l'altra copia. Per la voce 3, assente in entrambi, destinazione e campo lunghezza conservano il valore di prova.

Nei primi quattro casi entrambi i banchi terminano dopo il marcatore con 0xffff. Negli altri quattro, dopo il primo marcatore, il banco 0 contiene un record tipo 4 lungo 0x3601 e il banco 1 un record tipo 5. Un secondo marcatore distinguibile segue ciascun record: i contatori finali restano quelli dei primi marcatori. La prova verifica l'esito osservabile di questi due casi limite.

Confrontati tutti i 69120 byte delle cinque destinazioni, otto byte di guardia complessivi, le cinque lunghezze, i due flag di pubblicazione e i contatori. Verificati SP e RP al ritorno. Ghidra 12.1.4, programma B in sola lettura, profilo ILM AB-85. Log locale work/fourth-call-integrated.log: otto casi superati senza WARN/ERROR. Le sorgenti e la memoria di destinazione sono simulate. Il contenuto fisico e il significato delle cinque voci restano aperti.

## AB-145 · Quinta chiamata e nuovo confine INT

Componente B del campione AB-1. [D90FifthCallContext.java](../ghidra/D90FifthCallContext.java) identifica la quinta destinazione 0x112008. Nel campione originale a offset 0x1400ec si leggono gli otto byte 9f8c00112008971c: LDI di 0x112008 e CALL senza delay slot. Il ritorno atteso nel candidato è 0x1000f2.

La routine salva RP e chiama 0x11212a. Poi prepara R12=-23 tramite LDI 0xe9 ed EXTSB, R4=0x1c, R5=0 ed esegue INT 0x40 a 0x112014. Il seguito recupera RP e termina con RET a 0x112018. Questo ingresso avverrebbe nello stato utente ereditato dal candidato; l'adattatore AB-86 accetta soltanto ingressi con S=0 e una specifica istruzione INT.

[D90FifthInitContext.java](../ghidra/D90FifthInitContext.java) disassembla il prefisso a 0x11212a. Esso chiama 0x105b02 con R4=0x8f52e4b8 e poi 0x8f52e4b0. [D90FifthListContext.java](../ghidra/D90FifthListContext.java) conferma che questo helper salva la base in entrambe le parole della testa. Il ciclo successivo chiama due volte 0x105b16 con R4=0x8f52e4b8 e R5 rispettivamente 0x8f52e710 e 0x8f52e960, avanzando di 0x250. L'uso di 0x105b16 come collegamento in lista è già documentato nelle prove precedenti.

Ghidra 12.1.4, database B in sola lettura, log work/fifth-call-context.log, work/fifth-init-context.log e work/fifth-list-context.log. I tre listati raggiungono i ritorni senza WARN/ERROR. Nel prefisso esaminato i puntatori sono immediati e non compare una lettura dei blocchi ENDM o delle quattro copie già eseguite. La destinazione e gli effetti del servizio -23 restano da verificare.

## AB-146 · Liste della quinta chiamata e confine utente

Componente B del campione AB-1. [D90FifthLists.java](../ghidra/D90FifthLists.java) prosegue dallo stato AB-144 a 0x1000ea e arresta il firmware prima di INT a 0x112014. Le due teste e i due elementi sono regioni sintetiche riempite con 0xcd, con guardie di quattro byte per lato.

La testa 0x8f52e4b0 diventa una lista vuota con entrambe le parole uguali alla base. La testa 0x8f52e4b8 contiene avanti=0x8f52e960 e indietro=0x8f52e710. Il nodo 0x8f52e960 punta avanti a 0x8f52e710 e indietro alla testa. Il nodo 0x8f52e710 punta avanti alla testa e indietro a 0x8f52e960. Il confronto integrale delle tre regioni (24, 16 e 16 byte) verifica tutti i collegamenti e le guardie.

Al confine INT, SP è quattro byte sotto il valore all'ingresso e la parola salvata contiene il ritorno del candidato 0x1000f2. R8–R10 sono conservati. R12=0xffffffe9, R4=28, R5=0, S=I=1 e ILM=31. Il servizio richiesto è quindi -23 nello stato utente ereditato.

Otto casi superati con Ghidra 12.1.4 e profilo ILM AB-85. Log locale work/fifth-lists.log senza WARN/ERROR. Il prefisso esegue codice originale sui dati di prova. L'ingresso INT da stato utente e il servizio restano da modellare e verificare.

## AB-147 · Ingresso utente modellato e selezione del servizio -23

Componente B del campione AB-1. [D90FifthServiceEntry.java](../ghidra/D90FifthServiceEntry.java) prosegue da AB-146 verificando l'opcode 1f40 a 0x112014, TBR=0xdfc00 e vettore 0xdfefc→0x4067a. Il banco salva lo SP utente corrente in USP e costruisce esplicitamente su SSP il frame di otto byte con PC successivo 0x112016 e PS ricomposto dai flag del profilo ILM AB-85. Imposta lo stato supervisore con S=I=0 e trasferisce il controllo alla voce del vettore. Questa transizione è un'assunzione del banco specifica al sito verificato.

Il prefisso originale arriva a 0x406b6 con SP=FP=0x81003fb0. Le parole salvate a +16 e +20 valgono 28 e 0. Il selettore salvato a +48 vale 0x7fffffe9 e ILM attivo vale 30. La tabella originale contiene 0x0e68 a 0x40644. Le istruzioni del dispatcher portano il PC a 0x414da con RP=0x406c6. La prova verifica il frame PC/PS sullo stack supervisore, USP e tutti gli 80 byte ispezionati dello stack utente fino a questo confine.

Otto casi superati in Ghidra 12.1.4 con programma B in sola lettura. Log locale work/fifth-service-entry.log. L'esecuzione si ferma prima della prima istruzione a 0x414da. Gli effetti del servizio e la corrispondenza hardware dell'ingresso INT da S=1 restano aperti.

## AB-148 · Mappa statica del servizio OS -23

Componente B del campione AB-1. [D90Service23Context.java](../ghidra/D90Service23Context.java) esamina il database Ghidra esistente in sola lettura con `-noanalysis`, usando pseudo-disassemblaggio limitato perché nell'elenco analizzato non c'è una funzione che contenga 0x414da. Questa osservazione descrive la mappa delle funzioni del database, non l'assenza di codice nel firmware. Log locale work/service23-context.log; Ghidra 12.1.4, Java Temurin 21.0.13. I blocchi esaminati terminano ai ritorni previsti e non producono WARN/ERROR nel log.

Il servizio salva RP, passa 0x5c40c e R4 a 0x41852 per selezionare un descrittore, poi legge i campi +0x10 e +0x14 come R6 e R7 e il primo campo del descrittore come puntatore di controllo D. Per R4=28 la tabella ha intestazione count=38 e stride=28; il record selezionato è a 0x5c704: +0x00=0x680005e0, +0x04=0x001c000d, +0x08=0, +0x0c=0x40000000, +0x10=0x0011201a, +0x14=0x8f9b0204, +0x18=0. L'helper decrementa l'indice e calcola la base del record con la stride dichiarata.

Con i valori d'ingresso già verificati in AB-147 (R4=28, R5=0), il percorso principale confronta D con la parola a 0x68000000. Se coincidono, verifica bit e byte a 0x68000131 e nell'area adiacente, con un ramo che scrive R5 e il byte 1 a 0x44; il significato di questi registri/periferiche e le condizioni pratiche restano da identificare. Il flusso poi verifica il bit 0x10 a D+8. Se manca, torna R12=0xc1. Se è presente, chiama 0x41534.

La routine 0x41534 azzera D+8, chiama l'helper 0x4186e con il puntatore di contesto R7 nel delay slot, quindi scrive sul contesto: RP iniziale 0x4050a a +8, R5 a +0x24, R6 a +0, e PS 0x001f0030 a +4. Scrive inoltre R7 in D+0x18 e il valore zero iniziale in D+8 prima della chiamata. L'helper 0x4186e è già stato descritto in AB-90 come inserimento in lista scelto dal campo +0x0c e aggiornamento di una bitmap. Per il record 28 il campo alto/basso a +4 è 28/13; il costruttore documentato in AB-101 copia il valore basso nella struttura di controllo, compatibile con selettore lista 13. La lettura combinata suggerisce un percorso di preparazione/attivazione del contesto, ma il nome semantico del servizio e il fatto che il task venga effettivamente eseguito non sono provati.

AB-148 è un risultato statico sul database, non una riesecuzione emulata del servizio. La relazione tra tabella e codice è coerente con il confine emulato AB-147, ma in questo passaggio non è stato eseguito un nuovo confronto dei byte firmware con l'HEX né è stata eseguita la routine su RAM sintetica. Restano aperti i rami per altri argomenti, gli effetti fisici degli indirizzi 0x44 e 0x68000000/131, il comportamento dello scheduler e l'accettazione hardware dell'interrupt. Prossimo passo: preparare una prova emulata delimitata per R4=28/R5=0 con D e le aree di controllo sintetiche, confrontando contesto, lista e bitmap prima/dopo e controllando anche l'esito del ramo non eleggibile senza attribuire semantica hardware.

## AB-149 · Ritorno del servizio OS -23

Sul componente B di AB-1 il banco riparte a 0x414da dagli otto stati AB-147 con R4=28 e R5=0. In tutti verifica che D=0x680005e0 sia diverso dal controllo corrente, che D+8 valga 16 e che la lista 13 a 0x68000068 sia vuota. Nei quattro scenari pari conserva lo stato ereditato. Nei quattro dispari impone D+8=0 prima del servizio. In ogni caso inizializza con 0xa5 i 40 byte del contesto a 0x8f9b0204 e quattro byte di guardia per lato. [Script Ghidra](../ghidra/D90Service23Return.java)

Con il bit presente il servizio ritorna a 0x406c6 con R12=0 e SP recuperato. Il nodo e la sentinella si collegano in entrambe le direzioni, la bitmap acquisisce la maschera 0x00080000, D+8 diventa zero e D+0x18 contiene il puntatore al contesto. Le parole a +0, +4, +8 e +0x24 del contesto valgono 0x11201a, 0x001f0030, 0x4050a e zero. Il byte a 0x44 termina a 1 nella RAM simulata. Con il bit cancellato il ritorno locale ha R12=0xc1 e SP recuperato. I valori finali della regione OS, del contesto e del byte a 0x44 coincidono con quelli all'ingresso del servizio.

Il confronto copre i 4096 byte da 0x68000000, i 48 byte del contesto con guardie e il byte a 0x44. Ghidra 12.1.4, profilo AB-85, programma B in sola lettura, log locale `work/service23-return.log`. Il punto di arresto precede l'epilogo del dispatcher. Restano aperti il ramo con controllo corrente uguale a D, il ritorno al chiamante utente e l'esecuzione del contesto sulla fotocamera.

## AB-150 · Ritorno utente del servizio -23

Componente B del campione AB-1. Dagli otto stati AB-149 a 0x406c6, il dispatcher esegue l'epilogo originale, inclusa EXTSB R12 a 0x406d0, e RETI a 0x406f4. Nei quattro casi eleggibili il chiamante riceve R12=0. Nei quattro casi con bit 0x10 cancellato dal banco riceve R12=0xffffffc1 (-63). Il PC arriva a 0x112016 con PS uguale alla parola salvata nel frame, S=I=1 e ILM=31. Lo stop precede l'istruzione a 0x112016. [Script Ghidra](../ghidra/D90Service23UserReturn.java)

Prima di RETI, SP=SSP=0x81003ff8. Subito dopo, il modello espone SP=0x81004000, SSP=0x81003ff8 e USP conservato. Come in AB-123, il banco sincronizza esplicitamente SSP con lo stack supervisore consumato e seleziona USP come SP attivo. Gli 80 byte controllati dello stack utente coincidono con lo stato precedente all'epilogo. Il comportamento del cambio di banco sulla fotocamera resta aperto. Ghidra 12.1.4, profilo AB-85, database B in sola lettura, log locale `work/service23-user-return.log`.

## AB-151 · Ritorno della quinta chiamata al candidato

Componente B del campione AB-1. [D90FifthCallReturn.java](../ghidra/D90FifthCallReturn.java) estende AB-150 importando la finestra originale [0x112016,0x11201a). Dagli otto confini utente a 0x112016 esegue LD @SP+,RP e RET, raggiungendo 0x1000f2. Verifica SP aumentato di quattro byte, RP=0x1000f2, R12 conservato a 0 oppure 0xffffffc1 (-63), S=I=1 e ILM=31. Gli 80 byte dello stack utente confrontati restano uguali al confine AB-150. Il tratto eredita le precondizioni e la selezione esplicita dello stack di AB-149/150.

Il listato statico del seguito mostra LDI:32 #0x10fcc2,R12 a 0x1000f2 e CALL @R12 a 0x1000f8. Queste istruzioni sovrascrivono il risultato in R12 prima della chiamata successiva, senza confronto o ramo intermedio. Il punto di arresto dell'emulazione precede la LDI. La nuova chiamata e i suoi effetti restano da analizzare.

Otto ritorni verificati, quattro con risultato zero e quattro con -63. Ghidra 12.1.4, linguaggio isolato AB-85, database B in sola lettura con -noanalysis. Log locale `work/fifth-call-return.log` senza WARN/ERROR/eccezioni. Il risultato riguarda il percorso software sul banco con RAM sintetica e gli adattamenti INT/stack già documentati. Prossima priorità è mappare la routine 0x10fcc2 e le sue dipendenze.

## AB-152 · Sesta chiamata e richiesta per il record 8

Componente B del campione AB-1. A 0x10fcc2 la routine carica R4=8, R5=0 e R12=0xe9. EXTSB produce -23 in R12 prima di INT #0x40 a 0x10fcca. Il listato termina con RET a 0x10fccc. [Listato statico](../ghidra/D90SixthCallContext.java)

Negli otto scenari AB-151, quattro per ciascun risultato precedente, il firmware esegue la CALL da 0x1000f8 e raggiunge 0x10fcca con i tre argomenti previsti, RP=0x1000fa, S=1 e SP uguale al valore all'ingresso del tratto. La prova si arresta prima di INT. Nella tabella originale AB-148 il record 8 a 0x5c4d4 contiene +0=0x68000270, +0x10=0x0010fcce e +0x14=0x8f993e04. Questi valori provengono dalla lettura statica del database. Restano da verificare l'esito del servizio per questo record e le scritture nel relativo contesto. [Prova emulata](../ghidra/D90SixthServiceRequest.java)

Ghidra 12.1.4, database B in sola lettura, profilo isolato AB-85 per l'emulazione. Log locali `work/sixth-call-context.log` e `work/sixth-service-request.log`.

## AB-153 · Inserimento del record 8 nella lista 4

Componente B del campione AB-1. Negli otto scenari AB-152 il controllo del record 8 a 0x68000270 è diverso dal controllo corrente. Il campo +8 vale 16, la mezza parola +0xc vale 4 e la lista a 0x68000020 è vuota. Il banco conserva questo stato e prepara 48 byte a 0xa5 per il contesto e le guardie. Al sito 0x10fcca applica l'ingresso INT utente di AB-147, parametrizzato per sito e indice. Il dispatcher e il servizio eseguono le istruzioni originali fino al ritorno locale a 0x406c6. [Script Ghidra](../ghidra/D90SixthServiceReturn.java)

Il servizio restituisce zero in tutti gli otto casi, quattro per ciascun esito della prima richiesta. Nodo e sentinella puntano reciprocamente in entrambe le direzioni, la bitmap incorpora 0x10000000 e il campo +8 del nodo diventa zero. Il campo +0x18 contiene 0x8f993e04. A questa destinazione le parole +0, +4, +8 e +0x24 valgono rispettivamente 0x10fcce, 0x001f0030, 0x4050a e zero. SP recupera il valore all'ingresso del servizio.

Il confronto finale copre i 4096 byte della regione OS, i 40 byte del nuovo contesto con quattro byte di guardia per lato e i 40 byte del precedente contesto a 0x8f9b0204. I collegamenti della lista 13 e il contesto precedente conservano i valori ereditati in ciascuno scenario. Ghidra 12.1.4, profilo isolato AB-85, database B in sola lettura e log locale `work/sixth-service-return.log`. Il banco si ferma prima dell'epilogo del dispatcher. L'esecuzione del contesto preparato resta da verificare.

## AB-154 · Ritorno della sesta chiamata

Componente B di AB-1. [D90SixthCallReturn.java](../ghidra/D90SixthCallReturn.java) prosegue da AB-153 attraverso epilogo e RETI originali. Il banco applica la selezione esplicita dello stack utente già documentata in AB-150. Il PC raggiunge 0x10fccc con R12=0, PS recuperato, S=I=1 e ILM=31. La RET originale raggiunge 0x1000fa conservando SP, RP=0x1000fa e R12=0 in tutti gli otto scenari.

Il seguito statico mostra LDI:32 #0x14ca90,R12 a 0x1000fa e CALL @R12 a 0x100100. Lo stop emulato precede queste istruzioni. Log finale locale `work/sixth-call-return.log`, Ghidra 12.1.4, profilo AB-85 e database B in sola lettura, senza WARN/ERROR/eccezioni. La prima versione del banco generalizzava erroneamente il controllo della parola in cima allo stack dalla quinta chiamata alla sesta. La sesta conserva RP nel registro e non ha il push della quinta. Il controllo specifico della quinta è stato mantenuto nel suo punto originale e la versione finale è stata rieseguita attraverso entrambe le chiamate.

## AB-155 · Lista corrente dopo la preparazione dei task

Componente B di AB-1. Negli otto stati AB-154 il candidato esegue la settima CALL da 0x100100 a 0x14ca90. Questa routine salva RP e chiama 0x19d89c, già studiata isolatamente in AB-72. All'ingresso, RP=0x14ca9a e sullo stack utente è salvato 0x100102. Qui termina la nuova esecuzione. [Prova della settima chiamata](../ghidra/D90SeventhCallEntry.java)

Il controllo corrente 0x680001ec coincide con la testa della lista 1 in tutti gli otto casi. La lista 4 punta al controllo del record 8. Nei quattro casi con il record 28 in lista 13 la bitmap vale 0x90080000. Negli altri quattro la lista 13 è vuota e la bitmap vale 0x90000000. In entrambi i casi il primo bit impostato a partire da sinistra corrisponde alla lista 1. Applicando il criterio di ricerca AB-116/117 a questa istantanea si ritroverebbe quindi il controllo corrente. Questa deduzione presuppone lo stesso percorso del dispatcher. Il punto in cui il firmware cambia scelta resta da individuare.

Il listato dei due ingressi preparati mostra che il record 8 (0x10fcce) salva R8 e RP, esegue ENTER, carica 0x60 in R8 e chiama 0x10fd26. Il record 28 (0x11201a) salva R8 e RP, esegue ENTER e chiama 0x112244. Questi due prefissi sono osservazioni statiche. [Script per i tre prefissi](../ghidra/D90PreparedTaskEntries.java)

Ghidra 12.1.4, programma B in sola lettura, profilo AB-85 per il banco emulato. Log locali `work/prepared-task-entries.log` e `work/seventh-call-entry.log`. Prossima indagine mirata ai punti di attesa o uscita del candidato che possono cambiare il controllo in testa alla lista 1.

## AB-156 · Cambio della lista del controllo corrente

Componente B del campione AB-1. Nel seguito della settima chiamata compare INT a 0x14cac2 per il servizio -48 (R4=3, R5=0x200). Tornato al candidato, il listato da 0x100102 contiene dodici CALL prima di INT a 0x100164 per il servizio -27 (R4=0, R5=7). Un successivo INT a 0x100184 richiede -47. Le tre voci della tabella a 0x40612, 0x4063c e 0x40614 portano rispettivamente a 0x41a5a, 0x4168e e 0x41af4. Sono finestre statiche lineari. La raggiungibilità dipende dai ritorni delle chiamate intermedie. [Listati e calcolo delle voci](../ghidra/D90CandidateSchedulingSites.java)

Nel gestore -27, il bit 0x20 del byte a frame+0x4f e R4=0 selezionano il controllo corrente tramite la parola a 0x68000000. Il ramo con stato zero e indice valido chiama 0x418a0 e 0x4186e, scrivendo il nuovo indice a controllo+0xc nel delay slot. Nel banco si conservano le liste AB-155 e si impongono PC, RP, SP, FP, S, I, gli argomenti 0/7 e il byte 0x30 nel frame. L'esecuzione parte direttamente dal gestore e si arresta a 0x406c6. [Prova isolata](../ghidra/D90CurrentListChange.java)

Negli otto casi il servizio ritorna zero, ripristina SP, svuota la lista 1 e collega in entrambe le direzioni il controllo corrente 0x680001ec alla lista 7. La lista 4 conserva in testa il controllo del record 8. La bitmap perde 0x80000000 e acquisisce 0x02000000, diventando 0x12080000 o 0x12000000 secondo lo stato precedente del record 28. Il puntatore corrente conserva il suo valore. Secondo il criterio AB-117 la prima lista selezionabile è ora la 4. Il cambio di contesto e l'esecuzione del record 8 restano da verificare. Anche l'effetto delle chiamate intermedie sullo stato all'ingresso naturale del servizio resta aperto.

Ghidra 12.1.4, programma B in sola lettura, profilo AB-85 per il banco. Log locali `work/candidate-scheduling-sites.log` e `work/current-list-change.log`.

## AB-157 · Selezione ed esecuzione del record 8

Componente B di AB-1. Negli otto scenari AB-156 il banco conserva le liste, la bitmap e il contesto del record 8 preparato dal servizio -23. Impone PC=0x40e2a e S=1, SP=USP=0x820000c0 in RAM dedicata e SSP=0x81003ff8 con frame uscente sintetico PC=0x100166 e PS=0x1f0030. Ai campi +40…+76 del contesto entrante assegna parole distinguibili. I primi 40 byte ereditano le scritture firmware e il riempimento sintetico AB-153. L'ingresso isola la decisione dello scheduler dalle chiamate intermedie, dall'epilogo del servizio -27 e dal trigger reale del vettore. [Prova Ghidra](../ghidra/D90Record8Dispatch.java)

Il caricamento originale della bitmap e l'adattatore SRCH1 AB-116 producono indice 3. Il dispatcher seleziona 0x68000270 dalla lista 4 mentre il controllo corrente vale ancora 0x680001ec. Salva il contesto uscente, carica SP=0x8f993e04 dal record 8 e prepara il frame di ritorno sullo stack supervisore. Il banco applica esplicitamente i cambi di stack nei punti di transizione AB-119–123. Prima di RETI il puntatore corrente vale 0x68000270, RP vale 0x4050a e il frame contiene 0x10fcce e 0x001f0030.

RETI raggiunge 0x10fcce in stato utente. Il prologo originale del record 8 viene eseguito fino alla prima chiamata a 0x10fd26, con RP=0x10fcda e R8=0x60. Tutti gli otto scenari superano i controlli, inclusi i quattro con record 28 presente in lista 13. Ghidra 12.1.4, profilo AB-85, programma B in sola lettura, log locale `work/record8-dispatch.log`. Il comportamento del callee, la funzione fotografica del task e il momento in cui avviene la selezione sulla fotocamera restano aperti.

## AB-158 · Il task 8 inizializza la tabella RAM usata dal costruttore B

Componente B di AB-1. Dagli otto stati AB-157 a 0x10fd26 la routine salva registri e chiama 0x11031c. Questo helper inizializza tramite 0x105b02 le due parole della testa a 0x8f52f274 con il suo stesso indirizzo. Nel blocco adiacente a 0x8f52f27c scrive gli indici da 0 a 70 nel byte +8 di ciascuna delle 71 righe da 12 byte. Si ferma davanti a INT a 0x11034a con R12=-48, R4=3 e R5=0x80. Il controllo corrente resta 0x68000270. Il banco predispone 0xcd in testa, tabella e guardie e confronta tutti gli 868 byte finali. [Prova emulata](../ghidra/D90Record8TableInit.java)

Il precedente listato del costruttore B a 0x10ff08 (log locale `work/b-prepare.log`) mostra a 0x10ff68–0x10ff7a la stessa base RAM e lo stesso passo 12, con accesso al byte +9 della riga selezionata dal byte letto tramite R5. Questo stabilisce un uso comune della struttura. La tabella ROM a 0x1c3fa4 descritta in AB-43 ha invece voci da 16 byte. Resta aperto il significato fotografico delle singole righe.

Nel seguito statico di 0x10fd26, dopo il ritorno dell'helper, compaiono scritture agli indirizzi 0x60, 0x61, 0x63, 0x100 e 0x10e. A 0x10fd62 il codice testa un bit del byte a 0x50000107 e dirama a 0x10fdba oppure prosegue verso un'altra richiesta OS. Il confine emulato precede queste operazioni. [Listati statici](../ghidra/D90Record8FirstCallee.java)

Ghidra 12.1.4, database B in sola lettura, profilo AB-85 per il banco. Log locali `work/record8-first-callee.log` e `work/record8-table-init.log`. Otto inizializzazioni superate senza WARN/ERROR. L'esecuzione eredita l'ingresso sintetico nel dispatcher di AB-157.

## AB-159 · Servizio -48 e primo ramo periferico del task 8

Componente B del campione AB-1. Dal confine AB-158 a 0x11034a il banco applica l'ingresso INT utente esplicito e segue dispatcher, servizio -48, epilogo e RETI. Il servizio usa la tabella a 0x5cb68 (intestazione 22, stride 16) per selezionare l'oggetto 3 a 0x68000b20. La lista d'attesa ereditata è vuota. Negli otto scenari il campo +8 passa da zero a 0x80, R12 ritorna zero e il byte condiviso 0x68000132 termina col bit 0x80 impostato. Il confronto copre tutti i 4096 byte della regione OS. Un primo controllo del banco attendeva erroneamente il bit condiviso cancellato al ritorno. È stato corretto e rieseguito. [Prova del servizio](../ghidra/D90Record8Service48.java)

Il codice originale ritorna attraverso 0x11034c all'interno di 0x10fd26. Al confine 0x10fd5c il banco verifica i byte 0x60=0x40, 0x61=0x41, 0x63=0, la parola 0x100=0x278cf e la mezza parola 0x10e=0. Per ciascuno degli otto scenari si reimposta PC=0x10fd5c e si impone il byte sintetico a 0x50000107 prima a zero, poi a 0x40. Nel primo caso il BEQ raggiunge 0x10fdba. Nel secondo il PC arriva a 0x10fd66, prima del corpo del ramo alternativo. Il byte imposto resta conservato. Sono 16 prove del tratto LDI/ADD/BTSTH/BEQ. Le istruzioni precedenti di inizializzazione sono eseguite una sola volta per scenario. [Prova dei due rami](../ghidra/D90Record8HardwareBranch.java)

Ghidra 12.1.4, profilo AB-85, database B in sola lettura. Log finali locali `work/record8-service48.log` e `work/record8-hardware-branch.log` senza WARN/ERROR/eccezioni. Il modello usa RAM agli indirizzi periferici e conserva gli adattamenti di ingresso INT e selezione dello stack già dichiarati. Restano aperti l'effetto fisico delle scritture e la provenienza hardware del bit 0x40 a 0x50000107.

## AB-160 · Due preparazioni del task 8 e servizio -47

Componente B di AB-1. [D90Record8Modes.java](../ghidra/D90Record8Modes.java) riparte dal confine AB-159, salvando i registri usati e i 4096 byte iniziali di RAM bassa. Ogni variante ripristina questi valori, impone PC=0x10fd5c, assegna il byte sintetico a 0x50000107 e prepara il byte a 0x8f52f7fd con guardie. Le liste e i contesti OS sono quelli ereditati. Le due varianti raggiungono le rispettive richieste OS.

| Byte a 0x50000107 | Byte a 0x8f52f7fd | Mezza parola a 0x42, valore iniziale H | Sito INT | R4 | R5 | R12 |
|---|---|---|---|---|---|---|
| 0 | 1 | (H & 0xcfff) OR 0x2000 | 0x10fdf4 | 3 | 0xfffffffe | -47 |
| 0x40 | 3 | H OR 0x3000 | 0x10fd94 | 3 | 0xfffffffd | -47 |

In entrambi i rami il byte a 0x41 viene mascherato con 0xbf e il byte a 0x40 riceve 0xbf. Il confronto include tutti i 4096 byte di RAM bassa, il byte di modalità e le sue guardie. L'ingresso INT esplicito segue poi il dispatcher fino al servizio -47 a 0x41af4 e al ritorno locale a 0x406c6. R12 vale zero. Nei 4096 byte della regione OS i valori finali coincidono con quelli precedenti alla variante. In particolare l'oggetto 3 conserva il campo +8=0x80. I bit che le due maschere cancellerebbero erano già zero. Questi casi verificano il comportamento sullo stato ereditato, mentre l'effetto di cancellazione con bit inizialmente impostati resta da provare.

Sedici casi superati, due varianti per ciascuno degli otto scenari. Ghidra 12.1.4, profilo AB-85, database B in sola lettura e log locale `work/record8-modes.log` senza WARN/ERROR/eccezioni. I registri periferici sono RAM sintetica. Il significato fisico dei valori di modalità 1/3 e dei bit di controllo resta aperto. Il ramo con byte 0x40 ha una successiva lettura dello stesso bit e una possibile richiesta -170 a 0x10fdb4, da analizzare.

## AB-161 · Seconda lettura del task 8 e ingresso nel servizio -170

Componente B del campione AB-1. Dal ritorno del servizio -47 del ramo di modalità 3, l'epilogo e RETI riportano il task a 0x10fd96 con la selezione dello stack utente modellata in AB-150. Il firmware esegue BORH e prepara R0=0x50000107. Il banco varia il byte subito prima di BTSTH a 0x10fd9e. Con zero, BEQ porta a 0x10fdba. Ripetendo il test e il salto con 0x40, il firmware raggiunge INT 0x40 a 0x10fdb4 con R4=FP−4 (indirizzo di una parola locale), R5=3, R6=2, R7=2, AC=100 e R12=0xffffff56 (−170). Il banco riutilizza l’ingresso INT esplicito. Il dispatcher originale usa la voce a 0x4051e e raggiunge 0x41bfc. La parola locale conserva il valore precedente all'ingresso.

Otto scenari, ciascuno con entrambi gli esiti della seconda lettura. Ghidra 12.1.4, profilo AB-85, progetto B in sola lettura. [Script di riproduzione](../ghidra/D90Record8Request170.java), log locale `work/record8-request170.log`. La prova si ferma prima della prima istruzione del servizio. Restano aperti l'uso della parola locale da parte del servizio, il significato fisico del bit e l'unità del valore 100.

## AB-162 · Il task 8 prepara una richiesta di attesa

Componente B di AB-1. Dall'ingresso di AB-161 a 0x41bfc il servizio -170 legge il controllo corrente 0x68000270, risolve l'oggetto 3 e chiama 0x40cd0 con R4=100. Al ritorno a 0x41c46, FP indica l'oggetto a 0x68000b20. La parola a +8 vale 0x80. In modalità 2, il codice a 0x41c5a–0x41c5e verifica l'intersezione con la maschera 2. Il risultato è zero e il firmware prosegue a 0x41c60. A 0x41c6a scrive la maschera 2 nella parola a task+0x14 e a 0x41c6c scrive la modalità 2 nella mezza parola a task+0x0e. Il confine verificato è 0x41c76, prima della CALL:D a 0x41ecc e della sua istruzione nel delay slot.

Il banco confronta la parola utente indicata da R4 all'ingresso con quella al confine finale. I valori coincidono. Il confronto riguarda i due estremi e lascia aperti eventuali accessi intermedi. Il chiamante contiene una scrittura esplicita `ST AC,@R4` a 0x10fdb8 dopo INT (listato AB-158). Per attribuirla alla stessa parola occorre seguire il ritorno e controllare R4 e AC a quel sito. Anche l'inserimento in lista e l'unità temporale del valore 100 restano aperti per questo percorso.

[Prova AB-162](../ghidra/D90Record8WaitPrefix.java). Il banco usa il profilo AB-85, il database B in sola lettura e le stesse otto combinazioni di AB-161. Gli ingressi INT e i valori periferici mantengono gli adattamenti delle prove precedenti. Log locale `work/record8-wait-prefix.log`.

## AB-163 · Trasferimento del task 8 nella lista dell’oggetto 3

Componente B di AB-1. Al confine AB-162 (0x41c76) il task 8 a 0x68000270 è l'unico elemento della lista dei pronti 4, con testa a 0x68000020. La lista dell'oggetto 3, con testa a 0x68000b20, è vuota. La CALL:D carica in AC la parola a SP+0x34 e raggiunge 0x41ecc con RP=0x41c7a. Il servizio memorizza la mezza parola bassa (0xff56) a task+0x20 e il valore 0x00100004 a task+8. Chiama 0x418a0 per scollegare il nodo dai pronti. Le scritture da 0x41ee0 a 0x41ee8 lo collegano alla lista dell'oggetto.

| Stato al ritorno a 0x41c7a | Valore verificato |
|---|---|
| Due puntatori della testa dei pronti 4 | Entrambi 0x68000020 (lista vuota) |
| Due puntatori della testa dell'oggetto 3 | Entrambi 0x68000270 (task 8) |
| Due puntatori del task 8 | Entrambi 0x68000b20 (oggetto 3) |
| Bitmap dei pronti a 0x68000004 | Si azzera il solo bit 0x10000000 |
| Puntatore corrente a 0x68000000 | Ancora 0x68000270 |

Il banco confronta i 4096 byte della pagina OS con lo stato iniziale corretto per queste scritture e verifica il ripristino di SP. Il confronto include la maschera e la modalità di AB-162 e i flag dell'oggetto. Ghidra 12.1.4, database B in sola lettura, profilo AB-85 e otto scenari ereditati. [Script AB-163](../ghidra/D90Record8WaitList.java), log locale `work/record8-wait-list.log`. Il confine precede le operazioni sulla lista con testa a 0x68000128. Restano aperti la ripresa del task e i tempi sulla fotocamera.

## AB-164 · Secondo nodo del task 8 e ritorno al dispatcher

Componente B di AB-1. Al confine AB-163 a 0x41c7a, la parola a task+0x28 (0x68000298) contiene 0x68000cd0, lo stesso indirizzo presente in R10. Il campo +8 del nodo contiene già 100. R11 punta alla testa 0x68000128, i cui due collegamenti puntano alla testa stessa. Il confronto a 0x41c80 porta a 0x41c88. Fino a 0x41ca4, incluse le istruzioni nel delay slot, il firmware collega il nodo alla lista.

| Indirizzo | Parole a +0 e +4 dopo l'inserimento |
|---|---|
| Testa della seconda lista, 0x68000128 | Entrambe 0x68000cd0 |
| Nodo associato al task 8, 0x68000cd0 | Entrambe 0x68000128 |
| Testa dell'oggetto 3, 0x68000b20 | Entrambe 0x68000270, come in AB-163 |

Il confronto dei 4096 byte della pagina OS con la pagina ereditata, aggiornando le quattro parole previste, riesce sia a 0x41ca4 sia a 0x406c6. Al secondo confine il servizio ha restituito zero in R12 e SP è avanzato di quattro byte rispetto a 0x41c7a. Il task 8 è ancora collegato all'oggetto 3. L'esecuzione si arresta nel dispatcher supervisore, prima del ritorno al codice utente. Restano da verificare il percorso di ripresa e l'unità del valore 100.

[Script AB-164](../ghidra/D90Record8SecondList.java). Otto scenari ereditati, Ghidra 12.1.4, database B in sola lettura, profilo AB-85. Log locale `work/record8-second-list.log`.

## AB-165 · Ritorno al task 8 e scrittura nella parola locale

Componente B di AB-1. Da 0x406c6, con il task 8 ancora nella lista dell'oggetto 3, la chiamata a 0x404f8 esegue RET e torna a 0x406d0. L'epilogo ripristina i registri dal frame del servizio e RETI a 0x406f4 riporta il PC a 0x10fdb6. Si riutilizzano i controlli di AB-150 su PC, PS, S=I=1 e ILM=31 e la selezione esplicita dello stack utente dopo RETI. Il task che prosegue nel banco è lo stesso che aveva richiesto il servizio.

Dopo il NOP, a 0x10fdb8 R4 coincide sia con la parola salvata nel frame a +16 sia con FP utente meno quattro. AC coincide con la parola salvata a +52 e R12 vale zero. L'istruzione originale ST AC,@R4 scrive 100 a 0x8f993e2c, dove prima si leggeva il valore sintetico 0x12340028. Il confronto include quattro byte di guardia per lato. Il banco si arresta a 0x10fdba e confronta i 4096 byte della pagina OS con lo stato AB-164: il task resta nella lista dell'oggetto 3 e il suo secondo nodo nella lista a 0x68000128.

Nel tratto AB-165 il banco non consegna interrupt asincroni e rappresenta i registri periferici in RAM. Il ritorno osservato descrive l'epilogo sincrono in queste condizioni. L'eventuale prelazione, il risveglio fisico e la durata dell'attesa sulla D90 restano aperti. [Script AB-165](../ghidra/D90Record8WaitReturn.java), Ghidra 12.1.4, profilo AB-85, programma B in sola lettura e log locale `work/record8-wait-return.log`.

## AB-166 · Un ingresso nel vettore 0x18 decrementa il secondo nodo

Componente B di AB-1. Dal confine utente AB-165 a 0x10fdba, il banco costruisce esplicitamente un frame supervisore con PC e PS ricostruito dai flag, seleziona lo stack supervisore e trasferisce il controllo a 0x4048a. La destinazione è confrontata con la voce originale a 0xdff9c. Il gestore esegue il percorso fino a RETI a 0x404aa. La parola del nodo 0x68000cd0 a +8 passa da 100 a 99 e il contatore a 64 bit a 0x68000110 avanza di uno.

Il confronto dell'intera pagina OS di 4096 byte ammette soltanto queste due variazioni. Conserva quindi i collegamenti del task 8 all'oggetto 3, quelli del secondo nodo alla testa 0x68000128, la bitmap dei pronti e il puntatore corrente. RETI ripristina PC=0x10fdba, PS e S=1. Il banco sincronizza poi SSP/SP secondo l'adattamento esplicito già usato per il ritorno utente. I 18 registri generali/ausiliari salvati e 32 byte dello stack utente coincidono con i valori precedenti all'ingresso.

Otto scenari superati, Ghidra 12.1.4, profilo AB-85, database B in sola lettura. [Script AB-166](../ghidra/D90Record8WaitTick.java), log locale `work/record8-wait-tick.log` senza WARN, ERROR o eccezioni. L'ingresso è imposto dal banco anche rispetto a ILM e alla selezione degli stack. Frequenza, accettazione hardware e unità temporale del valore 100 restano aperte. Prossimo passo: verificare il ramo al raggiungimento di zero e il conseguente trattamento del task in attesa.

## AB-167 · Confine zero e reinserimento del task 8 tra i pronti

Componente B di AB-1. Dopo il passaggio 100→99 di AB-166, il banco imposta esplicitamente a 1 il campo del nodo a 0x68000cd8. Mantiene il resto dello stato ereditato e applica due ingressi modellati nel vettore 0x18. Al primo ingresso il valore diventa zero. Il confronto dei 4096 byte OS ammette solo il decremento e l'incremento del contatore a 0x68000110: entrambe le liste conservano i collegamenti.

Il salto BP:D a 0x40b90, con store nel delay slot a 0x40b92, prosegue a 0x40be4 anche per zero. Il profilo SLEIGH usato definisce BP con N==0. Al secondo ingresso, da zero a 0xffffffff (−1), il firmware percorre lo scollegamento da 0x40b94, raggiunge il task tramite node+0xc e chiama 0x41eac. Questa routine lo rimuove dalla lista dell'oggetto, applica la maschera 0xcf8ffffb al campo task+8 e, trovandolo zero, richiama 0x4186e per l'inserimento tra i pronti. A 0x40bde scrive 0x00ab nella mezza parola task+0x22.

| Campo dopo il secondo ingresso | Valore verificato |
|---|---|
| Testa 0x68000128, parole +0/+4 | Entrambe puntano alla testa, lista vuota |
| Testa dell'oggetto 3 a 0x68000b20, parole +0/+4 | Entrambe puntano alla testa, lista vuota |
| Testa dei pronti 4 a 0x68000020, parole +0/+4 | Entrambe 0x68000270 |
| Task 8, parole +0/+4 | Entrambe 0x68000020 |
| Task 8, parola +8 | Zero |
| Bitmap dei pronti | Acquisisce il bit 0x10000000 |
| Nodo a 0x68000cd0, parola +8 | 0xffffffff |

Il confronto dell'intera pagina OS verifica tutte le modifiche previste e la conservazione degli altri byte. Entrambi gli ingressi verificano il ritorno a 0x10fdba, PS, i 18 registri generali/ausiliari e 32 byte dello stack utente tramite il metodo condiviso con AB-166. L'estrazione di questo metodo è coperta anche dalla riesecuzione della prova 100→99 nella stessa catena.

[Script AB-167](../ghidra/D90Record8WaitExpiry.java), Ghidra 12.1.4, profilo AB-85, database B in sola lettura. Otto scenari per ciascuno dei due confini. Log finale `work/record8-wait-expiry.log` senza WARN, ERROR o eccezioni. Una prima aspettativa del banco collocava lo scollegamento a zero ed è fallita. Il log diagnostico `work/record8-wait-expiry-diagnostic.log` e la semantica di BP hanno permesso di correggerla. La prova finale usa un contatore forzato a 1 e due interrupt espliciti. Restano aperti il conteggio continuo da 100, i tempi fisici e la selezione effettiva del task da parte dello scheduler.
