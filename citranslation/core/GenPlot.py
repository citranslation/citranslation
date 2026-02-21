import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.patches import Patch
import seaborn as sns
from pathlib import Path
import os



def run(dir_name):
    base_dir = Path(__file__).resolve().parent.parent
    # base_dir = base_dir/"resources"/"csv"/"static_metrics"
    file_dir = base_dir/"resources"/"csv"/dir_name
    save_dir = base_dir/"resources"/"picture"/dir_name
    consine(file_dir, save_dir)
    # chrf(file_dir, save_dir)
    # euclidean(file_dir, save_dir)
    rouge(file_dir, save_dir)
    tree_edit_dis(file_dir, save_dir)
    x_information(file_dir, save_dir)

def consine(base_dir,save_dir):
    csv_path = base_dir/"consine_similarity.csv"
    # 1. 读入CSV文件（假设文件名为 data.csv）
    # 每一列都是一组数据
    df = pd.read_csv(csv_path)

    # 排除第一列
    df_plot = df.iloc[:, 1:]

    num_cols = len(df_plot.columns)

    # =========================
    # 画布尺寸
    # =========================
    fig_width = max(4.5, num_cols * 0.3)
    fig, ax = plt.subplots(figsize=(fig_width, 6))

    # =========================
    # 准备数据（matplotlib 需要 list）
    # =========================
    data = [df_plot[col].dropna().values for col in df_plot.columns]

    # =========================
    # 原生 boxplot（无间距）
    # =========================
    bp = ax.boxplot(
        data,
        widths=1.0,                 # 关键：箱体占满一个单位
        showfliers=False,
        showmeans=True,
        meanline=True,
        medianprops=dict(visible=False),
        meanprops=dict(color='black', linewidth=1),
        patch_artist=True           # 允许填充颜色
    )

    # =========================
    # 设置箱体颜色（Set2）
    # =========================
    colors = sns.color_palette("Set2", num_cols)
    for box, color in zip(bp['boxes'], colors):
        box.set_facecolor(color)
        box.set_edgecolor('black')
        box.set_linewidth(1)

    # =========================
    # 强制箱体无左右间距
    # =========================
    ax.set_xlim(0.5, num_cols + 0.5)
    ax.margins(x=0)

    # =========================
    # 均值数值标注
    # =========================
    for i, col in enumerate(df_plot.columns, start=1):
        mean_val = df_plot[col].mean()
        y_range = df_plot[col].max() - df_plot[col].min()
        offset = 0.02 * y_range

        va = 'bottom' if mean_val >= 0 else 'top'
        y_text = mean_val + offset if va == 'bottom' else mean_val - offset

        ax.text(
            i,
            y_text,
            f'{mean_val:.3f}',
            ha='center',
            va=va,
            color='black',
            fontname='Times New Roman',
            fontsize=14 if num_cols > 10 else 18
        )

    # =========================
    # 坐标轴美化
    # =========================
    # ax.set_xticks(range(1, num_cols + 1))
    # ax.set_xticklabels(
    #     df_plot.columns,
    #     fontsize=max(10, 24 - num_cols),
    #     fontname="Times New Roman"
    # )

    ax.tick_params(axis='y', labelsize=14)
    for label in ax.get_yticklabels():
        label.set_fontname("Times New Roman")

    ax.set_xlabel("Cosine Similarity", fontsize=24, fontname="Times New Roman")
    ax.set_ylabel("Values", fontsize=14, fontname="Times New Roman")

    plt.tight_layout()
    output_dir = save_dir  # 保存文件夹
    os.makedirs(output_dir, exist_ok=True)
    output_file = os.path.join(output_dir, f"consine.png")
    plt.savefig(output_file, dpi=300)  # dpi=300 适合打印和论文
    plt.close()  

    print(f"箱型图已保存到：{output_file}")


