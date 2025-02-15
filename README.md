# J. POO Morgan Chase & Co. - Etapa 2

## Student realizator
- **Plesa Marian-Cosmin**


## Arhitectura Proiectului

### Pachete

- **`account`**:
    - Găzduiește entitățile asociate conturilor bancare.
    - **Clase**:
        - `Account`: Clasa abstractă pentru conturi.
        - `AccountClassic`: Implementare pentru conturile clasice.
        - `AccountSavings`: Implementare pentru conturile de economii.
        - `AccountFactory`: Design pattern Factory pentru crearea dinamică a conturilor.
        - `AccountBusiness`: Implementare pentru conturile business.

- **`Components`**:
    - Entități principale folosite în operațiuni.
    - **Clase**:
        - `Bank`: Gestionarea generală a băncii.
        - `Card`: Reprezentarea cardurilor.
        - `ExchangeRate`: Gestionarea cursurilor valutare.
        - `Pair`: Pereche generică pentru mapări.
        - `User`: Reprezentarea utilizatorilor.
        - `BusinessComerciantPayment`: Reprezentarea plăților catre comercianți (pentru account business).
        - `PendingSplitPayment`: Coada de așteptare pentru plăți distribuite.
        - `Commerciant`: Reprezentarea comercianților.

- **`StrategyHandler`**:
    - Conține implementările pentru design pattern-ul Strategy, gestionând comenzile utilizatorilor.
    - **Clase**:
        - `AcceptSplitHandler`: Implementare pentru aprobare plată distribuită.
        - `AddAccountHandler`: Adăugarea unui cont.
        - `AddInterestHandler`: Încasarea dobânzii pentru conturi de economii.
        - `BusinessReportHandler`: Generarea unui raport de tipul din comanda.
        - `CashWithdrawalHandler`: Retragerea de numerar.
        - `ChangeDepositLimitHandler`: Modificarea limitelor de depunere.
        - `ChangeInterestRateHandler`: Modificarea dobânzii unui cont.
        - `ChangeSpendLimitHandler`: Modificarea limitelor de cheltuieli.
        - `CheckCardStatusHandler`: Verificarea status-ului unui card.
        - `CreateCardHandler`: Crearea unui card permanent.
        - `CreateOneTimeCardHandler`: Crearea unui card de tip „one-time pay”.
        - `DeleteAccountHandler`: Ștergerea unui cont bancar.
        - `DeleteCardHandler`: Ștergerea unui card asociat unui cont.
        - `DepositFundsHandler`: Depunerea de fonduri într-un cont.
        - `PayOnlineHandler`: Gestionarea plăților online.
        - `PrintTransactionsHandler`: Afișarea tranzacțiilor unui utilizator.
        - `PrintUsersHandler`: Afișarea tuturor utilizatorilor și conturilor asociate.
        - `ReportHandler`: Generarea de rapoarte generale.
        - `SendMoneyHandler`: Transfer de bani între conturi.
        - `SetAliasHandler`: Asignarea unui alias pentru un cont.
        - `SetMinimumBalanceHandler`: Setarea unei balanțe minime pentru un cont.
        - `SpendingsReportHandler`: Generarea unui raport de cheltuieli.
        - `SplitPaymentHandler`: Gestionarea plăților distribuite între conturi.
        - `UpgradePlanHandler`: Actualizarea planului unui cont.
        - `WithdrawSavingsHandler`: Retragerea de fonduri dintr-un cont de economii.

- **`ObserverPattern`**:
    - Implementarea design pattern-ului Observer.
    - **Clase**:
        - `Observer`: Interfața pentru observatori.
        - `Subject`: Interfața pentru subiecte.
        - `CashbackObserver`: Observator pentru debugging in privinta cashbackului
        - `ReportObserver`: Observator pentru creearea unui raport despre comerciant.

## Design Patterns

### 1. **Factory Pattern**:
- Implementat în `AccountFactory`.
- Permite crearea dinamică a tipurilor de conturi (`classic`, `savings`) pe baza input-ului.

### 2. **Strategy Pattern**:
- Gestionat prin pachetul `StrategyHandler`.
- Fiecare comandă este delegată unei clase specifice care implementează
- `CommandHandler`, oferind modularitate și ușurință în adăugarea de noi
- funcționalități.

### 3. **Singleton Pattern**:

- Implementat în `Bank` si folosit pentru a asigura o singură instanță a băncii in main.

### 4. **Observer Pattern**:

- Implementat in ObserverPattern(un pachet special) si l-am folosit pentru debugging si 
- pentru strangerea sumelor catre comercianti.

## Feedback

- O tema cu concepte foarte interesante si o complexitate buna pentru o ultima tema,
dar putea fi realizata mult mai bine. Un potential foarte mare, care s-a cam dus
pe apa sambetei.

