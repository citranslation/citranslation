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
    # model_tag = 'gemini3-guideline'
    model_tag = 'gemini3'
    count = '4'
    df = pd.read_csv(csv_path)
    for index, row in df.iterrows():
        # repo_name = "alphagov/govuk-country-and-territory-autocomplete"
        repo_name = row['repo_name']
        language = row['language']
        if row['gemini3-iterative-4'] =="fail":
        # if row['gemini3-guideline-iterative-4'] =="fail":
            print(index,repo_name)

            save_path = base_dir/'resources'/'configration_data'/language/repo_name/dir_name/f'{model_tag}-{prompt_type}-{count}.yml'
        # save_path = base_dir/'resources'/'configration_data'/row['language']/repo_name/dir_name/f'{model_tag}.yml'
        # file_content = readYmlfile(file_path)
        # cosine_path = base_dir / "resources" / "csv" / "cosine_repo.csv" 
        # cosine_sim_cal(csv_path,repo_name,row['language'],cosine_path)
        # continue
        # prompt = gen_prompt(row,prompt_type)
        # print(prompt)
        # break
            # message_path = base_dir/'resources'/'csv'/'error_message'/'guideline.csv'
            message_path = base_dir/'resources'/'csv'/'error_message'/'base.csv'
            df1 = pd.read_csv(message_path,encoding="gbk")
            error_message_1 = df1.loc[index, "1"]
            error_message_2 = df1.loc[index, "2"]
            error_message_3 = df1.loc[index, "3"]
            error_message_4 = df1.loc[index, "4"]
            # print(error_message)
            # translation_files = gen_files(model_tag,error_message_1,error_message_2,count,language,repo_name)
            # saveYmlfile(save_path,translation_files)
            # break
            try:
                # print(prompt)

                translation_files = gen_files(model_tag,error_message_1,error_message_2,error_message_3,error_message_4,count,language,repo_name)
                print(translation_files)
                saveYmlfile(save_path,translation_files)
            except:
                error_data = {'repo_name': repo_name,'error':'translate failed'}
                error_path = base_dir / "resources" / "error.csv"
                saveCsvfile(error_path,error_data)
            # break

def gen_files(model_tag, error_message_1,error_message_2,error_message_3,error_message_4,count, language,repo_name):
    if count == '1':
        return gen_gemini3_one_file(error_message_1,model_tag,language,repo_name)
    if count == '2':
        return gen_gemini3_two_file(error_message_1,error_message_2,model_tag,language,repo_name)
    if count == '3':
        return gen_gemini3_three_file(error_message_1,error_message_2,error_message_3,model_tag,language,repo_name)
    if count == '4':
        return gen_gemini3_four_file(error_message_1,error_message_2,error_message_3,error_message_4,model_tag,language,repo_name)


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
def gen_gemini3_one_file(error_message,model_tag,language,repo_name):
    base_dir = Path(__file__).resolve().parent.parent
    prompt1 = gen_base_prompt(language,repo_name)
    # action_path = base_dir/'resources'/'configration_data'/language/repo_name/'enhancement'/f'{model_tag}.yml'
    action_path = base_dir/'resources'/'configration_data'/language/repo_name/'translation'/f'{model_tag}.yml'

    action_content = readYmlfile(action_path)
    prompt2 = gen_iterative_prompt(error_message)
    # print(prompt2)
    # return
    client = OpenAI(
        base_url="https://openrouter.ai/api/v1",
        api_key=api_token(),
    )
    response = client.chat.completions.create(
        extra_body={},
        model="google/gemini-3-flash-preview",
        temperature=0,
        messages=[

            {"role": "user","content": prompt1},
            {"role": "assistant","content": action_content},
            {"role": "user","content": prompt2}
            
        ]
    )

    reply = response.choices[0].message.content
    return reply

def gen_gemini3_two_file(error_message_1,error_message_2,model_tag,language,repo_name):
    base_dir = Path(__file__).resolve().parent.parent
    prompt1 = gen_base_prompt(language,repo_name)
    # action_path_1 = base_dir/'resources'/'configration_data'/language/repo_name/'enhancement'/f'gemini3-guideline.yml'
    action_path_1 = base_dir/'resources'/'configration_data'/language/repo_name/'translation'/f'gemini3.yml'
    # action_path_2 = base_dir/'resources'/'configration_data'/language/repo_name/'iterative'/'gemini3-guideline-iterative-1.yml'
    action_path_2 = base_dir/'resources'/'configration_data'/language/repo_name/'iterative'/'gemini3-iterative-1.yml'
    action_content_1 = readYmlfile(action_path_1)
    action_content_2 = readYmlfile(action_path_2)
    prompt2 = gen_iterative_prompt(error_message_1)
    prompt3 = gen_iterative_prompt(error_message_2)
    # print(prompt2)
    # return
    client = OpenAI(
        base_url="https://openrouter.ai/api/v1",
        api_key=api_token(),
    )
    response = client.chat.completions.create(
        extra_body={},
        model="google/gemini-3-flash-preview",
        temperature=0,
        messages=[

            {"role": "user","content": prompt1},
            {"role": "assistant","content": action_content_1},
            {"role": "user","content": prompt2},
            {"role": "assistant","content": action_content_2},
            {"role": "user","content": prompt3}
            
        ]
    )

    reply = response.choices[0].message.content
    return reply

