## 1. Literature Review: The Architectural Evolution

To explain the "black box," the literature review maps the transition from a highly accurate but computationally heavy baseline (**Model X**) to a scalable, production-ready vector architecture (**Model Y**).

* **The Baseline Approach (Model X): Vanilla Cross-Encoder**
* *Mechanism:* The system takes a Candidate Resume ($R$) and a Job Description ($D$) and feeds them simultaneously into the self-attention layers of a standard Transformer network.
* *The Bottleneck:* While highly accurate, the time complexity scales multiplicatively at $\mathcal{O}(N \times M)$ (where $N$ is resumes and $M$ is JDs). Running a batch of 100 resumes against 20 JDs required 2,000 separate, heavy forward-inference passes through the deep neural network. The system would bottleneck, suffer extreme latency, and crash the server.


* **The Modified Architecture (Model Y): Dual-Stream Bi-Encoder**
* *Mechanism:* You modified the "black box" by decoupling the inputs using **Sentence-BERT (SBERT)** architecture. Stream A encodes all Resumes into structured vector pools independently. Stream B encodes all JDs.
* *The Achievement:* The computational complexity drops from multiplication to linear addition: $\mathcal{O}(N + M)$. For that same batch of 100 resumes and 20 JDs, the transformer model executes only 120 times. The actual matching is handled instantaneously on the CPU/GPU via **Cosine Similarity** ($\text{sim}(\mathbf{u}, \mathbf{v}) = \frac{\mathbf{u} \cdot \mathbf{v}}{\|\mathbf{u}\| \|\mathbf{v}\|}$). You achieved high-speed semantic matching without sacrificing contextual intelligence.



> **Key insight:** The widget above visualizes the exact reason your architectural pivot matters. As your portal scales, the Bi-Encoder approach ensures server costs and wait times remain minimal.

---

## 2. Literature Review: Candidate Models Matrix

Your system operates in three distinct phases. Here are the candidate models evaluated for each pipeline stage:

| Pipeline Stage | Candidate Models Evaluated | Selected Model & Rationale |
| --- | --- | --- |
| **Extraction** (Files to Raw Text) | Regex Parsers, Tesseract OCR, PyMuPDF, pdfminer.six | **Hybrid Approach:** `PyMuPDF` for native text/PDF layouts (preserves columns), and **Tesseract OCR** for scanned image resumes (`.jpg`, `.png`). |
| **Analysis** (Text to Structured Data) | Keyword string matching, Vanilla NLTK, spaCy Core Web | **spaCy (`en_core_web_md`) + Custom NER:** Chosen for its lightweight speed and ability to recognize named entities (Skills, Years of Experience) contextually. |
| **Matching** (Scoring Logic) | TF-IDF (Term Frequency), Vanilla BERT, Sentence Transformers | **SBERT (`all-MiniLM-L6-v2`)**: Chosen because it maps sentences to a dense vector space optimized specifically for fast cosine similarity comparisons. |

---

## 3. Workflow Block Diagram

This block diagram traces the data flow from multi-format ingestion through the decoupled NLP pipelines and into the frontend UI.

```text
+----------------------------------------------------------------------------+
|                            INPUT INGESTION POOL                            |
|    [ .txt files ]             [ .pdf files ]             [ Image scans ]   |
+----------------------------------------------------------------------------+
       |                           |                            |
       v                           v                            v
 [ Direct Read ]            [ Layout Analysis ]          [ OCR Engine ]
 (Native Buffer)            (PyMuPDF Stream)             (Tesseract/Paddle)
       |                           |                            |
       +---------------------------+----------------------------+
                                   |
                                   v Clean String Data
+----------------------------------------------------------------------------+
|                     ANALYSIS LAYER: SPACY NLP & NER                        |
|   * Tokenization & Stopword Stripping     * Entity Recognition (NER)       |
|   * Section Classification (Skills)       * Junk Terminology Filter        |
+----------------------------------------------------------------------------+
                                   |
                         +---------+---------+
                         |                   |
                         v Resumes           v Job Descriptions 
+----------------------------------------------------------------------------+
|                  THE MODIFIED BLACK BOX (Bi-Encoder)                       |
|                                                                            |
|   +-------------------------------+     +-------------------------------+  |
|   |   Transformer Block Stream A  |     |   Transformer Block Stream B  |  |
|   |        (Resume Encoder)       |     |          (JD Encoder)         |  |
|   +-------------------------------+     +-------------------------------+  |
|                   |                                     |                  |
|                   v Resume Vectors                      v JD Vectors       |
+----------------------------------------------------------------------------+
                    |                                     |
                    +------------------+------------------+
                                       |
                                       v
                     +-----------------------------------+
                     |      COSINE SIMILARITY ENGINE     |
                     |   (Matrix Math Multi-Matching)    |
                     +-----------------------------------+
                                       |
                                       v Scores Matrix
                     +-----------------------------------+
                     |       LIVELY FRONTEND UI          |
                     |  (Dashboards, Leaderboards, KPIs) |
                     +-----------------------------------+

```

---

## 4. Feature List

* **Multi-Format File Support:** Robust extraction pipeline handling unstructured `.txt`, multi-column `.pdf` formatting, and scanned image (`.jpg`/`.png`) documents.
* **Dynamic Matrix Matching:**
* *Many-to-1:* Batch applicant ranking for recruiters.
* *1-to-Many:* Career pathfinder matching for students against open JDs.


* **Role-Based Login System:** Secure authentication separating standard applicant views from recruiter/admin operations.
* **Interactive Analytics Dashboard:** Real-time data visualization showing match percentages, missing skill gaps, and automated shortlists.

---

## 5. Coverage & Reduction of Junk Data

A matching algorithm is only as good as its data hygiene. To ensure coverage while eliminating noise, the Analysis pipeline employs strict filtering logic:

1. **Stopword & Fluff Stripping:** Traditional parsers get confused by conversational filler (e.g., *"I am a highly motivated individual seeking a challenging position..."*). The spaCy preprocessing layer strips these out, retaining only actionable noun chunks (tools, languages, frameworks).
2. **Negation Handling:** The system identifies negative contexts. If a resume states *"Familiar with Docker but no production experience,"* the parser flags it to prevent a false-positive match against a JD demanding a *"Senior Docker Engineer."*
3. **Entity Standardization (Taxonomy):** Reduces fragmentation by mapping variations of a skill to a single core token (e.g., mapping *"ReactJS"*, *"React.js"*, and *"React"* all to the unified vector ID `react`).
