import re
import os
import pandas as pd
from pathlib import Path
from openai import OpenAI
from sentence_transformers import SentenceTransformer, util
from citranslation.utils.genToken import openai_token,api_token
from citranslation.utils.IOtools import readYmlfile,saveYmlfile,saveCsvfile


def run(csv_path,dir_name,prompt_type):
    base_dir = Path(__file__).resolve().parent.parent
    model_tag = 'gemini3'
    df = pd.read_csv(csv_path)
    for index, row in df.iterrows():
        # repo_name = "alphagov/govuk-country-and-territory-autocomplete"
        repo_name = row['repo_name']
        print(index,repo_name)

        save_path = base_dir/'resources'/'configration_data'/row['language']/repo_name/dir_name/f'{model_tag}-{prompt_type}.yml'
        # save_path = base_dir/'resources'/'configration_data'/row['language']/repo_name/dir_name/f'{model_tag}.yml'
        # file_content = readYmlfile(file_path)
        # cosine_path = base_dir / "resources" / "csv" / "cosine_repo.csv" 
        # cosine_sim_cal(csv_path,repo_name,row['language'],cosine_path)
        # continue
        # prompt = gen_prompt(row,prompt_type)
        # print(prompt)
        # break
        try:
            prompt = gen_prompt(row,prompt_type)

            # print(prompt)

            translation_files = gen_files(model_tag,prompt)
            print(translation_files)
            saveYmlfile(save_path,translation_files)
        except:
            error_data = {'repo_name': repo_name,'error':'translate failed'}
            error_path = base_dir / "resources" / "error.csv"
            saveCsvfile(error_path,error_data)
        # break

def gen_files(model_tag, prompt):
    if model_tag == 'gpt-4o-mini':
        return gen_gpt4omini_files(prompt)
    elif model_tag == 'gpt-4o':
        return gen_gpt4o_files(prompt)
    elif model_tag == 'gemini3':
        return gen_gemini3_file(prompt)
    elif model_tag == 'gemini':
        return gen_gemini2pro_file(prompt)
    elif model_tag == 'gpt5.1-mini':
        return gen_gpt5_file(prompt)
    elif model_tag == 'gpt5.1-code':  
        return gen_gpt5code_file(prompt)

def gen_gpt4omini_files(prompt):
    client = OpenAI(api_key=openai_token())
    completion = client.chat.completions.create(
        model='gpt-4o-mini',
        messages=[{"role": "user", "content": prompt}],
        temperature=0
    )

    return completion.choices[0].message.content 

def gen_gpt4o_files(prompt):
    client = OpenAI(api_key=openai_token())
    completion = client.chat.completions.create(
        model='gpt-4o',
        messages=[{"role": "user", "content": prompt}],
        temperature=0
    )

    return completion.choices[0].message.content 

def prompt_constructor(prompt_path):

    with open(prompt_path, 'r') as file:
            prompt = file.read()
    return prompt

def gen_prompt(row,prompt_type):
    base_dir = Path(__file__).resolve().parent.parent
    file_path = base_dir/'resources'/'configration_data'/row['language']/row['repo_name']/f'travis.yml'
    file_content = readYmlfile(file_path)
    if prompt_type == 'base':
        prompt_path = base_dir/'resources'/'prompts'/prompt_type
        prompt_template = prompt_constructor(prompt_path)
        prompt = prompt_template.format(source_travis_content =file_content,SOURCE_LANG = "Travis CI",TARGET_LANG = "GitHub Actions")
    elif prompt_type == 'oneshot':
        prompt_path = base_dir/'resources'/'prompts'/prompt_type
        prompt_template = prompt_constructor(prompt_path)
        travis_path = base_dir/'resources'/'configration_data'/row['ref_repo_1']/f'travis.yml'
        travis_content = readYmlfile(travis_path)
        actions_path = base_dir/'resources'/'configration_data'/row['ref_repo_1']/f'actions.yml'
        actions_content = readYmlfile(actions_path)
        prompt_template = prompt_constructor(prompt_path)
        prompt = prompt_template.format(source_travis_content =file_content,ref_travis_content=travis_content,ref_actions_content=actions_content,SOURCE_LANG = "Travis CI",TARGET_LANG = "GitHub Actions")
    elif prompt_type == 'fewshot':
        prompt_path = base_dir/'resources'/'prompts'/prompt_type
        prompt_template = prompt_constructor(prompt_path)
        travis_path_1 = base_dir/'resources'/'configration_data'/row['ref_repo_1']/f'travis.yml'
        travis_content_1 = readYmlfile(travis_path_1)
        actions_path_1 = base_dir/'resources'/'configration_data'/row['ref_repo_1']/f'actions.yml'
        actions_content_1 = readYmlfile(actions_path_1)

        travis_path_2 = base_dir/'resources'/'configration_data'/row['ref_repo_2']/f'travis.yml'
        travis_content_2 = readYmlfile(travis_path_2)
        actions_path_2 = base_dir/'resources'/'configration_data'/row['ref_repo_2']/f'actions.yml'
        actions_content_2 = readYmlfile(actions_path_2)

        travis_path_3 = base_dir/'resources'/'configration_data'/row['ref_repo_3']/f'travis.yml'
        travis_content_3 = readYmlfile(travis_path_3)
        actions_path_3 = base_dir/'resources'/'configration_data'/row['ref_repo_3']/f'actions.yml'
        actions_content_3 = readYmlfile(actions_path_3)

        prompt = prompt_template.format(
            source_travis_content =file_content,
            ref_travis_content_1=travis_content_1,
            ref_actions_content_1=actions_content_1,
            ref_travis_content_2=travis_content_2,
            ref_actions_content_2=actions_content_2,
            ref_travis_content_3=travis_content_3,
            ref_actions_content_3=actions_content_3,
            SOURCE_LANG = "Travis CI",
            TARGET_LANG = "GitHub Actions"
            )
    elif prompt_type == 'guideline':
        prompt_path = base_dir/'resources'/'prompts'/prompt_type
        prompt_template = prompt_constructor(prompt_path)
        prompt = prompt_template.format(source_travis_content =file_content,SOURCE_LANG = "Travis CI",TARGET_LANG = "GitHub Actions")
    return prompt


