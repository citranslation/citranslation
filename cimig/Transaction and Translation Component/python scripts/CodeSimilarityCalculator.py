import csv
from collections import Counter
from nltk.util import ngrams
import nltk
from crystalbleu import corpus_bleu as crystal_bleu
from bleu_ignoring import SmoothingFunction
from nltk.translate.bleu_score import corpus_bleu
from nltk import ngrams
import pandas as pd
import warnings
# warnings.filterwarnings("ignore")
import os
import shutil


def read_file(filename):
    with open(filename, "r") as f:
        tokens_cleaned_total = []
        for line in f:
            if line.startswith('#'):
                continue
            tokens = line.split(" ")
            tokens_cleaned = [t for t in tokens if t not in [':']]
            tokens_cleaned = [t.replace(":", "").replace("\n", "").replace("[", "").replace("]", "").lower() for t in
                              tokens_cleaned]
            tokens_cleaned = [t for t in tokens_cleaned if t != "" and t != "-"]
            tokens_cleaned_total.extend(tokens_cleaned)
        return tokens_cleaned_total


f = open('sim_stats_v2.csv', 'w', encoding='UTF8', newline='')
writer = csv.writer(f)
# write the header
header = ['GeneratefileName', 'ReferenceFiles', 'crystalBLEU_score_max', 'bleuscore_max', 'crystalBLEU_score_avg',
          'bleuscore_avg']
writer.writerow(header)

if __name__ == '__main__':
    nb = 0
    csv_df = pd.read_csv('/home/umd-002677/IdeaProjects/travis2ga/files-out.csv', sep=";")
    os.chdir("..")
    for index, row in csv_df.iterrows():
        if(nb>5):
            exit(0)
        print(os.getcwd())
        folder_Path= "./OriginalAndNewGHAFiles/"+row['GeneratefileName'].split('/')[2].split('_Max.yml')[0]
        if not os.path.exists(folder_Path):
            os.makedirs(folder_Path)
        reference_files_list = row['OriginalFilesList'].split(',')
        reference_file_path = "/".join(reference_files_list[0].split('/')[:5])+"/travis.yml"
        if not os.path.exists(folder_Path+"/original/"):
            os.makedirs(folder_Path+"/original/")

        shutil.copy(reference_file_path, folder_Path+"/original/")
        if not os.path.exists(folder_Path+"/generated/"):
            os.makedirs(folder_Path+"/generated/")
        shutil.copy(row['GeneratefileName'], folder_Path+"/generated/")

        if not os.path.exists(folder_Path+"/new/"):
            os.makedirs(folder_Path+"/new/")

        for reference_file in reference_files_list:
            if reference_file != "":
                shutil.copy(reference_file, folder_Path+"/new/")

        #
        # bleu_scores = []
        # crystal_bleu_scores = []
        # for reference_file in reference_files_list:
        #     if reference_file == "":
        #         continue
        #     reference_tokens = read_file('../' + reference_file)
        #     candidate_ngrams = []
        #     reference_ngrams = []
        #
        #     all_ngrams = []
        #     for n in range(1, 2):
        #         temp_ngrams_candi = list(ngrams(candidate_tokens, n))
        #         temp_ngrams_refer = list(ngrams(reference_tokens, n))
        #         candidate_ngrams.extend(temp_ngrams_candi)
        #         reference_ngrams.extend(temp_ngrams_refer)
        #         all_ngrams.extend(temp_ngrams_candi)
        #         all_ngrams.extend(temp_ngrams_refer)
        #
        #     # Calculate frequencies of all n-grams
        #     frequencies = Counter(all_ngrams)
        #     k = 1
        #     trivially_shared_ngrams = dict()
        #     crystalBLEU_score = crystal_bleu(
        #         [[reference_ngrams]], [candidate_ngrams], ignoring=trivially_shared_ngrams)
        #     crystal_bleu_scores.append(crystalBLEU_score)
        #     # sm_func = SmoothingFunction(epsilon=0.0001).method1
        #     bleuscore = corpus_bleu([[reference_ngrams]], [candidate_ngrams])
        #     bleu_scores.append(bleuscore)
        #
        # max_cb_score_string = "{:.3f}".format(max(crystal_bleu_scores))
        # max_b_score_string = "{:.3f}".format(max(bleu_scores))
        # avg_cb_score_string = "{:.3f}".format(sum(crystal_bleu_scores) / len(crystal_bleu_scores))
        # avg_b_score_string = "{:.3f}".format(sum(bleu_scores) / len(bleu_scores))
        # row = [row['GeneratefileName'], row['OriginalFilesList'], max_cb_score_string, max_b_score_string,
        #        avg_cb_score_string, avg_b_score_string]
        # writer.writerow(row)
        # print(row)
        # nb += 1
        # # if nb > 10:
        # #     exit(0)