def chrf(base_dir, save_dir):
    csv_path = base_dir/"chrf.csv"
    # 1. 读入CSV文件（假设文件名为 data.csv）
    # 每一列都是一组数据
    df = pd.read_csv(csv_path)

    # 排除第一列
    df_plot = df.iloc[:, 1:]

    num_cols = len(df_plot.columns)

    # =========================
    # 画布尺寸
    # =========================
    fig_width = max(4.5, num_cols * 0.3)
    fig, ax = plt.subplots(figsize=(fig_width, 6))

    # =========================
    # 准备数据（matplotlib 需要 list）
    # =========================
    data = [df_plot[col].dropna().values for col in df_plot.columns]

    # =========================
    # 原生 boxplot（无间距）
    # =========================
    bp = ax.boxplot(
        data,
        widths=1.0,                 # 关键：箱体占满一个单位
        showfliers=False,
        showmeans=True,
        meanline=True,
        medianprops=dict(visible=False),
        meanprops=dict(color='black', linewidth=1),
        patch_artist=True           # 允许填充颜色
    )

    # =========================
    # 设置箱体颜色（Set2）
    # =========================
    colors = sns.color_palette("Set2", num_cols)
    for box, color in zip(bp['boxes'], colors):
        box.set_facecolor(color)
        box.set_edgecolor('black')
        box.set_linewidth(1)

    # =========================
    # 强制箱体无左右间距
    # =========================
    ax.set_xlim(0.5, num_cols + 0.5)
    ax.margins(x=0)

    # =========================
    # 均值数值标注
    # =========================
    for i, col in enumerate(df_plot.columns, start=1):
        mean_val = df_plot[col].mean()
        y_range = df_plot[col].max() - df_plot[col].min()
        offset = 0.02 * y_range

        va = 'bottom' if mean_val >= 0 else 'top'
        y_text = mean_val + offset if va == 'bottom' else mean_val - offset

        ax.text(
            i,
            y_text,
            f'{mean_val:.3f}',
            ha='center',
            va=va,
            color='black',
            fontname='Times New Roman',
            fontsize=16 if num_cols > 10 else 16
        )

    # =========================
    # 坐标轴美化
    # =========================
    # ax.set_xticks(range(1, num_cols + 1))
    # ax.set_xticklabels(
    #     df_plot.columns,
    #     fontsize=max(10, 24 - num_cols),
    #     fontname="Times New Roman"
    # )

    ax.tick_params(axis='y', labelsize=14)
    for label in ax.get_yticklabels():
        label.set_fontname("Times New Roman")

    ax.set_xlabel("chrf", fontsize=24, fontname="Times New Roman")
    ax.set_ylabel("Values", fontsize=14, fontname="Times New Roman")

    plt.tight_layout()
    output_dir = save_dir  # 保存文件夹
    os.makedirs(output_dir, exist_ok=True)
    output_file = os.path.join(output_dir, f"chrf.png")
    plt.savefig(output_file, dpi=300)  # dpi=300 适合打印和论文
    plt.close()  

    print(f"箱型图已保存到：{output_file}")


def tree_edit_dis(base_dir, save_dir):
    csv_path = base_dir/"tree_edit_distance.csv"
    # 1. 读入CSV文件（假设文件名为 data.csv）
    # 每一列都是一组数据
    df = pd.read_csv(csv_path)

    # 排除第一列
    df_plot = df.iloc[:, 1:]

    num_cols = len(df_plot.columns)

    # =========================
    # 画布尺寸
    # =========================
    fig_width = max(4.5, num_cols * 0.3)
    fig, ax = plt.subplots(figsize=(fig_width, 6))

    # =========================
    # 准备数据（matplotlib 需要 list）
    # =========================
    data = [df_plot[col].dropna().values for col in df_plot.columns]

    # =========================
    # 原生 boxplot（无间距）
    # =========================
    bp = ax.boxplot(
        data,
        widths=1.0,                 # 关键：箱体占满一个单位
        showfliers=False,
        showmeans=True,
        meanline=True,
        medianprops=dict(visible=False),
        meanprops=dict(color='black', linewidth=1),
        patch_artist=True           # 允许填充颜色
    )

    # =========================
    # 设置箱体颜色（Set2）
    # =========================
    colors = sns.color_palette("Set2", num_cols)
    for box, color in zip(bp['boxes'], colors):
        box.set_facecolor(color)
        box.set_edgecolor('black')
        box.set_linewidth(1)

    # =========================
    # 强制箱体无左右间距
    # =========================
    ax.set_xlim(0.5, num_cols + 0.5)
    ax.margins(x=0)

    # =========================
    # 均值数值标注
    # =========================
    for i, col in enumerate(df_plot.columns, start=1):
        mean_val = df_plot[col].mean()
        y_range = df_plot[col].max() - df_plot[col].min()
        offset = 0.02 * y_range

        va = 'bottom' if mean_val >= 0 else 'top'
        y_text = mean_val + offset if va == 'bottom' else mean_val - offset

        ax.text(
            i,
            y_text,
            f'{mean_val:.3f}',
            ha='center',
            va=va,
            color='black',
            fontname='Times New Roman',
            fontsize=14 if num_cols > 10 else 18
        )

    # =========================
    # 坐标轴美化
    # =========================
    # ax.set_xticks(range(1, num_cols + 1))
    # ax.set_xticklabels(
    #     df_plot.columns,
    #     fontsize=max(10, 24 - num_cols),
    #     fontname="Times New Roman"
    # )

    ax.tick_params(axis='y', labelsize=14)
    for label in ax.get_yticklabels():
        label.set_fontname("Times New Roman")

    ax.set_xlabel("Tree Edit Similarity", fontsize=24, fontname="Times New Roman")
    ax.set_ylabel("Values", fontsize=14, fontname="Times New Roman")

    # =========================
    # save
    # =========================
    plt.tight_layout()

    output_dir = save_dir  # 保存文件夹
    os.makedirs(output_dir, exist_ok=True)
    output_file = os.path.join(output_dir, "tree_edit_distance.png")

    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    plt.close()

    print(f"箱型图已保存到：{output_file}")

