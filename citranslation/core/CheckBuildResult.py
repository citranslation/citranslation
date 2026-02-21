import os
import re
import io
import time
import shutil
import zipfile
import requests
import subprocess
import pandas as pd
from pathlib import Path
from citranslation.utils.genToken import github_token
from citranslation.utils.IOtools import saveYmlfile,readYmlfile


def run(repo_file,csv_path,file_name):

    base_dir = Path(__file__).resolve().parent.parent
    # print(base_dir)
    local_dir = base_dir.parent.parent/"test"/repo_file
    # print(local_dir)

    df = pd.read_csv(csv_path)
    for index, row in df.iterrows():
        # repo_name = "thegrill/grill"
        # run_id = '21128643914'
        # fetch_actions_logs(f"Tufuwu/{repo_file}", run_id,repo_name)
        # break
        repo_name = row['repo_name']

        # if row[file_name] != "success" and row[file_name] != "fail":
        if row[file_name] == "fail":

            print(index,repo_name,row[file_name])
            # break
            # file_path = base_dir/"resources"/"configration_data"/row["language"]/repo_name/"translation"/f"{file_name}.yml"
            file_path = base_dir/"resources"/"configration_data"/row["language"]/repo_name/"iterative"/f"{file_name}.yml"
            repo_path = base_dir.parent.parent/"repo"/f"repo_{row['language']}"/repo_name
            build_test(repo_file,repo_path,repo_name,file_path,local_dir)
            time.sleep(20)
            # if check_build_result(f"Tufuwu/{repo_file}",repo_name,file_name):
            #     col = file_name
            #     if col not in df.columns:
            #         df[col] = None 
            #     df.loc[index, col] = "success"
            #     df.to_csv(csv_path, index=False)
            #     print("build success")
            # else:
            #     col = file_name
            #     if col not in df.columns:
            #         df[col] = None 
            #     df.loc[index, col] = "fail"
            #     df.to_csv(csv_path, index=False)
            #     print("build fail")
        # break



def build_test(test_repo,repo_path,repo_name,file_path,local_dir):

    commit_message = f"Logs-{repo_name}"
    github_repo_url = f"https://github.com/Tufuwu/{test_repo}.git"
    inital_repo(local_dir,github_repo_url)
    delet_folder(local_dir)
    write_repo(repo_path,local_dir)
    replace_yml(file_path,local_dir)
    fix_case_sensitive_files(local_dir)
    push_repo(commit_message)


def inital_repo(local_dir,github_repo_url):
    # 确保Git仓库初始化
    os.chdir(local_dir)

    # 检查当前是否为Git仓库，如果没有则初始化
    if not os.path.isdir(".git"):
        subprocess.run(["git", "init"], check=True)
        print("Initialized a new git repository.")

    check_remote = subprocess.run(["git", "remote"], capture_output=True, text=True)
    if "origin" in check_remote.stdout:
        print("Remote 'origin' already exists. Removing it...")
        subprocess.run(["git", "remote", "remove", "origin"], check=True)

    # 设置远程仓库
    subprocess.run(["git", "remote", "add", "origin", github_repo_url], check=True)


def push_repo(commit_message):

    # 将所有更改加入暂存区
    subprocess.run(["git", "add", "."], check=True)

    # 检查工作区是否有改动
    status = subprocess.run(
        ["git", "status", "--porcelain"],
        capture_output=True,
        text=True
    )

    if status.stdout.strip() == "":
        print("No changes detected. Skipping commit and push.")
        return

    # 有改动才提交
    subprocess.run(["git", "commit", "-m", commit_message], check=True)
    print(f"Changes committed with message: {commit_message}")

    # 推送（你可以保留 -f 或去掉）
    subprocess.run(["git", "push", "origin", "main"], check=True)
    print("Changes pushed to GitHub.")


