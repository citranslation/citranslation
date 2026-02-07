import pandas as pd
import yaml
import re
import os
from sentence_transformers import SentenceTransformer, util
from joblib import Parallel, delayed
from tqdm import tqdm
from sacrebleu.metrics import CHRF
from ignite.metrics import RougeL
# from statsmodels.stats.multitest import multipletests
# import seaborn as sns
import warnings
import torch
from zss import simple_distance, Node
from scipy.stats import spearmanr
from scipy.stats import wilcoxon
from pathlib import Path

warnings.filterwarnings("ignore")

SentenceTransformerModelPath = 'all-MiniLM-L6-v2'

def run(csv_path,dir_name):
    col_lists = ['1']
    # col_lists = ['gpt-4o','gpt-4o-mini','gemini3','gpt5.1-code','gpt5.1-mini','llama','qwen3','deepseek','gemini']
    # col_lists = ['deepseek-oneshot','deepseek-fewshot','deepseek-guideline','gemini3-oneshot','gemini3-fewshot','gemini3-guideline']
    base_dir = Path(__file__).parent.parent
    df = pd.read_csv(csv_path)
    for index, row in df.iterrows():
        print(row['repo_name'])
        for col in col_lists:
            # file_name = f"{col}.yml"
            # file_name = row['base']
            repo_full_name = row['repo_name']
            # if file_name =='gemini3-guideline':
            #     candidate_path = base_dir/'resources'/'configration_data'/row['language']/repo_full_name/'enhancement'/f'{file_name}.yml'
            # else:
            #     candidate_path = base_dir/'resources'/'configration_data'/row['language']/repo_full_name/'iterative'/f'{file_name}.yml'

            reference_path = base_dir/'resources'/'configration_data'/row['language']/repo_full_name/'actions.yml'
            candidate_path = base_dir/'resources'/'configration_data'/row['language']/repo_full_name/'translation'/'importer.yml'
            # candidate_path = base_dir/'resources'/'configration_data'/row['language']/repo_full_name/dir_name/file_name

            reference_file = read_file(reference_path)
            candidate_file = read_file(candidate_path)
            
            # result = compute_consie_similarity(reference_file, candidate_file)
            # result = compute_euclidean_distance(reference_file, candidate_file)
            consie_similarity = compute_consie_similarity(reference_file, candidate_file)
            print(consie_similarity)
            # csv_path = base_dir/'resources'/'csv'/'static_metrics'/'consine_similarity.csv'
            csv_path = base_dir/'resources'/'csv'/dir_name/'consine_similarity.csv'
            save_result(col,index,consie_similarity,csv_path)

            euclidean_distance = compute_euclidean_distance(reference_file, candidate_file)
            euclidean_distance = 1 / (1 + euclidean_distance)
            print(euclidean_distance)
            # csv_path = base_dir/'resources'/'csv'/'static_metrics'/'euclidean_distance.csv'
            csv_path = base_dir/'resources'/'csv'/dir_name/'euclidean_distance.csv'
            save_result(col,index,euclidean_distance,csv_path)

            rouge_l, chrf = compute_metrics_yaml_lines(reference_file, candidate_file)
            print(rouge_l, chrf)
            # csv_path = base_dir/'resources'/'csv'/'static_metrics'/'rouge_l.csv'
            csv_path = base_dir/'resources'/'csv'/dir_name/'rouge_l.csv'
            save_result(col,index,rouge_l,csv_path)
            # csv_path = base_dir/'resources'/'csv'/'static_metrics'/'chrf.csv'
            csv_path = base_dir/'resources'/'csv'/dir_name/'chrf.csv'

            save_result(col,index,chrf,csv_path)

            tree_edit_distance = normalized_tree_edit_distance(reference_file, candidate_file)
            print(tree_edit_distance)
            # csv_path = base_dir/'resources'/'csv'/'static_metrics'/'tree_edit_distance.csv'
            csv_path = base_dir/'resources'/'csv'/dir_name/'tree_edit_distance.csv'
            save_result(col,index,tree_edit_distance,csv_path)
        # break


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

def yaml_line_tokenizer(yaml_text):
    return [line.strip() for line in yaml_text.splitlines() if line.strip()]