def rouge(base_dir, save_dir):
    csv_path = base_dir/"rouge_l.csv"
    # 1. 读入CSV文件（假设文件名为 data.csv）
    # 每一列都是一组数据
    df = pd.read_csv(csv_path)

    # 排除第一列
    df_plot = df.iloc[:, 1:]

    num_cols = len(df_plot.columns)

    # =========================
    # 画布尺寸
    # =========================
    fig_width = max(4.5, num_cols * 0.3)
    fig, ax = plt.subplots(figsize=(fig_width, 6))

    # =========================
    # 准备数据（matplotlib 需要 list）
    # =========================
    data = [df_plot[col].dropna().values for col in df_plot.columns]

    # =========================
    # 原生 boxplot（无间距）
    # =========================
    bp = ax.boxplot(
        data,
        widths=1.0,                 # 关键：箱体占满一个单位
        showfliers=False,
        showmeans=True,
        meanline=True,
        medianprops=dict(visible=False),
        meanprops=dict(color='black', linewidth=1),
        patch_artist=True           # 允许填充颜色
    )

    # =========================
    # 设置箱体颜色（Set2）
    # =========================
    colors = sns.color_palette("Set2", num_cols)
    for box, color in zip(bp['boxes'], colors):
        box.set_facecolor(color)
        box.set_edgecolor('black')
        box.set_linewidth(1)

    # =========================
    # 强制箱体无左右间距
    # =========================
    ax.set_xlim(0.5, num_cols + 0.5)
    ax.margins(x=0)

    # =========================
    # 均值数值标注
    # =========================
    for i, col in enumerate(df_plot.columns, start=1):
        mean_val = df_plot[col].mean()
        y_range = df_plot[col].max() - df_plot[col].min()
        offset = 0.02 * y_range

        va = 'bottom' if mean_val >= 0 else 'top'
        y_text = mean_val + offset if va == 'bottom' else mean_val - offset

        ax.text(
            i,
            y_text,
            f'{mean_val:.3f}',
            ha='center',
            va=va,
            color='black',
            fontname='Times New Roman',
            fontsize=14 if num_cols > 10 else 18
        )

    # =========================
    # 坐标轴美化
    # =========================
    # ax.set_xticks(range(1, num_cols + 1))
    # ax.set_xticklabels(
    #     df_plot.columns,
    #     fontsize=max(10, 24 - num_cols),
    #     fontname="Times New Roman"
    # )

    ax.tick_params(axis='y', labelsize=14)
    for label in ax.get_yticklabels():
        label.set_fontname("Times New Roman")

    ax.set_xlabel("ROUGE-L", fontsize=24, fontname="Times New Roman")
    ax.set_ylabel("Values", fontsize=14, fontname="Times New Roman")

    plt.tight_layout()
    output_dir = save_dir  # 保存文件夹
    os.makedirs(output_dir, exist_ok=True)
    output_file = os.path.join(output_dir, f"rouge_l.png")
    plt.savefig(output_file, dpi=300)  # dpi=300 适合打印和论文
    plt.close()  

    print(f"箱型图已保存到：{output_file}")