def gen_gpt5_file(prompt):
    client = OpenAI(
        base_url="https://openrouter.ai/api/v1",
        api_key=api_token(),
    )
    completion = client.chat.completions.create(
        model="openai/gpt-5-mini",
        temperature=0,  
        extra_headers={
            "HTTP-Referer": "<YOUR_SITE_URL>",  # 可选
            "X-Title": "<YOUR_SITE_NAME>",       # 可选
        },
        messages=[
            {
                "role": "user",
                "content": prompt
            }
        ]
    )

    reply = completion.choices[0].message.content
    return reply

def gen_gpt5code_file(prompt):
    client = OpenAI(
        base_url="https://openrouter.ai/api/v1",
        api_key=api_token(),
    )
    completion = client.chat.completions.create(
        model="openai/gpt-5.1-codex",
        temperature=0,  
        extra_headers={
            "HTTP-Referer": "<YOUR_SITE_URL>",  # 可选
            "X-Title": "<YOUR_SITE_NAME>",       # 可选
        },
        messages=[
            {
                "role": "user",
                "content": prompt
            }
        ]
    )

    reply = completion.choices[0].message.content
    return reply

def gen_gemini3_file(prompt):
    client = OpenAI(
        base_url="https://openrouter.ai/api/v1",
        api_key=api_token(),
    )
    response = client.chat.completions.create(
        extra_body={},
        model="google/gemini-3-flash-preview",
        temperature=0,
        messages=[
            {
                "role": "user",
                "content": prompt
            }
        ]
    )

    reply = response.choices[0].message.content
    return reply
def gen_gemini3_file(prompt):
    client = OpenAI(
        base_url="https://openrouter.ai/api/v1",
        api_key=api_token(),
    )
    response = client.chat.completions.create(
        extra_body={},
        model="google/gemini-3-flash-preview",
        temperature=0,
        messages=[
            {
                "role": "user",
                "content": prompt
            }
        ]
    )

    reply = response.choices[0].message.content
    return reply


def gen_gemini2pro_file(prompt):
    client = OpenAI(
        base_url="https://openrouter.ai/api/v1",
        api_key=api_token(),
    )
    response = client.chat.completions.create(
        extra_body={},
        model="google/gemini-2.5-pro",
        temperature=0,
        messages=[
            {
                "role": "user",
                "content": prompt
            }
        ]
    )

    reply = response.choices[0].message.content
    return reply

def remove_comments(text):
    if not isinstance(text, str):
        return ''
    flag = 0
    lines = []
    for line in text.splitlines():
        stripped = line.strip()
        if stripped.startswith('#'):
            continue
        if stripped.startswith('```') and flag ==0:
            flag += 1
            continue
        if stripped.startswith('|End-of-Code|') or (stripped.startswith('```') and flag !=0):
            break
        line_no_inline = re.sub(r'(?<!["\'])#.*', '', line)
        if line_no_inline.strip() != '':
            lines.append(line_no_inline.rstrip())
    return '\n'.join(lines)


# SentenceTransformerModelPath = 'all-MiniLM-L6-v2'
# model = SentenceTransformer(SentenceTransformerModelPath, device='cpu')
def compute_consie_similarity(reference_path, candidate_path):
    code = remove_comments(readYmlfile(reference_path))
    llm_output = remove_comments(readYmlfile(candidate_path))
    code_embedding = model.encode(str(code), convert_to_tensor=True, device='cpu')
    llm_output_embedding = model.encode(str(llm_output), convert_to_tensor=True, device='cpu')
    similarity = util.pytorch_cos_sim(code_embedding, llm_output_embedding)
    return similarity.item()


def cosine_sim_cal(csv_path, repo_name,language,save_path):
    df = pd.read_csv(csv_path)

    base_dir = Path(__file__).resolve().parent.parent
    reference_path = (
        base_dir / 'resources' / 'configration_data' / language / repo_name / 'travis.yml'
    )

    results = []

    for _, row in df.iterrows():
        if row['repo_name'] == repo_name:
            continue

        candidate_path = (
            base_dir / 'resources' / 'configration_data' / row['language'] / row['repo_name'] / 'travis.yml'
        )

        sim = compute_consie_similarity(reference_path, candidate_path)

        results.append({
            'ref_repo': f"{row['language']}/{row['repo_name']}",
            'cosine_similarity': sim
        })

    # 取相似度最高的 5 个
    top5 = (
        pd.DataFrame(results)
        .sort_values(by='cosine_similarity', ascending=False)
        .head(5)
    )

    # 构造一行数据：一个 repo_name 一行
    row_data = {
        'repo_name': repo_name,
        'ref_repo_1': top5.iloc[0]['ref_repo'],
        'ref_repo_2': top5.iloc[1]['ref_repo'],
        'ref_repo_3': top5.iloc[2]['ref_repo'],
        'ref_repo_4': top5.iloc[3]['ref_repo'],
        'ref_repo_5': top5.iloc[4]['ref_repo'],
    }

    result_df = pd.DataFrame([row_data])

    # 判断是否已存在 CSV，决定是否写表头
    write_header = (
        not os.path.exists(save_path)
        or os.path.getsize(save_path) == 0
    )

    result_df.to_csv(
        save_path,
        mode='a',
        header=write_header,
        index=False
    )