import csv
import os
from git import Repo

pathToClonedRepos = '/home/alaa/travis2gaMiscFiles/effortAnalysis/clone/'

# read file data.csv
with open('data_4.csv', 'r') as f:
    reader = csv.reader(f)
    data = list(reader)
    # remove the header
    data = data[1:]

def milliSecToDays(milliSec):
    return milliSec/(60*60*24)

#function to open git repository
def openGitRepository(path):
    return Repo(path)

def getTimeStampOfCommit(repo, commit):
    return repo.commit(commit).committed_date

def getNumberOfCommitsBetween(repo, commit1, commit2):
    return len(list(repo.iter_commits(commit1+'..'+commit2)))

def getNumberOfTravisCommitsBetween(repo, commit1, commit2):
    #get the list of commits between commit1 and commit2
    listOfCommitsBetween=list(repo.iter_commits(commit1+'..'+commit2))
    numberOfCommits=0
    for commit in listOfCommitsBetween:
        if('.travis.yml' in commit.stats.files):
            numberOfCommits+=1
        
    return numberOfCommits

def getNumberOfGithubActionsCommitBetween(repo, commit1, commit2,githubFileChanged):
    #get the list of commits between commit1 and commit2
    pathOfGithubFileChanged='.github/workflows/'+githubFileChanged
    listOfCommitsBetween=list(repo.iter_commits(commit1+'..'+commit2))
    numberOfCommits=0
    for commit in listOfCommitsBetween:
        if(pathOfGithubFileChanged in commit.stats.files):
            numberOfCommits+=1
        
    return numberOfCommits

def getNumberOfCICommitsBetween(repo, commit1, commit2,githubFileChanged):
    #get the list of commits between commit1 and commit2
    pathOfGithubFileChanged='.github/workflows/'+githubFileChanged
    listOfCommitsBetween=list(repo.iter_commits(commit1+'..'+commit2))
    numberOfCommits=0
    for commit in listOfCommitsBetween:
        if(pathOfGithubFileChanged in commit.stats.files) or ('.travis.yml' in commit.stats.files):
            numberOfCommits+=1
        
    return numberOfCommits

bucket2=[]
unparsableRepos=[]


#recursive function to find commit that last changed the file at or before the given commit
def getCommitThatLastChangedFile(repo, commit, filepath):
    commitObj=repo.commit(commit)
    if(filepath in commitObj.stats.files):
        return commit
    else:
        for parent in commitObj.parents:
            return getCommitThatLastChangedFile(repo, parent.hexsha, filepath)

for row in data:
    pathToRepo = pathToClonedRepos+'/'+row[0].replace('_','/',1)
    if(os.path.exists(pathToRepo) == False):
        continue
    repo = openGitRepository(pathToRepo)
    try:
        #simply using row[1] and row[2] as commit ids is wrong as these do not represent the id where that specific file version was introduced but just 
        #the commit id of the two versions with the highest similarity. So we need to find the commit id where the file was introduced.
        commitThatLastChangedGithubFile = getCommitThatLastChangedFile(repo, row[2],'.github/workflows/'+ row[4])
        row[2]=commitThatLastChangedGithubFile
        timeStampOfTravisCommit = getTimeStampOfCommit(repo, row[1])
        timeStampOfGithubCommit = getTimeStampOfCommit(repo, row[2])
        numberOfCommitsBetween = getNumberOfCommitsBetween(repo, row[1], row[2])
        numberOfCICommitsBetween = getNumberOfCICommitsBetween(repo, row[1], row[2],row[4])
        numberofTravisCommits = getNumberOfTravisCommitsBetween(repo, row[1], row[2])
        numberOfGithubActionsCommits = getNumberOfGithubActionsCommitBetween(repo, row[1], row[2],row[4])
        #if the github commit is older than the travis commit swap them before computing the number of commits between them
        swapped=False
        if(timeStampOfGithubCommit < timeStampOfTravisCommit):
            numberOfCommitsBetween = getNumberOfCommitsBetween(repo, row[2], row[1])
            numberOfCICommitsBetween = getNumberOfCICommitsBetween(repo, row[2], row[1],row[4])
            numberofTravisCommits = getNumberOfTravisCommitsBetween(repo, row[2], row[1])
            numberOfGithubActionsCommits = getNumberOfGithubActionsCommitBetween(repo, row[2], row[1],row[4])
            swapped=True
        bucket2.append((row[0],row[1],row[2],row[3],row[4],milliSecToDays(timeStampOfGithubCommit-timeStampOfTravisCommit),numberOfCommitsBetween,numberOfCICommitsBetween,numberofTravisCommits,numberOfGithubActionsCommits,swapped))
    except Exception as e:
        unparsableRepos.append(row[0])
        print(e.with_traceback)
        continue
   

#writre bucket2 to csv file
with open('combinedOldAndNewWithEffortAndTime_v3.csv', 'w') as f:
    writer = csv.writer(f)
    writer.writerow(['repo','travisCommit','githubCommit','travisBuild','githubFileChanged','timeBetween','numberOfCommitsBetween','numberOfCICommitsBetween','numberofTravisCommits','numberOfGithubActionsCommits','swapped'])

    writer.writerows(bucket2)

#write unparsable repos to csv file
with open('unparsableRepos.csv', 'w') as f:
    writer = csv.writer(f)
    writer.writerow(['repo'])
    writer.writerows([[repo] for repo in unparsableRepos])
    
