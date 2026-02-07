import os
from datetime import datetime

import pause
from dateutil import tz
def getFilesFromCommitByPath(repo,commit,path):
    treeAtCommit = repo.tree(commit)
    targetfile = treeAtCommit / path
    return targetfile

def getCommitJustBefore(listOfCommits,commit):
    indexOfCommit=listOfCommits.index(commit)
    if(listOfCommits[-1]==commit): return None
    return listOfCommits[indexOfCommit+1]

#write a blob to a file and create path if doesn't exist
def writeToFile(blobTowWrite,fileName):
    os.makedirs(os.path.dirname(fileName), exist_ok=True)
    with open(fileName, 'w+',encoding="utf-8") as f:
        f.write(blobTowWrite)

def fileExistsInTreeAtCommit(commit,fileName):
    return fileName in commit.tree


#generate filename following the convention repo_author/repo_name/file_name
def genFileName(fullDirPath,fileName):
    pathArray=fullDirPath.split('\\')
    return pathArray[-2]+'/'+pathArray[-1]+'/'+fileName

def getRepoName(fullDirPath):
    pathArray=fullDirPath.split('\\')
    return pathArray[-2]+'/'+pathArray[-1]

def getOwnerName(fullDirPath):
    pathArray=fullDirPath.split('\\')
    return pathArray[-2]

def getRepositoryName(fullDirPath):
    pathArray=fullDirPath.split('\\')
    return pathArray[-1]    

def is_over_core_rate(github):
    if(github.rate_limit.get().resources.core.remaining==0):
        return True
    if 'remaining=0' in str(github.rate_limit.get().resources.core):
        return True
    return False

def is_over_search_rate(github):
    if 'remaining=0' in str(github.rate_limit().search):
        return True
    return False


def sleep_until_core_rate_reset(github):
    dateutc = github.rate_limit.get().resources.core.reset
    from_zone = tz.gettz('UTC')
    to_zone = tz.gettz('UTC+1')
    utc = dateutc.replace(tzinfo=from_zone)
    central = utc.astimezone(to_zone)
    t = datetime.now()
    t = t.astimezone(to_zone)
    seconds = min((central - t).seconds, 3600)+180
    print('sleeping for ' + str(seconds) + ' seconds starting at' + str(t))
    pause.seconds(seconds)

def sleep_until_search_rate_reset(github):
    print(github.get_rate_limit().search)
    dateutc = github.get_rate_limit().search.reset
    from_zone = tz.gettz('UTC')
    to_zone = tz.gettz('America/New_York')
    utc = dateutc.replace(tzinfo=from_zone)
    central = utc.astimezone(to_zone)
    t = datetime.now()
    t = t.astimezone(to_zone)
    seconds = min((central - t).seconds, 600)+180
    print('sleeping for ' + str(seconds) + ' seconds starting at' + str(t))
    pause.seconds(seconds)
