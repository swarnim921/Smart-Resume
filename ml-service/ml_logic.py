import re
import json
import torch
import os
from functools import lru_cache
from sentence_transformers import SentenceTransformer, util

# Limit PyTorch threads to save memory and CPU churn
torch.set_num_threads(1)

# ==========================================
# 1. INITIALIZATION & CONFIGURATION
# ==========================================

print("⏳ Loading SentenceTransformer model...")
model = SentenceTransformer('all-MiniLM-L6-v2')
# Warm-up call to eliminate first-request latency
model.encode("warmup")
print("✅ Model loaded and warmed up.")

def load_skill_database():
    """
    Loads tech and soft skills from an external JSON file.
    """
    db_path = os.path.join(os.path.dirname(__file__), 'skills_database.json')
    try:
        with open(db_path, 'r') as f:
            data = json.load(f)
        return set(data.get("technical_skills", [])), set(data.get("soft_skills", []))
    except Exception as e:
        print(f"❌ Error loading skills database: {e}")
        # Fallback to minimal set if file missing
        return {"python", "java"}, {"communication"}

TECH_SKILLS, SOFT_SKILLS = load_skill_database()
ALL_SKILLS = TECH_SKILLS.union(SOFT_SKILLS)

def get_skill_aliases():
    """
    Mapping of shorthand or alternative names to standardized skill names.
    """
    return {
        "js": "javascript", "ts": "typescript", "gcp": "google cloud",
        "aws": "amazon web services", "ml": "machine learning",
        "dl": "deep learning", "nlp": "natural language processing",
        "ci": "ci/cd", "cd": "ci/cd", "kube": "kubernetes",
        "k8s": "kubernetes", "postgres": "postgresql", "mongo": "mongodb",
        "net": ".net", "dotnet": ".net", "ai": "artificial intelligence"
    }

SKILL_ALIASES = get_skill_aliases()

def get_implications():
    """
    Rules to infer skills. If a resume has 'react', it implies 'javascript'.
    """
    return {
        "oop": ["java", "c++", "c#", "python", "ruby"],
        "object oriented": ["java", "c++", "c#", "python", "ruby"],
        "cloud": ["aws", "azure", "gcp"],
        "frontend": ["react", "angular", "vue", "html", "css", "javascript"],
        "backend": ["node.js", "django", "spring", "flask"],
        "database": ["sql", "mysql", "postgresql", "mongodb", "oracle"],
        "ci/cd": ["jenkins", "gitlab", "github actions"],
        "devops": ["docker", "kubernetes", "jenkins", "terraform"]
    }

IMPLICATION_RULES = get_implications()

# ==========================================
# 2. HELPER FUNCTIONS
# ==========================================

def tokenize_text(text):
    """
    Industry-Grade Optimization: 
    1. Lowercase & Sanitize
    2. Special Regex for C++, C#, .NET
    3. Unigram, Bigram, and Trigram generation.
    4. Alias expansion into the Hash Set.
    """
    text_lower = text.lower().replace("-", " ")
    
    # Advanced tokenization: capture words and common technical symbols (+, #, .)
    words = re.findall(r'(?:\.\w+)|(?:\w+[+#]{1,2})|(?:\w+)', text_lower)
    
    tokens = set(words)
    
    # Generate bi-grams
    for i in range(len(words) - 1):
        tokens.add(f"{words[i]} {words[i+1]}")
        
    # Generate tri-grams
    for i in range(len(words) - 2):
        tokens.add(f"{words[i]} {words[i+1]} {words[i+2]}")
        
    # Alias Expansion
    added_via_alias = set()
    for token in tokens:
        if token in SKILL_ALIASES:
            added_via_alias.add(SKILL_ALIASES[token])
            
    return tokens.union(added_via_alias), text_lower

def calculate_keyword_score(resume_text, jd_text):
    """
    Calculates the percentage of JD keywords present in the Resume using Set Intersections.
    """
    resume_tokens, _ = tokenize_text(resume_text)
    jd_tokens, jd_str = tokenize_text(jd_text)
    
    # Identify skills required in JD
    required_skills = {s for s in ALL_SKILLS if s in jd_tokens or (len(s.split()) > 3 and s in jd_str)}

    if not required_skills:
        return 0.0

    matched_count = 0
    for skill in required_skills:
        if skill in resume_tokens:
            matched_count += 1
        elif skill in IMPLICATION_RULES:
            for evidence in IMPLICATION_RULES[skill]:
                if evidence in resume_tokens:
                    matched_count += 1
                    break
                    
    return (matched_count / len(required_skills)) * 100

def identify_gaps(resume_text, jd_text):
    """
    Identifies specific skills found in JD but missing in Resume.
    """
    resume_tokens, _ = tokenize_text(resume_text)
    jd_tokens, jd_str = tokenize_text(jd_text)
    
    missing_tech = []
    missing_soft = []

    def check_category(category_set, output_list):
        required = {s for s in category_set if s in jd_tokens or (len(s.split()) > 3 and s in jd_str)}

        for skill in required:
            if skill not in resume_tokens:
                # Check for implication
                found_implied = False
                if skill in IMPLICATION_RULES:
                    for evidence in IMPLICATION_RULES[skill]:
                        if evidence in resume_tokens:
                            found_implied = True
                            break
                if not found_implied:
                    output_list.append(skill.title())

    check_category(TECH_SKILLS, missing_tech)
    check_category(SOFT_SKILLS, missing_soft)
    
    return missing_tech, missing_soft

