import re
import os
import io
import json
import time
import shutil
import zipfile
import requests
import subprocess
from pathlib import Path
from openai import OpenAI


from ..utils.genToken import openai_token,api_token
from ..utils.IOtools import readYmlfile,saveYmlfile,saveCsvfile
from ..actions_remaker.gha_dispatcher import GHADispatcher
from ..actions_remaker.result_comparer import ResultComparer

def run(repo_name, language, test_repo, strategy):
    base_dir = Path(__file__).resolve().parent.parent
    local_dir = base_dir/'resources'/'test'/test_repo
    index = 0
    message = []
    translation_prompt = gen_base_prompt(language,repo_name)
    message.append({"role": "user","content": translation_prompt})
    build_result = None

    # init local test repo
    repo_path = base_dir/'resources'/'datasets'/language/repo_name/'enhancement'/f'gemini3-{strategy}.yml'
    github_repo_url = f"https://github.com/{test_repo}.git"
    inital_repo(local_dir,github_repo_url)
    delet_folder(local_dir)
    write_repo(repo_path,local_dir)

    # run test
    build_test(test_repo,repo_path,repo_name,repo_path,local_dir)

    model_tag = 'gemini3'
    build_result, log_content = check_build_result(response)
    while index < 6 and build_result != "success":
        try:
            if build_result == "failed":
                error_message = filter_log_content(log_content)
            else:
                error_message = log_content
            message.append({"role": "user","content": gen_iterative_prompt(error_message)})
            response = gen_gemini3_file(error_message)
            yml_path = base_dir/'resources'/'datasets'/language/repo_name/'iterative'/f'{model_tag}-iterative-{index}.yml'
            saveYmlfile(yml_path,response)
            message.append({"role": "assistant","content": response})
            build_test(repo_name,yml_path,local_dir)
            build_result, log_content = check_build_result(response)
            index += 1

        except:
            error_data = {'repo_name': repo_name,'error':'translate failed'}
            error_path = base_dir / "resources" / "error.csv"
            saveCsvfile(error_path,error_data)
            # break
    if build_result == "success":
        log_a_path = base_dir/'resources'/'logs'/language/repo_name/'actions_log'
        log_a = read_all_txt_logs(log_a_path)
        log_b = log_content
        compare_two_github_actions_logs(log_a, log_b, build_system=None, force=0)


    json_path = base_dir/'resources'/'iterative_message'/language/repo_name/f'{model_tag}-iterative-message.json'
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(message, f, ensure_ascii=False, indent=2)


def prompt_constructor(prompt_path):
    with open(prompt_path, 'r') as file:
            prompt = file.read()
    return prompt

def gen_base_prompt(language,repo_name):
    base_dir = Path(__file__).resolve().parent.parent
    file_path = base_dir/'resources'/'configration_data'/language/repo_name/f'travis.yml'
    file_content = readYmlfile(file_path)
    prompt_path = base_dir/'resources'/'prompts'/'origin'
    prompt_template = prompt_constructor(prompt_path)
    prompt = prompt_template.format(source_content =file_content)
    return prompt

def gen_iterative_prompt(error_message):
    base_dir = Path(__file__).resolve().parent.parent
    prompt_path = base_dir/'resources'/'prompts'/'iterative'
    prompt_template = prompt_constructor(prompt_path)
    prompt = prompt_template.format(error_message = error_message )
    return prompt

def gen_gemini3_file(message):
    client = OpenAI(
        base_url="https://openrouter.ai/api/v1",
        api_key=api_token(),
    )
    response = client.chat.completions.create(
        extra_body={},
        model="google/gemini-3-flash-preview",
        temperature=0,
        messages=message
    )

    reply = response.choices[0].message.content
    return reply

def filter_log_content(raw_text):
    timestamp_pattern = re.compile(r'^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d+Z\s*')
    lines = raw_text.splitlines()
    extracted = []
    start_collecting = False
    
    for line in lines:
        # 1. remove timestamp
        clean_line = timestamp_pattern.sub('', line)
        
        # 2. check end condition
        if "Post job cleanup." in clean_line:
            break
            
        # 3. check start condition
        if not start_collecting and "ERROR:" in clean_line:
            start_collecting = True
        
        if start_collecting:
            extracted.append(clean_line)
    
    return "\n".join(extracted)

def check_build_result(repo_path,repo_name):
    try:
        commit_sha = get_head_commit(repo_path)

        token = os.getenv("GITHUB_TOKEN")
        if not token:
            raise RuntimeError("GITHUB_TOKEN not set")

        headers = {
            "Authorization": f"token {token}",
            "Accept": "application/vnd.github+json"
        }


        result, state = wait_for_completion(repo_name, commit_sha, headers)

        if state == "NOT_TRIGGERED":
            return "NOT_TRIGGERED", "No workflow triggered for this commit"


        if state == "TIMEOUT":
            return "TIMEOUT", "Workflow did not complete within the expected time frame"

        zip_bytes = fetch_and_extract_logs(repo_name, result["id"], headers)

        if state == "COMPLETED" and result["conclusion"] == "success":
            return "success", zip_bytes
        
        return "failed", zip_bytes


    except Exception as e:
        print(f"Error: {e}")



