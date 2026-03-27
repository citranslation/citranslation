from pathlib import Path
from citranslation.core.SearchActions import run

def main():
    base_dir = Path(__file__).resolve().parent.parent
    csv_path = base_dir/"citranslation"/"resources"/"csv"/"origin_csv"
    save_path = base_dir/"citranslation"/"resources"/"csv"/"filter_csv"
    run(csv_path,save_path)



if __name__ == "__main__":
    main()