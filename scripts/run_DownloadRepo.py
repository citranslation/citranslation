from pathlib import Path
from citranslation.core.DownloadRepo import run



if __name__ == "__main__":
    base_dir = Path(__file__).resolve().parent.parent
    csv_path = base_dir/"citranslation"/"resources"/"csv"/"dataset.csv"
    save_path = base_dir/"citranslation"/"resources"/"repo"
    run(csv_path,save_path)
