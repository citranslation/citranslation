import csv
from pathlib import Path

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



def saveYmlfile(file_path,content):
    file_path = Path(file_path)
    file_path.parent.mkdir(parents=True, exist_ok=True)
    with open(file_path, mode='w', encoding='utf-8',errors='ignore') as file:
        file.write(content)
        print(f'已将内容写入到 {file_path}')


def readYmlfile(file_path):
    with open(file_path, mode='r', encoding='utf-8',errors='ignore') as file:
        content = file.read()
        print(f'已读取内容从 {file_path}')
        return content