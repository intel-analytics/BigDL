"""
	parse the input scala files to get the 
		- name of the class
		- arguments
		- default values
w	- unit test values 
			***need to map type to value
	writing unit test
		the first letter of the name in unitest is lower case
		*** unit test argument value
	init
		the last argument is bigdl_type
		argumetns are indented to align
		*** argument name should be converted to python style

        issue: there is no argument
        issue: false to False, true to True
"""
import re
import os
def unitTest(f, name, argument,prv_indent_level):
	current_indent_level = prv_indent_level+4
	f.write(current_indent_level*" ")
	f.write("'''\n")
	f.write(current_indent_level*" ")
	lower_name = name[0].lower() + name[1:]
	f.write(">>> %s = %s(%s)\n"%(lower_name,name,argument))
	f.write(current_indent_level*" ")
	f.write("creating: create%s\n"%(name))
	f.write(current_indent_level*" ")
	f.write("'''\n")
	f.write("\n")



def super_init(f,name,argument_name,prv_indent_level):
	current_indent_level = prv_indent_level+4
	f.write(current_indent_level*" ")
	lead_str = "super(%s, self).__init__("%name
	f.write(lead_str)
	argument_indent_level = current_indent_level+len(lead_str)
	f.write("None, bigdl_type")
	for idx, arg in enumerate(argument_name):
		f.write(",\n")
		f.write(argument_indent_level*" ")
		f.write(arg)
	f.write(")\n")
	f.write("\n")


def init(f,name, argument_name,default_value,prv_indent_level):
	"""
	argument_name and default_value are lists
	"NoD" is for no default value
	"""
	current_indent_level = prv_indent_level+4
	f.write(current_indent_level*" ")
	f.write("def __init__(self,\n")
	argument_indent_level = current_indent_level+12
	for arg,val in zip(argument_name,default_value):
		f.write(argument_indent_level*" ")
		f.write(arg)
		if val != "NoD":
			f.write("="+val)
		f.write(",\n")
	f.write(argument_indent_level*" ")
	f.write('bigdl_type="float"):\n')
	super_init(f,name,argument_name,current_indent_level)

	
def parse_arguments(args):
	args_list = []
	for arg in args:
		"""
		 alternative choice
		 tmp = re.findAll("([A-Z][a-z]*|^[a-z]*)",arg)
		 args_list.append("_".join(tmp))
		 
		"""
		new_arg = ''
		if len(arg)>2:
			for i in arg:
				if str.isupper(i):
					new_arg += ("_"+str.lower(i))
				else:
					new_arg += i
		else:
			new_arg = arg.lower()
		args_list.append(new_arg)
	return args_list

def parse_default_values(default_vals):
	new_default_vals = []
	for val in default_vals:
		if val == "false":
			val = "False"
		else:
                    if val == "true":
			val = "True"
                   
		new_default_vals.append(val)
	return new_default_vals


def parse(scala_filename):
	sf = open(scala_filename,"r")
	idx = 0
	lines = sf.readlines()
	
	for line in lines:
		if line.strip().split(' ')[0] =="object":
			break
		idx += 1
        if idx == len(lines):
            print "end of file", scala_filename
            return "",[],[],""
            
	raw_name = lines[idx].strip().split(' ')[1]
        name = re.findall("\w+",raw_name)[0]
        object_lines = ''.join(lines[idx:])
	print object_lines
	argument_line = (re.findall(r"\(.*?\)",object_lines,re.MULTILINE|re.S))[1]
        print argument_line
	pattern_args = "(\w+): \w+"
	pattern_default = "\w+: \w+ = (.*?(?=,|\)|\s))"
	pattern_type = "\w+: (\w+)"
	args = re.findall(pattern_args,argument_line,re.MULTILINE)
	vals = re.findall(pattern_default,argument_line,re.MULTILINE)
	types = re.findall(pattern_type, argument_line,re.MULTILINE)          
        default_vals = ["NoD" for i in range(len(args)-len(vals))]
	default_vals.extend(vals)
	parsed_args = parse_arguments(args)
	parsed_default_vals = parse_default_values(default_vals)
	print name, parsed_args, parsed_default_vals
	return name,parsed_args, parsed_default_vals, ", ".join(types)




def convert(f,name,args,default_vals,types):
	f.write("class %s(Model):\n"%name)
	unitTest(f,name,types,0)
	init(f,name,args,default_vals,0)


if __name__ == "__main__":
	scala_path = "scala/com/intel/analytics/bigdl/nn"
        success = []
        file_list = os.listdir(scala_path)
        file_list.sort()
        f = open("nn_layer.py","w")
	for file in file_list:
		print file
		if os.path.isfile(scala_path+"/"+file):
			name, args, default_vals,types = parse(scala_path+"/"+file)
			if name == "":
                            continue
			convert(f,name,args,default_vals,types)
                        success.append(file)
        print success,len(success)
	





