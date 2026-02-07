import os
import subprocess
import pandas as pd
from pathlib import Path
from citranslation.utils.IOtools import saveCsvfile

def run(csv_path,save_path):
    base_dir = Path(__file__).resolve().parent.parent
    df = pd.read_csv(csv_path)
    for index, row in df.iterrows():
        # repo_name = "nextstrain/nextstrain.org"
        repo_name = row['repo_name']
        print(index,repo_name)
        if row["download"] != "success":
            try:
                returncode = download_repo(repo_name,save_path)
                if returncode != 0:
                    error_data = {'repo_name': repo_name,'error':'Download failed'}
                    error_path = base_dir / "resources" / "error.csv"
                    saveCsvfile(error_path,error_data)
                else:
                    col = "download"
                    checkout_repo(repo_name, row['commit_sha'], save_path)
                    if col not in df.columns:
                        df[col] = None 
                    df.loc[index, col] = "success"
                    df.to_csv(csv_path, index=False)
                    # print("success")

            except Exception as e:
                error_data = {'repo_name': repo_name,'error':str(e)}
                error_path = base_dir / "resources" / "error.csv"
                saveCsvfile(error_path,error_data)






def download_repo(repo_name,save_path):

    repo_url = f"https://github.com/{repo_name}"
    output_path = os.path.join(save_path, repo_name)
    os.makedirs(output_path, exist_ok=True)
    print(f"正在克隆仓库: {repo_url} 到 {output_path}")
    result = subprocess.run(["git", "clone", repo_url, output_path])

    return result.returncode



def checkout_repo(repo_name, commit_sha, save_path):
    output_path = os.path.join(save_path, repo_name)
    if not os.path.exists(output_path):
        print(f"仓库 {repo_name} 不存在，请先克隆仓库。")
        return

    print(f"正在切换仓库 {repo_name} 到提交 {commit_sha}")
    result = subprocess.run(["git", "checkout", commit_sha], cwd=output_path)

    return result.returncode