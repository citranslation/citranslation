import json
import pandas as pd
import string
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.feature_extraction.text import CountVectorizer
import re
import csv
import time

def get_leaves(lhs, root=False):
    if root:
        if str(root).startswith('{{'):
            json_obj = json.loads(str(lhs)[1:-1])
        else:
            json_obj = json.loads(str(lhs))
    else:
        json_obj = lhs
    leaves_str = ''
    if len(json_obj['children']) > 0:
        for child in json_obj['children']:
            leaves_str += ' ' + get_leaves(child)
    else:
        # print(json_obj)
        leaves_str += ' ' + json_obj['type'].replace('-cmd', '')
    return leaves_str


df = pd.read_csv('rules_H2.csv', sep='@;@', on_bad_lines='warn', quotechar='"',
                 quoting=csv.QUOTE_ALL, engine='python')
file_out = open('rules_sim_based/rules_H2.csv', 'w+')
file_out_non_sim = open('rules_nonsim_based/rules_H2.csv', 'w+')
out_str = ''
for el in df.columns:
    out_str += el + '@;@'
out_str = out_str[:-1]
file_out.write(out_str + '\n')

for index, row in df.iterrows():
    if ' -> ' not in row[0]:
        continue
    rule = row[0]
    lhs = rule.split(' -> ')[0]
    lhs = str(lhs).replace('""', '"')
    rhs = rule.split(' -> ')[1]
    rhs = str(rhs).replace('""', '"')
    if ('"type":""}]' in lhs) or ('"type":""}]' in rhs):
        continue
    lhs_leaves = get_leaves(lhs, root=True)
    rhs_leaves = get_leaves(rhs, root=True)

    temp_list = [lhs_leaves, rhs_leaves]
    vectorizer = CountVectorizer()
    try:
        vectors = vectorizer.fit_transform(raw_documents=temp_list)
        csim = cosine_similarity(vectors[0].reshape(1, -1), vectors[1].reshape(1, -1))[0][0]
        # print(csim)
        if csim > 0.50:
            file_out.write('"' +
                           str(row[0]) + '"@;@' + str(row[1]) + '@;@' + str(row[2]) + '@;@' + str(row[3]) + '@;@' + str(
                row[4]) + '@;@' + str(row[5]) + '@;@' + str(row[6]) + '@;@' + str(row[7]) + '@;@' + str(row[8]) + '\n')
        else:
            file_out_non_sim.write('"' +
                                   str(row[0]) + '"@;@' + str(row[1]) + '@;@' + str(row[2]) + '@;@' + str(
                row[3]) + '@;@' + str(
                row[4]) + '@;@' + str(row[5]) + '@;@' + str(row[6]) + '@;@' + str(row[7]) + '@;@' + str(row[8]) + '\n')

    except Exception as e:
        print(temp_list)
        print(e)

