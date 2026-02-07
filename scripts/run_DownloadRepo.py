from pathlib import Path
from citranslation.core.DownloadRepo import run

def main():
    base_dir = Path(__file__).resolve().parent.parent
    csv_path = base_dir/"citranslation"/"resources"/"csv"/"js.csv"
    save_path = base_dir/"citranslation"/"repo_js"
    run(csv_path,save_path)



if __name__ == "__main__":
    main()