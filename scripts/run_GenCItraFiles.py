from pathlib import Path
from citranslation.core.GenCItraFiles import run




if __name__ == "__main__":
    """
    dir_name: save file in 'translation' or 'enhancement'
    prompt_type: prompt type 'base' or 'oneshot' or 'guideline'
    model_tag: gen ci file from 'gemini3' or 'gpt-4o' or 'gpt-40-mini'
    """

    base_dir = Path(__file__).resolve().parent.parent

    dir_name = 'enhancement'
    prompt_type = 'guideline'
    model_tag = 'gemini3'
    csv_path = base_dir/"citranslation"/"resources"/"csv"/'datasets.csv'

    run(csv_path, dir_name, prompt_type, model_tag)