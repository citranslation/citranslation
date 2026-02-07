import csv
from pathlib import Path
from efficient_apriori import apriori
import json
import pandas as pd
import string
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.feature_extraction.text import CountVectorizer
import os


def gencsvfile(csv_path):
    file1_path = f'{csv_path}/transactions_json_ast_L2.csv'
    file2_path = f'{csv_path}/transactions_json_ast_L2_alt.csv'
    output_path = f'{csv_path}/transactions_H2.csv'
    with open(file1_path, 'r', encoding='utf-8') as f1, \
        open(file2_path, 'r', encoding='utf-8') as f2, \
        open(output_path, 'w', encoding='utf-8') as out:

        for line1, line2 in zip(f1, f2):
            # 去掉换行符
            line1 = line1.rstrip('\n').rstrip('\r')
            line2 = line2.rstrip('\n').rstrip('\r')
            
            # 合并成四列
            out.write(f'{line1};{line2}\n')

def rule_filer(csv_path):
    df = pd.read_csv(f'{csv_path}/rules_H2.csv', sep='@;@', on_bad_lines='warn', engine='python')

    # 写入表头
    out_str = ''
    for col in df.columns:
        out_str += col + '@;@'
    out_str += 'conf_2@;@lift_2@;@supp_2@;@conv_2'

    file_out = open(f'{csv_path}/rules_H2_filtered.csv', 'w+',encoding='utf-8')
    file_out.write(out_str + '\n')

    # 遍历奇数行
    for i in range(0, len(df), 2):
        row_travis = df.iloc[i]      # travis
        row_actions = df.iloc[i + 1] # actions

        # 计算组合指标
        conf_2 = float(row_actions['conf']) * float(row_travis['conf'])
        lift_2 = float(row_actions['lift']) * float(row_travis['lift'])
        supp_2 = float(row_actions['supp']) * float(row_travis['supp'])
        conv_2 = float(row_actions['conv']) * float(row_travis['conv'])

        # 写入文件
        file_out.write(
            f"{row_travis['rule']}@;@{row_travis['conf']}@;@{row_travis['lift']}@;@{row_travis['supp']}@;@{row_travis['conv']}@;@"
            f"{conf_2}@;@{lift_2}@;@{supp_2}@;@{conv_2}\n"
        )
        file_out.flush()

def similarity(csv_path):
    sim_dir = os.path.join(csv_path, "rules_sim_based")
    nonsim_dir = os.path.join(csv_path, "rules_nonsim_based")
    os.makedirs(sim_dir, exist_ok=True)
    os.makedirs(nonsim_dir, exist_ok=True)
    df = pd.read_csv(f'{csv_path}/rules_H2_filtered.csv', sep='@;@', on_bad_lines='warn', quotechar='"',
                    quoting=csv.QUOTE_ALL, engine='python')
    file_out = open(f'{csv_path}/rules_sim_based/rules_H2.csv', 'w+')
    file_out_non_sim = open(f'{csv_path}/rules_nonsim_based/rules_H2.csv', 'w+')
    out_str = ''
    for el in df.columns:
        out_str += el + '@;@'
    out_str = out_str[:-1]
    file_out.write(out_str + '\n')

    for index, row in df.iterrows():
        if ' -> ' not in row[0]:
            continue
        # print(index)
        rule = row[0]
        lhs = rule.split(' -> ')[0]
        lhs = str(lhs).replace('""', '"')
        # print(lhs)
        rhs = rule.split(' -> ')[1]
        rhs = str(rhs).replace('""', '"')
        # print(rhs)
        if ('"type":""}]' in lhs) or ('"type":""}]' in rhs):
            continue
        lhs_leaves = get_leaves(lhs, root=True)
        rhs_leaves = get_leaves(rhs, root=True)
        
        if lhs_leaves is None or rhs_leaves is None:
            print(f"跳过坏数据行: {index}")
            continue

        temp_list = [lhs_leaves, rhs_leaves]
        vectorizer = CountVectorizer()
        try:
            vectors = vectorizer.fit_transform(raw_documents=temp_list)
            csim = cosine_similarity(vectors[0].reshape(1, -1), vectors[1].reshape(1, -1))[0][0]
            # print(csim)
            if csim > 0.50:
                file_out.write('"' +
                            str(row[0]) + '"@;@' + str(row[1]) + '@;@' + str(row[2]) + '@;@' + str(row[3]) + '@;@' + str(
                    row[4]) + '@;@' + str(row[5]) + '@;@' + str(row[6]) + '@;@' + str(row[7]) + '@;@' + str(row[8]) + '\n')
            else:
                file_out_non_sim.write('"' +
                                    str(row[0]) + '"@;@' + str(row[1]) + '@;@' + str(row[2]) + '@;@' + str(
                    row[3]) + '@;@' + str(
                    row[4]) + '@;@' + str(row[5]) + '@;@' + str(row[6]) + '@;@' + str(row[7]) + '@;@' + str(row[8]) + '\n')

        except Exception as e:
            print(temp_list)
            print(e)


