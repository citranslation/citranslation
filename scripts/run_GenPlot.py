from pathlib import Path
from citranslation.core.GenPlot import run


def main(dir_name):
    run(dir_name)



if __name__ == "__main__":
    # save dir
    dir_name = 'enhancement'

    main(dir_name)