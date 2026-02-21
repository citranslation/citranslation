import csv
import os

rowcol=[]
f = open('singleFileMigrationCommits.csv', 'w', newline='')
writer = csv.writer(f)
with open('migrationCommits.csv', newline='') as csvfile:
    projectReader = csv.reader(csvfile, delimiter=',', quotechar='"')
    for (index,row) in enumerate(projectReader):
        if(',' in row[2] or len(row[2])<=2): continue
        githubFileName=row[2].split('/')[-1]
        writer.writerow([row[0],row[0]+'/.travis.yml',row[0]+'/'+githubFileName[:-2]])

f.close()
