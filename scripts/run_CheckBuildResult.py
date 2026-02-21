from pathlib import Path
from citranslation.core.CheckBuildResult import run


if __name__ == "__main__":
    base_dir = Path(__file__).resolve().parent.parent
    repo_name = "gemini3-iterative-4"
    file_name = 'gemini3-iterative-4'
    csv_path = base_dir/"citranslation"/"resources"/"csv"/"base_1.csv"
    # csv_path = base_dir/"citranslation"/"resources"/"csv"/"guideline_1.csv"
    run(repo_name,csv_path,file_name)