# Progetti esterni valutati

Ricerca del 3 novembre 2026: query GitHub/web su Nikon D90 firmware reverse engineering e FR60/FR80 Ghidra, poi lettura dei sorgenti pertinenti. È una selezione mirata, non un censimento completo. Le descrizioni dei progetti non sono prove della loro compatibilità con la D90.

## Riferimenti effettivamente utili

| Progetto | Uso verificato nel nostro lavoro | Limite |
|---|---|---|
| [nikon-firmware-tools](https://github.com/simeonpilgrim/nikon-firmware-tools/tree/89ad7354b6d721260b3e999061fefc51f3580ee0) | Confronto del trasporto seriale e della topologia; dettagli in AB-38 | Il modello FR/TX con pannello D5100 non prova il cablaggio D90 |
| [ghidra-fr60](https://github.com/desrdev/ghidra-fr60/tree/02c151866e6b34a804fedc2b9f0c4736aadac69b) | Il profilo FR60 già installato esegue le nostre prove; i digest locali sono in `ghidra/language-profile.sha256` | La revisione esterna qui consultata non sostituisce l’identità del profilo locale |
| [ghidra_fujitsu_fr](https://github.com/simeonpilgrim/ghidra_fujitsu_fr/blob/72555db61a6e79413c93e3a6b4b9ec924cf6108f/data/languages/fr.sinc) | Confronto mirato delle definizioni SLEIGH di BANDL e BORH usate nella callback 0x34 | Non installato e non eseguito; non convalida tutte le altre istruzioni |
| [ghidra-fujitsu-risc](https://github.com/ammaraskar/ghidra-fujitsu-risc/blob/fe9c28ff836aef1c02635cfc09e3f9e30eb4a294/data/languages/fr.slaspec) | Seconda formulazione delle stesse due istruzioni, semanticamente equivalente per il byte modificato | Modulo nato per ricerca D3100, non un emulatore hardware D90 |

BANDL con immediato 8 conserva i bit 3–7 e cancella i bit 0–2 del byte puntato. BORH con immediato 2 imposta il bit 5 senza cancellare gli altri. Le definizioni consultate concordano con il profilo locale e con le trasformazioni attese dalla prova `D90BCallback34.java`. È un confronto di implementazioni, non una misura sulla CPU fisica né una garanzia che i progetti siano indipendenti fra loro. Non è stato copiato codice esterno nel test.

Il file [fr81ext.sinc di ghidra-fr60](https://github.com/desrdev/ghidra-fr60/blob/02c151866e6b34a804fedc2b9f0c4736aadac69b/data/languages/fr81ext.sinc) contiene anche SRCH1; un’estensione equivalente è presente nell’installazione locale, insieme alla definizione del linguaggio FR81. Questo precisa il limite precedente: SRCH1 non è coperta dal **profilo FR60 selezionato**, non è assente da tutto il pacchetto. La definizione nel modulo alternativo di Simeon Pilgrim richiama invece una userop `Srch1`, quindi la sola presenza del mnemonico non garantisce l’esecuzione. Non è stato cambiato il linguaggio dei programmi né dichiarata risolta l’emulazione di SRCH1. Un esperimento FR81 deve essere separato e verificare esplicitamente risultati e flag prima di adottarlo.

## Candidati non adottati in questa fase

- [Ghidra-Rare](https://github.com/aleph-void/Ghidra-Rare) elenca anche il modulo FR60/FR81 attribuito a desrdev. La ricerca non ha dimostrato un vantaggio aggiuntivo rispetto al modulo già presente; non è stato installato.
- [Fujihack](https://github.com/fujihack/fujihack) riguarda firmware Fujifilm. Nessun codice o indirizzo è stato trasferito alla D90.
- [nikon-fw-key-tool](https://github.com/WonderLantern/nikon-fw-key-tool) dichiara supporto a modelli Z; [z9-qemu](https://github.com/WonderLantern/z9-qemu) riguarda Z9. Non sono stati usati come decoder, modelli delle periferiche o fonti dei risultati D90. L’applicabilità tecnica non è stata stabilita.

Per usare un altro progetto occorrono un elemento specifico verificabile, una revisione identificata e una corrispondenza con il campione o l’istruzione studiata. Nessun download di firmware, nessuna esecuzione dei nuovi progetti e nessuna pubblicazione remota sono stati necessari per questo confronto.
