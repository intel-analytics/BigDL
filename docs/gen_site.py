#!/usr/bin/env python

import argparse
import sys
import os
from subprocess import Popen, PIPE
import subprocess


def run_cmd(cmds, err_msg):
    try:
        # p = Popen(cmds, stdout=PIPE, stderr=PIPE)
        p = Popen(cmds)
        p.communicate()
        # if len(stdout) > 0:
        #     print stdout
        # if len(stderr) > 0:
        #     print stderr
        if p.returncode != 0:
            print err_msg
            sys.exit()
    except OSError as e:
        print err_msg
        print e.strerror
        sys.exit()

parser = argparse.ArgumentParser(description='Process BigDL docs.')
parser.add_argument('-s', '--scaladocs',
    required=True, type=str, dest='scaladocsflag', 
    help='Add scala doc to site? Y/y or N/n')
parser.add_argument('-p', '--pythondocs',
    required=True, type=str, dest='pythondocsflag', 
    help='Add python doc to site? Y/y or N/n')


args = parser.parse_args()

scaladocs = False
if args.scaladocsflag == 'y' or args.scaladocsflag == 'Y':
    scaladocs = True
elif args.scaladocsflag == 'n' or args.scaladocsflag == 'N':
    scaladocs = False
else:
    parser.print_help()
    sys.exit() 

pythondocs = False
if args.pythondocsflag == 'y' or args.pythondocsflag == 'Y':
    pythondocs = True
elif args.pythondocsflag == 'n' or args.pythondocsflag == 'N':
    pythondocs = False 
else:
    parser.print_help()
    sys.exit()

script_path = os.path.realpath(__file__)
dir_name = os.path.dirname(script_path)
os.chdir(dir_name)
run_cmd(['rm', '-rf', '{}/readthedocs'.format(dir_name)],
    'rm readthedocs error')

# check if mkdoc is installed
run_cmd(['mkdocs', '--version'], 
    'Please install mkdocs and run this script again\n\te.g., pip install mkdocs')

# git clone docs
run_cmd(['rm', '-rf', '/tmp/bigdl-doc'],
    'rm readthedocs error')

run_cmd(['git', 'clone', 'https://github.com/helenlly/bigdl-project.github.io.git', '/tmp/bigdl-doc'],
    'git clone readthedocs error')

run_cmd(['mv', '/tmp/bigdl-doc/readthedocs', dir_name],
    'mv readthedocs error')

run_cmd(['rm', '-rf', '/tmp/bigdl-doc'],
    'rm readthedocs error')

# mkdocs build
run_cmd(['mkdocs', 'build'], 
    'mkdocs build error')

if scaladocs:
    print 'build scala doc'
    bigdl_dir = os.path.dirname(dir_name)
    os.chdir(bigdl_dir)
    run_cmd(['mvn', 'scala:doc'], 'Build scala doc error')
    scaladocs_dir = bigdl_dir + '/spark/dl/target/site/scaladocs'
    target_dir = dir_name + '/site/APIdocs/scaladoc'
    run_cmd(['cp', '-r', scaladocs_dir, target_dir],
        'mv scaladocs error')

if pythondocs:
    print 'build python'

os.chdir(dir_name)
run_cmd(['mkdocs', 'serve'], 
    'mkdocs start serve error')

