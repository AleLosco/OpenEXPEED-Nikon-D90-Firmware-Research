# Communication analysis

This document preserves the detailed findings on the A/B communication paths and their isolated instruction-level experiments.

<a id="ab-30"></a>
### AB-30 — Dallo slot 0x46 alla porta dati

Il descrittore per 0x46 copia il byte di lavoro nel buffer a 0x8f52f6f4. In seguito il firmware memorizza il cursore di invio a 0x8f52f5d4. La voce 228 della tabella dei vettori di B contiene 0x111f14. Sul ramo a 0x111f46 questo gestore legge una mezza parola da 0x64 e seleziona una routine dalla tabella a 0x1c4424 usando lo stato a 0x8f52f7fe. Lo stato 5 (0x111a1c) preleva il byte dal cursore, lo aggiunge all’accumulatore, aggiorna lo stato a 6 e lo scrive con DMOVH a 0x64. Per lunghezza iniziale uno, lo stato 6 (0x111a7c) scrive il complemento della somma e passa a 7. L’indirizzo diretto codificato 0x32 è scalato per due da DMOVH.

`D90OutputByte.java` esegue in Ghidra la copia del byte e il ramo di dispatch due volte sul programma B. Si parte dallo stato 5, con una parola già selezionata di lunghezza un byte, lettura iniziale della porta zero e somma parziale impostata a 0x47. È una prova differenziale, senza generazione dell’interrupt fisico. Con il valore 0x20 il byte basso della porta assume prima 0x20 e poi 0x98; con 0x21 assume 0x21 e poi 0x97. Lo script controlla anche il passaggio 5→6→7. Per riprodurre, eseguirlo sul progetto B con il profilo FR60.

Nel componente A, a offset file 0x3f8a, la stessa istruzione di lettura della porta 0x64 è seguita dalla copia del byte a una destinazione indiretta. Questa corrispondenza è una pista per cercare il ricevitore: non prova un collegamento fisico A–B o un riconoscimento dello specifico indice 0x46 sul lato A. Il percorso in B identifica il primo effetto sulla porta dati. L’effetto fotografico resta aperto.

<a id="ab-31"></a>
### AB-31 — Il ricevitore A a 0x3f8a tratta frasi di navigazione

