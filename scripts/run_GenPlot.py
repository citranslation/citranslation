from pathlib import Path
from citranslation.core.GenPlot import run





if __name__ == "__main__":
    """
    dir_name: input csv and output picture saved folder
    input_csv: from"resources/csv/dir_name"
    output_picture: in"resources/csv/picture/dir_name"
    """
    dir_name = 'enhancement'
    run(dir_name)
