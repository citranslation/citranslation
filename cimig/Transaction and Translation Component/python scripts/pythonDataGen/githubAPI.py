from ghapi.all import *
from utilFunctions import is_over_core_rate,sleep_until_core_rate_reset
def get_github_token(): # TODO add github token here
    return "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" #generate a token from github and add here

class githubAPIWrapper:
    def __init__(self, owner,repo):
        self.owner = owner
        self.repo=repo
        self.github=GhApi(owner,repo,get_github_token())

    def getWorkflowID(self,fileName):
        res=self.github.actions.list_repo_workflows()
        for workflow in res.get('workflows'):
            if workflow.get('path').endswith(fileName):
                return workflow.get('id')
        
    def getBuildFromCommit(self,commitID,workflowID):
        while (True):
            try:
                p=1
                workflowRuns=self.github.actions.list_workflow_runs(workflowID,page=p,per_page=100)
                total_count=workflowRuns.get('total_count')
                if(total_count==0):
                    return None
                if(total_count%100==0):
                    numberOfPages=total_count//100
                else:
                    numberOfPages=(total_count//100)+1
                while(numberOfPages>0):
                    workflowRuns=self.github.actions.list_workflow_runs(workflowID,page=numberOfPages,per_page=100)
                    workflowRuns=workflowRuns.get('workflow_runs')
                    for run in workflowRuns:
                        if(run.get('head_sha')==commitID.hexsha):
                            return run
                    numberOfPages-=1
                if(numberOfPages==0):
                    #got to zero without finding build build does not exist
                    return None
            except Exception as e:
                if(is_over_core_rate(self.github) or "API rate limit exceeded" in str(e) or "403" in str(e)):
                    print(e)
                    sleep_until_core_rate_reset(self.github)
                else:
                    print(e)
                    break

    def isBuildSuccesful(self,workflowID,commitID):
        build=self.getBuildFromCommit(commitID,workflowID)
        if(build==None):
            #if no builds are found assume it is succesful
            return True
        return build.get('conclusion')=='success'
    
    def isWorkflowCommitSuccesful(self,fileName,commitID):
        workflowID=self.getWorkflowID(fileName)
        return self.isBuildSuccesful(workflowID,commitID)
    
    def allWorkflowsAreSuccesful(self,migratedFiles,commitID):
        for file in migratedFiles:
            fileName=file.split('/')[-1]
            if(not self.isWorkflowCommitSuccesful(fileName,commitID)):
                return False
        return True

    def allChecksSuccesful(self,ref):
        try:
            checks=self.github.checks.list_for_ref(ref)
            for check in checks.get('check_runs'):
                if(check.get('status')!='completed' or check.get('conclusion')!='success'):
                    return False
            return True
        except Exception as e:
            if(is_over_core_rate(self.github) or "API rate limit exceeded" in str(e) or "403" in str(e)):
                print(e)
                sleep_until_core_rate_reset(self.github)
                return self.allChecksSuccesful(ref)
            else:
                print(e)
                
    



#g=githubAPIWrapper("Activiti","Activiti")

#wid=g.getWorkflowID('main.yml')
#print(g.getBuildFromCommit('23a6423bd40f04416a918fe4d47244202aacc7b1',wid))
#print(g.isBuildSuccesful(wid,'23a6423bd40f04416a918fe4d47244202aacc7b1'))


