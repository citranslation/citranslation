import re
import requests
import pandas as pd
from pathlib import Path
from citranslation.utils.genToken import github_token
from citranslation.utils.IOtools import saveCsvfile

def run(csv_path,save_path):
    base_dir = Path(__file__).resolve().parent.parent
    df = pd.read_csv(csv_path)
    for index, row in df.iterrows():
        # repo_name = "otsaloma/nfoview"
        repo_name = row['repo_name']
        print(index,repo_name)
        try:
            commit = CheckMigCommit(repo_name)
            for c in commit:
                b = c['commit']['message']
                # print(b)
                if (re_match("Migrate",b) or re_match('Move',b) or re_match('Replace',b) or re_match('switch',b)) and re_match('travis',b) and re_match('actions',b):

                    new_data = {'repo_name': repo_name,'commit_sha':c['sha'],'commit_date':c['commit']['committer']['date']}
                    saveCsvfile(save_path,new_data)
                    break

        except Exception as e:
            error_data = {'repo_name': repo_name,'error':str(e)}
            error_path = base_dir / "resources" / "error.csv"
            saveCsvfile(error_path,error_data)



def CheckMigCommit(repo_name):
    file_path = '.github/workflows/'
    page = 1
    per_page = 100
    result = []
    while True:
        url = f"https://api.github.com/repos/{repo_name}/commits"
        params = {
            "path": file_path,
            "per_page": per_page,
            "page": page
        }
        # print(github_token())
        headers = {
            "Authorization": f"token {github_token()}",
            "Accept": "application/vnd.github.v3+json"  
        }

        response = requests.get(url, headers=headers, params=params)
        print(response.status_code)

        if response.status_code == 200:
        
            data = response.json()
            if not data:
                break 
            result += data
            page += 1
        #print(data)
        else:
            break 
    return result


def re_match(tagert,string):
    return re.search(tagert,string,re.I)