import os
from collections import Counter
# import pandas as pd

c = Counter()
f_out= open('most_common_terms_Count_travis.csv', 'w+')
f_out_2= open('most_common_terms_travis.txt', 'w+')
f_out.write('term\n')
count_files=0
import glob


for file in glob.glob("/home/umd-002677/IdeaProjects/travis2ga/JsonAstsForTAR/travis*.json"):
    lines = open(file,'r').readlines()
    count_files+=1
    lines_clean=[]
    for line in lines:
        if 'value' in line or 'type' in line:
            lines_clean.append(line.strip().replace(' ','').replace('\n',''))
    c += Counter(lines_clean)

for file in glob.glob("/home/umd-002677/IdeaProjects/travis2ga/JsonAsts/travis*.json"):
    lines = open(file,'r').readlines()
    count_files+=1
    lines_clean=[]
    for line in lines:
        if 'value' in line or 'type' in line:
            lines_clean.append(line.strip().replace(' ','').replace('\n',''))
    c += Counter(lines_clean)


print(count_files)
for word, count in c.most_common(1000):
    f_out.write(str(word).replace(',','comma').replace('#','hashtag').replace(' ','')+","+str(count)+'\n')
    f_out_2.write(str(word).replace(',','comma').replace('#','hashtag').replace(' ','')+'\n')


c = Counter()
f_out= open('most_common_terms_Count_github.csv', 'w+')
f_out_2= open('most_common_terms_github.txt', 'w+')
f_out.write('term\n')
count_files=0
import glob


for file in glob.glob("/home/umd-002677/IdeaProjects/travis2ga/JsonAstsForTAR/github*.json"):
    lines = open(file,'r').readlines()
    count_files+=1
    lines_clean=[]
    for line in lines:
        if 'value' in line or 'type' in line:
            lines_clean.append(line.strip().replace(' ','').replace('\n',''))
    c += Counter(lines_clean)

for file in glob.glob("/home/umd-002677/IdeaProjects/travis2ga/JsonAsts/github*.json"):
    lines = open(file,'r').readlines()
    count_files+=1
    lines_clean=[]
    for line in lines:
        if 'value' in line or 'type' in line:
            lines_clean.append(line.strip().replace(' ','').replace('\n',''))
    c += Counter(lines_clean)


print(count_files)
for word, count in c.most_common(1000):
    f_out.write(str(word).replace(',','comma').replace('#','hashtag').replace(' ','')+","+str(count)+'\n')
    f_out_2.write(str(word).replace(',','comma').replace('#','hashtag').replace(' ','')+'\n')