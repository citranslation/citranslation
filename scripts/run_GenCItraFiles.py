from pathlib import Path
from citranslation.core.GenCItraFiles import run




if __name__ == "__main__":
    base_dir = Path(__file__).resolve().parent.parent
    # csv_path: dataset csv path 
    csv_path = base_dir/"citranslation"/"resources"/"csv"/'datasets.csv'
    # dir_name: save file in 'translation' or 'enhancement'
    dir_name = 'enhancement'
    # prompt_type: prompt type 'base' or 'oneshot' or 'guideline'
    prompt_type = 'guideline'
    # model_tag: gen ci file from 'gemini3' or 'gpt-4o' or 'gpt-40-mini' 
    model_tag = 'gemini3'
    run(csv_path, dir_name, prompt_type, model_tag)