def get_head_commit(repo_path: Path):
    """获取当前 HEAD commit"""
    return subprocess.run(
        ["git", "rev-parse", "HEAD"],
        cwd=repo_path,
        capture_output=True,
        text=True,
        check=True
    ).stdout.strip()


def get_branch(repo_path: Path):
    """获取当前分支"""
    return subprocess.run(
        ["git", "rev-parse", "--abbrev-ref", "HEAD"],
        cwd=repo_path,
        capture_output=True,
        text=True,
        check=True
    ).stdout.strip()


def fetch_runs(repo_name, commit_sha, headers):
    url = f"https://api.github.com/repos/{repo_name}/actions/runs?head_sha={commit_sha}"
    resp = requests.get(url, headers=headers, timeout=10)
    resp.raise_for_status()
    return resp.json().get("workflow_runs", [])

def wait_for_completion(repo_name, commit_sha, headers):
    """
    wait_for_completion
    """
    start = time.time()

    while True:
        runs = fetch_runs(repo_name, commit_sha, headers)

        if not runs:
            return None, "NOT_TRIGGERED"

        latest = runs[0]
        status = latest["status"]
        conclusion = latest["conclusion"]

        print(f"⏳ Status: {status} (elapsed: {int(time.time() - start)}s)")

        if status == "completed":
            return latest, "COMPLETED"

        if time.time() - start > 1200:
            return latest, "TIMEOUT"

        time.sleep(60)


def fetch_and_extract_logs(repo_name, run_id, headers):

    url = f"https://api.github.com/repos/{repo_name}/actions/runs/{run_id}/logs"
    
    # 1️⃣ 下载 zip
    resp = requests.get(url, headers=headers, timeout=30)
    resp.raise_for_status()
    zip_bytes = resp.content

    # 2️⃣ 解压并读取文本文件内容
    extracted = []
    with zipfile.ZipFile(io.BytesIO(zip_bytes)) as z:
        for name in z.namelist():
            if not name.endswith(".txt"):
                continue

            with z.open(name) as f:
                content = f.read().decode("utf-8", errors="ignore")
                extracted.append((name, content))

    return extracted


def compare_two_github_actions_logs(log_a, log_b, build_system=None, force=0):
    """
    Compare two GitHub Actions logs directly and determine whether they are equivalent.

    :param log_a: path to first log file
    :param log_b: path to second log file
    :param build_system: optional build system hint (e.g., maven, gradle)
    :param force: force analyzer (used for Java)
    :return: (match: bool, mismatched_attributes: dict)
    """

    dispatcher = GHADispatcher()

    # job_id is only used as an identifier; use dummy but consistent value
    dummy_job_id = 'local_compare'

    result_a = dispatcher.analyze(
        log_path=log_a,
        job_id=dummy_job_id,
        build_system=build_system,
        trigger_sha=None,
        repo=None,
        force=force
    )

    result_b = dispatcher.analyze(
        log_path=log_b,
        job_id=dummy_job_id,
        build_system=build_system,
        trigger_sha=None,
        repo=None,
        force=force
    )
    print(result_a)

    return ResultComparer.compare_attributes(result_a, result_b)


def read_all_txt_logs(log_dir):
    
    if not log_dir.exists() or not log_dir.is_dir():
        raise FileNotFoundError(f"Log directory not found: {log_dir}")

    all_contents = []

    # read all txt files in the log directory and concatenate their contents
    for txt_file in sorted(log_dir.glob("*.txt")):  
        with txt_file.open("r", encoding="utf-8", errors="ignore") as f:
            content = f.read()
            all_contents.append(f"\n--- {txt_file.name} ---\n{content}")

    # concat all contents into a single string
    return "\n".join(all_contents)


def build_test(repo_name,file_path,local_dir):
    commit_message = f"Logs-{repo_name}"

    replace_yml(file_path,local_dir)
    push_repo(commit_message)

def inital_repo(local_dir,github_repo_url):
    # ensure local_dir exists
    os.chdir(local_dir)

    # check if .git exists
    if not os.path.isdir(".git"):
        subprocess.run(["git", "init"], check=True)
        print("Initialized a new git repository.")

    check_remote = subprocess.run(["git", "remote"], capture_output=True, text=True)
    if "origin" in check_remote.stdout:
        print("Remote 'origin' already exists. Removing it...")
        subprocess.run(["git", "remote", "remove", "origin"], check=True)

    # setup remote
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
    print(f" Replaced YML file: success")

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