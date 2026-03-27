from pathlib import Path
from citranslation.core.CaculateAll import run



if __name__ == "__main__":
    base_dir = Path(__file__).resolve().parent.parent
    # csv_path = base_dir/"citranslation"/"resources"/"csv"/'test.csv'
    csv_path = base_dir/"citranslation"/"resources"/"csv"/'3.csv'
    # dir_name = 'enhancement'
    dir_name = 'base2' 
    run(csv_path,dir_name)