Gli indirizzi in questa voce sono offset nel file. Il ramo legge la porta 0x64, deposita il byte nel buffer indicato dalla parola a 0x3620c e può chiamare il selettore a 0x3fe2. La routine a 0x4056 confronta il byte con 0x24 (carattere dollaro). La fase successiva confronta i caratteri alle posizioni 3–5 con RMC, GGA o HDG, e alle posizioni 1–5 con PGRMM. Il codice controlla anche la virgola (0x2c), l’asterisco (0x2a) e il ritorno carrello (0x0d). L’insieme identifica un parser di frasi di navigazione compatibili con NMEA. [Formati delle frasi e delimitatori](https://gpsd.gitlab.io/gpsd/NMEA.html)

L’identità della porta evidenziata in AB-30 non basta a collegare questo ramo al record binario di B. Qui 0x46 può comparire nel controllo dei caratteri esadecimali: a 0x4b34 viene confrontato come limite superiore di A–F, e il percorso tratta anche 0–9 e a–f. Non abbiamo individuato in questa catena un dispatch dell’indice B 0x46 con lunghezza uno. L’attribuzione riguarda questo ricevitore e lascia aperti altri modi della periferica e altri canali di A. Il report riproduce la lettura della porta, i salti relativi e i confronti dei caratteri nel campo `a_serial_receiver`.

<a id="ab-32"></a>
### AB-32 — Mappa di A e ricezione selezionabile

La mappa di lavoro di A associa l’intervallo file [0, 0x80000) agli indirizzi CPU [0x80000, 0x100000). Il CRC finale resta escluso dalla memoria importata. Il vettore finale a offset 0x7fffc contiene 0xfc00e. A quell’indirizzo il codice imposta SP a 0x3e8cc e TBR a 0xffc00, poi chiama fra gli altri 0x80042. Questa coerenza tra vettore, istruzioni di avvio e riferimenti interni sostiene la mappa. Non descrive tutta la memoria fisica della fotocamera.

Lo slot 234 della tabella di A punta a 0x8139e. Questo gestore salva i registri, legge il puntatore a 0x3609c e lo chiama se diverso da zero. Quattro sequenze verificate scrivono in quel puntatore destinazioni diverse. Tutti gli indirizzi della tabella seguente sono CPU.

| Scrittura preparata a | Gestore installato | Evidenza nel percorso |
|---|---|---|
| 0x83f1a | 0x83f72 | Ricevitore di AB-31, lettura a 0x83f8a e parser di navigazione. |
| 0x84dd6 | 0x851b6 | Lettura da 0x64 a 0x851ec, buffer indiretto e contatore. |
| 0x84f8c | 0x85104 | Lettura da 0x64 a 0x85134, buffer indiretto e confronto dei primi due byte con 0x53, 0x53. |
| 0x87cbc | 0x87ce8 | Lettura da 0x64 a 0x87d02, buffer indiretto e contatore. |

Le tre ultime routine usano il puntatore buffer a 0x36208. Il gestore dello slot 233, a 0x813ca, usa invece la callback a 0x360a0; il percorso d’uscita a 0x852de scrive sulla porta 0x64. Esiste anche una catena distinta con callback a 0x36090 e lettura dalla porta 0x84 a 0xac87e. L’identità del destinatario fisico di questi canali resta aperta. Non attribuiamo a nessuno di essi l’indice B 0x46 senza ricostruirne il formato e il trattamento.

La conclusione di AB-31 riguarda il singolo parser. La porta 0x64 di A ha più utilizzatori selezionabili, quindi il parser NMEA non esclude un impiego differente della stessa porta in altri stati. L’ordine effettivo e le condizioni di selezione richiedono ancora analisi.

Per riprodurre l’importazione usare un programma Ghidra separato da B, con lo stesso linguaggio FR60:

```sh
python3 ghidra/prepare.py /percorso/D90_0101.bin --component A > local/D90-A.hex
```

`D90AChannels.java` verifica il vettore e disassembla gli ingressi. Per ciascuna delle quattro callback inizializza il puntatore RAM e riproduce cinque istruzioni originali da 0x813ac fino all’ingresso selezionato. Le quattro prove hanno raggiunto la destinazione attesa. La prova inizia dopo il salvataggio dei registri e si ferma prima del ricevitore; non genera un interrupt né simula la periferica. Il report `a_channels` conserva mappa, vettori e installazioni controllati sui byte originali. Il progetto locale contiene ora anche `d90-a.hex`.

Il prossimo passo è ricostruire le condizioni che selezionano queste modalità e confrontare i loro formati con la trasmissione di B. Modello consigliato per questa indagine, **Astra Medio**.

<a id="ab-33"></a>
### AB-33 — Attivazione e tabella dei comandi a due byte di A

Tutti gli indirizzi seguenti sono CPU. Nel ricevitore 0x85104, dopo aver completato i due byte iniziali, il ramo a 0x8514e confronta il buffer con 0x53, 0x53 (`SS`). La corrispondenza imposta il bit 0x20 del byte a 0x36db8. Una mancata corrispondenza porta alla chiamata di 0x83ee8, che reinstalla il parser di navigazione. La routine a 0x84db2 controlla proprio quel bit a 0x84dbc e, quando è impostato, prepara due byte da ricevere e installa 0x851b6. Questo collega un riconoscimento concreto alla selezione della modalità; la successione temporale delle chiamate nel sistema completo resta da verificare.

Quando i bit alti del byte di stato valgono zero, il ricevitore 0x851b6 combina i primi due byte del buffer in una parola big-endian e chiama 0x85076. Quest’ultima cerca la parola in una tabella a 0xe1fd4 con passo 12. Sono presenti 52 voci con chiavi distinte, seguite da una voce nulla. Le chiavi includono 0x4152, 0x4154, 0x5353 e 0x7161; non sono indici consecutivi. La routine restituisce l’indice della voce trovata, oppure 255 per le chiavi sconosciute provate. Nel percorso senza corrispondenza imposta anche il bit 0x02 dello stato. La chiave zero, che coincide con il terminatore, non è inclusa nelle prove negative.

Per una voce riconosciuta e con il bit 0x20 attivo, il byte basso del campo +2 diventa il contatore di ricezione. Il bit 0x8000 del medesimo campo seleziona la fase indicata dai bit alti di stato come 1. In quella fase, al termine del conteggio iniziale, il byte appena ricevuto diventa una nuova lunghezza. Per un campo senza 0x8000 e con conteggio positivo viene selezionata direttamente la fase 2. Quattro delle 52 voci usano 0x8000. Il percorso successivo a 0x84e32 usa l’indice salvato a 0x36db4, carica la callback dal campo +4 e le passa la mezza parola del campo +8. L’effetto delle singole callback non è ancora classificato.

`D90ACommands.java` esegue le istruzioni originali della ricerca per tutte le 52 chiavi, sia con stato iniziale zero sia con 0x20. Verifica indice restituito, stato e contatore. Esegue inoltre otto prove con le chiavi sconosciute 0x0046, 0x4601, 0x4620 e 0xffff nei due stati, due prove della selezione condizionale del ricevitore e due confronti `SS`/`SR`. **116 casi superati**. Le prove sono isolate: stack e RAM inizializzati dal test, ingresso diretto nei blocchi interessati, nessun interrupt fisico né traffico acquisito. Si fermano prima delle chiamate al sistema operativo o ai gestori finali. La selezione condizionale comprende il piccolo helper con impostazioni uguali, così che non esegua scritture alla periferica.

Il report `a_commands` conserva le 52 descrizioni e i punti di controllo sui byte del firmware. Le chiavi provate che contengono 0x46 non sono riconosciute da questa tabella. Ciò non esclude una traduzione intermedia, un altro canale o un diverso significato del byte: **non è ancora identificato il destinatario del descrittore B 0x46**. Rispetto al percorso B già verificato, abbiamo qui un riconoscimento a due byte e un’altra organizzazione dei messaggi; non è dimostrato che siano i due estremi dello stesso protocollo. Il confronto completo richiede ancora l’intestazione trasmessa da B e l’eventuale traduzione sul lato A.

Prossimo passo consigliato: ricostruire dall’inizio la transazione B, compresi intestazione, ricezione e controllo della somma, per confrontarla con i canali A sulla base dell’intero scambio. Modello consigliato, **Astra Medio**.

<a id="ab-34"></a>
### AB-34 — Scambio B per il descrittore 0x46, nei due ordini

`D90BTransaction.java` estende AB-30 all’invio iniziale e al controllo della ricezione. Tutti gli indirizzi sono CPU. Il byte richiesto viene preimpostato a 0x46 in 0x8f52f7fc. Il blocco a 0x10feba imposta lo stato 1 e lo scrive alla porta 0x64. La risposta introdotta dal test viene letta dal dispatch originale e salvata a 0x8f52f7fb; il gestore torna allo stato zero e prepara un evento 0x10. Il test si ferma prima dell’interrupt software che consegnerebbe l’evento al sistema operativo.

La preparazione a 0x10ff08 sceglie l’indice richiesto quando quella risposta è zero, altrimenti considera il byte di risposta come candidato indice, con un controllo superiore a 0x46. Le prove coprono risposte iniziali zero e 0x46, così che entrambe selezionino il descrittore già identificato. Il codice copia il valore dal suo indirizzo reale nel buffer e inizializza separatamente la somma di trasmissione con il byte richiesto e quella di ricezione con la risposta iniziale. Per il tipo 1, a 0x110080 la risposta iniziale decide l’ordine:

| Risposta iniziale simulata | Stati successivi | Ordine verificato |
|---|---|---|
| 0x00 | 8→9→10→11→12→13→0, oppure senza 12 per risposta vuota | Invia lunghezza, valore e controllo; poi riceve lunghezza, eventuali dati e controllo. |
| 0x46 | 2→3→4→5→6→7→0, oppure senza 3 per risposta vuota | Riceve lunghezza, eventuali dati e controllo; se valido, invia lunghezza, valore e controllo. |

Sul percorso di trasmissione del valore 0x20, i byte significativi in uscita sono `46 01 20 98`. Il codice scrive anche zeri sulla porta durante le fasi di ricezione; la sequenza indicata non rappresenta da sola tutti gli accessi alla periferica. Il controllo 0x98 è il complemento a otto bit di 0x46 + 1 + 0x20. Cambiando il valore in 0x21 diventa 0x97. Questi valori ora derivano dall’inizializzazione originale delle somme, mentre AB-30 iniziava da una somma parziale impostata dal test.

La ricezione accetta una somma complessiva il cui byte basso vale 0xff, includendo risposta iniziale, lunghezza, dati e controllo. Nel percorso che riceve prima, un controllo errato impedisce l’invio successivo del valore e prepara l’evento 0x20. Nel percorso che trasmette prima, il valore è già stato inviato quando viene rilevato l’errore di ricezione. La prova controlla i parametri degli eventi prima dell’interrupt software, senza simularne la consegna.

Sono passati **32 casi**: quattro valori del parametro (0, 0x20, 0x21, 255), due risposte iniziali, due lunghezze ricevute (0 e 2), controllo corretto o alterato. I casi controllano byte in uscita, transizioni di stato, copia dei dati ricevuti, somme iniziali ed esiti. Gli ingressi del dispositivo sono sintetici. La prova omette le operazioni di lock/unlock della coda e il relativo azzeramento di un flag, oltre alla chiamata di pulizia degli eventi; inizializza stack e RAM e riprende dai blocchi documentati. Non esegue il firmware dall’avvio, non riproduce tempi o interrupt fisici e non identifica l’altro dispositivo. Non copre risposte iniziali che selezionano descrittori diversi da 0x46, errori elettrici o timeout.

La tabella A di AB-33 non contiene chiavi il cui primo byte sia 0x46. Quindi la concatenazione diretta del comando iniziale di questo scambio con un secondo byte non trova una voce in quella tabella. Questo restringe la ricerca ma non esclude un intermediario, un’altra modalità o un altro canale. Il destinatario fisico rimane sconosciuto. I controlli statici sono riproducibili nel campo `b_transaction`; eseguire `D90BTransaction.java` sul programma B per le prove emulative.

Il prossimo passo è inventariare gli altri ingressi di comunicazione di A e cercare una macchina a stati compatibile con questo scambio, senza limitare la ricerca alla porta 0x64. Modello consigliato, **Astra Medio**.

<a id="ab-35"></a>
### AB-35 — Altro trasporto A sulla porta 0xd4 e limiti della scansione

Gli indirizzi sono CPU. La ricerca di parole opcode nei byte di A è stata usata soltanto per generare candidati. Due falsi positivi verificati in Ghidra: la parola 0x0928 a 0x88b1c appartiene all’immediato di `LDI:20 #0x928,R4` iniziato a 0x88b1a; la parola 0x096e a 0xd05c8 appartiene all’immediato di `LDI:20 #0xd096e,R12` a 0xd05c6. Non sono letture dalle porte 0x50 e 0xdc. Non consideriamo pertanto la scansione grezza un inventario delle periferiche.

È invece confermata la lettura dalla porta 0xd4 a 0x89b54, dentro il gestore 0x89b32. La sequenza a 0x89914 carica quell’ingresso e lo salva nella callback RAM a 0x36118. Il gestore esamina il bit 0x08 della porta 0xd2. Se non è impostato, legge una mezza parola da 0xd4, salva il byte basso nella destinazione indicata da 0x37380, incrementa il puntatore e il contatore a otto bit a 0x368cc. Imposta inoltre il contatore a 0x37388 a 1 oppure, nel ramo in cui il byte a 0x39d34 vale 0x32 e il conteggio coincide con il byte a 0x368cb, a 5000. Il caso del primo byte precede questi confronti e usa 1. Una routine separata a 0x89bde decrementa quel contatore; l’unità temporale non è determinata.

Con il bit 0x08 impostato, il percorso evita la copia nel buffer, imposta il bit 0x02 a 0x368ce e prepara l’evento 0x8000 per l’oggetto 8, fermandosi nel test prima della chiamata al sistema operativo. La routine vicina a 0x89a0e appartiene al percorso che scrive sulla stessa porta, ma non è ancora ricostruito l’intero scambio né il consumatore finale del buffer. Questi fatti identificano un trasporto distinto; non identificano il dispositivo collegato.

`D90AD4Receive.java` ha superato **16 prove isolate** sul codice originale: byte 0, 0x46, 0x98 e 255, ciascuno nei casi primo byte, byte successivo ordinario, contatore speciale ed errore della periferica. Controlla contenuto e avanzamento del buffer, conteggio, valori 1/5000 e mancata copia nel ramo di errore. Stato delle porte, RAM e buffer sono sintetici; non vengono eseguiti interrupt fisici o chiamate al sistema operativo. Il fatto che 0x46 venga copiato come qualunque altro byte non prova che venga riconosciuto come comando.

Sono confermate anche letture dalla porta 0x64 a 0xf839a e 0xfac1e, in due cicli che controllano uno stato e copiano byte in memoria. Non sono state attribuite al protocollo B. L’inventario resta parziale: sono ancora aperti accessi indiretti, altri candidati e la semantica dei buffer. Nessun collegamento A–B viene dichiarato sulla sola coincidenza di un indirizzo di periferica. Il report `a_transport` conserva questi riscontri e i due controesempi di scansione.

Prossimo passo: seguire chi prepara e consuma il buffer del canale 0xd4 e confrontarne il formato con AB-34. Modello consigliato, **Astra Medio**.

<a id="ab-36"></a>
### AB-36 — Buffer e controllo del canale 0xd4: somma zero

La routine A a 0x89430 prepara normalmente due buffer di 48 byte, azzerandoli a 0x3738a e 0x373ba. Salva le basi rispettivamente nei puntatori 0x3737c e 0x37380. Per il modo 0x32 usa invece 0x365c0 e 0x36648 e salta quell’azzeramento. Il modo deriva dai sei bit bassi del parametro passato a 0x89408 e viene salvato a 0x39d34. Il costruttore a 0x894e6 lo scrive come primo byte nel buffer di trasmissione. I significati fotografici dei modi restano aperti.

La tabella a 0xe9324 seleziona i blocchi che impostano i conteggi a 0x368cb e 0x368cd. Nel campione sono presenti queste assegnazioni esplicite:

| Modo | Conteggio passato alla preparazione TX | Conteggio passato al controllo RX |
|---|---:|---:|
| 1 | 47 | 48 |
| 2 | 10 | 11 |
| 3 | 32 | 33 |
| 4 | 45 | 46 |
| 5 | 4 | 5 |
| 0x30 | 4 | 13 |
| 0x31 | 5 | 6 |

Le altre voci tra 1 e 50 raggiungono il ritorno senza assegnare nuovi conteggi in questa routine. Il report le rappresenta con `counts: null`, non con lunghezze zero. Non è quindi lecito trattare tutte le voci come modi autonomi validi o assumere che cancellino i conteggi precedenti.

La routine 0x8988a ha due operazioni. Con R4=0 somma modulo 256 i primi R6−1 byte puntati da R5 e scrive nell’ultimo byte l’opposto della somma. Con R4 diverso da zero controlla invece che la somma includendo l’ultimo byte valga zero: cancella il bit 0x04 a 0x368ce se valido, lo imposta altrimenti. Dopo la ricezione, il percorso a 0x89854 riporta il cursore all’inizio sottraendo il conteggio, poi chiama proprio questo controllo. Il risultato viene letto a 0x89862; un errore imposta anche il bit 0x80 prima della chiamata successiva. L’interpretazione finale dei campi del buffer non è ancora completata.

`D90AD4Checksum.java` ha superato **768 prove di checksum e due di preparazione dei buffer**. Per ogni valore da 0 a 255 usa i primi tre byte `46 01 valore`: verifica la generazione del controllo, l’accettazione con somma zero e il rifiuto del controllo con somma 255 usato da B. Le prove impiegano le istruzioni originali, buffer sintetici e verificano che gli altri bit dello stato siano preservati. Non simulano il trasporto o il sistema operativo. Per esempio, `46 01 20 98` viene rifiutato, mentre `46 01 20 99` supera questo controllo. Il byte 0x46 è qui un dato della prova, non un modo riconosciuto dal costruttore A.

Questo risultato contraddice l’ipotesi che il percorso di controllo appena analizzato accetti direttamente, senza trasformazioni, i messaggi B di AB-34. Non identifica il destinatario fisico e non esclude una conversione o altri percorsi. I riscontri sono nel campo `a_d4_frame`. La ricerca del destinatario B proseguirà sul canale 0x84 e sugli accessi ancora non classificati, anziché assumere equivalenza fra queste due somme di controllo. Modello consigliato, **Astra Medio**.

<a id="ab-37"></a>
### AB-37 — Canale A 0x84: preparazione, buffer e controllo somma più uno

Gli indirizzi sono CPU. Il gestore a 0xac7cc usa il buffer 0x39dc5 e l’indice a otto bit a 0x39dfb. Nel blocco a 0xac87e legge dalla porta 0x84, copia il byte basso nella posizione indicata dall’indice, incrementa quest’ultimo e scrive zero nella posizione successiva. La prova riguarda indici 0, 1 e 45; non stabilisce la sicurezza dei limiti del buffer per tutti gli ingressi.

Il costruttore 0xab00a azzera 50 byte a 0x39d93, legge l’identificatore a 16 bit a 0x37476 e seleziona la preparazione. Sono stati eseguiti i seguenti casi fino al punto di uscita comune a 0xab874:

| Identificatore in RAM | Primo byte preparato | Contatore a 0x39dfa |
|---|---|---:|
| 0xa000 | 0xa0 | 22 |
| 0xa100 | 0xa1 | 18 |
| 0xa200 | 0xa2 | 46 |
| 0xa300 | 0xa3 | 2 |
| 0x0046 | 0xa1 | 18 |
| 0x4600 | 0xa1 | 18 |

Gli ultimi due casi raggiungono la preparazione predefinita a 0xab80e. Non costituiscono un riconoscimento dell’indice B 0x46. Il primo byte è osservato nel buffer di uscita, senza riprodurre qui tutta la successiva trasmissione. L’elenco è un sottoinsieme dei rami del costruttore, non l’intero repertorio del canale.

La routine a 0xac59a somma modulo 256 i primi R4 byte del buffer 0x39dc5 e confronta il successivo con **somma + 1**. In caso di differenza imposta il bit 0x10 del byte a 0x36930. In caso di corrispondenza ritorna senza cancellare un eventuale errore precedente. Due chiamanti verificati a 0xac4d2 e 0xac530 passano rispettivamente lunghezze 1 e 2 e controllano poi quel bit prima di usare i dati. Questa è una terza regola, distinta sia dalla somma totale zero di AB-36 sia dalla somma totale 255 di B.

`D90A84Protocol.java` ha superato **786 prove isolate**: 768 sul controllo della somma, sei sulla selezione del costruttore e dodici sulla copia dalla porta. Le prove della somma usano il buffer sintetico `46 01 valore controllo` con R4=3, per tutti i 256 valori. Controllano la formula prevista, la formula B e la conservazione dell’errore preesistente. Tale lunghezza serve al confronto differenziale e non dimostra che uno dei chiamanti reali richieda quel preciso messaggio.

Il controllo di B coincide accidentalmente con somma + 1 quando il valore vale 0x38 o 0xb8. Negli altri 254 casi della prova la formula di B imposta l’errore. Per esempio `46 01 20 98` fallisce qui, mentre `46 01 20 68` supera il solo controllo della somma. La coincidenza di pochi messaggi non dimostra equivalenza dei protocolli. Le prove usano RAM e ingressi di periferica sintetici, non generano interrupt e non identificano il dispositivo collegato. I risultati e i punti di controllo sono riproducibili in `a_port84` e nello script Ghidra.

A questo punto i percorsi di A esaminati hanno formati o controlli diversi dallo scambio B, ma la copertura è ancora parziale. Il passo successivo deve riesaminare anche gli accessi indiretti alle periferiche e i riferimenti hardware, senza assumere che i registri numericamente uguali dei due componenti siano collegati fra loro. Modello consigliato, **Astra Medio**.

<a id="ab-38"></a>
### AB-38 — Topologia esterna e inventario del trasporto B

Il confronto con il sorgente Nikon Hacker distingue il modello FR/TX con pannello D5100 dalla D90: il collegamento di CSIO0 a EI155 non identifica il destinatario del nostro B, e il protocollo implementato per EI155 non coincide con AB-34. Le fonti fissate a una revisione, le differenze e i limiti sono in [communication-topology.md](communication-topology.md).

L’analizzatore estrae ora tutte le 71 voci della tabella B a 0x1c3fa4, passo 16, con fine esclusiva 0x1c4414: puntatore dati, campo dimensione, tipo e callback. Sono presenti 41 voci di tipo 0, 25 di tipo 1 e 5 di tipo 2, con 41 callback distinte. Le cinque dimensioni zero vengono preservate. L’inventario non equivale a 71 comandi fotografici identificati né a 71 percorsi emulati. Tutti i precedenti campi del report restano invariati.

Una nuova pista statica collega le voci 0x33 e 0x34: la callback di 0x34 a 0x11151c confronta i bit bassi dei rispettivi buffer e può notificare 0x33. Occorre verificare i rami e seguire i chiamanti prima di attribuire un significato. Il destinatario fisico resta aperto; prossimo passo, prova delimitata di questa callback. Modello consigliato: **Astra Medio**.

<a id="ab-39"></a>
### AB-39 — Callback 0x34: rami verificati prima delle chiamate esterne

`D90BCallback34.java` ha superato 256 casi con ingressi sintetici, eseguendo le istruzioni da 0x11151c al primo ingresso in 0x1011b0, 0x110412 o 0x10a806, oppure prima dell’INT a 0x1115b8. Sono verificati gli aggiornamenti del primo byte della voce 0x33, la selezione della notifica 0x33 e la preparazione dell’evento 0x800. BANDL nel ramo di azzeramento cancella anche il bit 2: la maschera è 0xf8. Le funzioni esterne e il sistema operativo non vengono eseguiti. Matrice dei rami e limiti in [communication-topology.md](communication-topology.md); valutazione degli altri progetti in [github-projects.md](related-projects.md). Il significato fotografico e il destinatario fisico restano sconosciuti.

<a id="ab-40"></a>
### AB-40 — Effetti dopo la callback 0x34 e controllo interno

Il nuovo script `D90BCallback34Effects.java` esegue realmente la funzione 0x10a806: imposta il bit 0x80 a 0x8f52e35c e ritorna alla callback, che prosegue verso l’evento. Gli altri percorsi si fermano alla notifica o prima del primo INT nella funzione 0x1011b0. Sono passati 256 casi. `D90BStateGate.java` verifica inoltre 32 casi della funzione interna 0x1011ce fino alla preparazione delle chiamate con maschera 0x800 oppure 0xfffff7ff. Servizi del sistema operativo e funzioni riceventi non vengono eseguiti. È stato individuato staticamente un aggiornamento dei tre byte della voce 0x33, ma non ancora l’inizializzazione completa all’avvio. Risultati, rami esclusi e prossime piste in [communication-topology.md](communication-topology.md).

<a id="ab-41"></a>
### AB-41 — Maschere B e consumatori del bit 0x80

Le funzioni 0x106e14/0x106ea2 applicano AND/OR alla parola 0x8f52e484; il confronto con 0x8f52e480 decide il seguito e può cancellare/impostare il bit 0x02 del registro 0x50000106. `D90BMaskConsumers.java` verifica 80 casi, iniziando dopo l’acquisizione OS e fermandosi prima dei seguiti non modellati. Altre otto prove verificano due lettori del bit 0x80 a 0x8f52e35c: questo può saltare la preparazione di un evento oppure selezionare percorsi diversi nella funzione 0x10a810. Individuato staticamente anche un collegamento alla notifica della voce 0x0b. Nessun significato fotografico o destinatario fisico viene dedotto da questi soli effetti. Dettagli e punti di arresto in [communication-topology.md](communication-topology.md).

<a id="ab-42"></a>
### AB-42 — Notifica 0x0b e scritture hardware

`D90BMessage0B.java` verifica 512 casi dell’aggiornamento del bit 0x04 nel secondo byte del buffer 0x0b: notifica soltanto se il valore richiesto cambia. Altri 96 casi verificano il controllore fino alla prima notifica o al ritorno, senza simulare il servizio di notifica. `D90BHardwareSequence.java` esegue la funzione 0x108624 fino al ritorno e verifica le scritture in più blocchi hardware con due riempimenti iniziali; quattro casi delimitano inoltre il ramo a 0x10b3ce. Le periferiche sono memoria sintetica e il loro ruolo fotografico resta sconosciuto. Dettagli e indirizzi in [communication-topology.md](communication-topology.md).
