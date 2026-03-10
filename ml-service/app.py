from flask import Flask, request, jsonify
from flask_cors import CORS
import ml_logic

app = Flask(__name__)
CORS(app)  # Enable CORS for Spring Boot backend

@app.route('/', methods=['GET'])
def index():
    """Root endpoint to show service status and endpoints"""
    return jsonify({
        "service": "TalentSync ML Microservice",
        "version": "1.2.0 (Industry-Grade)",
        "status": "Running",
        "optimization": "O(N) Tokenization + Hybrid Scoring",
        "endpoints": {
            "health": "/health",
            "analyze": "/api/ml/analyze",
            "extract_skills": "/api/ml/extract-skills",
            "batch_analyze": "/api/ml/batch-analyze",
            "recommend_courses": "/api/ml/recommend-courses"
        },
        "message": "Service is live and ready for production."
    }), 200

@app.route('/health', methods=['GET'])
def health():
    """Health check endpoint"""
    return jsonify({"status": "UP"}), 200

@app.route('/api/ml/analyze', methods=['POST'])
def analyze():
    """Analyze resume-job match using real ML model"""
    try:
        data = request.json
        resume_text = data.get('resumeText', '')
        job_description = data.get('jobDescription', '')
        
        if not resume_text or not job_description:
            return jsonify({"error": "Missing resumeText or jobDescription"}), 400
        
        # Use real ML logic
        result = ml_logic.analyze_resume_job_match(resume_text, job_description)
        
        # Transform to match backend expected format
        response = {
            "matchScore": result["match_percentage"],
            "skillsMatched": result["matched_skills"],
            "skillsGap": result["missing_technical_skills"] + result["missing_soft_skills"],
            "experienceMatch": "Good" if result["match_percentage"] >= 70 else "Fair" if result["match_percentage"] >= 50 else "Poor",
            "recommendations": ml_logic.recommend_courses([], result["missing_technical_skills"][:3]),  # Top 3 gaps
            "confidence": result["semantic_score"] / 100
        }
        
        return jsonify(response), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/ml/extract-skills', methods=['POST'])
def extract_skills():
    """Extract skills from resume using real ML model"""
    try:
        data = request.json
        resume_text = data.get('resumeText', '')
        
        if not resume_text:
            return jsonify({"error": "Missing resumeText"}), 400
        
        # Use real ML logic
        result = ml_logic.extract_skills(resume_text)
        
        # Transform to match backend expected format
        response = {
            "technicalSkills": result["technical_skills"],
            "softSkills": result["soft_skills"],
            "experience": result["experience_detected"],
            "education": "Not extracted",  # ML model doesn't extract education
            "certifications": []  # ML model doesn't extract certifications
        }
        
        return jsonify(response), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/ml/recommend-courses', methods=['POST'])
def recommend_courses():
    """Get course recommendations based on skill gaps"""
    try:
        data = request.json
        current_skills = data.get('currentSkills', [])
        target_skills = data.get('targetSkills', [])
        
        # Use real ML logic
        recommendations = ml_logic.recommend_courses(current_skills, target_skills)
        
        # Transform to match backend expected format
        response = {
            "recommendations": [
                {
                    "courseName": rec["course_name"],
                    "provider": rec["platform"],
                    "duration": "Self-paced",
                    "priority": "High" if i == 0 else "Medium",
                    "reason": f"Learn {rec['skill']} to close skill gap"
                }
                for i, rec in enumerate(recommendations)
            ]
        }
        
        return jsonify(response), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/ml/batch-analyze', methods=['POST'])
def batch_analyze():
    """Batch process multiple applications"""
    try:
        data = request.json
        job_id = data.get('jobId')
        applications = data.get('applications', [])
        
        if not applications:
            return jsonify({"error": "No applications provided"}), 400
        
        # Process each application with real ML
        results = []
        for app in applications:
            resume_text = app.get('resumeText', '')
            job_description = app.get('jobDescription', '')
            
            if resume_text and job_description:
                analysis = ml_logic.analyze_resume_job_match(resume_text, job_description)
                results.append({
                    "applicationId": app.get('applicationId'),
                    "matchScore": analysis["match_percentage"],
                    "rank": 0  # Will be set after sorting
                })
        
        # Sort by match score and assign ranks
        results.sort(key=lambda x: x["matchScore"], reverse=True)
        for i, result in enumerate(results):
            result["rank"] = i + 1
        
        response = {
            "results": results
        }
        
        return jsonify(response), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    print("🚀 ML Service starting on http://localhost:5000")
    print("📊 Endpoints available:")
    print("  - GET  /health")
    print("  - POST /api/ml/analyze")
    print("  - POST /api/ml/extract-skills")
    print("  - POST /api/ml/recommend-courses")
    print("  - POST /api/ml/batch-analyze")
    print("✅ Using real ML model (SentenceTransformer)")
    app.run(host='0.0.0.0', port=5000, debug=False)