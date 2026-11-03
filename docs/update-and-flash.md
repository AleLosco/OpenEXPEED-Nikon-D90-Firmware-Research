# Update package and flash path

This document preserves the detailed static analysis of update selection, validation, flash operations, and the related display paths.

## Selezione dei file di aggiornamento

<a id="ab-6"></a>
### AB-6 — Cinque percorsi candidati

In [0x1ab924, 0x1ab9d4) si ripete la stessa sequenza FR. Un caricamento in r4 punta a un modello di nome. Un secondo caricamento in r12 precede una chiamata indiretta con delay slot. Il codice esamina r4 dopo il ritorno e un salto condizionale separa la prosecuzione della ricerca dalla chiamata specifica per quel modello. Gli offset nella tabella sono nel file originale, incluse le destinazioni convertite con AB-4.

| Offset del caricamento | Modello | Routine comune | Routine specifica |
|---|---|---|---|
| 0x1ab924 | AD90????.BIN | 0x1ad10e | 0x1aca10 |
| 0x1ab94c | BD90????.BIN | 0x1ad10e | 0x1ac9a0 |
| 0x1ab96e | D90_????.BIN | 0x1ad10e | 0x1aca7e |
| 0x1ab990 | S???????.BIN | 0x1ad10e | 0x1acc06 |
| 0x1ab9b2 | NKLD????.BIN | 0x1ad10e | 0x1aced8 |

<a id="ab-7"></a>
### AB-7 — Controllo di dimensione candidato

Nel percorso D90 combinato, a 0x1aca8e il codice carica in r1 il valore 0x00780002, identico alla lunghezza del campione, poi lo confronta con r0 e prende un ramo su uguaglianza. Il percorso NKLD carica invece 0x00700000 a 0x1acee8 e usa una condizione diversa. L’origine del valore in r0 e il significato del limite NKLD sono da verificare. La presenza di percorsi distinti nel codice della D90 offre un punto di partenza per seguire la validazione dei due campioni. Il modello del nome non determina da solo quali regioni vengano scritte in flash.

Per riprodurre AB-6 usare il comando iniziale e consultare il campo `address_map.update_patterns`. Per AB-7 ispezionare i 12 byte a ciascuno degli offset indicati, interpretando le parole come big-endian.

<a id="ab-8"></a>
### AB-8 — Provenienza di r0 e biforcazione per il file D90

Tutti gli offset citati sono nel file. Il chiamante carica 0x8f545e4c in r8 a 0x1ab92a. Il delay slot della chiamata specifica copia r8 in r4 a 0x1ab98a. Nel callee la coppia di parole `c14d 0040` a 0x1aca8a imposta r13 a 0x14 e carica in r0 una parola da [r4 + r13]. Il confronto legge quindi il campo a **0x8f545e60**. La routine comune passa lo stesso puntatore alla funzione a offset 0x1f4016. Questa conserva il secondo argomento in r10 a 0x1f401c e a 0x1f416a scrive r0 nel campo [r10 + r13], con r13 impostato a 0x14. È un produttore staticamente identificabile del campo. Per attribuirgli definitivamente la semantica di lunghezza del file resta da ricostruire il formato del record sorgente.

| Valore del campo | Percorso a partire dal confronto a 0x1aca94 |
|---|---|
| Diverso da 0x780002 | Il salto ritardato a 0x1aca98 esegue il delay slot che azzera r4, poi raggiunge direttamente l’epilogo a 0x1acaf2. |
| Uguale a 0x780002 | Il salto a 0x1aca96 raggiunge 0x1aca9c. Il codice usa la stringa di radice del volume a offset 0x21303a e arriva a una chiamata indiretta a 0x1acac6. Ulteriori controlli possono ancora interrompere il percorso. |

