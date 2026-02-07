from git import Repo
import csv

with open('data(random).csv', newline='') as csvfile:
    projectReader = csv.reader(csvfile, delimiter=',', quotechar='"')
    for row in projectReader:
        print("cloning from git@github.com:"+row[0])
        repo = Repo.clone_from("git@github.com:"+row[0],"gitclone/"+row[0])      
        repo.close()

