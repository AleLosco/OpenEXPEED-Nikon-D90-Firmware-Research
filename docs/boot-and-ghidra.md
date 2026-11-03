# Boot analysis and Ghidra work

This document preserves the B-image import notes and startup analysis. CPU addresses and file offsets remain distinguished as in the original findings.

## Importare la mappa B in Ghidra

Dalla radice del repository, con Python 3:

```sh
mkdir -p local
python3 ghidra/prepare.py /percorso/D90_0101.bin > local/D90-B.hex
```

L’HEX contiene byte del firmware ed è destinato alla sola analisi locale. Nel nuovo progetto Ghidra importarlo con il loader Intel Hex, scegliendo un linguaggio FR big-endian compatibile. La mappa comprende le due regioni programmate e una copia dei byte rilocati all’indirizzo RAM. In Script Manager aggiungere la cartella `ghidra` del repository ed eseguire `D90Labels.java`. Verificare il vettore finale a 0xdfffc e il suo riferimento a 0x40000. Alla prima analisi controllare anche il disassemblato della funzione RAM a 0x8f52d79e, confrontandolo con AB-14–AB-19 prima di estendere il profilo a tutta l’immagine.

Il round trip dell’HEX ha verificato somme di controllo, indirizzi e contenuto delle tre regioni. Il progetto nativo locale è in `local/ghidra/NikonD90.gpr`. Ghidra 12.1.4 con Temurin 21 e il linguaggio `fr60:BE:16:default:fcc911` ha importato B e salvato le sette etichette. Una seconda sessione ha riaperto il progetto, letto il vettore di reset, disassemblato e creato sei funzioni con `D90Functions.java`. L’ingresso a 0x40000 si decodifica come caricamento immediato di 0x50000000 in R0. A 0x8f52d79e si osserva il salvataggio di R8–R11. Il progetto contiene byte del firmware e resta escluso da Git. Per riprodurre la verifica, dopo le etichette eseguire anche `D90Functions.java`. È stato usato il solo modulo linguaggio del [progetto ghidra-fr60](https://github.com/desrdev/ghidra-fr60); le etichette automatiche di periferiche fornite da quel profilo richiedono verifica sulla D90.

Su questa macchina la copia di lavoro con il linguaggio FR installato è in `work/ghidra` e il JDK in `work/jdk/Contents/Home`, rispetto alla radice della chat. Per aprire il progetto, avviare la copia di Ghidra con quel JDK, poi scegliere File → Open Project e il file .gpr locale.

<a id="ab-24"></a>
### AB-24 — Dal reset alla seconda fase di avvio di B

Nel progetto Ghidra il reset a indirizzo CPU 0x40000 imposta SP a 0x8400 e la base vettori a 0xdfc00. Seguono scritture a indirizzi fissi, fra cui 0x600, 0x640, 0x680 e la famiglia 0x40070000. La loro funzione hardware precisa resta aperta. Dai cicli a partire da 0x40366 emergono due azzeramenti di lunghezza non nulla: 0x1bb414 byte a 0x8f52e0b4 e 0x3214 byte a 0x8dec. I cicli di copia successivi hanno tre trasferimenti attivi:

| Sorgente CPU | Destinazione RAM | Byte |
|---|---|---:|
| 0x287a6c | 0x8f6e94c8 | 0x1ddd0 |
| 0x2a622c | 0x8f96eab0 | 0x193a8 |
| 0x2a5838 | 0x8f52d6c0 | 0x9f4 |

Due ulteriori descrizioni hanno lunghezza zero. L’ultima copia attiva corrisponde al codice di aggiornamento già analizzato. A 0x40468 un salto indiretto porta a 0x40704, etichettata B_InitStage2 nel progetto locale. Questa routine scrive zero a 0x6800cf20 e contiene dodici chiamate relative consecutive. La prima, a 0x407f0, contiene cicli di azzeramento tra cui [0x68000000, 0x68000ec0) e [0x8000, 0x8400). L’indirizzo 0x6800cf20 riceve poi i valori 1 e 2 lungo il percorso di ritorno previsto. Dopo il salto a offset file 0x80752 si trova una stringa che nomina Softune REALOS/FR e micro-ITRON. È un indizio sul sistema operativo; l’identità di uno scheduler in esecuzione richiede ulteriori evidenze. La traccia deriva dal disassemblato statico. Per riprodurre le annotazioni eseguire D90Startup.java dopo D90Functions.java.

<a id="ab-25"></a>
### AB-25 — Le dodici inizializzazioni e il passaggio di contesto

Ghidra ha riconosciuto le dodici chiamate relative con destinazioni distinte. Il report `startup.init_tables` conserva gli header big-endian e la prima parola di ogni record contato. Gli indirizzi seguenti sono nello spazio CPU.

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

<a id="ab-26"></a>
### AB-26 — Primo PC preparato per RETI

La voce ROM a 0x5c838 contiene controllo 0x680007c4, ingresso 0x407ee e base del frame 0x68000ed0. La routine a 0x41578 carica R6 dall’offset +0x10 e R7 da +0x14 del descrittore. A 0x4153c il delay slot salva R7 a +0x18 del controllo. Il codice usa poi R7 come base del frame: vi scrive R6 a +0, PS=0x001f0030 a +4 e RP=0x4050a a +8.

A 0x407cc il ripristino rilegge la base del frame e la imposta come stack utente. Preleva PC e PS, li trasferisce sullo stack di sistema, ripristina gli altri registri e a 0x407ea esegue RETI. Sul percorso ricostruito, senza interventi asincroni, PC diventa **0x407ee**. A quell’indirizzo la parola 0xe0ff è un salto relativo a se stesso. Il comportamento è coerente con un contesto di attesa. Il codice a 0x407f0 è la routine di azzeramento adiacente e non viene raggiunto per caduta dal salto.

Tra le 39 voci elaborate dalla quinta inizializzazione, due hanno il bit alto del campo a +0x0c. Oltre alla voce iniziale c’è la voce a indice 4, controllo 0x680001ec, campo di priorità 1 e ingresso 0x1000ac. L’helper a 0x4186e collega il controllo a una lista indicizzata e aggiorna una bitmap in RAM. Questa è una pista per il cambio di contesto. L’interrupt che effettivamente lo innesca e l’ordine di esecuzione dei servizi richiedono ulteriori verifiche. Il report conserva `prepared_contexts` e `first_frame`.

<a id="ab-27"></a>
### AB-27 — Contesto a priorità 1 e selezione del prossimo lavoro

Il descrittore a 0x5c480 associa l’ingresso 0x1000ac al controllo 0x680001ec. Il suo stack iniziale è a 0x8f991204. Nel percorso di ingresso, dopo il prologo e quattro caricamenti di indirizzi globali, si trovano 19 chiamate consecutive di cui il report calcola le destinazioni. La prima, 0x19a542, cerca una corrispondenza con la firma ENDM a 0x1f2b44: prova prima 0x7c7b00, poi 0x7e7b00. In caso di corrispondenza copia 0x7b20 byte dalla base corrispondente in RAM a 0x8f95130c, altrimenti usa i dati interni a 0x1eb044. La seconda, 0x18522c, opera su un blocco diverso. Il ruolo generale di inizializzazione è un’inferenza; non sono ancora noti i nomi dei sottosistemi o la provenienza fisica delle due sorgenti.

Lo slot 192 della tabella (indice dal primo indirizzo, non numero hardware dell’interrupt) punta a 0x40d44. Sul percorso a 0x40e2a viene letta la bitmap a 0x68000004. La parola 0x97dd a 0x40e32 è SRCH1 AC nella definizione FR81. Per bit 31 impostato la ricerca produce zero, quindi il codice legge la testa della lista a 0x68000008. La routine a 0x4186e popola proprio quella lista per il contesto con campo 1. Se la testa è 0x680001ec e il controllo corrente è ancora quello di attesa, i controlli successivi hanno un candidato distinto da ripristinare. Questa catena è statica: l’evento abilitante e l’ordine temporale non sono acquisiti.

Ghidra FR60 riconosce le istruzioni circostanti ma lascia un buco su 0x97dd. Lo script D90FirstTask.java controlla la parola e riporta esplicitamente la lettura del profilo esteso; il progetto conserva FR60. La singola estensione non identifica il modello del chip. [Definizione SLEIGH FR81](https://github.com/desrdev/ghidra-fr60/blob/main/data/languages/fr81ext.sinc)

<a id="ab-28"></a>
### AB-28 — Uso del blocco ENDM

La funzione a 0x19a542 pubblica la base RAM 0x8f95130c tramite la parola a 0x8f958e2c, nel delay slot del ritorno a 0x19a5ea. La scansione delle parole allineate di B trova 36 caricamenti letterali del puntatore condiviso (incluso il produttore). Il solo numero di corrispondenze è un limite di ricerca. Due utilizzi verificati con disassemblato FR:

- A 0x100684 viene dereferenziato il puntatore, letta la mezza parola a +0x150 e copiato il byte basso a 0x8f52edee. Un altro percorso a 0x168c32 scrive nel medesimo campo. Nei valori di fabbrica vale 0x0020. Il suo significato operativo è aperto.
- La routine di aggiornamento in RAM già trattata in AB-20 usa lo stesso puntatore, legge i campi +0x15a e +0x15c e passa i valori in un percorso che chiama la routine di attesa. I default sono 1 e 70; l’unità temporale non è stata determinata.

A 0x19a5ec è presente una copia integrale dei valori interni in RAM, seguita dalla chiamata alla routine di persistenza a 0x19a60c. Per i due indirizzi 0x7c0000 e 0x7e0000 il codice passa per i wrapper 0x13a1aa e 0x13a1bc: questi delegano rispettivamente a EraseRange e ProgramAndVerifyRange, già identificati. L’ampiezza passata è 0x7b20. Questo sostiene la classificazione come blocco riscrivibile di parametri, con due copie in flash e valori di ripristino in ROM. La ricostruzione è statica; il report conserva i valori del campione interno, non quelli di una particolare fotocamera.

<a id="ab-29"></a>
### AB-29 — Scrittura del parametro ENDM +0x150

Nella tabella a offset file 0x212956 il codice 0xfc57 precede il puntatore all’handler CPU 0x168bf2. L’handler preleva una parola da 0x8f54545c, chiama un controllo preliminare e, se autorizzato da quel controllo, considera solo i 16 bit bassi. Fino a 255 inclusi scrive una mezza parola in ENDM +0x150 e ritorna tramite il percorso di risposta 0x2001. Per valori bassi a 16 bit tra 256 e 65535 imbocca il percorso 0x201d senza quella scrittura. Questi valori di risposta corrispondono a OK e Invalid Parameter in PTP; la raggiungibilità dall’esterno di 0xfc57 resta da verificare. [Definizioni libgphoto2](https://github.com/gphoto/libgphoto2/blob/master/camlibs/ptp2/ptp.h)

La copia del byte a 0x8f52edee e la notifica 0x46 appartengono sia al percorso di impostazione sia a quello di inizializzazione. La routine 0x110412 calcola lo slot RAM con passo 12 (0x8f52f5c4 per questo indice). La routine che prepara i dati moltiplica l’indice per 16 per selezionare il descrittore a 0x1c4404. Esso specifica proprio 0x8f52edee, lunghezza 1, tipo 1 e callback 0x11046c, un ritorno immediato. Il ramo del tipo 1 a 0x10ffa8 usa la lunghezza e copia il byte nel buffer d’uscita. Questo identifica il trasporto interno del valore, senza ancora associarlo a una funzione fotografica. Il setter modifica la copia RAM; non è dimostrato che ogni chiamata scriva in flash.
