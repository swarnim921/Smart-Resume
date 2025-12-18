import re
import pandas as pd
from sentence_transformers import SentenceTransformer, util

# ==========================================
# 1. INITIALIZATION & CONFIGURATION
# ==========================================

# Load the model globally so it's only loaded once when the Flask app starts
print("⏳ Loading SentenceTransformer model...")
model = SentenceTransformer('all-MiniLM-L6-v2')
print("✅ Model loaded.")

def get_skill_taxonomies():
    """
    Returns sets of technical and soft skills for matching.
    """
    tech_skills = {
        "python", "java", "c++", "c#", "javascript", "typescript", "ruby", "php", "swift", "kotlin",
        "react", "angular", "vue", "node.js", "django", "flask", "spring boot", "dotnet",
        "html", "css", "sql", "nosql", "mysql", "postgresql", "mongodb", "oracle",
        "aws", "azure", "gcp", "docker", "kubernetes", "jenkins", "terraform", "git", "github",
        "gitlab", "jira", "confluence", "tableau", "powerbi", "excel", "figma",
        "machine learning", "deep learning", "nlp", "tensorflow", "pytorch", "scikit-learn",
        "oop", "object oriented", "functional programming", "rest api", "graphql", "ci/cd",
        "devops", "agile", "scrum", "kanban", "sdlc", "data structures", "algorithms",
        "linux", "unix", "bash", "shell"
    }
    soft_skills = {
        "communication", "leadership", "teamwork", "problem solving", "critical thinking",
        "time management", "adaptability", "collaboration", "creativity", "emotional intelligence",
        "negotiation", "conflict resolution", "decision making", "mentoring", "presentation",
        "active listening", "flexibility", "work ethic", "detail oriented", "stakeholder management"
    }
    return tech_skills, soft_skills

# Initialize taxonomies
TECH_SKILLS, SOFT_SKILLS = get_skill_taxonomies()
ALL_SKILLS = TECH_SKILLS.union(SOFT_SKILLS)

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

def calculate_keyword_score(resume_text, jd_text):
    """
    Calculates the percentage of JD keywords present in the Resume.
    """
    jd_lower = jd_text.lower()
    resume_lower = resume_text.lower()
    
    # 1. Identify skills explicitly required in JD
    required_skills = set()
    for skill in ALL_SKILLS:
        pattern = r'\b' + re.escape(skill) + r'\b'
        if re.search(pattern, jd_lower):
            required_skills.add(skill)

    if not required_skills:
        return 0.0

    # 2. Check if required skills exist in Resume (or are implied)
    matched_skills = 0
    for skill in required_skills:
        pattern = r'\b' + re.escape(skill) + r'\b'
        if re.search(pattern, resume_lower):
            matched_skills += 1
        elif skill in IMPLICATION_RULES:
            # Check implications (e.g., if JD asks for 'OOP' and Resume has 'Java')
            for evidence in IMPLICATION_RULES[skill]:
                evidence_pattern = r'\b' + re.escape(evidence) + r'\b'
                if re.search(evidence_pattern, resume_lower):
                    matched_skills += 1
                    break
                    
    return (matched_skills / len(required_skills)) * 100

def identify_gaps(resume_text, jd_text):
    """
    Identifies specific skills found in JD but missing in Resume.
    """
    resume_lower = resume_text.lower()
    jd_lower = jd_text.lower()
    missing_tech = []
    missing_soft = []

    def check_category(category_set, output_list):
        # Find what JD needs
        required = set()
        for skill in category_set:
            pattern = r'\b' + re.escape(skill) + r'\b'
            if re.search(pattern, jd_lower):
                required.add(skill)

        # Check if Resume has it
        for skill in required:
            pattern = r'\b' + re.escape(skill) + r'\b'
            if not re.search(pattern, resume_lower):
                # Check for implication before declaring it missing
                found_implied = False
                if skill in IMPLICATION_RULES:
                    for evidence in IMPLICATION_RULES[skill]:
                        evidence_pattern = r'\b' + re.escape(evidence) + r'\b'
                        if re.search(evidence_pattern, resume_lower):
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
    resume_lower = resume_text.lower()
    found_tech = []
    found_soft = []
    
    # Extract Tech Skills
    for skill in TECH_SKILLS:
        pattern = r'\b' + re.escape(skill) + r'\b'
        if re.search(pattern, resume_lower):
            found_tech.append(skill.title())
            
    # Extract Soft Skills
    for skill in SOFT_SKILLS:
        pattern = r'\b' + re.escape(skill) + r'\b'
        if re.search(pattern, resume_lower):
            found_soft.append(skill.title())
            
    # Extract Experience (Heuristic)
    experience = "Not specified"
    exp_pattern = r'(\d+(\+)?)\s*(years?|yrs?)'
    matches = re.findall(exp_pattern, resume_lower)
    if matches:
        try:
            years = [int(m[0].replace('+', '')) for m in matches]
            experience = f"{max(years)} years detected"
        except:
            pass
            
    return {
        "technical_skills": found_tech,
        "soft_skills": found_soft,
        "experience_detected": experience
    }

def analyze_resume_job_match(resume_text, job_description):
    """
    Computes a hybrid match score (Semantic + Keyword) and identifies gaps.
    """
    # 1. Semantic Score (Cosine Similarity)
    resume_embedding = model.encode(resume_text, convert_to_tensor=True)
    jd_embedding = model.encode(job_description, convert_to_tensor=True)
    semantic_score = util.cos_sim(resume_embedding, jd_embedding).item() * 100

    # 2. Keyword Score (Exact Matching with Implications)
    keyword_score = calculate_keyword_score(resume_text, job_description)

    # 3. Final Weighted Score (70% Semantic, 30% Keyword)
    final_score = (semantic_score * 0.70) + (keyword_score * 0.30)
    
    # 4. Identify Skill Gaps
    missing_tech, missing_soft = identify_gaps(resume_text, job_description)
    
    # 5. Extract matched skills for display
    extracted = extract_skills(resume_text)
    all_resume_skills = set([s.lower() for s in extracted['technical_skills'] + extracted['soft_skills']])
    
    matched_skills = []
    jd_lower = job_description.lower()
    for skill in all_resume_skills:
        if skill in jd_lower:
            matched_skills.append(skill.title())

    return {
        "match_percentage": round(final_score, 1),
        "semantic_score": round(semantic_score, 1),
        "keyword_score": round(keyword_score, 1),
        "matched_skills": matched_skills,
        "missing_technical_skills": missing_tech,
        "missing_soft_skills": missing_soft
    }

def recommend_courses(current_skills, target_skills):
    """
    Generates course recommendations based on missing skills.
    """
    recommendations = []
    
    for skill in target_skills:
        skill_clean = skill.strip().title()
        rec = {
            "skill": skill_clean,
            "course_name": f"Mastering {skill_clean}: From Zero to Hero",
            "platform": "Coursera / Udemy / EdX",
            "query_link": f"https://www.coursera.org/search?query={skill_clean}"
        }
        recommendations.append(rec)
        
    return recommendations
