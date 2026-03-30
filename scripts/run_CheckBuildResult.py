import pandas as pd
from pathlib import Path
from citranslation.core.Iterative import run




if __name__ == "__main__":
    """
    test repo dir: personal repo
    model tag: gemini3 or gpt-4o or gpt-40-mini or deepseek or importer

    """
    base_dir = Path(__file__).resolve().parent.parent

    test_dir = 'XXXX'
    model_tag = 'gemini3'
    csv_path = base_dir/"citranslation"/"resources"/"csv"/'datasets.csv'

    df = pd.read_csv(csv_path)
    for index, row in df.iterrows():
        repo_name = row['repo_name']
        language = row['language']
        run(repo_name, language, test_dir, model_tag,index)