def delet_folder(local_dir):
    # delete all files and folders in local_dir except .git
    for item in os.listdir(local_dir):
        item_path = os.path.join(local_dir, item)

        if item == ".git" :
            continue  # keep .git folder

        if os.path.isdir(item_path):
            shutil.rmtree(item_path)  # delete folder
            print(f"Deleted folder: {item_path}")
        else:
            os.remove(item_path)  # delete file
            print(f"Deleted file: {item_path}")

    print(" Repository cleaned (except .git).")


def write_repo(repo_path, local_dir):
    for item in os.listdir(repo_path):
        if item == ".git":  
            continue  
        item_path = os.path.join(repo_path, item)
        destination_item_path = os.path.join(local_dir, item)

        if os.path.isdir(item_path):  
            # copytree 
            shutil.copytree(item_path, destination_item_path, ignore=shutil.ignore_patterns('.git'), dirs_exist_ok=True)
            print(f" Copied folder: {item_path} → {destination_item_path}")
        else:  
            # copy file
            shutil.copy(item_path, destination_item_path)
            print(f"Copied file: {item_path} → {destination_item_path}")

    print("copy source repo: success")

def replace_yml(file_path,local_dir):

    content = readYmlfile(file_path)
    lines = content.splitlines(keepends=True)
    lines = controlTriggerEvent(lines)
    content = ''.join(lines)
    workflow_path = local_dir/".github"
    delet_folder(workflow_path)
    workflow_path = workflow_path/"workflows"
    os.makedirs(workflow_path, exist_ok=True)
    # 写入到目标位置，覆盖原有文件
    file_path = workflow_path/"actions.yml"
    saveYmlfile(file_path, content)
    add_exec(content,local_dir)
    print(f" Replaced YML file: success")



def check_build_result(test_repo,repo_name,file_name):
    commit_sha = check_commit_history(test_repo,repo_name)
    result = check_run_result(test_repo, commit_sha,repo_name,file_name)
    if result == "success":
        return True
    else:
        return False



def check_commit_history(test_repo,repo_name):
    page = 1
    per_page = 100
    while True:
        url = f"https://api.github.com/repos/{test_repo}/commits"
        params = {
            "path": ".github/workflows/",
            "per_page": per_page,
            "page": page
        }

        headers = {
            "Authorization": f"token {github_token()}",
            "Accept": "application/vnd.github.v3+json"  
        }

        response = requests.get(url, headers=headers, params=params)
        if response.status_code == 200:
        
            datas = response.json()
            if not datas:
                break 
            for data in datas:
                commit_message = data['commit']['message']
                if re.search(f"{repo_name}",commit_message) :
                    return data["sha"]

            page += 1
        #print(data)
        else:
            break  

    return None   

def check_run_result(test_repo, commit_sha,repo_name,file_name):
    runs_url = f"https://api.github.com/repos/{test_repo}/commits/{commit_sha}/check-runs"
    headers = {
        "Authorization": f"token {github_token()}",
        "Accept": "application/vnd.github.v3+json"
    }

    while True:
        response = requests.get(runs_url, headers=headers)

        if response.status_code != 200:
            print("GitHub API error:", response.text)
            return None

        check_runs = response.json().get("check_runs", [])

        # 查找你需要的 actor
        for run in check_runs:

            # if run["actor"]["login"] == "Tufuwu":
                status = run["status"]          # queued / in_progress / completed
                conclusion = run["conclusion"]  # null / success / failure ...

                print(f"Status = {status}, Conclusion = {conclusion}")

                if status != "completed":
                    print("➡ Actions 还在运行，等待 30 秒后再次查询...")
                    time.sleep(60)
                    break  # 跳出 for，进入下一轮 while 重查

                # completed
                print("✔ Actions 已完成:", conclusion)
                run_id = get_run_id(run)
                fetch_actions_logs(test_repo, run_id,repo_name,file_name)
                # if conclusion == "success":
                #     run_id = get_run_id(run)
                #     # run_id = '21108477748'
                #     fetch_actions_logs(test_repo, run_id,repo_name,file_name)
                return conclusion

        else:
            # for 未 break（未找到 run）
            print("没有找到对应的 check-run")
            return None
        
