# TalentSync ML Service

## Quick Start

1. Install dependencies:
```bash
pip install -r requirements.txt
```

2. Run the service:
```bash
python app.py
```

3. Test:
```bash
curl http://localhost:5000/health
```

## API Endpoints

- `POST /api/ml/analyze` - Analyze resume-job match
- `POST /api/ml/extract-skills` - Extract skills from resume
- `POST /api/ml/recommend-courses` - Get course recommendations
- `POST /api/ml/batch-analyze` - Batch process applications

See main project README for detailed API documentation.
