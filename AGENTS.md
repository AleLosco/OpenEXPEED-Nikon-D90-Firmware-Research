# Workflow locale Nikon D90

## Fonte di verità e continuità
- Usare lo stato corrente dei file locali come riferimento operativo principale; il remoto può essere arretrato. Leggere `STATUS.md`, poi solo le sezioni tecniche necessarie in `docs/` e i sorgenti associati.
- All'inizio controllare lo stato Git e preservare tutte le modifiche esistenti. Non ripetere analisi completate senza una ragione tecnica esplicita (regressione, input cambiato, lacuna di evidenza).
- `STATUS.md` è il registro operativo unico: aggiornare risultati, ipotesi, problemi e prossime priorità; mantenere indirizzi, prove e limiti nei documenti tematici, tramite collegamenti. Conservare gli identificativi AB/L e le rettifiche storiche.

## Esecuzione efficiente
- Raggruppare operazioni ripetitive in script batch quando utile; parallelizzare attività indipendenti quando sicuro. Serializzare scritture sullo stesso file/database e attività dipendenti.
- Ridurre chiamate ridondanti e output superflui; evitare riletture di file invariati senza necessità. Salvare output lunghi in log locali e leggerne porzioni mirate. Non usare output troncati come file completi.
- Procedere autonomamente nelle attività previste. Privilegiare accuratezza e completezza delle evidenze rispetto al risparmio di risorse.
- Usare gli strumenti già disponibili. Non introdurre nuove dipendenze soltanto per adottare UTCP.

## Verifica e ricerca
- Eseguire verifiche e test riproducibili appropriati alla modifica; registrare comando, input, versione, risultato e limiti. Controllare i log oltre al codice di uscita. Non rilanciare prove immutate senza motivo.
- Distinguere evidenze confermate, ipotesi e risultati inconcludenti. RAM sintetica e interrupt forzati non dimostrano comportamento elettrico, tempi o accettazione hardware.
- Firmware originali in sola lettura. Usare copie isolate del linguaggio e programmi transitori; progetti Ghidra esistenti con `-readOnly -noanalysis`. Non confondere controllo dei log con riesecuzione indipendente.
- Istruzioni per Python/Ghidra e versioni: `CONTRIBUTING.md`; identità dei campioni e limiti: `docs/firmware-formats.md`. Log di lavoro fuori dal repository o sotto `local/`.

## Locale e pubblico
- Non eseguire commit, push, pubblicazioni o modifiche remote senza autorizzazione esplicita riferita all'operazione corrente. Le precedenti pubblicazioni non costituiscono autorizzazione permanente.
- Prima di una pubblicazione autorizzata esaminare diff, file non tracciati ed esclusioni. Firmware, dump, database, temporanei e dati riservati restano locali. `.gitignore` non protegge file già tracciati.
- Non eliminare o spostare file senza necessità. Preservare le modifiche manuali dell'utente.

## Comunicazione
- Indicare sempre il modello GPT consigliato per il passaggio corrente o successivo, con una breve motivazione commisurata alla complessità. La raccomandazione non cambia automaticamente il modello della chat.