Il campo `main_size_check` del report conserva indirizzo del campo, costante e bersagli dei salti. L’analisi è statica secondo la semantica FR e la mappatura AB-4. [Semantica delle istruzioni FR](https://sourceware.org/cgen/gen-doc/fr30-insn.html)

<a id="ab-9"></a>
### AB-9 — Dal record di directory alla dimensione

Nella routine a 0x1f4016 la base locale del record candidato è r14 − 36. A 0x1f40dc viene caricato 0xdc in r4, esteso con segno e sommato al frame pointer. Il primo byte viene esaminato a 0x1f40e8. Nel percorso che prepara l’output, i byte seguenti corrispondono al layout di una voce breve FAT.

| Byte nel record (base 0) | Campo FAT | Offset nel descrittore |
|---|---|---|
| 11 | Attributi | +0x0d |
| 22–23 | Ora di ultima scrittura | +0x10 |
| 24–25 | Data di ultima scrittura | +0x0e |
| 28–31 | Dimensione in byte | +0x14 |

Il modulo `tools/directory_entry.py` interpreta le istruzioni originali in [0x1f4122, 0x1f416c) su un record sintetico e un descrittore in memoria. Il blocco lineare elabora data, ora e dimensione. L’attributo è copiato subito prima del blocco ed è identificato staticamente. Per ogni bit dei 32 byte il report verifica i tre valori prodotti rispetto alla decodifica little-endian indipendente. Un secondo input inserisce la lunghezza reale del campione e produce 0x780002 a +0x14. La prova copre la trasformazione in memoria, senza accessi alla scheda o esecuzione del boot. [Specifica Microsoft FAT, tabella delle directory](https://www.cs.fsu.edu/~cop4610t/assignments/project3/spec/fatspec.pdf) · [Semantica FR30](https://sourceware.org/cgen/gen-doc/fr30-insn.html)

<a id="ab-10"></a>
### AB-10 — Letture nel percorso combinato

Dopo la costruzione del percorso, due chiamate relative ritardate a 0x1acad4 e 0x1acae0 hanno come destinazioni nel file 0x1acd72 e 0x1acafa. La seconda è raggiunta solo se la prima restituisce un valore diverso da zero. Le due routine riutilizzano una coppia di chiamate con argomenti coerenti con posizionamento e lettura. Il primo stadio richiede 6 byte a posizione zero. Il secondo richiede 4 byte a 0x11fbf6 (nel campione: 00000421) e li confronta con una sequenza in memoria. In seguito richiede 4 byte a 0x11fbf2, li usa come puntatore e somma 0x40002 prima di richiedere altri 9 byte. Sul campione questa catena porta esattamente a `Ver.1.01a`. Le chiamate verificano anche il numero di byte trasferiti. L’operazione fornisce un riscontro nel codice per la traslazione AB-4. Restano aperte la funzione del valore 0x421, la semantica completa delle API di I/O e le destinazioni di scrittura dei componenti A/B.

<a id="ab-11"></a>
### AB-11 — Confine A/B nel pacchetto combinato

I gestori dei nomi AD90 e BD90 confrontano la dimensione del file rispettivamente con 0x80002 (offset 0x1aca20) e 0x700000 (0x1ac9b0). Nel percorso combinato di stato 3 la stessa coppia ricompare come offset di sorgente e lunghezza di una copia. Le due lunghezze coprono esattamente il campione. Anche i checksum calcolati separatamente coincidono con i trailer.

| Blocco logico | Intervallo nel file | CRC-16/XMODEM escluso il trailer | Trailer big-endian |
|---|---|---|---|
| A | [0, 0x80002) | 0x63cc | 0x63cc |
| B | [0x80002, 0x780002) | 0xc2ab | 0xc2ab |

Il CRC di A, calcolato includendo il proprio trailer, ha residuo zero. Per questo il CRC globale precedentemente osservato coincide con quello di B. Il lungo padding a 0x120002 si trova **dentro B**.

<a id="ab-12"></a>
### AB-12 — Vettori di B

La prima istruzione di B è all’offset 0x80002, corrispondente a 0x40000 nella mappa AB-4. A 0x80014 il codice carica 0x000dfc00 in r0 e trasferisce il valore al registro dedicato 0 (`b300`). La tabella a quell’indirizzo comincia proprio a 0x11fc02 nel file. La sua ultima voce rimanda a 0x40000, quindi all’inizio di B. Questo chiude il collegamento statico tra inizializzazione, tabella e candidato ingresso di reset. [Istruzioni FR](https://sourceware.org/cgen/gen-doc/fr30-insn.html)

<a id="ab-13"></a>
### AB-13 — Consumatore dello stato

Il percorso di validazione azzera 0x8f545e64. Il primo stadio combinato può scrivervi 2. Il secondo verifica quel valore prima di sostituirlo con 3. A 0x1abf84 il codice rilegge lo stato e indicizza una tabella di indirizzi a 0x001d30c0 usando lo stato meno uno. Le cinque destinazioni nel file sono nel campo `update_layout` del report. Lo stato 3 conduce a 0x1abfae. Qui il codice usa la lunghezza combinata con la routine a offset 0x1ac1ce che passa 0x80000000 come buffer a una lettura. Nel ramo successivo alla validazione, la chiamata a offset 0x1ac058 usa come argomenti destinazione 0x80000000, sorgente 0x80080002 e lunghezza 0x700000. Il corpo chiamato a 0x1fc8e4 implementa una copia a byte con scelta della direzione. La regione B viene così portata all’inizio dello stesso buffer. Tra le chiamate seguenti, la routine a 0x17a1d0 invoca l’indirizzo 0x8f52d79e. Il suo contenuto runtime e le destinazioni fisiche della flash restano da verificare.

<a id="ab-14"></a>
### AB-14 — Copia del codice in RAM

Nello startup a offset 0x80428 il contatore vale 0x9f4. Il ciclo decrementa di due e copia una mezza parola per iterazione dalla base ROM 0x2a5838 alla RAM 0x8f52d6c0. Nel file i byte occupano [0x2e583a, 0x2e622e). Il target di AB-13, 0x8f52d79e, è 0xde byte dopo la base e corrisponde a offset 0x2e5918. Due ulteriori target di chiamate relative, 0x8f52da8c e 0x8f52dad2, ricadono nello stesso blocco e puntano a prologhi di funzione agli offset 0x2e5c06 e 0x2e5c4c.

<a id="ab-15"></a>
### AB-15 — Destinazioni del percorso combinato

Sul percorso che supera i controlli, la routine rilocata passa tre coppie (indirizzo, lunghezza) a 0x8f52da8c: (0xd0000, 0x10000), (0x40000, 0x90000), (0x100000, 0x640000). Il callee allinea l’indirizzo iniziale con la maschera 0xfffe0000. Un helper contiene, da offset 0x2e5dd0, scritture a 16 bit con valori AA, 55, 80, AA, 55 e infine 30 all’indirizzo di settore. È la sequenza di cancellazione di settore documentata per NOR parallela. Le coppie descrivono le richieste software. La geometria fisica dei settori e il modello del chip restano da identificare. [Datasheet Infineon, tabella 10.1](https://community.infineon.com/gfawx74859/attachments/gfawx74859/nor-flash/5448/1/S29AL016J_Rev06.pdf)

Seguono due chiamate a 0x8f52dad2 con argomenti compatibili con programmazione da buffer. Intervalli a estremo finale escluso, nell’ordine di chiamata:

| Offset chiamata | Byte del file combinato (AB-13) | Sorgente RAM | Destinazione nello spazio CPU | Lunghezza |
|---|---|---|---|---|
| 0x2e5ab0 | [0x140002, 0x780002) | 0x800c0000 | [0x100000, 0x740000) | 0x640000 |
| 0x2e5ac4 | [0x80002, 0x120002) | 0x80000000 | [0x40000, 0xe0000) | 0xa0000 |

Su questo percorso le due richieste saltano i 128 KiB di 0xff tra 0x120002 e 0x140002. Il campo `ram_update` del report riproduce le basi, la rilocazione e gli argomenti delle cinque chiamate dal codice. Questa traccia riguarda B nel pacchetto combinato. Gli indirizzi descrivono il comportamento previsto dal codice statico.

<a id="ab-16"></a>
### AB-16 — Programmazione NOR e verifica di un settore in memoria

La routine rilocata a 0x8f52dad2 (offset 0x2e5c4c) carica una mezza parola dalla sorgente nel delay slot della chiamata a 0x2e5e84. Il callee salta le parole 0xffff. In uno dei due rami, carica 0xaa, 0x55 e 0xa0 e li scrive rispettivamente agli indirizzi CPU 0x40aaa, 0x40554 e 0x40aaa. La scrittura del dato all’indirizzo di destinazione è nel delay slot della chiamata al polling a 0x2e5f10. Il polling confronta il bit 7 del valore letto con quello atteso. Sul percorso selezionato dal flag del dispositivo controlla anche il bit 5 e ripete la lettura prima di segnalare errore. Dopo il ciclo di programmazione, la funzione a 0x2e5cd2 confronta sorgente e destinazione a 16 bit. Una differenza porta al ritorno zero. [Definizioni dei comandi NOR e polling nel datasheet Infineon](https://community.infineon.com/gfawx74859/attachments/gfawx74859/nor-flash/5448/1/S29AL016J_Rev06.pdf)

L’analizzatore decodifica le scritture del ramo con unlock a 0x40aaa e 0x40554. Il modello sincrono usa un settore di 0x20000 byte, scelto dal passo di iterazione del firmware, con destinazione [0x40000, 0x60000) e sorgente nel file [0x80002, 0xa0002). Parte da un settore pieno di zeri, applica la cancellazione e programma le mezze parole con transizioni da 1 a 0. Il confronto a 16 bit copre tutti i 131072 byte. Su una seconda lettura, l’inversione di un bit nell’ultimo byte viene rilevata nella parola a offset relativo 0x1fffe. Conteggi e SHA-256 prima del guasto sono riproducibili nel campo `nor_sector`. La sequenza bus deriva dai byte originali. L’effetto dei comandi e il confronto sono modellati in Python. Il test non misura i tempi del dispositivo, né esegue il firmware completo.

<a id="ab-17"></a>
### AB-17 — Esecuzione del ciclo FR con polling NOR simulato

Il modulo `tools/fr_poll.py` esegue le istruzioni del campione dalla routine a offset 0x2e5c4c fino al ritorno. Segue le chiamate di programmazione, polling e verifica, inclusi i delay slot. La prova usa una destinazione simulata inizialmente cancellata a 0x40000 e quattro byte della sezione B (9f80 5000). Il bus risponde ai comandi del firmware con stati DQ7 e DQ5 controllati. Il risultato viene letto da r4 dopo il ritorno della routine esterna.

| Risposta del dispositivo simulato | Letture di polling per parola | Istruzioni eseguite | Ritorno |
|---|---|---:|---:|
| Due letture occupate, poi pronto | 3, 3 | 259 | 1 |
| DQ5 attivo, DQ7 ancora errato alla rilettura | 4 sulla prima | 122 | 0 |
| DQ5 attivo, DQ7 corretto alla rilettura | 4, 4 | 283 | 1 |

In entrambi i casi di successo il firmware effettua altre due letture per confrontare le parole scritte con la sorgente. Nell’errore invia 0xf0 a 0x40aaa e ritorna prima della seconda programmazione. Tutti gli indirizzi sono virtuali nel processo Python. Le latenze e la mancata programmazione nel caso di errore sono scelte del dispositivo simulato. Il profilo FR copre le istruzioni e gli stati percorsi dai tre scenari. Il limite di 2000 istruzioni segnala un ciclo imprevisto. [Semantica delle istruzioni FR](https://sourceware.org/cgen/gen-doc/fr30-insn.html)

<a id="ab-18"></a>
### AB-18 — Cancellazione e scrittura sullo stesso settore simulato

Il banco di prova inizializza a zero tutti i 128 KiB nell’intervallo CPU [0x40000, 0x60000). Esegue la routine FR di cancellazione a offset 0x2e5da6 sul bus NOR. Dopo tre letture di polling la routine restituisce 1 e tutte le parole risultano 0xffff. Con la stessa memoria, il banco chiama quindi la routine a 0x2e5c4c: i primi quattro byte diventano 9f 80 50 00, la verifica del firmware riesce e tutte le altre parole restano cancellate. Sono state eseguite 82 istruzioni nella prima chiamata e 259 nella seconda. In una prova distinta il bus mantiene l’errore di cancellazione: il firmware invia il reset del dispositivo e restituisce zero. In questo scenario il modello conserva il contenuto iniziale. Il banco invoca direttamente le due routine e simula completamento e guasto del dispositivo. Sequenziamento da parte del chiamante esterno e comportamento fisico di una cancellazione interrotta restano da verificare.

<a id="ab-19"></a>
### AB-19 — Politica di tentativi del percorso combinato

Il ciclo FR a offset 0x2e5a54 inizializza il contatore a 3. Per ogni tentativo dispone tre cancellazioni e, solo dopo tre esiti positivi, due programmazioni. Se una delle cinque operazioni restituisce zero, il codice decrementa il contatore e, finché è positivo, ricomincia dalla prima cancellazione. La prova si ferma a 0x2e5ad0, dove il firmware esamina il contatore residuo.

Nel caso di errore persistente DQ5 il banco esegue anche il codice FR della cancellazione: la prima richiesta per [0xd0000, 0xe0000) viene allineata a 0xc0000. Il bus restituisce errore in tutti e tre i tentativi, con quattro letture di polling ciascuno. Nessuna chiamata di programmazione viene raggiunta. Per verificare anche guasti successivi si iniettano esiti alle chiamate di intervallo: se fallisce la seconda cancellazione o la seconda programmazione, il tentativo seguente riparte dalla prima cancellazione e può completarsi. Con errore persistente sulla prima programmazione vengono eseguite tre sequenze di tre cancellazioni e una programmazione fallita. Il campo `fr_poll.controller` conserva ordine, argomenti e risultati. La simulazione copre il ciclo interno del firmware B; l’esito successivo per l’utente della fotocamera resta aperto.

<a id="ab-20"></a>
### AB-20 — Selezione dell’esito dopo i tentativi

A 0x2e5ad0 il firmware controlla il contatore r9. Quando vale zero, il salto ritardato a 0x2e5ad2 sceglie il secondo di due gruppi di indirizzi. Entrambi i percorsi scrivono sei parole nella stessa area a base 0x40040000. Gli offset sono 0x18, 0x1c, 0x20, 0x818, 0x81c e 0x820. Il primo valore scritto è 0x895136c0 per il successo, 0x89583ec0 per l’esaurimento dei tentativi. La differenza di 0x70800 ricorre per tutte e sei le parole. Anche il percorso che riesce al secondo tentativo seleziona il gruppo di successo. Il profilo di esecuzione parte dal ciclo dei tentativi con r8 e r11 inizializzati dai caricamenti precedenti nella stessa routine.

I percorsi convergono a 0x2e5b5c. La spaziatura di 0x4b000 e 0x12c00 byte tra i primi tre indirizzi è compatibile con tre piani di un’immagine 640×480 con crominanza sottocampionata. L’identità del dispositivo, il formato dei dati e il contenuto eventualmente mostrato restano ipotesi. A valle ci sono attese condizionate da valori hardware. Se raggiunge 0x2e5c04, il firmware esegue un salto a se stesso. Il banco verifica le scritture fino alla convergenza; non riproduce i segnali hardware che governano quelle attese.

<a id="ab-21"></a>
### AB-21 — Testi inglesi delle due schermate candidate

Il chiamante carica in r4 prima 0x60e7 (file 0x27ae40), poi 0x601e (0x27ae54). Per entrambi chiama la stessa routine a offset 0x29107e. A ciascuna chiamata segue una routine distinta di preparazione, rispettivamente a 0x17867c e 0x1786ba. La prima usa 0x895136c0 come base; la seconda aggiunge 0x70800 alla stessa base. Sono proprio i due gruppi selezionati dal contatore dei tentativi (AB-20). Nella routine comune a 0x177836 si osservano tre operazioni con distanze 0, 0x4b000 e 0x5dc00 e parametri dimensionali 640, 480, 320. Questo sostiene l’interpretazione dei gruppi come piani di immagine.

La tabella candidata di puntatori inglesi inizia a 0x463712 per il gruppo 0x6000. Per l’ID si prende la voce a `0x463712 + 4 × (ID − 0x6000)` e si applica AB-4 al puntatore. A 0x46378a e 0x463aae si trovano gli indirizzi dei record inglesi di errore e successo. Anche le voci vicine sono coerenti: 0x601b chiede conferma, 0x601c indica aggiornamento in corso e 0x601f segnala batteria scarica. I due record di esito iniziano con il numero delle righe. Il report conserva i testi delimitati dal puntatore successivo.

| Percorso AB-20 | ID candidato | Offset del record | Significato del testo inglese |
|---|---|---|---|
| Successo | 0x60e7 | 0x4706b1 | Aggiornamento completato; spegnere la fotocamera. |
| Tentativi esauriti | 0x601e | 0x46fae3 | Aggiornamento fallito; contattare l’assistenza autorizzata Nikon. |

L’associazione è statica. Il contenuto testuale e gli ID al sito di chiamata sono verificabili sui byte. La ricerca non ha ancora eseguito il selettore di lingua e il rendering fino ai pixel effettivi del display.

<a id="ab-22"></a>
### AB-22 — Selezione della lingua e messaggi italiani

La routine a 0x29107e inoltra l’ID in r4 alla funzione a 0x2917ec. Quest’ultima usa lo stesso ID in r5 chiamando 0x1904be, che invoca il risolutore a 0x19094c. Nel risolutore si trovano la maschera del gruppo (0xe000), la selezione di 0x6000 e la lettura della tabella a offset +16 del descrittore con base 0x8f6f8b30. Il blocco a 0x190a50 carica due puntatori consecutivi per delimitare il record.

La routine a 0x1c75c6 usa una tabella di salti e imposta quel descrittore. Per i casi 1 e 4 il codice carica rispettivamente le tabelle ai file offset 0x463712 e 0x4a096a. L’identificazione delle lingue deriva dal contenuto dei record. Il report segue le destinazioni dei salti e ricava entrambe le tabelle dalle istruzioni. Per gli stessi ID di AB-21, il caso 4 restituisce i testi italiani originali, con due righe per il successo e tre per l’errore. La traccia è statica. La generazione dei glifi e dei pixel resta aperta.

<a id="ab-23"></a>
### AB-23 — Dal messaggio al blocco che elabora il testo

Il percorso di presentazione a 0x2917ec chiama 0x1900d6 (caricamento dell’indirizzo a 0x291934), che a sua volta chiama 0x191380. Quest’ultima funzione usa il descrittore di lingua e seleziona il gruppo 0x6000. Il salto di uguaglianza a 0x191406 porta a 0x191506. Qui la routine legge dal descrittore il puntatore a offset +16 e indicizza la tabella con passi di quattro byte. Passa alla funzione a 0x1915c2 l’indirizzo del testo dopo il byte iniziale e il limite ricavato dalla voce successiva. Questo collega i record di AB-22 a un secondo percorso di elaborazione del testo, distinto da quello di misura. Il disassemblato osservato non basta ancora a localizzare il bitmap di ciascun carattere o a ricostruire i pixel.
