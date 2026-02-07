from git import Repo
import csv
import os
from utilFunctions import getFilesFromCommitByPath
from utilFunctions import writeToFile
listOfRepos = []




def genFileName(fullDirPath,commit,fileName):
    pathArray=fullDirPath.split('\\')
    return pathArray[-2]+'/'+pathArray[-1]+'/'+commit.hexsha+'/'+fileName

def sortCommits(listOfCommits):
    return sorted(listOfCommits, key=lambda x: x.committed_date)

with open('data(random).csv', newline='') as csvfile:
    projectReader = csv.reader(csvfile, delimiter=',', quotechar='"')
    for row in projectReader:
        listOfRepos.append(Repo("gitclone/"+row[0]) )

for repo in listOfRepos:
    travisCommits=list(repo.iter_commits(paths='.travis.yml'))
    githubCommits=list(repo.iter_commits(paths='.github/workflows'))
    allCommits=travisCommits+githubCommits
    allCommits=sortCommits(allCommits)
    for(i,commit) in enumerate(allCommits):
        try: 
            travisBlob= getFilesFromCommitByPath(repo,commit,'.travis.yml')
            writeToFile(travisBlob.data_stream.read().decode('utf-8'),"versions/"+genFileName(repo.working_tree_dir,commit,'.travis.yml'))
        except Exception as e:
            print(e)
            print("error in file "+repo.working_tree_dir+" in commit "+commit.hexsha)
        try:
            githubTrees=getFilesFromCommitByPath(repo,commit,'.github/workflows')
            for githubTree in githubTrees:
                writeToFile(githubTree.data_stream.read().decode('utf-8'),"versions/"+genFileName(repo.working_tree_dir,commit,githubTree.name))
        except Exception as e:
            print(e)
            print("error in file "+repo.working_tree_dir+" in commit "+commit.hexsha)



   



    

