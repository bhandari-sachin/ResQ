
# Requirements for the Simulation

- **Conceptual Model**  
  - At least 4 different service points  
  - Service point network cannot be a straight line  
  - Several paths through the system  

- **Distributions**  
  - Possible to change distributions  
  - Possible to modify parameters as inputs  

- **User Interface (JavaFX)**  
  - Graphical interface for inputs and outputs  
  - Simulation run can be visualized or animated  
  - Interface suitable for general use (fonts, colors, usability)  

- **Simulation Control**  
  - Operates autonomously once started  
  - User can influence during runtime:  
    - Slow down  
    - Speed up  
    - Step through execution  

- **Data Repositories**  
  - File-based solution  
  - Database solution  

- **Added Value**  
  - Implementation of variability  





```mermaid
graph TD
    A[Survivor Arrives] --> B[Generate Attributes]
    B --> C[Enter Gate]
    C --> D{Health Check}
    D -->|Injured| E[Medical Queue]
    D -->|Healthy| F[Registration]
    E --> G[Medical Care]
    G --> H[Registration]
    F --> I[Registered]
    H --> I
    I --> J{Services Needed}
    J -->|Supplies Only| K[Supply Queue]
    J -->|Both| L[Communication Queue]
    K --> M[Get Supplies]
    L --> N[Communication]
    N --> O[Supply Queue]
    O --> P[Get Supplies]
    M --> Q{Age Check}
    P --> Q
    Q -->|Child| R[Child Housing]
    Q -->|Adult| S[Adult Housing]
    R --> T[Settled]
    S --> T
    T --> U[Record Stats]
    U --> V[Complete]
```