def get_leaves(lhs, root=False):
    try:
        if root:
            if str(root).startswith('{{'):
                json_obj = json.loads(str(lhs)[1:-1])
            else:
                json_obj = json.loads(str(lhs))
        else:
            json_obj = lhs
    except json.JSONDecodeError:
        return None   # ← 关键：标记为坏数据

    leaves_str = ''
    if 'children' in json_obj and len(json_obj['children']) > 0:
        for child in json_obj['children']:
            child_leaves = get_leaves(child)
            if child_leaves is None:
                return None
            leaves_str += ' ' + child_leaves
    else:
        if 'type' not in json_obj:
            return None
        leaves_str += ' ' + json_obj['type'].replace('-cmd', '')

    return leaves_str.strip()

def apriori_gen(csv_path):
    df = pd.read_csv(f'{csv_path}/transactions_H2.csv', sep=';',on_bad_lines='skip', engine='python')
    transactions = df.iloc[:, [0,1]].astype(str).values.tolist()

    transactions_4 = df.iloc[:, [1,3]].astype(str).values.tolist()

    print(transactions[1])
    # print(transactions_2[1])
    # print(transactions_3[1])
    print(transactions_4[1])
    # exit(0)
    # Get the current time in seconds before the task

    itemsets, rules = apriori(transactions,min_confidence=0.000001, min_support=0.000001, verbosity=1, max_length=2)

    itemsets_4, rules_4 = apriori(transactions_4,min_confidence=0.000001, min_support=0.000001, verbosity=1, max_length=2)


    csv_out = open(f'{csv_path}/rules_H2.csv','w+', encoding='utf-8')
    csv_out.write("rule@;@conf@;@lift@;@supp@;@conv\n")
    for rule in rules:
        rule_str = str(rule.lhs[0]) +' -> '+ str(rule.rhs[0])
        if rule_str.startswith('"'):
            rule_str=rule_str[1:]
        if rule_str.endswith('"'):
            rule_str=rule_str[:-1]
    
        csv_out.write(rule_str+'@;@'+str(rule.confidence)+"@;@"+str(rule.lift)+"@;@"+str(rule.support)+"@;@"+str(rule.conviction)+"\n")


    csv_out = open(f'{csv_path}/rules_H2_parent_child_github.csv','w+', encoding='utf-8')
    csv_out.write("rule@;@conf@;@lift@;@supp@;@conv\n")
    for rule in rules_4:
        rule_str = str(rule.lhs[0]) +' -> '+ str(rule.rhs[0])
        if rule_str.startswith('"'):
            rule_str=rule_str[1:]
        if rule_str.endswith('"'):
            rule_str=rule_str[:-1]
        csv_out.write(rule_str+'@;@'+str(rule.confidence)+"@;@"+str(rule.lift)+"@;@"+str(rule.support)+"@;@"+str(rule.conviction)+"\n")

def saveCsvfile(file_path,data):
    
    with open(file_path, mode='a', newline='', encoding='utf-8',errors='ignore') as file:
        # 创建一个CSV写入器
        writer = csv.DictWriter(file, fieldnames=data.keys())

        # 如果文件是空的（或者是首次写入），就写入表头
        if file.tell() == 0:
            writer.writeheader()

        # 写入新的数据行
        writer.writerow(data)

        print(f'已将新数据添加到 {file_path}')


if __name__ =="__main__":
    base_dir = Path(__file__).resolve().parent.parent
    csv_file = "D:/vscode/3/CItranslation/cimig/Transaction and Translation Component/error.csv"
    df = pd.read_csv(csv_file)
    for index, row in df.iterrows():
        repo_name = row['repo_name']
        lan = row['language']
        csv_path = f'{base_dir}/genrules/rules_h2/{lan}/{repo_name}'
        print(csv_path)
        try:
            gencsvfile(csv_path)
            apriori_gen(csv_path)
            rule_filer(csv_path)
            similarity(csv_path)
        except:
            error_data = {'repo_name': repo_name}
            error_path = base_dir / "error.csv"
            saveCsvfile(error_path,error_data)