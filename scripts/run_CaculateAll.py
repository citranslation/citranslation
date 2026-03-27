from pathlib import Path
from citranslation.core.CaculateAll import run



if __name__ == "__main__":
    base_dir = Path(__file__).resolve().parent.parent
    # file that to be caculate
    col_lists = ['deepseek-oneshot','deepseek-fewshot','deepseek-guideline','gemini3-oneshot','gemini3-fewshot','gemini3-guideline']
    # test csv
    csv_path = base_dir/"citranslation"/"resources"/"csv"/'dataset.csv'
    # save in dir
    dir_name = 'enhacement' 
    run(csv_path,dir_name,col_lists)
