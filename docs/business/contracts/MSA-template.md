# Master Services Agreement — TransitKit

> Template MSA per primo cliente. Bilingue EN/IT. Da personalizzare per ogni deal.
> Versione: 0.1 (draft 2026-05-25, non revisionato da legale)

---

## STRUTTURA DEL CONTRATTO

Due documenti separati:
- **MSA** (questo): termini generali, valido a tempo indeterminato, firmato una volta
- **Order Form** (`order-form-template.md`): specifico per deal, indica servizi+prezzi+durata, firmato per ogni rinnovo/cambio scope

---

# MASTER SERVICES AGREEMENT (EN)

**This Master Services Agreement** ("Agreement") is entered into as of **[EFFECTIVE DATE]** ("Effective Date") between:

- **Provider**: [PROVIDER LEGAL NAME], with registered office at [PROVIDER ADDRESS], VAT/Tax ID [VAT/TAX ID] ("Provider")
- **Customer**: [CUSTOMER LEGAL NAME], with registered office at [CUSTOMER ADDRESS], Tax ID [CUSTOMER TAX ID] ("Customer")

Provider and Customer are each a "Party" and collectively the "Parties".

## 1. Services

1.1 Provider shall provide Customer with the **TransitKit Platform** ("Platform"), a white-label public transit information software comprising:
- (a) iOS native application branded for Customer (App Store listing under Customer name or Provider name with Customer branding, as specified in the Order Form);
- (b) Android native application branded for Customer (Google Play listing per (a));
- (c) Web stop pages (mobile web, QR-accessible) at a Customer-specified subdomain;
- (d) GTFS data ingestion pipeline (static schedules + real-time vehicle positions, trip updates, service alerts);
- (e) Operator console for service alerts and push notifications.

1.2 The specific scope, pricing, and term of services for any deployment is set out in an **Order Form** signed by both Parties and incorporated into this Agreement.

1.3 Provider shall use commercially reasonable efforts to deliver the Platform with availability of 99.5% measured monthly, excluding scheduled maintenance windows (announced 48 hours in advance) and force majeure events.

## 2. Customer responsibilities

Customer shall:

(a) Maintain and publish accurate GTFS-static and GTFS-realtime feeds at URLs accessible to Provider;
(b) Provide brand assets (logos, colors, descriptions) in formats specified by Provider within 14 days of Effective Date;
(c) Promptly notify Provider of service changes affecting GTFS data;
(d) Designate one or more authorized administrators for the operator console;
(e) Comply with App Store and Google Play guidelines as applicable to its branding.

## 3. Fees and payment

3.1 Customer shall pay the fees set out in the applicable Order Form.

3.2 Fees are payable **monthly in advance** unless otherwise specified in the Order Form. Default amount: **USD $299/month** per operator deployment.

3.3 Invoices are due within **30 days** of issue. Late payments accrue interest at 1.5% per month (or maximum allowed by law, whichever is lower).

3.4 Fees are **exclusive of taxes**. Customer is responsible for all sales, use, VAT, and similar taxes, except taxes on Provider's net income.

3.5 Provider may suspend Services for non-payment after 30 days past due, following 15 days written notice.

## 4. Term and termination

4.1 The initial term is set out in the Order Form. Default: **12 months**, auto-renewing for successive 12-month terms unless either Party gives 60 days' written notice prior to renewal.

4.2 Either Party may terminate this Agreement immediately upon written notice if the other Party:
- (a) materially breaches and fails to cure within 30 days of notice;
- (b) becomes insolvent or files for bankruptcy.

4.3 Upon termination, Provider shall:
- (a) Within 30 days, remove or transfer Customer-branded App Store/Play Store listings as instructed;
- (b) Within 60 days, delete Customer-specific data, except as required by law or for legitimate business records;
- (c) Provide Customer with an export of all Customer data in standard formats (JSON, CSV) within 14 days of request.

4.4 Sections 5 (IP), 6 (Confidentiality), 7 (Data), 9 (Limitation), 10 (Indemnification), and 12 (Governing Law) survive termination.

## 5. Intellectual property

5.1 **Provider IP**: Provider retains all right, title, and interest in the Platform, including source code, design systems, infrastructure, and any improvements made during the term. Customer receives a non-exclusive, non-transferable, limited license to use the Platform during the term solely for its passenger information services.

