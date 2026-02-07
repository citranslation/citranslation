from pathlib import Path
from citranslation.core.Iterative import run

def main():
    prompt_type = 'iterative'
    base_dir = Path(__file__).resolve().parent.parent
    csv_path = base_dir/"citranslation"/"resources"/"csv"/'3.csv'
    # csv_path = base_dir/"citranslation"/"resources"/"csv"/'guideline_1.csv'
    # csv_path = base_dir/"citranslation"/"resources"/"csv"/'3.csv'
    # dir_name = 'translation'
    dir_name = 'iterative'
    run(csv_path,dir_name,prompt_type)



if __name__ == "__main__":
    main()