def gen_gemini3_three_file(error_message_1,error_message_2,error_message_3,model_tag,language,repo_name):
    base_dir = Path(__file__).resolve().parent.parent
    prompt1 = gen_base_prompt(language,repo_name)
    # action_path_1 = base_dir/'resources'/'configration_data'/language/repo_name/'enhancement'/f'gemini3-guideline.yml'
    # action_path_2 = base_dir/'resources'/'configration_data'/language/repo_name/'iterative'/'gemini3-guideline-iterative-1.yml'
    # action_path_3 = base_dir/'resources'/'configration_data'/language/repo_name/'iterative'/'gemini3-guideline-iterative-2.yml'
    action_path_1 = base_dir/'resources'/'configration_data'/language/repo_name/'translation'/f'gemini3.yml'
    action_path_2 = base_dir/'resources'/'configration_data'/language/repo_name/'iterative'/'gemini3-iterative-1.yml'
    action_path_3 = base_dir/'resources'/'configration_data'/language/repo_name/'iterative'/'gemini3-iterative-2.yml'
    action_content_1 = readYmlfile(action_path_1)
    action_content_2 = readYmlfile(action_path_2)
    action_content_3 = readYmlfile(action_path_3)
    prompt2 = gen_iterative_prompt(error_message_1)
    prompt3 = gen_iterative_prompt(error_message_2)
    prompt4 = gen_iterative_prompt(error_message_3)
    # print(prompt2)
    # return
    client = OpenAI(
        base_url="https://openrouter.ai/api/v1",
        api_key=api_token(),
    )
    response = client.chat.completions.create(
        extra_body={},
        model="google/gemini-3-flash-preview",
        temperature=0,
        messages=[

            {"role": "user","content": prompt1},
            {"role": "assistant","content": action_content_1},
            {"role": "user","content": prompt2},
            {"role": "assistant","content": action_content_2},
            {"role": "user","content": prompt3},
            {"role": "assistant","content": action_content_3},
            {"role": "user","content": prompt4}
            
        ]
    )

    reply = response.choices[0].message.content
    return reply

def gen_gemini3_four_file(error_message_1,error_message_2,error_message_3,error_message_4,model_tag,language,repo_name):
    base_dir = Path(__file__).resolve().parent.parent
    prompt1 = gen_base_prompt(language,repo_name)
    # action_path_1 = base_dir/'resources'/'configration_data'/language/repo_name/'enhancement'/f'gemini3-guideline.yml'
    # action_path_2 = base_dir/'resources'/'configration_data'/language/repo_name/'iterative'/'gemini3-guideline-iterative-1.yml'
    # action_path_3 = base_dir/'resources'/'configration_data'/language/repo_name/'iterative'/'gemini3-guideline-iterative-2.yml'
    # action_path_4 = base_dir/'resources'/'configration_data'/language/repo_name/'iterative'/'gemini3-guideline-iterative-3.yml'
    action_path_1 = base_dir/'resources'/'configration_data'/language/repo_name/'translation'/f'gemini3.yml'
    action_path_2 = base_dir/'resources'/'configration_data'/language/repo_name/'iterative'/'gemini3-iterative-1.yml'
    action_path_3 = base_dir/'resources'/'configration_data'/language/repo_name/'iterative'/'gemini3-iterative-2.yml'
    action_path_4 = base_dir/'resources'/'configration_data'/language/repo_name/'iterative'/'gemini3-iterative-3.yml'
    action_content_1 = readYmlfile(action_path_1)
    action_content_2 = readYmlfile(action_path_2)
    action_content_3 = readYmlfile(action_path_3)
    action_content_4 = readYmlfile(action_path_4)
    prompt2 = gen_iterative_prompt(error_message_1)
    prompt3 = gen_iterative_prompt(error_message_2)
    prompt4 = gen_iterative_prompt(error_message_3)
    prompt5 = gen_iterative_prompt(error_message_4)
    # print(prompt2)
    # return
    client = OpenAI(
        base_url="https://openrouter.ai/api/v1",
        api_key=api_token(),
    )
    response = client.chat.completions.create(
        extra_body={},
        model="google/gemini-3-flash-preview",
        temperature=0,
        messages=[

            {"role": "user","content": prompt1},
            {"role": "assistant","content": action_content_1},
            {"role": "user","content": prompt2},
            {"role": "assistant","content": action_content_2},
            {"role": "user","content": prompt3},
            {"role": "assistant","content": action_content_3},
            {"role": "user","content": prompt4},
            {"role": "assistant","content": action_content_4},
            {"role": "user","content": prompt5}
        ]
    )

    reply = response.choices[0].message.content
    return reply
