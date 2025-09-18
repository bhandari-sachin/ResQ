# Rescue Camp Simulation Flowchart (Top-to-Bottom)

```mermaid
flowchart TD
    START(["Survivor Arrives at Camp"]) --> ATTR["Generate Random Attributes<br>Age, Health Status, Family Size"]
    ATTR --> FAM{"Has Family?"}
    FAM -- YES --> FAMID["Assign Family Group ID<br>+ Family Size"]
    FAM -- NO --> INDIV["Assign Individual ID"]

    FAMID --> GATE["Enter Camp Gate (Family Group Together)"]
    INDIV --> GATE

    %% Health check per member
    GATE --> PER_MEMBER_HEALTH{"Check Health Status Per Member"}
    PER_MEMBER_HEALTH -- Injured --> MQ["Queue for Medical"] --> EMERG["Medical Care<br>10-15 min per injured member"] --> REG_AFTER_MED["Registration After Medical"]
    PER_MEMBER_HEALTH -- Healthy --> REG["Registration<br>3-5 min per individual"]

    %% Merge after registration
    REG_AFTER_MED --> POST_REG["Member Registered → Join Family Group"]
    REG --> POST_REG

    POST_REG --> SERVICES["Determine Services Needed"]

    %% Services (Supplies mandatory, Communication optional)
    SERVICES --> NEED{"Services Needed?"}
    NEED -- Supplies Only --> SQ["Queue for Supplies"] --> SUPPLY["Supply Distribution<br>4-7 min +1 min per Family Member"]
    NEED -- Supplies + Communication --> CQ1["Queue: Communication"] --> COMM1["Communication<br>3-6 min"] --> SQ1["Queue for Supplies"] --> SUPPLY1["Supplies<br>4-7 min +1 min per Family Member"]

    SUPPLY --> HOUSING
    SUPPLY1 --> HOUSING

    %% Housing assignment
    HOUSING --> ASSIGN{"Housing Assignment"}
    ASSIGN -- Child (<18) Alone --> CHILD["Children Home<br>5 min"]
    ASSIGN -- Adult Alone --> ADULT["Adult Housing<br>5 min"]
    ASSIGN -- Family Group --> FAMILY["Family Housing<br>8 min +1 min per Member"]

    CHILD --> SETTLED["Settled in Camp"]
    ADULT --> SETTLED
    FAMILY --> SETTLED

    %% Statistics
    SETTLED --> STATS["Record Statistics<br>Per Member & Per Family:<br>Wait Times, Total Time, Resource Usage"]
    STATS --> END(["Complete Process"])
