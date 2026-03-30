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


from ..utils.genToken import openai_token,api_token,github_token
from ..utils.IOtools import readYmlfile,saveYmlfile,saveCsvfile
from ..actions_remaker.gha_dispatcher import GHADispatcher
from ..actions_remaker.result_comparer import ResultComparer

def run(repo_name, language, test_repo, strategy):
    base_dir = Path(__file__).resolve().parent.parent
    local_dir = base_dir.parent/'tests'/test_repo
    print(local_dir)
    index = 0
    message = []
    translation_prompt = gen_base_prompt(language,repo_name)
    message.append({"role": "user","content": translation_prompt})
    build_result = None


    # init local test repo
    repo_path = base_dir/'resources'/'repo'/repo_name
    github_repo_url = get_github_url(local_dir)
    inital_repo(local_dir,github_repo_url)
    delet_folder(local_dir)
    write_repo(repo_path,local_dir)

    # run test
    file_path = base_dir/'resources'/'datasets'/language/repo_name/'enhancement'/f'gemini3-{strategy}.yml'
    build_test(repo_name, file_path, local_dir)

    model_tag = 'gemini3'
    build_result, log_content = check_build_result(local_dir)

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
        log_a = base_dir/'resources'/'logs'/repo_name/'actions_log'
        log_b = base_dir/'resources'/'logs'/repo_name/f'{model_tag}-iterative-{index}_log'
        save_logs_dict(log_content, log_b)
        temp = compare_two_github_actions_logs(log_a, log_b, build_system=None, force=0)



    json_path = base_dir/'resources'/'iterative_message'/language/repo_name/f'{model_tag}-iterative-message.json'
    json_path.parent.mkdir(parents=True, exist_ok=True)
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(message, f, ensure_ascii=False, indent=2)


def prompt_constructor(prompt_path):
    with open(prompt_path, 'r') as file:
            prompt = file.read()
    return prompt

def gen_base_prompt(language,repo_name):
    base_dir = Path(__file__).resolve().parent.parent
    file_path = base_dir/'resources'/'datasets'/language/repo_name/f'travis.yml'
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
    timestamp_pattern = re.compile(r'^/d{4}-/d{2}-/d{2}T/d{2}:/d{2}:/d{2}/./d+Z/s*')
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
    
    return "/n".join(extracted)

def check_build_result(repo_path):
    try:
        commit_sha = get_head_commit(repo_path)

        token = github_token()
        if not token:
            raise RuntimeError("GITHUB_TOKEN not set")

        headers = {
            "Authorization": f"token {token}",
            "Accept": "application/vnd.github+json"
        }

        owner, repo = get_repo_info(repo_path)
        start = time.time()

        while True:
            runs_url = f"https://api.github.com/repos/{owner}/{repo}/commits/{commit_sha}/check-runs"
            headers = {
                "Authorization": f"token {github_token()}",
                "Accept": "application/vnd.github.v3+json"
            }

            while True:
                response = requests.get(runs_url, headers=headers)

                if response.status_code != 200:
                    print("GitHub API error:", response.text)
                    return 'not_triggered', "No workflow triggered for this commit"

                check_runs = response.json().get("check_runs", [])

                if not check_runs:

                    print("➡ Actions still running, waiting for 20s before re-checking...")
                    time.sleep(20)
                    continue

                # completed
                run = check_runs[0]
                status = run["status"]          # queued / in_progress / completed
                conclusion = run["conclusion"]  # null / success / failure ...
                if status != "completed":
                    print("➡ Actions still running, waiting for 40s before re-checking...")
                    time.sleep(40)
                    continue

                print(f"Status = {status}, Conclusion = {conclusion}")
                print("✔ Actions completed:", conclusion)

                run_id = get_run_id(run)

                url = f"https://api.github.com/repos/{owner}/{repo}/actions/runs/{run_id}/logs"
                r = requests.get(url, headers=headers)
                r.raise_for_status()
                zip_bytes = io.BytesIO(r.content)
                logs = {}

                with zipfile.ZipFile(zip_bytes) as z:
                    for name in z.namelist():
                        if name.endswith(".txt") and "/" not in name:
                            # reading log content and decode to string
                            content = z.read(name).decode("utf-8", errors="ignore")
                            logs[name] = content


                return conclusion, logs


    except Exception as e:
        print(f"Error: {e}")
        return 'error', str(e)



