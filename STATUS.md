# Stato del progetto

Aggiornato il 3 novembre 2026. Questo file riassume il risultato attuale; il README conserva il registro storico delle evidenze, incluse ipotesi poi precisate o ridimensionate.

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

## Blocco di ricerca attuale

L’ipotesi che un ricevitore A sulla porta numericamente uguale a quella di B fosse il suo destinatario non è stata confermata. Abbiamo identificato un parser di navigazione, un protocollo a comandi di due byte e altri trasporti con controlli differenti. Questo restringe la ricerca, ma non dimostra che A non comunichi con B.

Il confronto con l’emulatore Nikon Hacker non identifica il destinatario del trasporto D90. Abbiamo un inventario delle 71 voci B e prove delimitate del collegamento fra le voci 0x33/0x34, le maschere di stato e la notifica 0x0b. Quest’ultima viene richiesta quando cambia un bit del suo buffer; sono verificate anche le condizioni del controllore fino alla prima notifica o al ritorno.

La funzione 0x108624 è ora eseguita isolatamente fino al ritorno: scrive valori precisi in sei blocchi hardware e altri registri. Le verifiche usano memoria sintetica, quindi non identificano gli effetti fisici di quelle scritture. La prossima fase deve identificare la periferica e risalire agli ingressi che abilitano il bit del messaggio 0x0b. L’inizializzazione completa all’avvio, il significato fotografico e il destinatario fisico restano aperti.

Fonti, indirizzi e limiti delle prove sono in [communication-topology.md](docs/communication-topology.md); la valutazione dei riferimenti esterni è in [related-projects.md](docs/related-projects.md). Modello consigliato per la prossima indagine: Astra Medio.

## Prima pubblicazione

Il contenuto è confezionato per una prima versione pubblica di ricerca, presentata come alpha. Non occorre attendere una patch o l’emulazione completa per rendere utili agli altri le mappe, gli strumenti e le verifiche già disponibili.

Criteri per preparare quella versione:

- Campioni separati dal codice e mantenuti in sola lettura. Firmware, immagini estratte e database Ghidra esclusi da Git.
- Report riproducibile da una copia pulita dei sorgenti.
- Istruzioni di importazione e prove Ghidra, con identificazione del profilo linguaggio utilizzato.
- Distinzione esplicita fra fatti, esperimenti con ingressi sintetici, ipotesi e copertura mancante.
- Guida ai contributi e licenza dei contributi originali.

Una riproduzione su una seconda macchina resta utile come verifica indipendente. Non è stata eseguita e non va confusa con una nuova importazione sullo stesso host. Le prove numerose non misurano la percentuale di firmware compresa: molti casi variano gli ingressi dello stesso blocco di poche istruzioni.

Preparazione locale per GitHub: nome proposto `nikon-d90-re`; visibilità proposta pubblica; descrizione “Independent research into Nikon D90 firmware structure, protocols, and emulation. No firmware included.” Argomenti: `nikon`, `firmware`, `reverse-engineering`, `ghidra`, `camera`. La cronologia Git controllata non contiene firmware, immagini estratte, HEX o database Ghidra.

## Verifica di preparazione del 3 novembre 2026

- Il report completo dell’analizzatore coincide byte per byte con `reports/reference.json`, anche da un nuovo checkout locale privo dei derivati ignorati.
- Le cinque prove recenti di A e le due di B elencate in CONTRIBUTING sono passate sia sui programmi di lavoro sia dopo nuove importazioni in progetti Ghidra temporanei. Gli script aggiornati inizializzano esplicitamente anche i registri salvati dai prologhi e il registro di periferica usato dalla selezione A. Nei log delle nuove importazioni finali non risultano avvisi di letture non inizializzate o errori.
- Tutti i blob della cronologia raggiungibile esaminata sono testo UTF-8 senza byte NUL. Il controllo dei nomi non ha trovato firmware, HEX o database Ghidra nella cronologia. Si tratta di un controllo degli artefatti, non di una certificazione legale o di una revisione indipendente di ogni riga.
- Le esclusioni Git comprendono ora anche HEX e progetti Ghidra. La documentazione indica come riprodurre le prove e conserva i digest del profilo linguaggio.

Valutazione: la base è pronta per essere proposta come prima alpha di ricerca. Restano da scegliere il repository remoto e presentare la prima release quando l’utente deciderà di pubblicarla. Una verifica su un’altra macchina rafforzerebbe la riproducibilità, ma non richiede prima di risolvere il collegamento A–B o l’emulazione completa.
