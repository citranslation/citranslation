from pathlib import Path
from citranslation.core.SearchActions import run




if __name__ == "__main__":
    base_dir = Path(__file__).resolve().parent.parent
    # csv_file contain repo that to be searched
    csv_path = base_dir/"citranslation"/"resources"/"csv"/"origin_csv"
    # csv_file that to save result
    save_path = base_dir/"citranslation"/"resources"/"csv"/"filter.csv"
    run(csv_path,save_path)