5.2 **Customer IP**: Customer retains all right to its brand assets (logos, names, colors, content), GTFS data, and operator console content. Customer grants Provider a worldwide, royalty-free license to use Customer IP solely as necessary to provide the Services.

5.3 **Feedback**: Customer-suggested improvements become part of Provider IP without compensation, but Provider has no obligation to implement.

## 6. Confidentiality

6.1 Each Party shall protect the other's Confidential Information with the same degree of care it uses for its own confidential information, but no less than reasonable care.

6.2 "Confidential Information" includes business strategies, technical architecture, pricing not in public Order Form, customer counts, and any information marked confidential.

6.3 Exclusions: information that (a) is or becomes public without breach, (b) was independently developed, (c) was lawfully received from a third party, or (d) is required to be disclosed by law (with prompt notice to the other Party).

6.4 Confidentiality obligations survive 3 years after termination.

## 7. Data protection and privacy

7.1 Provider acts as a **Data Processor** with respect to passenger personal data (device identifiers, location queries, push notification tokens) processed via the Platform. Customer is the **Data Controller**.

7.2 Provider shall:
- (a) Process passenger data only as necessary to provide the Services;
- (b) Implement reasonable technical and organizational security measures (TLS in transit, encrypted at rest where applicable);
- (c) Not use passenger data for advertising or sell it to third parties;
- (d) Notify Customer within 72 hours of becoming aware of any data breach involving Customer's passengers;
- (e) Assist Customer with data subject requests (access, deletion) at reasonable cost.

7.3 If Customer is subject to **GDPR**, **CCPA**, **FERPA**, or similar laws, the Parties shall execute a separate Data Processing Agreement (DPA) incorporating standard contractual clauses where required.

7.4 Default sub-processors used by Provider include: Apple Push Notification service, Google Firebase Cloud Messaging, Vercel (hosting), Hetzner (real-time proxy), GitHub (CDN). Updated list available on request. Provider will notify Customer 30 days before adding material new sub-processors.

## 8. Warranties

8.1 Provider warrants that it has the legal right to provide the Services and that the Platform will substantially perform as described in the documentation.

8.2 **Disclaimer**: EXCEPT AS EXPRESSLY STATED, THE PLATFORM IS PROVIDED "AS IS" AND PROVIDER DISCLAIMS ALL OTHER WARRANTIES INCLUDING MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT.

8.3 Customer acknowledges that the Platform depends on third-party services (App Store, Google Play, Apple Maps, MapBox/Google Maps APIs) and that Provider is not liable for outages or changes in those services.

## 9. Limitation of liability

9.1 EXCEPT FOR (a) BREACHES OF CONFIDENTIALITY, (b) INDEMNIFICATION OBLIGATIONS, (c) GROSS NEGLIGENCE OR WILLFUL MISCONDUCT, AND (d) AMOUNTS DUE UNDER ORDER FORMS, NEITHER PARTY'S TOTAL CUMULATIVE LIABILITY SHALL EXCEED THE FEES PAID OR PAYABLE BY CUSTOMER TO PROVIDER IN THE 12 MONTHS PRECEDING THE CLAIM.

9.2 IN NO EVENT SHALL EITHER PARTY BE LIABLE FOR INDIRECT, INCIDENTAL, CONSEQUENTIAL, SPECIAL, OR PUNITIVE DAMAGES, OR LOST PROFITS, EVEN IF ADVISED OF THE POSSIBILITY.

## 10. Indemnification

10.1 Provider shall indemnify and defend Customer against third-party claims alleging that the Platform (excluding Customer Content) infringes any U.S./EU patent, copyright, or trademark, provided Customer (a) promptly notifies Provider, (b) gives Provider sole control of defense, and (c) cooperates reasonably.

10.2 Customer shall indemnify Provider against claims arising from Customer Content (GTFS data, brand assets, console content) infringing third-party rights or violating law.

## 11. Independent contractors

The Parties are independent contractors. Nothing in this Agreement creates a partnership, joint venture, agency, or employment relationship.

## 12. Governing law and dispute resolution

12.1 This Agreement is governed by the laws of **[Italy / State of Delaware — to be selected per Order Form]** without regard to conflict-of-laws principles.

12.2 The Parties shall first attempt to resolve disputes through good-faith negotiation for 30 days. Unresolved disputes shall be submitted to:
- (a) **For Italy governance**: Tribunale di [CITY OF PROVIDER];
- (b) **For Delaware governance**: state or federal courts located in Wilmington, DE, USA.

