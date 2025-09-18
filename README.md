flowchart TD
    START(["Survivor Arrives at Camp"]) --> ATTR["Generate Random Attributes<br>Age, Health Status, Family Size"]
    ATTR --> FAM{"Has Family?"}
    FAM -- YES --> FAMID["Assign Family Group ID<br>(e.g., FAM_23)<br>+ Family Size"]
    FAM -- NO --> INDIV["Assign Individual ID<br>(e.g., IND_105)"]

    FAMID --> GATE["Enter Camp Gate (Family Group Together)"]
    INDIV --> GATE

    %% --- Health check per member ---
    GATE --> PER_MEMBER_HEALTH{"Check Health Status Per Member"}
    PER_MEMBER_HEALTH -- Injured --> MQ["Queue for Medical"] --> EMERG["Medical Care<br>10-15 min per injured member"] --> REG_AFTER_MED["Registration After Medical"]
    PER_MEMBER_HEALTH -- Healthy --> REG["Registration<br>3-5 min per individual"]

    %% --- Merge after registration ---
    REG_AFTER_MED --> POST_REG["Member Registered → Join Family Group"]
    REG --> POST_REG

    POST_REG --> SERVICES["Determine Services Needed"]

    %% --- Services (Supplies mandatory, Communication optional) ---
    SERVICES --> NEED{"Services Needed?"}
    NEED -- Supplies Only --> SQ["Queue for Supplies"] --> SUPPLY["Supply Distribution<br>4-7 min +1 min per Family Member"]
    NEED -- Supplies + Communication --> CQ1["Queue: Communication"] --> COMM1["Communication<br>3-6 min"] --> SQ1["Queue for Supplies"] --> SUPPLY1["Supplies<br>4-7 min +1 min per Family Member"]

    SUPPLY --> HOUSING
    SUPPLY1 --> HOUSING

    HOUSING --> ASSIGN{"Housing Assignment"}
    ASSIGN -- Child (<18) Alone --> CHILD["Children Home<br>5 min"]
    ASSIGN -- Adult Alone --> ADULT["Adult Housing<br>5 min"]
    ASSIGN -- Family Group (FAMID) --> FAMILY["Family Housing<br>8 min +1 min per Member"]

    CHILD --> SETTLED["Settled in Camp"]
    ADULT --> SETTLED
    FAMILY --> SETTLED

    SETTLED --> STATS["Record Statistics<br>Per Member & Per Family:<br>Wait Times, Total Time, Resource Usage"]
    STATS --> END(["Complete Process"])

    %% --- Styles ---
    classDef startEnd fill:#2c3e50,stroke:#34495e,stroke-width:3px,color:#fff
    classDef process fill:#3498db,stroke:#2980b9,stroke-width:2px,color:#fff
    classDef decision fill:#e74c3c,stroke:#c0392b,stroke-width:2px,color:#fff
    classDef emergency fill:#e67e22,stroke:#d35400,stroke-width:3px,color:#fff
    classDef service fill:#27ae60,stroke:#229954,stroke-width:2px,color:#fff
    classDef housing fill:#9b59b6,stroke:#8e44ad,stroke-width:2px,color:#fff

    %% --- Assign Classes ---
    START:::startEnd
    ATTR:::process
    FAM:::decision
    FAMID:::process
    INDIV:::process
    GATE:::process
    PER_MEMBER_HEALTH:::decision
    MQ:::process
    EMERG:::emergency
    REG_AFTER_MED:::process
    REG:::process
    POST_REG:::process
    SERVICES:::service
    NEED:::decision
    SQ:::process
    SUPPLY:::process
    CQ1:::process
    COMM1:::process
    SQ1:::process
    SUPPLY1:::process
    HOUSING:::housing
    ASSIGN:::decision
    CHILD:::housing
    ADULT:::housing
    FAMILY:::housing
    SETTLED:::housing
    STATS:::process
    END:::startEnd