def euclidean(base_dir, save_dir):
    csv_path = base_dir/"euclidean_distance.csv"
    # 1. 读入CSV文件（假设文件名为 data.csv）
    # 每一列都是一组数据
    df = pd.read_csv(csv_path)

    # 排除第一列
    df_plot = df.iloc[:, 1:]

    num_cols = len(df_plot.columns)

    # =========================
    # 画布尺寸
    # =========================
    fig_width = max(4.5, num_cols * 0.3)
    fig, ax = plt.subplots(figsize=(fig_width, 6))

    # =========================
    # 准备数据（matplotlib 需要 list）
    # =========================
    data = [df_plot[col].dropna().values for col in df_plot.columns]

    # =========================
    # 原生 boxplot（无间距）
    # =========================
    bp = ax.boxplot(
        data,
        widths=1.0,                 # 关键：箱体占满一个单位
        showfliers=False,
        showmeans=True,
        meanline=True,
        medianprops=dict(visible=False),
        meanprops=dict(color='black', linewidth=1),
        patch_artist=True           # 允许填充颜色
    )

    # =========================
    # 设置箱体颜色（Set2）
    # =========================
    colors = sns.color_palette("Set2", num_cols)
    for box, color in zip(bp['boxes'], colors):
        box.set_facecolor(color)
        box.set_edgecolor('black')
        box.set_linewidth(1)

    # =========================
    # 强制箱体无左右间距
    # =========================
    ax.set_xlim(0.5, num_cols + 0.5)
    ax.margins(x=0)

    # =========================
    # 均值数值标注
    # =========================
    for i, col in enumerate(df_plot.columns, start=1):
        mean_val = df_plot[col].mean()
        y_range = df_plot[col].max() - df_plot[col].min()
        offset = 0.02 * y_range

        va = 'bottom' if mean_val >= 0 else 'top'
        y_text = mean_val + offset if va == 'bottom' else mean_val - offset

        ax.text(
            i,
            y_text,
            f'{mean_val:.3f}',
            ha='center',
            va=va,
            color='black',
            fontname='Times New Roman',
            fontsize=14 if num_cols > 10 else 18
        )

    # =========================
    # 坐标轴美化
    # =========================
    # ax.set_xticks(range(1, num_cols + 1))
    # ax.set_xticklabels(
    #     df_plot.columns,
    #     fontsize=max(10, 24 - num_cols),
    #     fontname="Times New Roman"
    # )

    ax.tick_params(axis='y', labelsize=14)
    for label in ax.get_yticklabels():
        label.set_fontname("Times New Roman")

    ax.set_xlabel("Euclidean Similarity", fontsize=24, fontname="Times New Roman")
    ax.set_ylabel("Values", fontsize=14, fontname="Times New Roman")


    plt.tight_layout()
    output_dir = save_dir  # 保存文件夹
    os.makedirs(output_dir, exist_ok=True)
    output_file = os.path.join(output_dir, f"euclidean_distance.png")
    plt.savefig(output_file, dpi=300)  # dpi=300 适合打印和论文
    plt.close()  

    print(f"箱型图已保存到：{output_file}")


def x_information(base_dir, save_dir):
    csv_path = base_dir / "rouge_l.csv"

    # =========================
    # 读 CSV，只用于拿列名
    # =========================
    df = pd.read_csv(csv_path)
    df_plot = df.iloc[:, 1:]   # 排除第一列
    labels = list(df_plot.columns)
    num_cols = len(labels)

    # =========================
    # 构造 legend 元素
    # =========================
    colors = sns.color_palette("Set2", num_cols)

    legend_elements = [
        Patch(
            facecolor=color,
            edgecolor='0.3',
            label=label
        )
        for color, label in zip(colors, labels)
    ]

    # =========================
    # 创建仅包含 legend 的 figure
    # =========================
    fig, ax = plt.subplots(figsize=(4, 0.6 * num_cols))
    ax.axis('off')   # 关键：不显示任何坐标轴

    ax.legend(
        handles=legend_elements,
        title="Strategies",
        title_fontsize=18,
        fontsize=16,
        loc='center',
        frameon=True,
        framealpha=1,
        edgecolor='0.85'
    )

    # =========================
    # 保存 legend
    # =========================
    output_dir = save_dir  # 保存文件夹
    os.makedirs(output_dir, exist_ok=True)

    legend_path = os.path.join(output_dir, "x_information.png")
    plt.savefig(
        legend_path,
        dpi=300,
        bbox_inches='tight',
        pad_inches=0.05
    )
    plt.close()

    print(f"Legend 已保存到：{legend_path}")