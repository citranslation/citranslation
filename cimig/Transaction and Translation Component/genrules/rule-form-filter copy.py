import csv
import pandas as pd


gh_row=[]

df = pd.read_csv('rules_H2_parent_child_github.csv', sep='@;@', on_bad_lines='warn', engine='python')

out_str = ''
for el in df.columns:
    out_str += el + '@;@'
out_str += 'conf_2@;@lift_2@;@supp_2@;@conv_2'
file_out=open('/rules_H2_parent_child_github_form_filtered.csv', 'w+')
file_out.write(out_str +'\n')

counter = 0
for index,row in df.iterrows():
    counter+=1
    if '{'  not in str(row[0]).split('->')[0]: #"origin"":""github"
        row_gh=row
    else:
        row_tr =row
    if(counter==1):
        continue
    else:
        counter = 0
        rule = row_tr['rule']
        conf_2= float(row_tr['conf']) * float(row_gh['conf'])
        lift_2= float(row_tr['lift']) * float(row_gh['lift'])
        supp_2= float(row_tr['supp']) * float(row_gh['supp'])
        conv_2= float(row_tr['conv']) * float(row_gh['conv'])
        file_out.write(str(row_tr['rule']) + '@;@' + str(row_tr['conf']) + '@;@' + str(row_tr['lift']) + '@;@' + str(row_tr['supp']) + '@;@' + str(row_tr['conv']) + '@;@' + str(conf_2) + '@;@' + str(lift_2) + '@;@' + str(supp_2) + '@;@' + str(conv_2) + '\n')
        file_out.flush()
file_out.flush()

# 
# for index,row in df.iterrows():
#     counter+=1
#     if '"origin"":""github"'  in str(row[0]).split('->')[0]: #
#         row_gh=row
#     else:
#         row_tr =row
#     if(counter==1):
#         continue
#     else:
#         counter = 0
#         rule = row_tr['rule']
#         conf_2= float(row_tr['conf']) * float(row_gh['conf'])
#         lift_2= float(row_tr['lift']) * float(row_gh['lift'])
#         supp_2= float(row_tr['supp']) * float(row_gh['supp'])
#         conv_2= float(row_tr['conv']) * float(row_gh['conv'])
#         file_out.write(str(row_gh['rule']) + '@;@' + str(row_gh['conf']) + '@;@' + str(row_gh['lift']) + '@;@' + str(row_gh['supp']) + '@;@' + str(row_gh['conv']) + '@;@' + str(conf_2) + '@;@' + str(lift_2) + '@;@' + str(supp_2) + '@;@' + str(conv_2) + '\n')
#         file_out.flush()
# file_out.flush()