12.3 Notwithstanding 12.2, either Party may seek injunctive relief in any court of competent jurisdiction for breaches of confidentiality or IP.

## 13. General provisions

13.1 **Notices**: Written notices shall be sent to the email addresses on the Order Form, with effective date being the date of sending if sent before 5pm in recipient's timezone on a business day, otherwise the next business day.

13.2 **Assignment**: Neither Party may assign this Agreement without the other's written consent (not unreasonably withheld), except in connection with a merger, acquisition, or sale of substantially all assets.

13.3 **Force majeure**: Neither Party is liable for delays caused by events beyond reasonable control (natural disasters, war, government action, internet/cloud outages affecting industry-wide infrastructure).

13.4 **Severability**: If any provision is held unenforceable, the remainder of this Agreement remains in effect.

13.5 **Entire agreement**: This Agreement, together with any signed Order Forms and DPAs, constitutes the entire agreement and supersedes all prior discussions.

13.6 **Amendment**: Only by written instrument signed by both Parties.

13.7 **Counterparts**: May be executed in counterparts including electronic signatures.

---

**Signed by:**

**Provider — [PROVIDER LEGAL NAME]**

Name: ____________________  
Title: ____________________  
Date: ____________________  
Signature: ____________________

**Customer — [CUSTOMER LEGAL NAME]**

Name: ____________________  
Title: ____________________  
Date: ____________________  
Signature: ____________________

---
---

# CONTRATTO QUADRO DI SERVIZI (IT)

> Versione italiana — usare per clienti italiani. Mantenere il contratto in lingua del cliente; in caso di discrepanza con la versione EN prevale la versione firmata.

**Il presente Contratto Quadro di Servizi** ("Contratto") è stipulato in data **[DATA DI EFFICACIA]** tra:

- **Fornitore**: [DENOMINAZIONE FORNITORE], con sede legale in [INDIRIZZO], P.IVA/CF [P.IVA] ("Fornitore")
- **Cliente**: [DENOMINAZIONE CLIENTE], con sede legale in [INDIRIZZO], P.IVA/CF [P.IVA] ("Cliente")

Le Parti sottoscrivono quanto segue.

## 1. Servizi

1.1 Il Fornitore mette a disposizione del Cliente la **Piattaforma TransitKit** ("Piattaforma"), software white-label per informazioni trasporto pubblico composto da:
- (a) app iOS nativa brandizzata Cliente;
- (b) app Android nativa brandizzata Cliente;
- (c) pagine web fermata accessibili via QR su sottodominio Cliente;
- (d) pipeline ingestione GTFS statico + real-time (posizioni veicoli, trip updates, avvisi servizio);
- (e) console operatore per avvisi e push notification.

1.2 Scope, prezzi e durata specifici sono indicati nell'**Ordine di Servizio** firmato dalle Parti e parte integrante del Contratto.

1.3 Il Fornitore garantisce uptime ≥ 99,5% mensile, esclusi manutenzioni programmate (notificate 48h prima) e cause di forza maggiore.

## 2. Obblighi del Cliente

Il Cliente si impegna a:
- (a) mantenere pubblici e accurati i feed GTFS statico e real-time;
- (b) fornire asset brand (loghi, colori) entro 14 giorni dalla data efficacia;
- (c) notificare tempestivamente cambi di servizio impattanti GTFS;
- (d) designare almeno un amministratore console;
- (e) rispettare linee guida App Store e Google Play per il proprio brand.

## 3. Corrispettivi e pagamento

3.1 Il Cliente paga i corrispettivi indicati nell'Ordine di Servizio applicabile.

3.2 Pagamento **mensile anticipato** salvo diversa indicazione. Corrispettivo standard: **EUR 299/mese** per deployment operatore (equivalente USD $299 al cambio del giorno).

3.3 Fatture pagabili entro **30 giorni** dall'emissione. Mora: interessi al tasso legale + 2% (D.Lgs. 231/2002 per transazioni B2B).

3.4 Corrispettivi al netto di IVA. IVA applicata secondo regime fiscale Fornitore (per regime forfettario: "operazione effettuata da soggetto in regime forfettario ex L. 190/2014 — operazione esente IVA").

