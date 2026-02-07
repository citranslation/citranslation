from http.client import HTTPException
from operator import indexOf
from git import Repo
import csv
import os
from githubAPI import githubAPIWrapper
from utilFunctions import getFilesFromCommitByPath,getCommitJustBefore,writeToFile,fileExistsInTreeAtCommit,genFileName,getRepoName,getOwnerName,getRepositoryName
listOfRepos = []
gitCloneAbsPath="D:\gitclone"

#get most recent commit of a repo that is both a travis commit(modifies .travis.yml) and a github commit(modifies .github/workflows)
def getMostRecentCommonTravisAndGithubCommit(travisCommits,githubCommits):
    for travisCommit in travisCommits: #first travis commit returned is most recent
        for githubCommit in githubCommits:#first github commit returned is most recent
            if(travisCommit==githubCommit):
                return travisCommit
    return None



#did the commit delete .travis.yml and introduce new .github/workflow files
def isMigrationCommit(commit,previousCommit):
    #it is a migration commit if the deleted a .travis.yml file and addes a .github/workflows file
    if(not fileExistsInTreeAtCommit(commit,'.travis.yml')):
        #.travis.yml does not exist in the tree at that commit therefore it was deleted
        filesChanged=commit.stats.files
        print(commit.repo.working_dir)
        print("commit :"+commit.hexsha)
        if(previousCommit==None): 
            return True #no github commits before the most recent github commit therefore it is a migration commit
        if(not fileExistsInTreeAtCommit(previousCommit,".github")): return True
        for fileChanged in list(filesChanged.keys()):
            if(fileChanged.startswith(".github/workflows") and (not fileChanged in previousCommit.tree[".github"]["workflows"])):
                print("migration commit found: "+fileChanged)
                return True
    
    return False










#get .github/workflows files that were introduced in the commit
def getMigratedFiles(commit,previousCommit):
    migratedFiles=[]
    #if no previous commit or .github folder was deleted in the previous commit( Adopted -> reverted -> adopted again )
    if(previousCommit==None or fileExistsInTreeAtCommit(previousCommit,".github")==False):
        for file in  list(commit.stats.files):
            if(file.startswith('.github/workflows') and file.endswith('.yml')):
                print("file changed: "+file)
                migratedFiles.append(file)
    else:
        #if it is not the first GA commit get the files that were introduced in this commit (did not exist in the previous)
        for file in  list(commit.stats.files):
            if(file.startswith('.github/workflows') and (not file in previousCommit.tree[".github"]["workflows"]) and file.endswith(".yml")):
                print("migration commit found: "+file)
                migratedFiles.append(file)
                
        
    return migratedFiles
def getNextGithubCommit(githubCommits,commit):
    indexOfCommit=githubCommits.index(commit)
    if(indexOfCommit == None or indexOfCommit==0):
        return None
    return githubCommits[indexOfCommit-1]

def getGithubWrapper(repo):
    return githubAPIWrapper(getOwnerName(repo.working_tree_dir),getRepositoryName(repo.working_tree_dir))
#read repos from and initialize repo object
with open('data.csv', newline='') as csvfile:
    projectReader = csv.reader(csvfile, delimiter=',', quotechar='"')
    for (index,row) in enumerate(projectReader):
        if(index==0):continue #skip header
        listOfRepos.append(Repo(gitCloneAbsPath+"/"+row[0]) )

migrationCommits=[]
f = open('migrationCommits_succesfulBuild.csv', 'w',newline='')
writer = csv.writer(f)
#traverse repos and get relevant commits
try:
    for repo in listOfRepos:
        print(repo.working_tree_dir)
        travisCommits=list(repo.iter_commits(paths='.travis.yml'))
        githubCommits=list(repo.iter_commits(paths='.github/workflows'))
        mostRecentCommonCommit=getMostRecentCommonTravisAndGithubCommit(travisCommits,githubCommits)
        if(mostRecentCommonCommit==None): #if no common commit found then disregard the entry
            continue
        theOneJustBeforeIt=getCommitJustBefore(githubCommits,mostRecentCommonCommit)
        if(isMigrationCommit(mostRecentCommonCommit,theOneJustBeforeIt)):
            ghw=getGithubWrapper(repo)
            githubTrees=getFilesFromCommitByPath(repo,mostRecentCommonCommit.hexsha,'.github/workflows')
            temp = mostRecentCommonCommit
            migratedFiles=[]
            for tree in githubTrees:
                migratedFiles.append(tree.name)
            migratedFiles=list(filter(lambda file:ghw.getWorkflowID(file)!=None,migratedFiles))
            skipBuildCheck=(len(migratedFiles)==0)
            while( not skipBuildCheck and not ghw.allWorkflowsAreSuccesful(migratedFiles,temp) ):
                print(repo.working_tree_dir+"commit"+temp.hexsha)
                temp=getNextGithubCommit(githubCommits,temp)
                if(temp==None):
                    #no next commit found 
                    skipBuildCheck=True
                    break
                githubTrees=getFilesFromCommitByPath(repo,temp.hexsha,'.github/workflows')
            if(skipBuildCheck==True):
                temp=mostRecentCommonCommit
                for tree in githubTrees:
                    migratedFiles.append(tree.name)
            print("migration commit found in "+repo.working_tree_dir + "at commit " +mostRecentCommonCommit.hexsha)
            print("at commit "+mostRecentCommonCommit.hexsha)
            writer.writerow([getRepoName(repo.working_tree_dir),temp.hexsha,migratedFiles])
            travisBlob=getFilesFromCommitByPath(repo,getCommitJustBefore(travisCommits,mostRecentCommonCommit).hexsha,'.travis.yml')
            writeToFile(travisBlob.data_stream.read().decode('utf-8'),"migrations_builds/"+genFileName(repo.working_tree_dir,'.travis.yml'))
            
            for githubTree in githubTrees:
                if(githubTree.name.endswith('.yml')):
                    writeToFile(githubTree.data_stream.read().decode('utf-8'),"migrations_builds/"+genFileName(repo.working_tree_dir,githubTree.name))
except HTTPException as e:
    print(e.code)
            
 



   



    

