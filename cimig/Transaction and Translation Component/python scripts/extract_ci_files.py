GHA_only_projects = '/media/umd-002677/HDD/travis2ga-GHA-only-set/*/*/.github/**/*.yml'
Travis_only_projects = '/media/umd-002677/HDD/travis2ga-travis-only-set/*/*/.travis.yml'
Travis_files_out = '/home/umd-002677/IdeaProjects/travis2ga/data_projects_for_TAR_mining/travis_files/'
GHA_files_out = '/home/umd-002677/IdeaProjects/travis2ga/data_projects_for_TAR_mining/github_files/'

import glob
import os
import shutil
# file_list = glob.iglob(Travis_only_projects,recursive=True)
# for file in file_list:
#     print(file)
#     try:
#         out_dir = Travis_files_out + file.lower().split('/')[5] + '/' + file.lower().split('/')[6] + '/'
#         os.makedirs(out_dir)
#         shutil.copy(file, out_dir + '.travis.yml')
#     except Exception as e:
#         print(e)
#         continue
past_dir=''
counter=0
for file in glob.iglob(GHA_only_projects, recursive=True):
    print(file)
    try:
        if len(file.lower().split('/')) >=9 and file.lower().split('/')[8] == 'workflows':
            out_dir = GHA_files_out + file.lower().split('/')[5] + '/' + file.lower().split('/')[6] + '/.github/workflows/'
        else:
            out_dir = GHA_files_out + file.lower().split('/')[5] + '/' + file.lower().split('/')[6] + '/.github/'
        try:
            os.makedirs(out_dir)
        except:
            pass
        print(os.path.isfile(out_dir+file.lower().split('/')[-1]))
        if(not os.path.isfile(out_dir+file.lower().split('/')[-1])):
            print(out_dir+file.lower().split('/')[-1])
            shutil.copy(file, out_dir)
        else:
            if(out_dir == past_dir):
                counter+=1
            else:
                counter=0
                past_dir=out_dir
            shutil.copy(file, out_dir + file.lower().split('/')[-1].split('.')[0] +'-' +str(counter)+'.yml')
    except Exception as e:
        print(e)
        continue