# ==========================================
# 3. MAIN API FUNCTIONS
# ==========================================

def extract_skills(resume_text):
    """
    Extracts explicit skills and estimates experience from text.
    """
    resume_tokens, resume_str = tokenize_text(resume_text)
    found_tech = [s.title() for s in TECH_SKILLS if s in resume_tokens]
    found_soft = [s.title() for s in SOFT_SKILLS if s in resume_tokens]
            
    # Experience Detection (Regex remains optimal)
    experience = "Not specified"
    exp_pattern = r'(\d+(\+)?)\s*(years?|yrs?)'
    matches = re.findall(exp_pattern, resume_str)
    if matches:
        try:
            years = [int(m[0].replace('+', '')) for m in matches]
            experience = f"{max(years)} years detected"
        except: pass
            
    return {
        "technical_skills": found_tech,
        "soft_skills": found_soft,
        "experience_detected": experience
    }

@lru_cache(maxsize=128)
def get_embedding(text):
    """
    Performance Optimization: Caches embeddings to avoid redundant
    GPU/CPU compute for the same text (especially Job Descriptions).
    """
    with torch.no_grad():
        return model.encode(text, convert_to_tensor=True)

def recommend_courses(current_skills, target_skills):
    """
    Generates course recommendations based on missing skills.
    """
    recommendations = []
    for skill in target_skills:
        skill_clean = skill.strip().title()
        recommendations.append({
            "skill": skill_clean,
            "course_name": f"Mastering {skill_clean}: From Zero to Hero",
            "platform": "Coursera / Udemy / EdX",
            "query_link": f"https://www.coursera.org/search?query={skill_clean}"
        })
    return recommendations

def predict_job_role(resume_text):
    """
    Advanced Industry Feature: Predicts the most likely job role based on detected skills.
    Uses categorical skill density to classify the candidate.
    """
    tokens, _ = tokenize_text(resume_text)
    
    # Role definitions based on skill clusters
    role_weights = {
        "Backend Developer": ["java", "spring boot", "node.js", "express.js", "nestjs", "django", "flask", "postgresql", "mysql", "microservices", "api gateway"],
        "Frontend Developer": ["react", "angular", "vue", "javascript", "typescript", "html5", "css3", "tailwind css", "redux", "next.js", "bootstrap"],
        "Full Stack Developer": ["react", "node.js", "javascript", "java", "spring boot", "databases", "rest api", "html", "css"],
        "Data Scientist": ["python", "pandas", "numpy", "scikit-learn", "machine learning", "tensorflow", "pytorch", "nlp", "deep learning", "r"],
        "DevOps Engineer": ["docker", "kubernetes", "jenkins", "terraform", "ansible", "aws", "azure", "gcp", "ci/cd", "yaml", "github actions"],
        "Mobile Developer": ["flutter", "react native", "android", "ios", "swift", "kotlin", "dart", "xcode", "android studio"],
        "QA Engineer": ["selenium", "cypress", "jest", "unit testing", "playwright", "test automation", "mocha", "junit", "testing tools"]
    }
    
    role_scores = {}
    for role, cluster in role_weights.items():
        score = sum(1 for skill in cluster if skill in tokens)
        role_scores[role] = score
        
    # Get the top role with at least some matches
    predicted_role = max(role_scores, key=role_scores.get) if any(role_scores.values()) else "Software Engineer"
    
    # If it's a tie or low confidence, default to Software Engineer or return top 1
    return predicted_role

def analyze_resume_job_match(resume_text, job_description):
    """
    Industry-grade hybrid match score: 50% Semantic, 30% Keyword, 20% Skill Overlap.
    Optimized for ~300ms latency using embedding caching.
    """
    # 1. Semantic (Cosine Similarity) - Uses cached embeddings
    resume_embedding = get_embedding(resume_text)
    jd_embedding = get_embedding(job_description)
    semantic_score = util.cos_sim(resume_embedding, jd_embedding).item() * 100

    # 2. Keyword Score (Exact + Implied)
    keyword_score = calculate_keyword_score(resume_text, job_description)

    # 3. Skill Overlap (Direct intersection percentage)
    resume_tokens, _ = tokenize_text(resume_text)
    jd_tokens, jd_str = tokenize_text(job_description)
    jd_skills = {s for s in ALL_SKILLS if s in jd_tokens or (len(s.split()) > 3 and s in jd_str)}
    
    overlap_score = 0.0
    if jd_skills:
        matched_skills = {s for s in jd_skills if s in resume_tokens}
        overlap_score = (len(matched_skills) / len(jd_skills)) * 100

    # Weighted Calculation
    final_score = (semantic_score * 0.50) + (keyword_score * 0.30) + (overlap_score * 0.20)
    
    missing_tech, missing_soft = identify_gaps(resume_text, job_description)
    extracted = extract_skills(resume_text)
    
    # Matched skills for display
    display_matched = [s.title() for s in jd_skills if s in resume_tokens]

    return {
        "match_percentage": round(final_score, 1),
        "semantic_score": round(semantic_score, 1),
        "keyword_score": round(keyword_score, 1),
        "skill_overlap": round(overlap_score, 1),
        "matched_skills": display_matched,
        "missing_technical_skills": missing_tech,
        "missing_soft_skills": missing_soft
    }
