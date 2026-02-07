import pandas as pd
from sklearn.model_selection import train_test_split

# Read the csv file into a dataframe
df = pd.read_csv("../combinedOldAndNew.csv")

# Get the list of files from the dataframe
files = df["ProjectName"].tolist()
files = [*set(files)]
print(len(files))
print(df.columns)
df.drop(['Unnamed: 3'], axis=1,inplace=True)

# Split the files into train and test sets with a 80/20 ratio
train_files, test_files = train_test_split(files, test_size=0.2, random_state=2154851)



df["train"] = df["ProjectName"].apply(lambda x: x in train_files)
print("Train files:", len(train_files))
print("Test files:", len(test_files))
df.to_csv("train_test_split.csv", index=False)