def get_head_commit(repo_path: Path):
    """获取当前 HEAD commit"""
    return subprocess.run(
        ["git", "rev-parse", "HEAD"],
        cwd=repo_path,
        capture_output=True,
        text=True,
        check=True
    ).stdout.strip()


def get_repo_info(repo_path: Path):
    try:
        origin_url = subprocess.run(
            ["git", "remote", "get-url", "origin"],
            cwd=repo_path,
            capture_output=True,
            text=True,
            check=True
        ).stdout.strip()
    except subprocess.CalledProcessError:
        raise RuntimeError("Not a git repository or missing origin")

    match = re.search(r"(?:github\.com[:/])(.+)/([^.]+)(?:\.git)?", origin_url)
    if not match:
        raise RuntimeError(f"Cannot parse GitHub repo from {origin_url}")

    owner_part, repo = match.group(1), match.group(2)
    owner = owner_part.split("/")[-1]

    return owner, repo




def get_run_id(run):
    url = run.get("html_url") or run.get("details_url")
    m = re.search(r"/actions/runs/(\d+)", url)
    return m.group(1) if m else None                





def fetch_and_extract_logs(repo_name, run_id, headers):

    url = f"https://api.github.com/repos/{repo_name}/actions/runs/{run_id}/logs"
    
    # 1. fetch logs as zip
    resp = requests.get(url, headers=headers, timeout=30)
    resp.raise_for_status()
    zip_bytes = resp.content

    # 2️. extract logs from zip
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
    return "/n".join(all_contents)

def save_logs_dict(logs: dict, log_dir: Path):
    log_dir.mkdir(parents=True, exist_ok=True)

    for name, content in logs.items():
        # keep the same name as in the zip, and save to log_dir
        file_path = log_dir / name
        file_path.parent.mkdir(parents=True, exist_ok=True)

        with file_path.open("w", encoding="utf-8") as f:
            f.write(content)

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
    subprocess.run(["git", "add", "."], check=True)

    status = subprocess.run(
        ["git", "status", "--porcelain"],
        capture_output=True,
        text=True
    )

    if status.stdout.strip() == "":
        # no changes → create empty commit to trigger CI
        subprocess.run(
            ["git", "commit", "--allow-empty", "-m", commit_message],
            check=True
        )
        print("Empty commit created (trigger CI)")
    else:
        # have changes → normal commit
        subprocess.run(
            ["git", "commit", "-m", commit_message],
            check=True
        )
        print("Normal commit created")

    subprocess.run(["git", "push", "origin", "main"], check=True)

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
    # writeYmlfile(workflow_path/"actions.yml", content)
    file_path = workflow_path/"actions.yml"
    saveYmlfile(file_path, content)
    print(f" Replaced YML file: success")

def controlTriggerEvent(lines):
    result = []
    started = False   # flag to indicate if we've started copying lines
    before_jobs = True

    for line in lines:
        # 1. start condition (include that line)
        if not started:
            if re.search(r'^name\s*:', line):
                started = True
                result.append(line)
            continue

        # 2. end condition
        if re.search(r'^```', line) or re.search(r'\|End-of-Code\|', line):
            break

        # 3. remove "on: pull_request" and "on: push"
        if re.search(r'^\s*jobs\s*:', line):
            before_jobs = False

        # 4. special handling for branch name in "on: push" if it exists before "jobs"
        if before_jobs:
            line = re.sub(r'\bmaster\b', 'main', line)

        result.append(line)

    return result

def get_github_url(repo_path):
    path_obj = Path(repo_path).resolve()
    origin_url = subprocess.run(
    ["git", "remote", "get-url", "origin"],
    cwd=path_obj,
    capture_output=True,
    text=True,
    check=True
).stdout.strip()
    return origin_url