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
    plot_all(file_dir, save_dir)
    x_information(file_dir, save_dir)

def plot_all(base_dir, save_dir):
    plot_picture(base_dir, save_dir, "consine_similarity.csv")
    plot_picture(base_dir, save_dir, "euclidean_distance.csv")
    plot_picture(base_dir, save_dir, "rouge_l.csv")
    plot_picture(base_dir, save_dir, "tree_edit_distance.csv")
    plot_picture(base_dir, save_dir, "chrf.csv")



def plot_picture(base_dir, save_dir, pictue_name):
    csv_path = base_dir / f"{pictue_name}.csv"

    # =========================
    # read CSV
    # =========================
    df = pd.read_csv(csv_path)

    # exclude first column
    df_plot = df.iloc[:, 1:]
    num_cols = len(df_plot.columns)

    # =========================
    # figure size
    # =========================
    fig_width = max(6, num_cols * 0.4)   # 稍微加宽一点
    fig, ax = plt.subplots(figsize=(fig_width, 6))

    # =========================
    # prepare data
    # =========================
    data = [df_plot[col].dropna().values for col in df_plot.columns]

    # =========================
    # boxplot（有间距）
    # =========================
    bp = ax.boxplot(
        data,
        widths=0.6,   # ✅ 改这里（核心）
        showfliers=False,
        showmeans=True,
        meanline=True,
        medianprops=dict(visible=False),
        meanprops=dict(color='black', linewidth=1),
        patch_artist=True
    )

    # =========================
    # colors
    # =========================
    colors = sns.color_palette("Set2", num_cols)
    for box, color in zip(bp['boxes'], colors):
        box.set_facecolor(color)
        box.set_edgecolor('black')
        box.set_linewidth(1)

    # =========================
    # ❌ 删除这两行（否则永远挤在一起）
    # =========================
    # ax.set_xlim(0.5, num_cols + 0.5)
    # ax.margins(x=0)

    # =========================
    # mean annotation
    # =========================
    for i, col in enumerate(df_plot.columns, start=1):
        mean_val = df_plot[col].mean()
        y_range = df_plot[col].max() - df_plot[col].min()
        offset = 0.02 * y_range if y_range != 0 else 0.01

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
    # ✅ x-axis labels（关键新增）
    # =========================
    ax.set_xticks(range(1, num_cols + 1))
    ax.set_xticklabels(
        df_plot.columns,
        rotation=45,          # 倾斜
        ha='right',
        fontsize=max(8, 20 - num_cols * 0.3),
        fontname="Times New Roman"
    )

    # =========================
    # y-axis style
    # =========================
    ax.tick_params(axis='y', labelsize=14)
    for label in ax.get_yticklabels():
        label.set_fontname("Times New Roman")

    ax.set_xlabel(pictue_name, fontsize=24, fontname="Times New Roman")
    ax.set_ylabel("Values", fontsize=14, fontname="Times New Roman")

    # =========================
    # layout（防裁剪）
    # =========================
    plt.tight_layout()
    plt.subplots_adjust(bottom=0.25)

    # =========================
    # save
    # =========================
    os.makedirs(save_dir, exist_ok=True)
    output_file = os.path.join(save_dir, f"{pictue_name}.png")

    plt.savefig(output_file, dpi=300)
    plt.close()

    print(f"save: {output_file}")



def x_information(base_dir, save_dir):
    csv_path = base_dir / "rouge_l.csv"

    # =========================
    # read CSV
    # =========================
    df = pd.read_csv(csv_path)
    df_plot = df.iloc[:, 1:]   # exclude first column
    labels = list(df_plot.columns)
    num_cols = len(labels)

    # =========================
    # create legend 
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
    # create only legend figure
    # =========================
    fig, ax = plt.subplots(figsize=(4, 0.6 * num_cols))
    ax.axis('off')   

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
    # save legend
    # =========================
    output_dir = save_dir  # save folder
    os.makedirs(output_dir, exist_ok=True)

    legend_path = os.path.join(output_dir, "x_information.png")
    plt.savefig(
        legend_path,
        dpi=300,
        bbox_inches='tight',
        pad_inches=0.05
    )
    plt.close()

    print(f"Legend saved in:{legend_path}")