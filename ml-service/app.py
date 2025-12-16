from flask import Flask, request, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)  # Enable CORS for Spring Boot backend

@app.route('/health', methods=['GET'])
def health():
    """Health check endpoint"""
    return jsonify({"status": "UP"}), 200

@app.route('/api/ml/analyze', methods=['POST'])
def analyze():
    """Analyze resume-job match"""
    data = request.json
    resume_text = data.get('resumeText', '')
    job_description = data.get('jobDescription', '')
    
    # TODO: Implement ML logic here
    # For now, return mock data
    result = {
        "matchScore": 85.5,
        "skillsMatched": ["Python", "Java", "React"],
        "skillsGap": ["AWS", "Docker"],
        "experienceMatch": "Good",
        "recommendations": [
            {
                "courseName": "AWS Certified Developer",
                "provider": "Udemy",
                "duration": "30 hours",
                "priority": "High",
                "reason": "Missing AWS skills for cloud deployment"
            }
        ],
        "confidence": 0.87
    }
    
    return jsonify(result), 200

@app.route('/api/ml/extract-skills', methods=['POST'])
def extract_skills():
    """Extract skills from resume"""
    data = request.json
    resume_text = data.get('resumeText', '')
    
    # TODO: Implement skill extraction using NLP
    result = {
        "technicalSkills": ["Python", "Java", "React", "MongoDB"],
        "softSkills": ["Leadership", "Communication", "Problem Solving"],
        "experience": "5 years",
        "education": "B.Tech Computer Science",
        "certifications": ["AWS Certified Developer"]
    }
    
    return jsonify(result), 200

@app.route('/api/ml/recommend-courses', methods=['POST'])
def recommend_courses():
    """Get course recommendations based on skill gaps"""
    data = request.json
    current_skills = data.get('currentSkills', [])
    target_skills = data.get('targetSkills', [])
    
    # TODO: Implement course recommendation logic
    result = {
        "recommendations": [
            {
                "courseName": "React - The Complete Guide",
                "provider": "Udemy",
                "duration": "40 hours",
                "priority": "High",
                "reason": "Essential for Full Stack development"
            },
            {
                "courseName": "AWS Certified Developer",
                "provider": "AWS Training",
                "duration": "30 hours",
                "priority": "Medium",
                "reason": "Valuable cloud skills for modern applications"
            }
        ]
    }
    
    return jsonify(result), 200

@app.route('/api/ml/batch-analyze', methods=['POST'])
def batch_analyze():
    """Batch process multiple applications"""
    data = request.json
    job_id = data.get('jobId')
    applications = data.get('applications', [])
    
    # TODO: Implement batch processing
    results = []
    for i, app in enumerate(applications):
        results.append({
            "applicationId": app.get('applicationId'),
            "matchScore": 85.5 - (i * 5),  # Mock decreasing scores
            "rank": i + 1
        })
    
    result = {
        "results": results
    }
    
    return jsonify(result), 200

if __name__ == '__main__':
    print("🚀 ML Service starting on http://localhost:5000")
    print("📊 Endpoints available:")
    print("  - GET  /health")
    print("  - POST /api/ml/analyze")
    print("  - POST /api/ml/extract-skills")
    print("  - POST /api/ml/recommend-courses")
    print("  - POST /api/ml/batch-analyze")
    app.run(host='0.0.0.0', port=5000, debug=True)
