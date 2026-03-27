from pathlib import Path
from citranslation.core.GenPlot import run


def main(dir_name):
    run(dir_name)



if __name__ == "__main__":
    dir_name = 'enhancement'
    # dir_name = 'translation'
    main(dir_name)