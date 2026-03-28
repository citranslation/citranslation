# CItranslation

This repository contains the replication package for the **CI Translation** project.
README file coming soon 2026-03-27-preparing

## Installation
------------

Install using poetry:

```bash
poetry install
```
## 📁 Repository Structure

## 📁 Repository Structure

```text
replication_package/
├── cimig/
├── citranslation/                       # Main code
│   ├── actions_remaker/                 # Actions-remaker analyzer
│   ├── core/
│   │   ├── CaculateALL.py               # Caculate similarity metric
│   │   └── SearchActions.py             # Search target repo
│   ├── resources/
│   └── utils/
├── test/
├── scripts/                             # Scripts
│   ├── run_CaculateALL.py
│   ├── run_CheckBuildResult.py
│   ├── run_DownloadRepo.py
│   ├── run_GenCItraFiles.py
│   ├── run_GenPlot.py
│   ├── run_Iterative.py
│   └── run_SearchActions.py
├── .env                                 # need to be created
├── pyproject.toml                       # project dependencies
└── README.md                            # This file

## Datasets

The dataset is located under the `citranslation/resource/datasets` directory and includes **301 software projects**.

## Project Collection Process

The original CSV files were generated using the **SEART GitHub Search Engine**. We used the script `scripts/run_SearchActions.py` to:

* Search and filter candidate GitHub projects
* Generate intermediate CSV files saved in the `filter` folder
```bash
 python -m scripts.run_SearchActions
```

After **manual screening and cleaning**, the final selected projects were consolidated and saved into `datasets.csv`.

## Translation File Gen

```bash
 python -m scripts.run_GenCItraFiles
```
## Caculate similarity metric

```bash
 python -m scripts.run_CaculateAll
```

## Plot Generation

Use the script `scripts/run_GenPlot.py` to generate plots.
```bash
 python -m scripts.run_GenPlot
```
#
More updates coming soon.