# Load model on CPU only
model = SentenceTransformer(SentenceTransformerModelPath, device='cpu')
def compute_consie_similarity(code, llm_output):
    code_embedding = model.encode(str(code), convert_to_tensor=True, device='cpu')
    llm_output_embedding = model.encode(str(llm_output), convert_to_tensor=True, device='cpu')
    similarity = util.pytorch_cos_sim(code_embedding, llm_output_embedding)
    return similarity.item()

def compute_euclidean_distance(code, llm_output):
    code_embedding = model.encode(str(code), convert_to_tensor=True, device='cpu')
    llm_output_embedding = model.encode(str(llm_output), convert_to_tensor=True, device='cpu')
    distance = torch.norm(code_embedding - llm_output_embedding, p=2)

    return distance.item()

chrf_metric_sacrebleu = CHRF(lowercase=True, whitespace=True, eps_smoothing=True)

rouge_metric_ignite = RougeL(multiref="best")
def compute_rouge_ignite(code, llm_output):
    candidate = llm_output.split()
    references = [code.split()]
    rouge_metric_ignite.reset()
    rouge_metric_ignite.update(([candidate], [references]))
    scores = rouge_metric_ignite.compute()
    return max(scores['Rouge-L-P'], scores['Rouge-L-R'], scores['Rouge-L-F'])

# ROUGE-L and chrF
def compute_metrics_yaml_lines(code, llm_output):
    code_tokens = yaml_line_tokenizer(code)
    llm_output_tokens = yaml_line_tokenizer(llm_output)
    code_str = ' '.join(code_tokens)
    llm_output_str = ' '.join(llm_output_tokens)

    if not code_str.strip() or not llm_output_str.strip():
        return 0.0, 0.0
    rouge_l = compute_rouge_ignite(code_str, llm_output_str)
    chrf = chrf_metric_sacrebleu.sentence_score(llm_output_str, [code_str]).score
    return rouge_l, chrf

def yaml_to_tree(yaml_text):
    def build_tree(obj, name="root"):
        node = Node(str(name))
        if isinstance(obj, dict):
            for k, v in obj.items():
                node.addkid(build_tree(v, k))
        elif isinstance(obj, list):
            for i, v in enumerate(obj):
                node.addkid(build_tree(v, f'item_{i}'))
        elif obj is not None:
            node.addkid(Node(str(obj)))
        return node

    try:
        data = yaml.safe_load(yaml_text)
        if data is None:
            return Node("root")
        return build_tree(data)
    except Exception:
        return Node("root")
    
# def normalized_tree_edit_distance(yaml1, yaml2):
#     tree1 = yaml_to_tree(yaml1)
#     tree2 = yaml_to_tree(yaml2)
#     ted = simple_distance(tree1, tree2)
#     max_size = max(len(list(Node.get_children(tree1))) + 1, len(list(Node.get_children(tree2))) + 1)
#     return 1.0 if max_size == 0 else 1.0 - (ted / max_size)

def normalized_tree_edit_distance(yaml1, yaml2):
    tree1 = yaml_to_tree(yaml1)
    tree2 = yaml_to_tree(yaml2)
    
    def count_nodes(node):
        stack = [node]
        count = 0
        while stack:
            current = stack.pop()
            count += 1
            stack.extend(Node.get_children(current))
        return count
    
    # Get sizes including all nodes
    size1 = count_nodes(tree1)
    size2 = count_nodes(tree2)
    
    # Compute edit distance
    ted = simple_distance(tree1, tree2)
    
    # Handle empty trees
    if size1 == 0 and size2 == 0:
        return 1.0
    elif size1 == 0 or size2 == 0:
        return 0.0
        
    # Normalize by maximum possible edit distance
    max_ops = max(size1, size2)
    similarity = 1.0 - (ted / (2 * max_ops))
    
    return max(0.0, min(1.0, similarity))  # Make result is between 0 and 1


def read_file(path):
    try:
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
            return remove_comments(content)

    except FileNotFoundError:
        print(f"Error: 文件未找到 -> {path}")
        return None
    except Exception as e:
        print(f"读取文件失败: {e}")
        return None

def save_result(col, index, result, csv_path):
    csv_path = Path(csv_path)

    # ✅ 确保目录存在
    csv_path.parent.mkdir(parents=True, exist_ok=True)

    # 如果 CSV 不存在，先创建一个空 DataFrame
    if csv_path.exists():
        df = pd.read_csv(csv_path)
    else:
        df = pd.DataFrame()

    if col not in df.columns:
        df[col] = None

    df.loc[index, col] = result
    df.to_csv(csv_path, index=False)


