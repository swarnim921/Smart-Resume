# TalentSync Architecture Diagrams

As requested by the mentor, here are the architectural diagrams outlining the system's database structure, data flow, and workflow models. These diagrams utilize Mermaid.js.

## 1. Database Diagram (DBD)

The Database Diagram outlines the primary entities stored in MongoDB and their relationships. Since MongoDB is NoSQL, these relationships represent logical links (e.g., ObjectIds) rather than strict foreign keys.

```mermaid
erDiagram
    USERS ||--o{ RESUMES : owns
    USERS ||--o{ JOBS : posts
    USERS ||--o{ APPLICATIONS : submits
    JOBS ||--o{ APPLICATIONS : receives
    JOBS ||--o{ INTERVIEW_PANELS : configures

    USERS {
        ObjectId _id PK
        String name
        String email
        String password
        String role "USER | RECRUITER"
        String authProvider
        List skills
        Number yearsOfExperience
    }

    RESUMES {
        ObjectId _id PK
        ObjectId ownerId FK
        String filename
        String contentType
        String fileType "CANDIDATE_RESUME | JD_DOC"
        String gridFsId "Reference to GridFS chunks"
    }

    JOBS {
        ObjectId _id PK
        ObjectId postedBy FK
        String title
        String company
        String description
        String requirements
        String status "ACTIVE | CLOSED"
        Date createdAt
    }

    APPLICATIONS {
        ObjectId _id PK
        ObjectId jobId FK
        ObjectId userId FK
        ObjectId resumeId FK
        Number matchScore
        List missingSkills
        String status "APPLIED | ATS_REJECTED | UNDER_REVIEW | INTERVIEW | OFFERED | REJECTED"
        Date appliedAt
    }

    INTERVIEW_PANELS {
        ObjectId _id PK
        ObjectId jobId FK
        String roundName
        List interviewerEmails
        List slots
    }
```

## 2. Data Flow Diagram (DFD Level 3) - Enterprise Batch Screening

This Level 3 DFD illustrates the deep technical flow of data during the B2B Many-to-1 resume screening process.

```mermaid
graph TD
    subgraph External Entities
        R[Recruiter]
    end

    subgraph "Process 1: Upload & Initial Storage"
        P1[1.1 Store JD & Resumes]
        G[GridFS Storage]
    end

    subgraph "Process 2: File Parsing Engine"
        P2A[2.1 Detect MIME Type]
        P2B[2.2 Extract TXT]
        P2C[2.3 Extract PDF via PDFBox]
        P2D[2.4 Extract Image via Tesseract OCR]
        P2E[2.5 Consolidate Text]
    end

    subgraph "Process 3: ML Transformer Analysis"
        P3A[3.1 BERT Tokenization]
        P3B[3.2 Cosine Similarity Match]
        P3C[3.3 Skill Gap Extraction]
    end

    subgraph "Process 4: Aggregation & Response"
        P4A[4.1 Rank Candidates]
        P4B[4.2 Render Dashboard]
    end

    %% Data Flows
    R -- "MultipartFiles (JD + Resumes)" --> P1
    P1 -- "Byte Streams" --> G
    P1 -- "GridFS Object IDs" --> P2A
    
    P2A -- "text/plain" --> P2B
    P2A -- "application/pdf" --> P2C
    P2A -- "image/*" --> P2D
    
    P2B -- "Raw Text" --> P2E
    P2C -- "Parsed Text" --> P2E
    P2D -- "OCR Text" --> P2E
    
    P2E -- "JSON Payload (1 JD : N Resumes)" --> P3A
    P3A -- "Vectors" --> P3B
    P3A -- "Tokens" --> P3C
    
    P3B -- "Match Scores" --> P4A
    P3C -- "Missing Skills" --> P4A
    
    P4A -- "Ranked JSON Array" --> P4B
    P4B -- "Display UI" --> R
```

## 3. Workflow Block Diagram - Many-to-1 Processing

This diagram shows the high-level workflow of the system from a user's perspective, highlighting the B2B Service aspect.

```mermaid
flowchart LR
    Start([Recruiter Logs In]) --> Auth{Authenticated?}
    Auth -- No --> Login(Redirect to Login)
    Auth -- Yes --> Dash(Recruiter Dashboard)
    
    Dash --> Tab(Select 'Enterprise Batch' Tab)
    Tab --> UploadJD[Upload Primary Job Description File]
    UploadJD --> UploadRes[Upload Batch of N Resumes]
    
    UploadRes --> Run(Click 'Run Batch Screening')
    
    subgraph "TalentSync Core API"
        Run --> ParseJD(Parse JD File)
        ParseJD --> ParseRes(Parse N Resume Files)
        ParseRes --> SendML(Dispatch Payload to ML Microservice)
    end
    
    subgraph "Python ML Service"
        SendML --> Analyze(Deep Learning Transformer Model)
        Analyze --> Rank(Sort Candidates by Fit)
    end
    
    Rank --> ReturnData(Return Ranked Array to Frontend)
    
    ReturnData --> DisplayUI{Are Candidates Parsed?}
    DisplayUI -- Yes --> ShowTable(Display Interactive Ranking Table)
    ShowTable --> End([End Workflow])
```