def add_exec(content_lines, base_dir):

    content_str = "".join(content_lines)

    # 1) 匹配所有 .sh 和显式调用的 .py（如 ./xxx.py）
    #    捕获格式：./xxx.sh、xxx.sh、./xxx.py、xxx.py
    scripts = sorted(set(re.findall(
        r"(?:\.\/)?([a-zA-Z0-9_\-/]+\.(?:sh|py))",
        content_str
    )))

    # 2) 遍历 base_dir 文件，构建映射
    file_map = {}
    for root, dirs, files in os.walk(base_dir):
        for f in files:
            if f.endswith(".sh") or f.endswith(".py"):  # ← 支持 .py
                file_map.setdefault(f, []).append(os.path.join(root, f))

    found_scripts = []
    missing_scripts = []

    for script in scripts:
        filename = os.path.basename(script)
        if filename in file_map:
            for real_path in file_map[filename]:
                rel_path = os.path.relpath(real_path, base_dir).replace("\\", "/")
                subprocess.run(
                    ["git", "update-index", "--add", "--chmod=+x", rel_path],
                    cwd=base_dir, check=True
                )
                found_scripts.append(rel_path)
        else:
            missing_scripts.append(script)

    print({
        "detected_scripts": scripts,
        "found_scripts": found_scripts,
        "missing_scripts": missing_scripts
    })


def fetch_actions_logs(repo, run_id,repo_name,file_name):
    HEADERS = {
        "Authorization": f"Bearer {github_token()}",
        "Accept": "application/vnd.github+json"
    }
    url = f"https://api.github.com/repos/{repo}/actions/runs/{run_id}/logs"
    r = requests.get(url, headers=HEADERS)
    r.raise_for_status()

    output_dir = f"D:/vscode/3/CItranslation/citranslation/resources/logs/{repo_name}/{file_name}_log"
    # 确保输出目录存在
    os.makedirs(output_dir, exist_ok=True)

    zip_bytes = io.BytesIO(r.content)
    logs = {}

    with zipfile.ZipFile(zip_bytes) as z:
        for name in z.namelist():
            if name.endswith(".txt"):
                # 读取内容
                content = z.read(name).decode("utf-8", errors="ignore")
                logs[name] = content

                # 保留 zip 内的目录结构
                file_path = os.path.join(output_dir, name)
                os.makedirs(os.path.dirname(file_path), exist_ok=True)

                # 写入解压后的文件
                with open(file_path, "w", encoding="utf-8", errors="ignore") as f:
                    f.write(content)
    return logs

def get_run_id(run):
    url = run.get("html_url") or run.get("details_url")
    m = re.search(r"/actions/runs/(\d+)", url)
    return m.group(1) if m else None

def controlTriggerEvent(lines):
    result = []
    flag = 1
    flag2 = 0
    for line in lines:
        if(re).search(r'```yaml',line) and flag2==0:
            flag2 += 1
            continue
        if((re).search(r'```',line) and flag2!=0) or (re).search(r'\|End-of-Code\|',line):

            # result.append("\n")
            break
        if(re).search(r'master',line) and flag==1:
            line = re.sub(r'master','main',line)
        if(re).search(r'jobs',line):
            flag = 0
        result.append(line)

    return result

def fix_case_sensitive_files(repo_dir):
    """
    修复在 Linux / GitHub Actions 中必须大小写正确的文件
    """
    os.chdir(repo_dir)

    case_map = {
        "readme.md": "README.md",
        "license": "LICENSE",
        "contributing.md": "CONTRIBUTING.md",
    }

    for src, dst in case_map.items():
        if os.path.exists(src) and not os.path.exists(dst):
            subprocess.run(["git", "mv", src, dst], check=True)
            print(f"Fixed case: {src} -> {dst}")