3.5 Il Fornitore può sospendere i Servizi per mancato pagamento dopo 30 giorni dalla scadenza, previo preavviso scritto di 15 giorni.

## 4. Durata e recesso

4.1 Durata iniziale come da Ordine di Servizio. Default: **12 mesi**, rinnovo tacito 12 mesi salvo disdetta scritta con preavviso di 60 giorni.

4.2 Recesso immediato per giusta causa per:
- (a) inadempimento sostanziale non sanato entro 30 giorni dalla diffida;
- (b) procedura concorsuale o stato di insolvenza.

4.3 Alla cessazione:
- (a) entro 30 giorni il Fornitore rimuove o trasferisce le listing App Store/Play Store secondo istruzioni Cliente;
- (b) entro 60 giorni cancella i dati Cliente, salvo obblighi di legge;
- (c) entro 14 giorni dalla richiesta fornisce export dati Cliente in formati standard.

4.4 Sopravvivono alla cessazione gli artt. 5, 6, 7, 9, 10 e 12.

## 5. Proprietà intellettuale

5.1 **IP Fornitore**: tutti i diritti sulla Piattaforma (sorgente, design, infrastruttura, miglioramenti) restano del Fornitore. Il Cliente riceve licenza non esclusiva, non trasferibile, limitata alla durata del Contratto e all'uso passenger information.

5.2 **IP Cliente**: brand, dati GTFS, contenuti console restano del Cliente, che concede al Fornitore licenza mondiale gratuita ai soli fini dell'erogazione del Servizio.

5.3 **Feedback**: suggerimenti di miglioramento del Cliente diventano IP Fornitore senza compenso; il Fornitore non ha obbligo di implementazione.

## 6. Riservatezza

6.1 Le Parti proteggono le Informazioni Riservate altrui con la stessa diligenza adottata per le proprie, comunque non inferiore alla diligenza ordinaria.

6.2 Sono Informazioni Riservate: strategie commerciali, architettura tecnica, prezzi non pubblici, conteggi clienti, e ogni informazione marcata riservata.

6.3 Esclusioni: informazioni pubbliche senza colpa, sviluppate indipendentemente, ricevute lecitamente da terzi, o divulgate per obbligo di legge (con preavviso alla Parte).

6.4 Obblighi di riservatezza per 3 anni post cessazione.

## 7. Protezione dati e privacy

7.1 Il Fornitore agisce come **Responsabile del trattamento** (Data Processor) ex GDPR per i dati personali passeggeri trattati via Piattaforma. Il Cliente è **Titolare** (Data Controller).

7.2 Il Fornitore si impegna a:
- (a) trattare dati passeggeri solo per erogazione del Servizio;
- (b) implementare misure di sicurezza adeguate (TLS in transito, cifratura at-rest dove applicabile);
- (c) non utilizzare dati passeggeri per advertising né venderli a terzi;
- (d) notificare al Cliente entro 72h ogni data breach;
- (e) assistere il Cliente nelle richieste degli interessati a costi ragionevoli.

7.3 Le Parti firmano separato **DPA** (Data Processing Agreement) ex art. 28 GDPR, parte integrante del Contratto, contenente clausole standard UE per trasferimenti extra-UE se applicabili.

7.4 Sub-responsabili attualmente utilizzati: Apple Push Notification Service, Google Firebase Cloud Messaging, Vercel (hosting), Hetzner (proxy real-time), GitHub (CDN). Elenco aggiornato disponibile su richiesta; il Fornitore notifica al Cliente almeno 30 giorni prima di aggiungere nuovi sub-responsabili materiali.

## 8. Garanzie

8.1 Il Fornitore garantisce di avere titolo per erogare i Servizi e che la Piattaforma funziona sostanzialmente come da documentazione.

8.2 **Esclusione**: salvo quanto espressamente garantito, la Piattaforma è fornita "as is" e il Fornitore esclude ogni altra garanzia di commerciabilità, idoneità a uno scopo specifico, e non-violazione.

8.3 Il Cliente prende atto che la Piattaforma dipende da servizi di terzi (App Store, Google Play, Apple Maps, MapBox/Google Maps) e che il Fornitore non risponde di interruzioni o modifiche di tali servizi.

## 9. Limitazione di responsabilità

9.1 Salvo per (a) violazioni riservatezza, (b) obblighi di manleva, (c) dolo o colpa grave, (d) corrispettivi dovuti, la responsabilità cumulativa totale di ciascuna Parte non eccede i corrispettivi pagati o pagabili dal Cliente nei 12 mesi precedenti la pretesa.

