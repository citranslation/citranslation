# CItranslation

This repository contains the replication package for the **CI Translation** project.


## Installation
------------

Install using poetry:

```bash
poetry install
```
## Configration
------------

Need '.env' file to config GitHub token and LLM api token

```text
OPENAI_API_KEY = XXXXXXXX
GITHUB_TOKEN = XXXXXXXX
```

## Repository Structure


```text
replication_package/
├── cimig/
├── citranslation/                       # Main code
│   ├── actions_remaker/                 # Actions-remaker analyzer
│   ├── core/
│   │   ├── CaculateALL.py               # Caculate similarity metric
│   │   ├── CheckBuildResult.py          # Get the run result
│   │   ├── DownloadRepo.py              # Download repo
│   │   ├── GenCItraFiles.py             # Gen translate files
│   │   ├── GenPlot.py                   # Gen similarity metric pictures
│   │   ├── Iterative.py                 # Iterative strategy
│   │   └── SearchActions.py             # Search target repo
│   ├── resources/
│   │   ├── csv                          # Csv files
│   │   ├── datasets                     
│   │   ├── prompts                      # Prompts used
│   │   └── repo                         # Download repo
│   └── utils/
├── tests/                               # local test repo to be create
├── scripts/                             # Scripts
│   ├── run_CaculateALL.py               # Scripts to run CaculateALL.py 
│   ├── run_CheckBuildResult.py          # Scripts to run CheckBuildResult.py
│   ├── run_DownloadRepo.py              # Scripts to run DownloadRepo.py 
│   ├── run_GenCItraFiles.py             # Scripts to run GenCItraFiles.py
│   ├── run_GenPlot.py                   # Scripts to run GenPlot.py
│   ├── run_Iterative.py                 # Scripts to run Iterative.py
│   └── run_SearchActions.py             # Scripts to run SearchActions.py
├── .env                                 # need to be created
├── pyproject.toml                       # project dependencies
└── README.md                            # This file
```

## Datasets

The dataset is located under the `citranslation/resource/datasets` directory and includes **301 software projects**.

## Project Collection Process

The original CSV files were generated using the **SEART GitHub Search Engine**. We used the script `scripts/run_SearchActions.py` to:

* Search and filter candidate GitHub projects
* Generate intermediate CSV files saved in the `origin` folder
```bash
 python -m scripts.run_SearchActions
```

After **manual screening and cleaning**, the final selected projects were consolidated and saved into `datasets.csv`.
## Download repo
Use DownloadRepo to download repo and switch to target version according to `datasets.csv`. The repo will be saved in `resources/repo` folder.

```bash
 python -m scripts.run_DownloadRepo.py
```
## Translation File Gen
Use the script `scripts/GenCItraFiles.py` to generate `{prompt_type}` by `{model_tag}` model, the result will be saved in `resoucces/datasets/{target_repo}/{dir_name}`.

```bash
 python -m scripts.run_GenCItraFiles
```
## Check Build Result
Use the script `scripts/CheckBuildResult.py` run actions workflow, will read test repo from `resources/repo` and write repo in `tests/{repo}`, the result will be saved in `resources/results/build_result.csv`.

```bash
 python -m scripts.run_Iterative
```

## Iterative
Use the script `scripts/Iterative.py` run actions workflow, will read test repo from `resources/repo` and write repo in `tests/{repo}`, the result will be saved in `resources/results/build_result.csv`.

```bash
 python -m scripts.run_Iterative
```
## Caculate similarity metric

```bash
 python -m scripts.run_CaculateAll
```

## Plot Generation

Use the script `scripts/run_GenPlot.py` to generate plots, the result will be saved in `resoucces/picture/{dir_name}`.
```bash
 python -m scripts.run_GenPlot
```

