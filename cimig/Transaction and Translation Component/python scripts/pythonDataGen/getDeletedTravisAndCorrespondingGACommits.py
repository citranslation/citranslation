from git import Repo
import csv
import os
from utilFunctions import getFilesFromCommitByPath,fileExistsInTreeAtCommit,genFileName,writeToFile,getRepoName


listOfRepos = []
gitCloneAbsPath="D:\gitclone"

def getLastCommitBefore(listOfGithubCommits,travisCommit):
    for commit in listOfGithubCommits:
        if(commit.committed_date<=travisCommit.committed_date):
            return commit
    return None

with open('data.csv', newline='') as csvfile:
    projectReader = csv.reader(csvfile, delimiter=',', quotechar='"')
    for (index,row) in enumerate(projectReader):
        if(index==0):continue #skip header
        listOfRepos.append(Repo(gitCloneAbsPath+"/"+row[0]) )

f = open('deletedTravisCommits.csv', 'w',newline='')
writer = csv.writer(f)


for repo in listOfRepos:
    travisCommits=list(repo.iter_commits(paths='.travis.yml'))
    githubCommits=list(repo.iter_commits(paths='.github/workflows'))
    if( fileExistsInTreeAtCommit(travisCommits[0],'.travis.yml')): continue
    #if travis was not deleted disregard project
    prevGithubCommit=getLastCommitBefore(githubCommits,travisCommits[0])
    if(prevGithubCommit==None): continue
    if(travisCommits[0] in githubCommits): continue
    try:
        githubTrees=getFilesFromCommitByPath(repo,prevGithubCommit,'.github/workflows')
        if(len(githubTrees)>1): continue
        for githubTree in githubTrees:
            writeToFile(githubTree.data_stream.read().decode('utf-8'),"deletedTravis/"+genFileName(repo.working_tree_dir,githubTree.name))
        #Get previous travis commit to get file from tree (as it was deleted in the last commit)
        travisBlob = getFilesFromCommitByPath(repo,travisCommits[1],'.travis.yml')
        writeToFile(travisBlob.data_stream.read().decode('utf-8'),"deletedTravis/"+genFileName(repo.working_tree_dir,'.travis.yml'))
        writer.writerow([getRepoName(repo.working_tree_dir),getRepoName(repo.working_tree_dir)+'/.travis.yml',getRepoName(repo.working_tree_dir)+'/'+githubTrees[0].name])
    except Exception as e:
        print(e)

    
    