9.2 In nessun caso le Parti rispondono di danni indiretti, incidentali, consequenziali, speciali, punitivi, o lucro cessante, anche se informate della possibilità.

## 10. Manleva

10.1 Il Fornitore tiene indenne il Cliente da pretese di terzi per violazione di brevetti, copyright o marchi USA/UE relativi alla Piattaforma (esclusi i Contenuti Cliente), purché il Cliente (a) notifichi tempestivamente, (b) ceda il controllo della difesa al Fornitore, (c) cooperi ragionevolmente.

10.2 Il Cliente tiene indenne il Fornitore da pretese derivanti dai Contenuti Cliente (dati GTFS, brand, contenuti console) violanti diritti di terzi o di legge.

## 11. Indipendenza delle Parti

Le Parti operano come contraenti indipendenti. Nulla nel Contratto costituisce associazione, joint venture, agenzia, o rapporto di lavoro.

## 12. Legge applicabile e foro

12.1 Il Contratto è regolato dalla legge **italiana**.

12.2 Le Parti tentano composizione bonaria entro 30 giorni. Le controversie non risolte sono devolute al Tribunale di **[CITTÀ DEL FORNITORE]**, foro esclusivo.

12.3 Nonostante 12.2, ciascuna Parte può richiedere provvedimenti urgenti a qualsiasi foro competente per violazioni di riservatezza o IP.

## 13. Disposizioni generali

13.1 **Comunicazioni**: via email agli indirizzi indicati nell'Ordine di Servizio; per atti formali, via PEC.

13.2 **Cessione**: nessuna Parte cede il Contratto senza consenso scritto dell'altra (non irragionevolmente negato), salvo operazioni straordinarie (fusione, acquisizione, vendita ramo d'azienda).

13.3 **Forza maggiore**: nessuna responsabilità per ritardi dovuti a eventi al di fuori del ragionevole controllo (calamità, guerra, atto autorità, outage industry-wide cloud/internet).

13.4 **Indipendenza clausole**: l'invalidità di una clausola non inficia le altre.

13.5 **Integrale accordo**: il presente Contratto + Ordini di Servizio + DPA costituiscono l'intero accordo, sostituendo trattative pregresse.

13.6 **Modifiche**: solo per atto scritto firmato da entrambe le Parti.

13.7 **Originali**: il Contratto può essere sottoscritto in più originali, anche con firma elettronica qualificata o avanzata.

---

**Firmato per accettazione:**

**Fornitore — [DENOMINAZIONE]**

Nome e cognome: ____________________  
Qualifica: ____________________  
Data: ____________________  
Firma: ____________________

**Cliente — [DENOMINAZIONE]**

Nome e cognome: ____________________  
Qualifica: ____________________  
Data: ____________________  
Firma: ____________________

---

## NOTE LEGALI E DA REVISIONARE PRIMA DI USARE IN PRODUZIONE

> Questo template è una **bozza tecnica scritta da Andrea + Claude**, NON è stata revisionata da un avvocato. Prima del primo uso in produzione:

1. **Far revisionare da legale** specializzato in SaaS B2B (italiano se primo cliente IT, USA se primo cliente USA).
2. **Verificare regime IVA** specifico: la dicitura forfettario è corretta solo se Andrea è in regime forfettario; se ordinario va modificata.
3. **DPA separato**: scriverne uno dedicato (art. 28 GDPR ha contenuti minimi obbligatori) — non basta menzionarlo qui.
4. **Sub-processor list**: tenere allineato all'elenco reale + URL pubblico aggiornabile.
5. **Per gare PA italiane**: aggiungere clausole specifiche su CIG, tracciabilità flussi finanziari (L. 136/2010), DURC, antimafia se importo lo richiede.
6. **Per clienti USA pubblici**: aggiungere section su federal funding compliance, FOIA, accessibility (ADA Section 508), eventualmente Buy America se finanziato FTA.
7. **Insurance**: alcuni clienti USA richiedono COI (Certificate of Insurance) con minimi $1M general liability / $1M cyber. Valutare polizza prima del primo deal serio.
8. **Indemnification cap**: il 12-month fees è basso per cliente enterprise; alcuni chiedono uncapped per IP infringement. Negoziabile caso per caso.
