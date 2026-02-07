from pathlib import Path
from citranslation.core.ControlVersion import run



if __name__ == "__main__":
    baseZ_dir = Path(__file__).resolve().parent.parent
    file_name = "deepseek.yml"
    csv_path = baseZ_dir/"citranslation"/"resources"/"csv"/"test.csv"
    run(csv_path,file_name)