# Contribuire alla ricerca

Iniziare da [STATUS.md](STATUS.md) e consultare l’[indice della documentazione tecnica](docs/README.md) per trovare i risultati e le relative prove. È utile contribuire anche con una riproduzione indipendente, un caso che fallisce o una correzione di un’interpretazione.

## Campioni e risultati

Procurarsi separatamente i campioni descritti nel README. Non includere firmware, immagini decodificate, dump, HEX o database Ghidra nei contributi. Conservare gli originali fuori dal repository e aprirli soltanto in lettura; per i derivati locali usare `local/`.

Riprodurre anzitutto il report con il comando iniziale del README. Un digest diverso identifica un campione diverso: non aggirare il controllo per trattarlo come il riferimento esistente. Non aggiornare automaticamente `reports/reference.json` per far sparire una differenza; spiegare e verificare il cambiamento.

Per ogni nuovo risultato indicare campione e hash, componente, indirizzi CPU oppure offset file, comandi o script, ingressi sintetici e risultato atteso. Dichiarare i blocchi saltati, le chiamate non eseguite, i dispositivi simulati e i limiti della ricerca. Una sequenza di byte compatibile con un opcode non dimostra da sola che si tratti di codice.

## Ghidra

Le prove locali usano Ghidra 12.1.4, Temurin 21 e il linguaggio `fr60:BE:16:default` con compiler spec `fcc911`. Il linguaggio proviene da [ghidra-fr60](https://github.com/desrdev/ghidra-fr60). Il file [language-profile.sha256](ghidra/language-profile.sha256) identifica i sorgenti e le definizioni del profilo locale, escludendo il file `.sla` generato. Non sostituisce il processo d’installazione del modulo o una prova su un’altra versione.

Generare due file locali usando `ghidra/prepare.py`, con `--component A` e `--component B`. Importarli come programmi distinti con il loader Intel HEX. Per le verifiche recenti, aggiungere la cartella `ghidra/` allo Script Manager ed eseguire:

- Programma A: `D90AChannels.java`, `D90ACommands.java`, `D90AD4Receive.java`, `D90AD4Checksum.java`, `D90A84Protocol.java`.
- Programma B: D90OutputByte.java, D90BTransaction.java, D90BCallback34.java, D90BCallback34Effects.java, D90BStateGate.java, D90BMaskConsumers.java, D90BMessage0B.java, D90BHardwareSequence.java, D90AtamVector.java, D90AtamRequest.java, D90AtamTransfer.java, D90AtamWake.java, D90EventMode.java, D90AtamCommand.java, D90AtamCommandCallers.java, D90AtamStoreScan.java, D90AtamStatusStores.java, D90AtamR8Stores.java, D90AtamInterrupt.java e D90AtamCommandMask.java. Le prove più recenti richiedono anche gli HEX separati, generati in locale con ghidra/prepare.py.

Controllare sia i messaggi di successo sia l’assenza di eccezioni nel log; il solo codice d’uscita di Ghidra headless può non segnalare il fallimento di uno script. Queste sono prove di blocchi originali con memoria e ingressi preparati. Non eseguono l’intera fotocamera e non sostituiscono una convalida hardware.

La licenza MIT riguarda i contributi originali. Conservare attribuzioni e provenienza dei materiali esterni; gli strumenti e i firmware dei rispettivi autori non diventano MIT perché utilizzati nella ricerca.

Per verificare un’importazione nuova in modalità headless, creare una directory locale dedicata ai progetti temporanei ed eseguire `analyzeHeadless` dalla propria installazione di Ghidra. Esempio per A, dalla radice del repository:

```sh
mkdir -p local/ghidra-check
python3 ghidra/prepare.py /percorso/D90_0101.bin --component A > local/D90-A.hex
/percorso/ghidra/support/analyzeHeadless local/ghidra-check D90CheckA \
  -import local/D90-A.hex -processor fr60:BE:16:default -cspec fcc911 \
  -noanalysis -scriptPath "$PWD/ghidra" \
  -postScript D90AChannels.java -postScript D90ACommands.java \
  -postScript D90AD4Receive.java -postScript D90AD4Checksum.java \
  -postScript D90A84Protocol.java -deleteProject
```

Usare un nome di progetto temporaneo non esistente. `-deleteProject` elimina il progetto temporaneo al termine; non usarlo sul proprio database di ricerca. Per B generare l’HEX con `--component B`, scegliere un altro nome di progetto e sostituire gli script con `D90OutputByte.java`, `D90BTransaction.java` e `D90BCallback34.java`, `D90BCallback34Effects.java`, `D90BStateGate.java`, `D90BMaskConsumers.java`, `D90BMessage0B.java`, `D90BHardwareSequence.java`. Ghidra e il profilo FR60 devono essere già installati e Java configurato secondo la propria installazione.
