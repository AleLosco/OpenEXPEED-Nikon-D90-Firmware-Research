# Nikon D90 · ricerca sul firmware

> **Stato: alpha di ricerca.** Analisi riproducibile dei firmware A/B e L, con prove isolate su codice FR originale. Destinatario di alcune comunicazioni, periferiche, comportamento fotografico ed emulazione completa sono ancora aperti. Contributi e riproduzioni indipendenti sono benvenuti.


Laboratorio indipendente per ricostruire il formato degli aggiornamenti Nikon D90 e preparare esperimenti fuori dalla fotocamera. Il primo risultato è un analizzatore statico riproducibile dei due campioni di riferimento.

Per il quadro aggiornato e le lacune leggere [Stato del progetto](STATUS.md). Per riprodurre le prove Ghidra e proporre risultati leggere [Contribuire](CONTRIBUTING.md). Il registro seguente conserva anche le ipotesi storiche, poi precisate nelle voci successive.

## Riprodurre la verifica

Serve Python 3.8 o successivo. Dalla radice del repository eseguire:

```sh
python3 tools/analyze.py "/percorso/D90_0101.bin" "/percorso/NKLD0218.BIN" > /tmp/d90-report.json
diff -u reports/reference.json /tmp/d90-report.json
```

Un confronto senza differenze riproduce il report di riferimento. Il programma apre i due file in lettura, verifica ciascun SHA-256 prima di interpretarne il formato e decodifica in memoria. I digest identificano i campioni analizzati. Le schede Nikon citate sotto non pubblicano un digest del file interno con cui autenticarli.

## Registro dei risultati

Gli intervalli sono offset nel file originale, in byte, con estremo finale escluso.

| Campione | Dimensione | SHA-256 |
|---|---:|---|
| D90_0101.bin | 7 864 322 | `623906f285c656a7c6e4f3cecd26bc5e2b6da38ba07f0cc76a235f7e524814af` |
| NKLD0218.BIN | 39 588 | `bc26b7413adabeabbf21e341197bc14c58666b0be5f5a8c2eac29564c73f5f1d` |

**AB-1 · Identità della release.** Nikon identifica il primo nome come file di aggiornamento D90 A 1.00 / B 1.01. La modifica dichiarata è il supporto ai dati di controllo distorsione L 2.xxx. Nel campione l'intestazione a `0x8` nomina D90 Firmware A. A `0x20ac4a` compare una stringa di build che inizia con `Ver.1.01a D90` e include la data 2014/07/18. La stringa da sola non identifica la partizione B. [Scheda Nikon D90](https://downloadcenter.nikonimglib.com/it/download/fw/9.html)

**AB-2 · Integrità e disposizione.** Il CRC-16/XMODEM (polinomio 0x1021, valore iniziale 0) dei byte in [0, 0x780000) vale 0xc2ab e coincide con il valore big-endian in coda. Tre lunghe sequenze di 0xff sono riportate in [reference.json](reports/reference.json). Per il confine del pacchetto e la mappa di B si vedano AB-11 e AB-12.

