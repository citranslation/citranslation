from pathlib import Path
from citranslation.core.DiffWorkflow import run 

if __name__ == "__main__":
    baseZ_dir = Path(__file__).resolve().parent.parent
    file = "js"
    csv_path = baseZ_dir/"citranslation"/"resources"/"csv"/f"{file}.csv"
    save_path = baseZ_dir/"citranslation"/"resources"/"configration_data"/file
    run()