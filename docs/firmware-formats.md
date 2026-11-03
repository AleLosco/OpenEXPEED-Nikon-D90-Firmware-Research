# Firmware samples and package structure

This document contains the sample identification, integrity checks, image mapping, and their stated limits. Finding IDs are stable references.

## Registro dei risultati

Gli intervalli sono offset nel file originale, in byte, con estremo finale escluso.

| Campione | Dimensione | SHA-256 |
|---|---:|---|
| D90_0101.bin | 7 864 322 | `623906f285c656a7c6e4f3cecd26bc5e2b6da38ba07f0cc76a235f7e524814af` |
| NKLD0218.BIN | 39 588 | `bc26b7413adabeabbf21e341197bc14c58666b0be5f5a8c2eac29564c73f5f1d` |

<a id="ab-1"></a>
### AB-1 — Identità della release

Nikon identifica il primo nome come file di aggiornamento D90 A 1.00 / B 1.01. La modifica dichiarata è il supporto ai dati di controllo distorsione L 2.xxx. Nel campione l'intestazione a `0x8` nomina D90 Firmware A. A `0x20ac4a` compare una stringa di build che inizia con `Ver.1.01a D90` e include la data 2014/07/18. La stringa da sola non identifica la partizione B. [Scheda Nikon D90](https://downloadcenter.nikonimglib.com/it/download/fw/9.html)

<a id="ab-2"></a>
### AB-2 — Integrità e disposizione

Il CRC-16/XMODEM (polinomio 0x1021, valore iniziale 0) dei byte in [0, 0x780000) vale 0xc2ab e coincide con il valore big-endian in coda. Tre lunghe sequenze di 0xff sono riportate in [reference.json](../reports/reference.json). Per il confine del pacchetto e la mappa di B si vedano AB-11 e AB-12.

<a id="l-1"></a>
### L-1 — Versione e natura

Nikon descrive L 2.018 come dati di controllo distorsione per più modelli, inclusa D90 con A 1.00 e B 1.01. Nel campione, i byte 0x02 e 0x12 agli offset 4 e 5 corrispondono a 2 e 18 decimali. Il conteggio big-endian a offset 6 vale 307. [Scheda Nikon L 2.018](https://downloadcenter.nikonimglib.com/it/download/fw/348.html)

<a id="l-2"></a>
### L-2 — Record e controlli

Applicando la ricorrenza documentata dal progetto Nikon Hacker, la regione [0x20, 0x99a0) si decodifica in 307 record contigui. Il primo campo di ciascun record indica 0x80 byte e l'ultimo termina esattamente al confine previsto. Il CRC sui byte originali in [0, 0x99a0) vale 0x5920. Seguono il CRC, due byte nulli e un suffisso di 256 byte a 0x99a4. Nel suffisso un secondo CRC su [0x99a4, 0x9a88) vale 0xead2 e coincide con la parola successiva. Restano da identificare i campi ottici e il significato del suffisso. Il conteggio riguarda i record e non dimostra il numero di obiettivi distinti. Nessuna osservazione dimostra esecuzione di codice in L. [Lettore NKLD di Nikon Hacker](https://github.com/simeonpilgrim/nikon-firmware-tools/blob/main/firmware%20decode/firmware%20decode/Program.cs) · [Algoritmo di decodifica](https://github.com/simeonpilgrim/nikon-firmware-tools/blob/main/firmware%20decode/firmware%20decode/Decrypt.cs)

## Metodo e limiti

È stata usata la skill REA e invocata la CLI REA 6.7.0. Su questo host macOS, per entrambi i campioni, `inspect-firmware-regions` ha restituito `capability_unavailable` (richiede Linux e Binwalk) e `inspect-artifact` ha restituito `unsupported_target`. I risultati AB-1–L-2 derivano dalla documentazione pubblica e dai controlli Python sui file originali. Non sono record Evidence generati da REA. [Progetto REA](https://github.com/morluto/rea)

## Mappa preliminare A/B

<a id="ab-3"></a>
### AB-3 — Collegamento fra le regioni

La parola big-endian a offset `0x11fbf2` vale `0x001cac48`. Aggiungendo `0x40002` si raggiunge esattamente il primo byte della stringa di build D90 a `0x20ac4a`. Questo collega dati situati prima del lungo intervallo di riempimento con un bersaglio situato dopo.

<a id="ab-4"></a>
### AB-4 — Verifica della traslazione

Nell’intervallo [0x140002, 0x604cc5) la sonda cerca su offset pari la codifica FR big-endian di `ldi:32` (0x9f80–0x9f8f seguito da un immediato di 32 bit). Con la relazione `indirizzo = offset − 0x40002`, 268 valori immediati distinti puntano all’inizio di stringhe ASCII stampabili di almeno sei caratteri, delimitate da zero su entrambi i lati. Spostando la relazione di due byte in ciascuna direzione i riscontri scendono a zero. Con indirizzo uguale all’offset sono 11. La scansione può includere dati che somigliano a istruzioni. Il risultato sostiene la mappatura per questi riferimenti, senza estenderla automaticamente a ogni byte. [Codifica FR nel sorgente GNU binutils](https://gnu.googlesource.com/binutils-gdb/+/37f7f684de0e53c8add5ff43d73082bf98985321/opcodes/fr30-opc.c)

Per una verifica manuale, a offset `0x1ab96e` il candidato caricamento contiene `0x001d307c`, che con questa mappa punta alla stringa `D90_????.BIN` a offset `0x21307e`. È un punto da cui cercare il chiamante che gestisce i nomi di aggiornamento. La stringa da sola non stabilisce la funzione del chiamante.

<a id="ab-5"></a>
### AB-5 — Possibili vettori

Interpretando [0x11fc02, 0x120002) come 256 parole big-endian, 206 hanno valore `0x0004050a` e l’ultima vale `0x00040000`. Subito dopo iniziano 128 KiB di 0xff. La disposizione è compatibile con una tabella di vettori. Restano da verificare il reset e la base della regione iniziale. La relazione AB-4 proietta l’offset `0x140002` sull’indirizzo `0x00100000`, una base candidata per l’importazione separata della seconda regione. L’attribuzione a B è verificata in AB-11. Il CRC globale è verificato, mentre un test CRC-16/XMODEM sul prefisso [0, 0x120000) dà 0x1440 contro 0x0000 nei due byte successivi.

## Prossimi esperimenti

1. **Mappa A/B.** Cercare intestazioni interne, vettori e puntatori ripetuti. Registrare per ogni segmento l'intervallo nel file e, separatamente, l'indirizzo ipotizzato in memoria.
2. **CPU e Ghidra.** Provare un profilo ISA per segmento su una copia locale. Accettare una mappatura solo quando convergono più evidenze, per esempio vettori allineati, destinazioni di salto valide e riferimenti a stringhe note.
3. **Emulazione minima.** Selezionare una routine con input e output osservabili. Modellare soltanto la RAM e gli accessi periferici che usa, segnalando gli accessi sconosciuti. Confrontare i risultati con vettori di prova prima di ampliare il modello.
4. **Prima patch sperimentale.** Partire da un comportamento riproducibile e da una funzione identificata. Registrare hash base, offset, precondizioni e test prima/dopo su copia locale. La fattibilità di modifiche funzionali e del boot emulato è ancora da verificare.

Per contribuire, includere nel report digest, comando, offset, risultato osservato e ipotesi ancora aperte. Conservare firmware, dump e database Ghidra fuori dal repository o sotto `local/`.

La licenza MIT si applica ai contributi originali del progetto. I firmware Nikon sono forniti separatamente dai rispettivi titolari.