**L-1 · Versione e natura.** Nikon descrive L 2.018 come dati di controllo distorsione per più modelli, inclusa D90 con A 1.00 e B 1.01. Nel campione, i byte 0x02 e 0x12 agli offset 4 e 5 corrispondono a 2 e 18 decimali. Il conteggio big-endian a offset 6 vale 307. [Scheda Nikon L 2.018](https://downloadcenter.nikonimglib.com/it/download/fw/348.html)

**L-2 · Record e controlli.** Applicando la ricorrenza documentata dal progetto Nikon Hacker, la regione [0x20, 0x99a0) si decodifica in 307 record contigui. Il primo campo di ciascun record indica 0x80 byte e l'ultimo termina esattamente al confine previsto. Il CRC sui byte originali in [0, 0x99a0) vale 0x5920. Seguono il CRC, due byte nulli e un suffisso di 256 byte a 0x99a4. Nel suffisso un secondo CRC su [0x99a4, 0x9a88) vale 0xead2 e coincide con la parola successiva. Restano da identificare i campi ottici e il significato del suffisso. Il conteggio riguarda i record e non dimostra il numero di obiettivi distinti. Nessuna osservazione dimostra esecuzione di codice in L. [Lettore NKLD di Nikon Hacker](https://github.com/simeonpilgrim/nikon-firmware-tools/blob/main/firmware%20decode/firmware%20decode/Program.cs) · [Algoritmo di decodifica](https://github.com/simeonpilgrim/nikon-firmware-tools/blob/main/firmware%20decode/firmware%20decode/Decrypt.cs)

## Metodo e limiti

È stata usata la skill REA e invocata la CLI REA 6.7.0. Su questo host macOS, per entrambi i campioni, `inspect-firmware-regions` ha restituito `capability_unavailable` (richiede Linux e Binwalk) e `inspect-artifact` ha restituito `unsupported_target`. I risultati AB-1–L-2 derivano dalla documentazione pubblica e dai controlli Python sui file originali. Non sono record Evidence generati da REA. [Progetto REA](https://github.com/morluto/rea)

## Mappa preliminare A/B

**AB-3 · Collegamento fra le regioni.** La parola big-endian a offset `0x11fbf2` vale `0x001cac48`. Aggiungendo `0x40002` si raggiunge esattamente il primo byte della stringa di build D90 a `0x20ac4a`. Questo collega dati situati prima del lungo intervallo di riempimento con un bersaglio situato dopo.

**AB-4 · Verifica della traslazione.** Nell’intervallo [0x140002, 0x604cc5) la sonda cerca su offset pari la codifica FR big-endian di `ldi:32` (0x9f80–0x9f8f seguito da un immediato di 32 bit). Con la relazione `indirizzo = offset − 0x40002`, 268 valori immediati distinti puntano all’inizio di stringhe ASCII stampabili di almeno sei caratteri, delimitate da zero su entrambi i lati. Spostando la relazione di due byte in ciascuna direzione i riscontri scendono a zero. Con indirizzo uguale all’offset sono 11. La scansione può includere dati che somigliano a istruzioni. Il risultato sostiene la mappatura per questi riferimenti, senza estenderla automaticamente a ogni byte. [Codifica FR nel sorgente GNU binutils](https://gnu.googlesource.com/binutils-gdb/+/37f7f684de0e53c8add5ff43d73082bf98985321/opcodes/fr30-opc.c)

Per una verifica manuale, a offset `0x1ab96e` il candidato caricamento contiene `0x001d307c`, che con questa mappa punta alla stringa `D90_????.BIN` a offset `0x21307e`. È un punto da cui cercare il chiamante che gestisce i nomi di aggiornamento. La stringa da sola non stabilisce la funzione del chiamante.

**AB-5 · Possibili vettori.** Interpretando [0x11fc02, 0x120002) come 256 parole big-endian, 206 hanno valore `0x0004050a` e l’ultima vale `0x00040000`. Subito dopo iniziano 128 KiB di 0xff. La disposizione è compatibile con una tabella di vettori. Restano da verificare il reset e la base della regione iniziale. La relazione AB-4 proietta l’offset `0x140002` sull’indirizzo `0x00100000`, una base candidata per l’importazione separata della seconda regione. L’attribuzione a B è verificata in AB-11. Il CRC globale è verificato, mentre un test CRC-16/XMODEM sul prefisso [0, 0x120000) dà 0x1440 contro 0x0000 nei due byte successivi.

## Prossimi esperimenti

1. **Mappa A/B.** Cercare intestazioni interne, vettori e puntatori ripetuti. Registrare per ogni segmento l'intervallo nel file e, separatamente, l'indirizzo ipotizzato in memoria.
2. **CPU e Ghidra.** Provare un profilo ISA per segmento su una copia locale. Accettare una mappatura solo quando convergono più evidenze, per esempio vettori allineati, destinazioni di salto valide e riferimenti a stringhe note.
3. **Emulazione minima.** Selezionare una routine con input e output osservabili. Modellare soltanto la RAM e gli accessi periferici che usa, segnalando gli accessi sconosciuti. Confrontare i risultati con vettori di prova prima di ampliare il modello.
4. **Prima patch sperimentale.** Partire da un comportamento riproducibile e da una funzione identificata. Registrare hash base, offset, precondizioni e test prima/dopo su copia locale. La fattibilità di modifiche funzionali e del boot emulato è ancora da verificare.

Per contribuire, includere nel report digest, comando, offset, risultato osservato e ipotesi ancora aperte. Conservare firmware, dump e database Ghidra fuori dal repository o sotto `local/`.

La licenza MIT si applica ai contributi originali del progetto. I firmware Nikon sono forniti separatamente dai rispettivi titolari.

## Selezione dei file di aggiornamento

**AB-6 · Cinque percorsi candidati.** In [0x1ab924, 0x1ab9d4) si ripete la stessa sequenza FR. Un caricamento in r4 punta a un modello di nome. Un secondo caricamento in r12 precede una chiamata indiretta con delay slot. Il codice esamina r4 dopo il ritorno e un salto condizionale separa la prosecuzione della ricerca dalla chiamata specifica per quel modello. Gli offset nella tabella sono nel file originale, incluse le destinazioni convertite con AB-4.

| Offset del caricamento | Modello | Routine comune | Routine specifica |
|---|---|---|---|
| 0x1ab924 | AD90????.BIN | 0x1ad10e | 0x1aca10 |
| 0x1ab94c | BD90????.BIN | 0x1ad10e | 0x1ac9a0 |
| 0x1ab96e | D90_????.BIN | 0x1ad10e | 0x1aca7e |
| 0x1ab990 | S???????.BIN | 0x1ad10e | 0x1acc06 |
| 0x1ab9b2 | NKLD????.BIN | 0x1ad10e | 0x1aced8 |

**AB-7 · Controllo di dimensione candidato.** Nel percorso D90 combinato, a 0x1aca8e il codice carica in r1 il valore 0x00780002, identico alla lunghezza del campione, poi lo confronta con r0 e prende un ramo su uguaglianza. Il percorso NKLD carica invece 0x00700000 a 0x1acee8 e usa una condizione diversa. L’origine del valore in r0 e il significato del limite NKLD sono da verificare. La presenza di percorsi distinti nel codice della D90 offre un punto di partenza per seguire la validazione dei due campioni. Il modello del nome non determina da solo quali regioni vengano scritte in flash.

Per riprodurre AB-6 usare il comando iniziale e consultare il campo `address_map.update_patterns`. Per AB-7 ispezionare i 12 byte a ciascuno degli offset indicati, interpretando le parole come big-endian.

**AB-8 · Provenienza di r0 e biforcazione per il file D90.** Tutti gli offset citati sono nel file. Il chiamante carica 0x8f545e4c in r8 a 0x1ab92a. Il delay slot della chiamata specifica copia r8 in r4 a 0x1ab98a. Nel callee la coppia di parole `c14d 0040` a 0x1aca8a imposta r13 a 0x14 e carica in r0 una parola da [r4 + r13]. Il confronto legge quindi il campo a **0x8f545e60**. La routine comune passa lo stesso puntatore alla funzione a offset 0x1f4016. Questa conserva il secondo argomento in r10 a 0x1f401c e a 0x1f416a scrive r0 nel campo [r10 + r13], con r13 impostato a 0x14. È un produttore staticamente identificabile del campo. Per attribuirgli definitivamente la semantica di lunghezza del file resta da ricostruire il formato del record sorgente.

| Valore del campo | Percorso a partire dal confronto a 0x1aca94 |
|---|---|
| Diverso da 0x780002 | Il salto ritardato a 0x1aca98 esegue il delay slot che azzera r4, poi raggiunge direttamente l’epilogo a 0x1acaf2. |
| Uguale a 0x780002 | Il salto a 0x1aca96 raggiunge 0x1aca9c. Il codice usa la stringa di radice del volume a offset 0x21303a e arriva a una chiamata indiretta a 0x1acac6. Ulteriori controlli possono ancora interrompere il percorso. |

Il campo `main_size_check` del report conserva indirizzo del campo, costante e bersagli dei salti. L’analisi è statica secondo la semantica FR e la mappatura AB-4. [Semantica delle istruzioni FR](https://sourceware.org/cgen/gen-doc/fr30-insn.html)

**AB-9 · Dal record di directory alla dimensione.** Nella routine a 0x1f4016 la base locale del record candidato è r14 − 36. A 0x1f40dc viene caricato 0xdc in r4, esteso con segno e sommato al frame pointer. Il primo byte viene esaminato a 0x1f40e8. Nel percorso che prepara l’output, i byte seguenti corrispondono al layout di una voce breve FAT.

| Byte nel record (base 0) | Campo FAT | Offset nel descrittore |
|---|---|---|
| 11 | Attributi | +0x0d |
| 22–23 | Ora di ultima scrittura | +0x10 |
| 24–25 | Data di ultima scrittura | +0x0e |
| 28–31 | Dimensione in byte | +0x14 |

Il modulo `tools/directory_entry.py` interpreta le istruzioni originali in [0x1f4122, 0x1f416c) su un record sintetico e un descrittore in memoria. Il blocco lineare elabora data, ora e dimensione. L’attributo è copiato subito prima del blocco ed è identificato staticamente. Per ogni bit dei 32 byte il report verifica i tre valori prodotti rispetto alla decodifica little-endian indipendente. Un secondo input inserisce la lunghezza reale del campione e produce 0x780002 a +0x14. La prova copre la trasformazione in memoria, senza accessi alla scheda o esecuzione del boot. [Specifica Microsoft FAT, tabella delle directory](https://www.cs.fsu.edu/~cop4610t/assignments/project3/spec/fatspec.pdf) · [Semantica FR30](https://sourceware.org/cgen/gen-doc/fr30-insn.html)

**AB-10 · Letture nel percorso combinato.** Dopo la costruzione del percorso, due chiamate relative ritardate a 0x1acad4 e 0x1acae0 hanno come destinazioni nel file 0x1acd72 e 0x1acafa. La seconda è raggiunta solo se la prima restituisce un valore diverso da zero. Le due routine riutilizzano una coppia di chiamate con argomenti coerenti con posizionamento e lettura. Il primo stadio richiede 6 byte a posizione zero. Il secondo richiede 4 byte a 0x11fbf6 (nel campione: 00000421) e li confronta con una sequenza in memoria. In seguito richiede 4 byte a 0x11fbf2, li usa come puntatore e somma 0x40002 prima di richiedere altri 9 byte. Sul campione questa catena porta esattamente a `Ver.1.01a`. Le chiamate verificano anche il numero di byte trasferiti. L’operazione fornisce un riscontro nel codice per la traslazione AB-4. Restano aperte la funzione del valore 0x421, la semantica completa delle API di I/O e le destinazioni di scrittura dei componenti A/B.

**AB-11 · Confine A/B nel pacchetto combinato.** I gestori dei nomi AD90 e BD90 confrontano la dimensione del file rispettivamente con 0x80002 (offset 0x1aca20) e 0x700000 (0x1ac9b0). Nel percorso combinato di stato 3 la stessa coppia ricompare come offset di sorgente e lunghezza di una copia. Le due lunghezze coprono esattamente il campione. Anche i checksum calcolati separatamente coincidono con i trailer.

| Blocco logico | Intervallo nel file | CRC-16/XMODEM escluso il trailer | Trailer big-endian |
|---|---|---|---|
| A | [0, 0x80002) | 0x63cc | 0x63cc |
| B | [0x80002, 0x780002) | 0xc2ab | 0xc2ab |

Il CRC di A, calcolato includendo il proprio trailer, ha residuo zero. Per questo il CRC globale precedentemente osservato coincide con quello di B. Il lungo padding a 0x120002 si trova **dentro B**.

**AB-12 · Vettori di B.** La prima istruzione di B è all’offset 0x80002, corrispondente a 0x40000 nella mappa AB-4. A 0x80014 il codice carica 0x000dfc00 in r0 e trasferisce il valore al registro dedicato 0 (`b300`). La tabella a quell’indirizzo comincia proprio a 0x11fc02 nel file. La sua ultima voce rimanda a 0x40000, quindi all’inizio di B. Questo chiude il collegamento statico tra inizializzazione, tabella e candidato ingresso di reset. [Istruzioni FR](https://sourceware.org/cgen/gen-doc/fr30-insn.html)

**AB-13 · Consumatore dello stato.** Il percorso di validazione azzera 0x8f545e64. Il primo stadio combinato può scrivervi 2. Il secondo verifica quel valore prima di sostituirlo con 3. A 0x1abf84 il codice rilegge lo stato e indicizza una tabella di indirizzi a 0x001d30c0 usando lo stato meno uno. Le cinque destinazioni nel file sono nel campo `update_layout` del report. Lo stato 3 conduce a 0x1abfae. Qui il codice usa la lunghezza combinata con la routine a offset 0x1ac1ce che passa 0x80000000 come buffer a una lettura. Nel ramo successivo alla validazione, la chiamata a offset 0x1ac058 usa come argomenti destinazione 0x80000000, sorgente 0x80080002 e lunghezza 0x700000. Il corpo chiamato a 0x1fc8e4 implementa una copia a byte con scelta della direzione. La regione B viene così portata all’inizio dello stesso buffer. Tra le chiamate seguenti, la routine a 0x17a1d0 invoca l’indirizzo 0x8f52d79e. Il suo contenuto runtime e le destinazioni fisiche della flash restano da verificare.

**AB-14 · Copia del codice in RAM.** Nello startup a offset 0x80428 il contatore vale 0x9f4. Il ciclo decrementa di due e copia una mezza parola per iterazione dalla base ROM 0x2a5838 alla RAM 0x8f52d6c0. Nel file i byte occupano [0x2e583a, 0x2e622e). Il target di AB-13, 0x8f52d79e, è 0xde byte dopo la base e corrisponde a offset 0x2e5918. Due ulteriori target di chiamate relative, 0x8f52da8c e 0x8f52dad2, ricadono nello stesso blocco e puntano a prologhi di funzione agli offset 0x2e5c06 e 0x2e5c4c.

**AB-15 · Destinazioni del percorso combinato.** Sul percorso che supera i controlli, la routine rilocata passa tre coppie (indirizzo, lunghezza) a 0x8f52da8c: (0xd0000, 0x10000), (0x40000, 0x90000), (0x100000, 0x640000). Il callee allinea l’indirizzo iniziale con la maschera 0xfffe0000. Un helper contiene, da offset 0x2e5dd0, scritture a 16 bit con valori AA, 55, 80, AA, 55 e infine 30 all’indirizzo di settore. È la sequenza di cancellazione di settore documentata per NOR parallela. Le coppie descrivono le richieste software. La geometria fisica dei settori e il modello del chip restano da identificare. [Datasheet Infineon, tabella 10.1](https://community.infineon.com/gfawx74859/attachments/gfawx74859/nor-flash/5448/1/S29AL016J_Rev06.pdf)

Seguono due chiamate a 0x8f52dad2 con argomenti compatibili con programmazione da buffer. Intervalli a estremo finale escluso, nell’ordine di chiamata:

| Offset chiamata | Byte del file combinato (AB-13) | Sorgente RAM | Destinazione nello spazio CPU | Lunghezza |
|---|---|---|---|---|
| 0x2e5ab0 | [0x140002, 0x780002) | 0x800c0000 | [0x100000, 0x740000) | 0x640000 |
| 0x2e5ac4 | [0x80002, 0x120002) | 0x80000000 | [0x40000, 0xe0000) | 0xa0000 |

Su questo percorso le due richieste saltano i 128 KiB di 0xff tra 0x120002 e 0x140002. Il campo `ram_update` del report riproduce le basi, la rilocazione e gli argomenti delle cinque chiamate dal codice. Questa traccia riguarda B nel pacchetto combinato. Gli indirizzi descrivono il comportamento previsto dal codice statico.

**AB-16 · Programmazione NOR e verifica di un settore in memoria.** La routine rilocata a 0x8f52dad2 (offset 0x2e5c4c) carica una mezza parola dalla sorgente nel delay slot della chiamata a 0x2e5e84. Il callee salta le parole 0xffff. In uno dei due rami, carica 0xaa, 0x55 e 0xa0 e li scrive rispettivamente agli indirizzi CPU 0x40aaa, 0x40554 e 0x40aaa. La scrittura del dato all’indirizzo di destinazione è nel delay slot della chiamata al polling a 0x2e5f10. Il polling confronta il bit 7 del valore letto con quello atteso. Sul percorso selezionato dal flag del dispositivo controlla anche il bit 5 e ripete la lettura prima di segnalare errore. Dopo il ciclo di programmazione, la funzione a 0x2e5cd2 confronta sorgente e destinazione a 16 bit. Una differenza porta al ritorno zero. [Definizioni dei comandi NOR e polling nel datasheet Infineon](https://community.infineon.com/gfawx74859/attachments/gfawx74859/nor-flash/5448/1/S29AL016J_Rev06.pdf)

L’analizzatore decodifica le scritture del ramo con unlock a 0x40aaa e 0x40554. Il modello sincrono usa un settore di 0x20000 byte, scelto dal passo di iterazione del firmware, con destinazione [0x40000, 0x60000) e sorgente nel file [0x80002, 0xa0002). Parte da un settore pieno di zeri, applica la cancellazione e programma le mezze parole con transizioni da 1 a 0. Il confronto a 16 bit copre tutti i 131072 byte. Su una seconda lettura, l’inversione di un bit nell’ultimo byte viene rilevata nella parola a offset relativo 0x1fffe. Conteggi e SHA-256 prima del guasto sono riproducibili nel campo `nor_sector`. La sequenza bus deriva dai byte originali. L’effetto dei comandi e il confronto sono modellati in Python. Il test non misura i tempi del dispositivo, né esegue il firmware completo.

**AB-17 · Esecuzione del ciclo FR con polling NOR simulato.** Il modulo `tools/fr_poll.py` esegue le istruzioni del campione dalla routine a offset 0x2e5c4c fino al ritorno. Segue le chiamate di programmazione, polling e verifica, inclusi i delay slot. La prova usa una destinazione simulata inizialmente cancellata a 0x40000 e quattro byte della sezione B (9f80 5000). Il bus risponde ai comandi del firmware con stati DQ7 e DQ5 controllati. Il risultato viene letto da r4 dopo il ritorno della routine esterna.

| Risposta del dispositivo simulato | Letture di polling per parola | Istruzioni eseguite | Ritorno |
|---|---|---:|---:|
| Due letture occupate, poi pronto | 3, 3 | 259 | 1 |
| DQ5 attivo, DQ7 ancora errato alla rilettura | 4 sulla prima | 122 | 0 |
| DQ5 attivo, DQ7 corretto alla rilettura | 4, 4 | 283 | 1 |

In entrambi i casi di successo il firmware effettua altre due letture per confrontare le parole scritte con la sorgente. Nell’errore invia 0xf0 a 0x40aaa e ritorna prima della seconda programmazione. Tutti gli indirizzi sono virtuali nel processo Python. Le latenze e la mancata programmazione nel caso di errore sono scelte del dispositivo simulato. Il profilo FR copre le istruzioni e gli stati percorsi dai tre scenari. Il limite di 2000 istruzioni segnala un ciclo imprevisto. [Semantica delle istruzioni FR](https://sourceware.org/cgen/gen-doc/fr30-insn.html)

**AB-18 · Cancellazione e scrittura sullo stesso settore simulato.** Il banco di prova inizializza a zero tutti i 128 KiB nell’intervallo CPU [0x40000, 0x60000). Esegue la routine FR di cancellazione a offset 0x2e5da6 sul bus NOR. Dopo tre letture di polling la routine restituisce 1 e tutte le parole risultano 0xffff. Con la stessa memoria, il banco chiama quindi la routine a 0x2e5c4c: i primi quattro byte diventano 9f 80 50 00, la verifica del firmware riesce e tutte le altre parole restano cancellate. Sono state eseguite 82 istruzioni nella prima chiamata e 259 nella seconda. In una prova distinta il bus mantiene l’errore di cancellazione: il firmware invia il reset del dispositivo e restituisce zero. In questo scenario il modello conserva il contenuto iniziale. Il banco invoca direttamente le due routine e simula completamento e guasto del dispositivo. Sequenziamento da parte del chiamante esterno e comportamento fisico di una cancellazione interrotta restano da verificare.

**AB-19 · Politica di tentativi del percorso combinato.** Il ciclo FR a offset 0x2e5a54 inizializza il contatore a 3. Per ogni tentativo dispone tre cancellazioni e, solo dopo tre esiti positivi, due programmazioni. Se una delle cinque operazioni restituisce zero, il codice decrementa il contatore e, finché è positivo, ricomincia dalla prima cancellazione. La prova si ferma a 0x2e5ad0, dove il firmware esamina il contatore residuo.

Nel caso di errore persistente DQ5 il banco esegue anche il codice FR della cancellazione: la prima richiesta per [0xd0000, 0xe0000) viene allineata a 0xc0000. Il bus restituisce errore in tutti e tre i tentativi, con quattro letture di polling ciascuno. Nessuna chiamata di programmazione viene raggiunta. Per verificare anche guasti successivi si iniettano esiti alle chiamate di intervallo: se fallisce la seconda cancellazione o la seconda programmazione, il tentativo seguente riparte dalla prima cancellazione e può completarsi. Con errore persistente sulla prima programmazione vengono eseguite tre sequenze di tre cancellazioni e una programmazione fallita. Il campo `fr_poll.controller` conserva ordine, argomenti e risultati. La simulazione copre il ciclo interno del firmware B; l’esito successivo per l’utente della fotocamera resta aperto.

**AB-20 · Selezione dell’esito dopo i tentativi.** A 0x2e5ad0 il firmware controlla il contatore r9. Quando vale zero, il salto ritardato a 0x2e5ad2 sceglie il secondo di due gruppi di indirizzi. Entrambi i percorsi scrivono sei parole nella stessa area a base 0x40040000. Gli offset sono 0x18, 0x1c, 0x20, 0x818, 0x81c e 0x820. Il primo valore scritto è 0x895136c0 per il successo, 0x89583ec0 per l’esaurimento dei tentativi. La differenza di 0x70800 ricorre per tutte e sei le parole. Anche il percorso che riesce al secondo tentativo seleziona il gruppo di successo. Il profilo di esecuzione parte dal ciclo dei tentativi con r8 e r11 inizializzati dai caricamenti precedenti nella stessa routine.

I percorsi convergono a 0x2e5b5c. La spaziatura di 0x4b000 e 0x12c00 byte tra i primi tre indirizzi è compatibile con tre piani di un’immagine 640×480 con crominanza sottocampionata. L’identità del dispositivo, il formato dei dati e il contenuto eventualmente mostrato restano ipotesi. A valle ci sono attese condizionate da valori hardware. Se raggiunge 0x2e5c04, il firmware esegue un salto a se stesso. Il banco verifica le scritture fino alla convergenza; non riproduce i segnali hardware che governano quelle attese.

**AB-21 · Testi inglesi delle due schermate candidate.** Il chiamante carica in r4 prima 0x60e7 (file 0x27ae40), poi 0x601e (0x27ae54). Per entrambi chiama la stessa routine a offset 0x29107e. A ciascuna chiamata segue una routine distinta di preparazione, rispettivamente a 0x17867c e 0x1786ba. La prima usa 0x895136c0 come base; la seconda aggiunge 0x70800 alla stessa base. Sono proprio i due gruppi selezionati dal contatore dei tentativi (AB-20). Nella routine comune a 0x177836 si osservano tre operazioni con distanze 0, 0x4b000 e 0x5dc00 e parametri dimensionali 640, 480, 320. Questo sostiene l’interpretazione dei gruppi come piani di immagine.

La tabella candidata di puntatori inglesi inizia a 0x463712 per il gruppo 0x6000. Per l’ID si prende la voce a `0x463712 + 4 × (ID − 0x6000)` e si applica AB-4 al puntatore. A 0x46378a e 0x463aae si trovano gli indirizzi dei record inglesi di errore e successo. Anche le voci vicine sono coerenti: 0x601b chiede conferma, 0x601c indica aggiornamento in corso e 0x601f segnala batteria scarica. I due record di esito iniziano con il numero delle righe. Il report conserva i testi delimitati dal puntatore successivo.

| Percorso AB-20 | ID candidato | Offset del record | Significato del testo inglese |
|---|---|---|---|
| Successo | 0x60e7 | 0x4706b1 | Aggiornamento completato; spegnere la fotocamera. |
| Tentativi esauriti | 0x601e | 0x46fae3 | Aggiornamento fallito; contattare l’assistenza autorizzata Nikon. |

L’associazione è statica. Il contenuto testuale e gli ID al sito di chiamata sono verificabili sui byte. La ricerca non ha ancora eseguito il selettore di lingua e il rendering fino ai pixel effettivi del display.

**AB-22 · Selezione della lingua e messaggi italiani.** La routine a 0x29107e inoltra l’ID in r4 alla funzione a 0x2917ec. Quest’ultima usa lo stesso ID in r5 chiamando 0x1904be, che invoca il risolutore a 0x19094c. Nel risolutore si trovano la maschera del gruppo (0xe000), la selezione di 0x6000 e la lettura della tabella a offset +16 del descrittore con base 0x8f6f8b30. Il blocco a 0x190a50 carica due puntatori consecutivi per delimitare il record.

La routine a 0x1c75c6 usa una tabella di salti e imposta quel descrittore. Per i casi 1 e 4 il codice carica rispettivamente le tabelle ai file offset 0x463712 e 0x4a096a. L’identificazione delle lingue deriva dal contenuto dei record. Il report segue le destinazioni dei salti e ricava entrambe le tabelle dalle istruzioni. Per gli stessi ID di AB-21, il caso 4 restituisce i testi italiani originali, con due righe per il successo e tre per l’errore. La traccia è statica. La generazione dei glifi e dei pixel resta aperta.

**AB-23 · Dal messaggio al blocco che elabora il testo.** Il percorso di presentazione a 0x2917ec chiama 0x1900d6 (caricamento dell’indirizzo a 0x291934), che a sua volta chiama 0x191380. Quest’ultima funzione usa il descrittore di lingua e seleziona il gruppo 0x6000. Il salto di uguaglianza a 0x191406 porta a 0x191506. Qui la routine legge dal descrittore il puntatore a offset +16 e indicizza la tabella con passi di quattro byte. Passa alla funzione a 0x1915c2 l’indirizzo del testo dopo il byte iniziale e il limite ricavato dalla voce successiva. Questo collega i record di AB-22 a un secondo percorso di elaborazione del testo, distinto da quello di misura. Il disassemblato osservato non basta ancora a localizzare il bitmap di ciascun carattere o a ricostruire i pixel.

## Importare la mappa B in Ghidra

Dalla radice del repository, con Python 3:

```sh
mkdir -p local
python3 ghidra/prepare.py /percorso/D90_0101.bin > local/D90-B.hex
```

L’HEX contiene byte del firmware ed è destinato alla sola analisi locale. Nel nuovo progetto Ghidra importarlo con il loader Intel Hex, scegliendo un linguaggio FR big-endian compatibile. La mappa comprende le due regioni programmate e una copia dei byte rilocati all’indirizzo RAM. In Script Manager aggiungere la cartella `ghidra` del repository ed eseguire `D90Labels.java`. Verificare il vettore finale a 0xdfffc e il suo riferimento a 0x40000. Alla prima analisi controllare anche il disassemblato della funzione RAM a 0x8f52d79e, confrontandolo con AB-14–AB-19 prima di estendere il profilo a tutta l’immagine.

Il round trip dell’HEX ha verificato somme di controllo, indirizzi e contenuto delle tre regioni. Il progetto nativo locale è in `local/ghidra/NikonD90.gpr`. Ghidra 12.1.4 con Temurin 21 e il linguaggio `fr60:BE:16:default:fcc911` ha importato B e salvato le sette etichette. Una seconda sessione ha riaperto il progetto, letto il vettore di reset, disassemblato e creato sei funzioni con `D90Functions.java`. L’ingresso a 0x40000 si decodifica come caricamento immediato di 0x50000000 in R0. A 0x8f52d79e si osserva il salvataggio di R8–R11. Il progetto contiene byte del firmware e resta escluso da Git. Per riprodurre la verifica, dopo le etichette eseguire anche `D90Functions.java`. È stato usato il solo modulo linguaggio del [progetto ghidra-fr60](https://github.com/desrdev/ghidra-fr60); le etichette automatiche di periferiche fornite da quel profilo richiedono verifica sulla D90.

Su questa macchina la copia di lavoro con il linguaggio FR installato è in `work/ghidra` e il JDK in `work/jdk/Contents/Home`, rispetto alla radice della chat. Per aprire il progetto, avviare la copia di Ghidra con quel JDK, poi scegliere File → Open Project e il file .gpr locale.

**AB-24 · Dal reset alla seconda fase di avvio di B.** Nel progetto Ghidra il reset a indirizzo CPU 0x40000 imposta SP a 0x8400 e la base vettori a 0xdfc00. Seguono scritture a indirizzi fissi, fra cui 0x600, 0x640, 0x680 e la famiglia 0x40070000. La loro funzione hardware precisa resta aperta. Dai cicli a partire da 0x40366 emergono due azzeramenti di lunghezza non nulla: 0x1bb414 byte a 0x8f52e0b4 e 0x3214 byte a 0x8dec. I cicli di copia successivi hanno tre trasferimenti attivi:

| Sorgente CPU | Destinazione RAM | Byte |
|---|---|---:|
| 0x287a6c | 0x8f6e94c8 | 0x1ddd0 |
| 0x2a622c | 0x8f96eab0 | 0x193a8 |
| 0x2a5838 | 0x8f52d6c0 | 0x9f4 |

Due ulteriori descrizioni hanno lunghezza zero. L’ultima copia attiva corrisponde al codice di aggiornamento già analizzato. A 0x40468 un salto indiretto porta a 0x40704, etichettata B_InitStage2 nel progetto locale. Questa routine scrive zero a 0x6800cf20 e contiene dodici chiamate relative consecutive. La prima, a 0x407f0, contiene cicli di azzeramento tra cui [0x68000000, 0x68000ec0) e [0x8000, 0x8400). L’indirizzo 0x6800cf20 riceve poi i valori 1 e 2 lungo il percorso di ritorno previsto. Dopo il salto a offset file 0x80752 si trova una stringa che nomina Softune REALOS/FR e micro-ITRON. È un indizio sul sistema operativo; l’identità di uno scheduler in esecuzione richiede ulteriori evidenze. La traccia deriva dal disassemblato statico. Per riprodurre le annotazioni eseguire D90Startup.java dopo D90Functions.java.

**AB-25 · Le dodici inizializzazioni e il passaggio di contesto.** Ghidra ha riconosciuto le dodici chiamate relative con destinazioni distinte. Il report `startup.init_tables` conserva gli header big-endian e la prima parola di ogni record contato. Gli indirizzi seguenti sono nello spazio CPU.

| Chiamate | Evidenza statica |
|---|---|
| 1, 0x407f0 | Azzera tre intervalli non vuoti, inclusi [0x8000, 0x8400) e [0x68000000, 0x68000f20). |
| 2, 0x40878 | Scrive 0x10 nel byte a 0x46f e lo rilegge. L’identità della periferica resta aperta. |
| 3, 0x40886 | Legge 0x20 da 0x5c400 e inizializza coppie di puntatori a se stesse a partire da 0x68000008, con passo di otto byte. |
| 4, 0x408a0 | Prepara altre tre coppie a partire da 0x68000118 e scrive 0xff nel byte a 0x68000134. |
| 5, 0x408be | Tabella a 0x5c40c: header 38, passo 28 byte. Il ciclo comprende anche la voce successiva ai 38 record, a 0x5c838. È quella ripresa nel passaggio finale. |
| 6–7, 0x40906 e 0x4092c | Header rispettivamente di 49 e 22 record, entrambi con passo 16. I primi puntatori destinazione sono 0x680007f0 e 0x68000b00. |
| 8–12, da 0x40952 a 0x40a30 | Cinque tabelle con conteggio zero; i cicli sui record vengono saltati in questo campione. |

Dopo le dodici chiamate, il percorso richiama anche 0x4046a. La diramazione a 0x4074e salta la stringa REALOS e porta a 0x407bc. Il codice carica il primo puntatore della voce a 0x5c838 (0x680007c4), lo salva a 0x68000000 e prosegue con ripristino di registri e `RETI` a 0x407ea. La sequenza è compatibile con l’attivazione di un contesto iniziale. Per identificare il primo servizio serve ricostruire lo stack ripristinato e determinare il PC effettivo. I conteggi non dimostrano il tipo degli oggetti né il numero di servizi attivi.

**AB-26 · Primo PC preparato per RETI.** La voce ROM a 0x5c838 contiene controllo 0x680007c4, ingresso 0x407ee e base del frame 0x68000ed0. La routine a 0x41578 carica R6 dall’offset +0x10 e R7 da +0x14 del descrittore. A 0x4153c il delay slot salva R7 a +0x18 del controllo. Il codice usa poi R7 come base del frame: vi scrive R6 a +0, PS=0x001f0030 a +4 e RP=0x4050a a +8.

A 0x407cc il ripristino rilegge la base del frame e la imposta come stack utente. Preleva PC e PS, li trasferisce sullo stack di sistema, ripristina gli altri registri e a 0x407ea esegue RETI. Sul percorso ricostruito, senza interventi asincroni, PC diventa **0x407ee**. A quell’indirizzo la parola 0xe0ff è un salto relativo a se stesso. Il comportamento è coerente con un contesto di attesa. Il codice a 0x407f0 è la routine di azzeramento adiacente e non viene raggiunto per caduta dal salto.

Tra le 39 voci elaborate dalla quinta inizializzazione, due hanno il bit alto del campo a +0x0c. Oltre alla voce iniziale c’è la voce a indice 4, controllo 0x680001ec, campo di priorità 1 e ingresso 0x1000ac. L’helper a 0x4186e collega il controllo a una lista indicizzata e aggiorna una bitmap in RAM. Questa è una pista per il cambio di contesto. L’interrupt che effettivamente lo innesca e l’ordine di esecuzione dei servizi richiedono ulteriori verifiche. Il report conserva `prepared_contexts` e `first_frame`.

**AB-27 · Contesto a priorità 1 e selezione del prossimo lavoro.** Il descrittore a 0x5c480 associa l’ingresso 0x1000ac al controllo 0x680001ec. Il suo stack iniziale è a 0x8f991204. Nel percorso di ingresso, dopo il prologo e quattro caricamenti di indirizzi globali, si trovano 19 chiamate consecutive di cui il report calcola le destinazioni. La prima, 0x19a542, cerca una corrispondenza con la firma ENDM a 0x1f2b44: prova prima 0x7c7b00, poi 0x7e7b00. In caso di corrispondenza copia 0x7b20 byte dalla base corrispondente in RAM a 0x8f95130c, altrimenti usa i dati interni a 0x1eb044. La seconda, 0x18522c, opera su un blocco diverso. Il ruolo generale di inizializzazione è un’inferenza; non sono ancora noti i nomi dei sottosistemi o la provenienza fisica delle due sorgenti.

Lo slot 192 della tabella (indice dal primo indirizzo, non numero hardware dell’interrupt) punta a 0x40d44. Sul percorso a 0x40e2a viene letta la bitmap a 0x68000004. La parola 0x97dd a 0x40e32 è SRCH1 AC nella definizione FR81. Per bit 31 impostato la ricerca produce zero, quindi il codice legge la testa della lista a 0x68000008. La routine a 0x4186e popola proprio quella lista per il contesto con campo 1. Se la testa è 0x680001ec e il controllo corrente è ancora quello di attesa, i controlli successivi hanno un candidato distinto da ripristinare. Questa catena è statica: l’evento abilitante e l’ordine temporale non sono acquisiti.

Ghidra FR60 riconosce le istruzioni circostanti ma lascia un buco su 0x97dd. Lo script D90FirstTask.java controlla la parola e riporta esplicitamente la lettura del profilo esteso; il progetto conserva FR60. La singola estensione non identifica il modello del chip. [Definizione SLEIGH FR81](https://github.com/desrdev/ghidra-fr60/blob/main/data/languages/fr81ext.sinc)

**AB-28 · Uso del blocco ENDM.** La funzione a 0x19a542 pubblica la base RAM 0x8f95130c tramite la parola a 0x8f958e2c, nel delay slot del ritorno a 0x19a5ea. La scansione delle parole allineate di B trova 36 caricamenti letterali del puntatore condiviso (incluso il produttore). Il solo numero di corrispondenze è un limite di ricerca. Due utilizzi verificati con disassemblato FR:

- A 0x100684 viene dereferenziato il puntatore, letta la mezza parola a +0x150 e copiato il byte basso a 0x8f52edee. Un altro percorso a 0x168c32 scrive nel medesimo campo. Nei valori di fabbrica vale 0x0020. Il suo significato operativo è aperto.
- La routine di aggiornamento in RAM già trattata in AB-20 usa lo stesso puntatore, legge i campi +0x15a e +0x15c e passa i valori in un percorso che chiama la routine di attesa. I default sono 1 e 70; l’unità temporale non è stata determinata.

A 0x19a5ec è presente una copia integrale dei valori interni in RAM, seguita dalla chiamata alla routine di persistenza a 0x19a60c. Per i due indirizzi 0x7c0000 e 0x7e0000 il codice passa per i wrapper 0x13a1aa e 0x13a1bc: questi delegano rispettivamente a EraseRange e ProgramAndVerifyRange, già identificati. L’ampiezza passata è 0x7b20. Questo sostiene la classificazione come blocco riscrivibile di parametri, con due copie in flash e valori di ripristino in ROM. La ricostruzione è statica; il report conserva i valori del campione interno, non quelli di una particolare fotocamera.

**AB-29 · Scrittura del parametro ENDM +0x150.** Nella tabella a offset file 0x212956 il codice 0xfc57 precede il puntatore all’handler CPU 0x168bf2. L’handler preleva una parola da 0x8f54545c, chiama un controllo preliminare e, se autorizzato da quel controllo, considera solo i 16 bit bassi. Fino a 255 inclusi scrive una mezza parola in ENDM +0x150 e ritorna tramite il percorso di risposta 0x2001. Per valori bassi a 16 bit tra 256 e 65535 imbocca il percorso 0x201d senza quella scrittura. Questi valori di risposta corrispondono a OK e Invalid Parameter in PTP; la raggiungibilità dall’esterno di 0xfc57 resta da verificare. [Definizioni libgphoto2](https://github.com/gphoto/libgphoto2/blob/master/camlibs/ptp2/ptp.h)

La copia del byte a 0x8f52edee e la notifica 0x46 appartengono sia al percorso di impostazione sia a quello di inizializzazione. La routine 0x110412 calcola lo slot RAM con passo 12 (0x8f52f5c4 per questo indice). La routine che prepara i dati moltiplica l’indice per 16 per selezionare il descrittore a 0x1c4404. Esso specifica proprio 0x8f52edee, lunghezza 1, tipo 1 e callback 0x11046c, un ritorno immediato. Il ramo del tipo 1 a 0x10ffa8 usa la lunghezza e copia il byte nel buffer d’uscita. Questo identifica il trasporto interno del valore, senza ancora associarlo a una funzione fotografica. Il setter modifica la copia RAM; non è dimostrato che ogni chiamata scriva in flash.

**AB-30 · Dallo slot 0x46 alla porta dati.** Il descrittore per 0x46 copia il byte di lavoro nel buffer a 0x8f52f6f4. In seguito il firmware memorizza il cursore di invio a 0x8f52f5d4. La voce 228 della tabella dei vettori di B contiene 0x111f14. Sul ramo a 0x111f46 questo gestore legge una mezza parola da 0x64 e seleziona una routine dalla tabella a 0x1c4424 usando lo stato a 0x8f52f7fe. Lo stato 5 (0x111a1c) preleva il byte dal cursore, lo aggiunge all’accumulatore, aggiorna lo stato a 6 e lo scrive con DMOVH a 0x64. Per lunghezza iniziale uno, lo stato 6 (0x111a7c) scrive il complemento della somma e passa a 7. L’indirizzo diretto codificato 0x32 è scalato per due da DMOVH.

`D90OutputByte.java` esegue in Ghidra la copia del byte e il ramo di dispatch due volte sul programma B. Si parte dallo stato 5, con una parola già selezionata di lunghezza un byte, lettura iniziale della porta zero e somma parziale impostata a 0x47. È una prova differenziale, senza generazione dell’interrupt fisico. Con il valore 0x20 il byte basso della porta assume prima 0x20 e poi 0x98; con 0x21 assume 0x21 e poi 0x97. Lo script controlla anche il passaggio 5→6→7. Per riprodurre, eseguirlo sul progetto B con il profilo FR60.

Nel componente A, a offset file 0x3f8a, la stessa istruzione di lettura della porta 0x64 è seguita dalla copia del byte a una destinazione indiretta. Questa corrispondenza è una pista per cercare il ricevitore: non prova un collegamento fisico A–B o un riconoscimento dello specifico indice 0x46 sul lato A. Il percorso in B identifica il primo effetto sulla porta dati. L’effetto fotografico resta aperto.

**AB-31 · Il ricevitore A a 0x3f8a tratta frasi di navigazione.** Gli indirizzi in questa voce sono offset nel file. Il ramo legge la porta 0x64, deposita il byte nel buffer indicato dalla parola a 0x3620c e può chiamare il selettore a 0x3fe2. La routine a 0x4056 confronta il byte con 0x24 (carattere dollaro). La fase successiva confronta i caratteri alle posizioni 3–5 con RMC, GGA o HDG, e alle posizioni 1–5 con PGRMM. Il codice controlla anche la virgola (0x2c), l’asterisco (0x2a) e il ritorno carrello (0x0d). L’insieme identifica un parser di frasi di navigazione compatibili con NMEA. [Formati delle frasi e delimitatori](https://gpsd.gitlab.io/gpsd/NMEA.html)

L’identità della porta evidenziata in AB-30 non basta a collegare questo ramo al record binario di B. Qui 0x46 può comparire nel controllo dei caratteri esadecimali: a 0x4b34 viene confrontato come limite superiore di A–F, e il percorso tratta anche 0–9 e a–f. Non abbiamo individuato in questa catena un dispatch dell’indice B 0x46 con lunghezza uno. L’attribuzione riguarda questo ricevitore e lascia aperti altri modi della periferica e altri canali di A. Il report riproduce la lettura della porta, i salti relativi e i confronti dei caratteri nel campo `a_serial_receiver`.

**AB-32 · Mappa di A e ricezione selezionabile.** La mappa di lavoro di A associa l’intervallo file [0, 0x80000) agli indirizzi CPU [0x80000, 0x100000). Il CRC finale resta escluso dalla memoria importata. Il vettore finale a offset 0x7fffc contiene 0xfc00e. A quell’indirizzo il codice imposta SP a 0x3e8cc e TBR a 0xffc00, poi chiama fra gli altri 0x80042. Questa coerenza tra vettore, istruzioni di avvio e riferimenti interni sostiene la mappa. Non descrive tutta la memoria fisica della fotocamera.

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

**AB-33 · Attivazione e tabella dei comandi a due byte di A.** Tutti gli indirizzi seguenti sono CPU. Nel ricevitore 0x85104, dopo aver completato i due byte iniziali, il ramo a 0x8514e confronta il buffer con 0x53, 0x53 (`SS`). La corrispondenza imposta il bit 0x20 del byte a 0x36db8. Una mancata corrispondenza porta alla chiamata di 0x83ee8, che reinstalla il parser di navigazione. La routine a 0x84db2 controlla proprio quel bit a 0x84dbc e, quando è impostato, prepara due byte da ricevere e installa 0x851b6. Questo collega un riconoscimento concreto alla selezione della modalità; la successione temporale delle chiamate nel sistema completo resta da verificare.

Quando i bit alti del byte di stato valgono zero, il ricevitore 0x851b6 combina i primi due byte del buffer in una parola big-endian e chiama 0x85076. Quest’ultima cerca la parola in una tabella a 0xe1fd4 con passo 12. Sono presenti 52 voci con chiavi distinte, seguite da una voce nulla. Le chiavi includono 0x4152, 0x4154, 0x5353 e 0x7161; non sono indici consecutivi. La routine restituisce l’indice della voce trovata, oppure 255 per le chiavi sconosciute provate. Nel percorso senza corrispondenza imposta anche il bit 0x02 dello stato. La chiave zero, che coincide con il terminatore, non è inclusa nelle prove negative.

Per una voce riconosciuta e con il bit 0x20 attivo, il byte basso del campo +2 diventa il contatore di ricezione. Il bit 0x8000 del medesimo campo seleziona la fase indicata dai bit alti di stato come 1. In quella fase, al termine del conteggio iniziale, il byte appena ricevuto diventa una nuova lunghezza. Per un campo senza 0x8000 e con conteggio positivo viene selezionata direttamente la fase 2. Quattro delle 52 voci usano 0x8000. Il percorso successivo a 0x84e32 usa l’indice salvato a 0x36db4, carica la callback dal campo +4 e le passa la mezza parola del campo +8. L’effetto delle singole callback non è ancora classificato.

`D90ACommands.java` esegue le istruzioni originali della ricerca per tutte le 52 chiavi, sia con stato iniziale zero sia con 0x20. Verifica indice restituito, stato e contatore. Esegue inoltre otto prove con le chiavi sconosciute 0x0046, 0x4601, 0x4620 e 0xffff nei due stati, due prove della selezione condizionale del ricevitore e due confronti `SS`/`SR`. **116 casi superati**. Le prove sono isolate: stack e RAM inizializzati dal test, ingresso diretto nei blocchi interessati, nessun interrupt fisico né traffico acquisito. Si fermano prima delle chiamate al sistema operativo o ai gestori finali. La selezione condizionale comprende il piccolo helper con impostazioni uguali, così che non esegua scritture alla periferica.

Il report `a_commands` conserva le 52 descrizioni e i punti di controllo sui byte del firmware. Le chiavi provate che contengono 0x46 non sono riconosciute da questa tabella. Ciò non esclude una traduzione intermedia, un altro canale o un diverso significato del byte: **non è ancora identificato il destinatario del descrittore B 0x46**. Rispetto al percorso B già verificato, abbiamo qui un riconoscimento a due byte e un’altra organizzazione dei messaggi; non è dimostrato che siano i due estremi dello stesso protocollo. Il confronto completo richiede ancora l’intestazione trasmessa da B e l’eventuale traduzione sul lato A.

Prossimo passo consigliato: ricostruire dall’inizio la transazione B, compresi intestazione, ricezione e controllo della somma, per confrontarla con i canali A sulla base dell’intero scambio. Modello consigliato, **Astra Medio**.

**AB-34 · Scambio B per il descrittore 0x46, nei due ordini.** `D90BTransaction.java` estende AB-30 all’invio iniziale e al controllo della ricezione. Tutti gli indirizzi sono CPU. Il byte richiesto viene preimpostato a 0x46 in 0x8f52f7fc. Il blocco a 0x10feba imposta lo stato 1 e lo scrive alla porta 0x64. La risposta introdotta dal test viene letta dal dispatch originale e salvata a 0x8f52f7fb; il gestore torna allo stato zero e prepara un evento 0x10. Il test si ferma prima dell’interrupt software che consegnerebbe l’evento al sistema operativo.

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

**AB-35 · Altro trasporto A sulla porta 0xd4 e limiti della scansione.** Gli indirizzi sono CPU. La ricerca di parole opcode nei byte di A è stata usata soltanto per generare candidati. Due falsi positivi verificati in Ghidra: la parola 0x0928 a 0x88b1c appartiene all’immediato di `LDI:20 #0x928,R4` iniziato a 0x88b1a; la parola 0x096e a 0xd05c8 appartiene all’immediato di `LDI:20 #0xd096e,R12` a 0xd05c6. Non sono letture dalle porte 0x50 e 0xdc. Non consideriamo pertanto la scansione grezza un inventario delle periferiche.

È invece confermata la lettura dalla porta 0xd4 a 0x89b54, dentro il gestore 0x89b32. La sequenza a 0x89914 carica quell’ingresso e lo salva nella callback RAM a 0x36118. Il gestore esamina il bit 0x08 della porta 0xd2. Se non è impostato, legge una mezza parola da 0xd4, salva il byte basso nella destinazione indicata da 0x37380, incrementa il puntatore e il contatore a otto bit a 0x368cc. Imposta inoltre il contatore a 0x37388 a 1 oppure, nel ramo in cui il byte a 0x39d34 vale 0x32 e il conteggio coincide con il byte a 0x368cb, a 5000. Il caso del primo byte precede questi confronti e usa 1. Una routine separata a 0x89bde decrementa quel contatore; l’unità temporale non è determinata.

Con il bit 0x08 impostato, il percorso evita la copia nel buffer, imposta il bit 0x02 a 0x368ce e prepara l’evento 0x8000 per l’oggetto 8, fermandosi nel test prima della chiamata al sistema operativo. La routine vicina a 0x89a0e appartiene al percorso che scrive sulla stessa porta, ma non è ancora ricostruito l’intero scambio né il consumatore finale del buffer. Questi fatti identificano un trasporto distinto; non identificano il dispositivo collegato.

`D90AD4Receive.java` ha superato **16 prove isolate** sul codice originale: byte 0, 0x46, 0x98 e 255, ciascuno nei casi primo byte, byte successivo ordinario, contatore speciale ed errore della periferica. Controlla contenuto e avanzamento del buffer, conteggio, valori 1/5000 e mancata copia nel ramo di errore. Stato delle porte, RAM e buffer sono sintetici; non vengono eseguiti interrupt fisici o chiamate al sistema operativo. Il fatto che 0x46 venga copiato come qualunque altro byte non prova che venga riconosciuto come comando.

Sono confermate anche letture dalla porta 0x64 a 0xf839a e 0xfac1e, in due cicli che controllano uno stato e copiano byte in memoria. Non sono state attribuite al protocollo B. L’inventario resta parziale: sono ancora aperti accessi indiretti, altri candidati e la semantica dei buffer. Nessun collegamento A–B viene dichiarato sulla sola coincidenza di un indirizzo di periferica. Il report `a_transport` conserva questi riscontri e i due controesempi di scansione.

Prossimo passo: seguire chi prepara e consuma il buffer del canale 0xd4 e confrontarne il formato con AB-34. Modello consigliato, **Astra Medio**.

**AB-36 · Buffer e controllo del canale 0xd4: somma zero.** La routine A a 0x89430 prepara normalmente due buffer di 48 byte, azzerandoli a 0x3738a e 0x373ba. Salva le basi rispettivamente nei puntatori 0x3737c e 0x37380. Per il modo 0x32 usa invece 0x365c0 e 0x36648 e salta quell’azzeramento. Il modo deriva dai sei bit bassi del parametro passato a 0x89408 e viene salvato a 0x39d34. Il costruttore a 0x894e6 lo scrive come primo byte nel buffer di trasmissione. I significati fotografici dei modi restano aperti.

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

**AB-37 · Canale A 0x84: preparazione, buffer e controllo somma più uno.** Gli indirizzi sono CPU. Il gestore a 0xac7cc usa il buffer 0x39dc5 e l’indice a otto bit a 0x39dfb. Nel blocco a 0xac87e legge dalla porta 0x84, copia il byte basso nella posizione indicata dall’indice, incrementa quest’ultimo e scrive zero nella posizione successiva. La prova riguarda indici 0, 1 e 45; non stabilisce la sicurezza dei limiti del buffer per tutti gli ingressi.

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

**AB-38 · Topologia esterna e inventario del trasporto B.** Il confronto con il sorgente Nikon Hacker distingue il modello FR/TX con pannello D5100 dalla D90: il collegamento di CSIO0 a EI155 non identifica il destinatario del nostro B, e il protocollo implementato per EI155 non coincide con AB-34. Le fonti fissate a una revisione, le differenze e i limiti sono in [communication-topology.md](research/communication-topology.md).

L’analizzatore estrae ora tutte le 71 voci della tabella B a 0x1c3fa4, passo 16, con fine esclusiva 0x1c4414: puntatore dati, campo dimensione, tipo e callback. Sono presenti 41 voci di tipo 0, 25 di tipo 1 e 5 di tipo 2, con 41 callback distinte. Le cinque dimensioni zero vengono preservate. L’inventario non equivale a 71 comandi fotografici identificati né a 71 percorsi emulati. Tutti i precedenti campi del report restano invariati.

Una nuova pista statica collega le voci 0x33 e 0x34: la callback di 0x34 a 0x11151c confronta i bit bassi dei rispettivi buffer e può notificare 0x33. Occorre verificare i rami e seguire i chiamanti prima di attribuire un significato. Il destinatario fisico resta aperto; prossimo passo, prova delimitata di questa callback. Modello consigliato: **Astra Medio**.

**AB-39 · Callback 0x34: rami verificati prima delle chiamate esterne.** `D90BCallback34.java` ha superato 256 casi con ingressi sintetici, eseguendo le istruzioni da 0x11151c al primo ingresso in 0x1011b0, 0x110412 o 0x10a806, oppure prima dell’INT a 0x1115b8. Sono verificati gli aggiornamenti del primo byte della voce 0x33, la selezione della notifica 0x33 e la preparazione dell’evento 0x800. BANDL nel ramo di azzeramento cancella anche il bit 2: la maschera è 0xf8. Le funzioni esterne e il sistema operativo non vengono eseguiti. Matrice dei rami e limiti in [communication-topology.md](research/communication-topology.md); valutazione degli altri progetti in [github-projects.md](research/github-projects.md). Il significato fotografico e il destinatario fisico restano sconosciuti.

**AB-40 · Effetti dopo la callback 0x34 e controllo interno.** Il nuovo script `D90BCallback34Effects.java` esegue realmente la funzione 0x10a806: imposta il bit 0x80 a 0x8f52e35c e ritorna alla callback, che prosegue verso l’evento. Gli altri percorsi si fermano alla notifica o prima del primo INT nella funzione 0x1011b0. Sono passati 256 casi. `D90BStateGate.java` verifica inoltre 32 casi della funzione interna 0x1011ce fino alla preparazione delle chiamate con maschera 0x800 oppure 0xfffff7ff. Servizi del sistema operativo e funzioni riceventi non vengono eseguiti. È stato individuato staticamente un aggiornamento dei tre byte della voce 0x33, ma non ancora l’inizializzazione completa all’avvio. Risultati, rami esclusi e prossime piste in [communication-topology.md](research/communication-topology.md).

**AB-41 · Maschere B e consumatori del bit 0x80.** Le funzioni 0x106e14/0x106ea2 applicano AND/OR alla parola 0x8f52e484; il confronto con 0x8f52e480 decide il seguito e può cancellare/impostare il bit 0x02 del registro 0x50000106. `D90BMaskConsumers.java` verifica 80 casi, iniziando dopo l’acquisizione OS e fermandosi prima dei seguiti non modellati. Altre otto prove verificano due lettori del bit 0x80 a 0x8f52e35c: questo può saltare la preparazione di un evento oppure selezionare percorsi diversi nella funzione 0x10a810. Individuato staticamente anche un collegamento alla notifica della voce 0x0b. Nessun significato fotografico o destinatario fisico viene dedotto da questi soli effetti. Dettagli e punti di arresto in [communication-topology.md](research/communication-topology.md).

**AB-42 · Notifica 0x0b e scritture hardware.** `D90BMessage0B.java` verifica 512 casi dell’aggiornamento del bit 0x04 nel secondo byte del buffer 0x0b: notifica soltanto se il valore richiesto cambia. Altri 96 casi verificano il controllore fino alla prima notifica o al ritorno, senza simulare il servizio di notifica. `D90BHardwareSequence.java` esegue la funzione 0x108624 fino al ritorno e verifica le scritture in più blocchi hardware con due riempimenti iniziali; quattro casi delimitano inoltre il ramo a 0x10b3ce. Le periferiche sono memoria sintetica e il loro ruolo fotografico resta sconosciuto. Dettagli e indirizzi in [communication-topology.md](research/communication-topology.md).
