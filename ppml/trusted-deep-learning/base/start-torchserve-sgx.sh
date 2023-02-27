configFile=""
backend_core_list=""
frontend_core=""

# TODO: shall we allow the user to now specify core numbers and falls back to default?

usage() {
        echo "Usage: $0 [-c <configfile>] [-b <core# for each backend worker>] [-f <core# for frontend worker>]"

        echo "The following example command will launch 2 backend workers (as specified in /ppml/torchserve_config)."
        echo "The first backend worker will be pinned to core 0 while the second backend worker will be pinned to core 1."
        echo "The frontend worker will be pinned to core 5"
        echo "Example: $0 -c /ppml/torchserve_config -t '0,1' -f 5"
        exit 0
}


while getopts ":b:c:f:" opt
do
    case $opt in
        b)
            backend_core_list=$OPTARG
            ;;
        c)
            configFile=$OPTARG
            ;;
        f)
            frontend_core=$OPTARG
            ;;
        *)
            echo "Error: unknown positional arguments"
            usage
            ;;
    esac
done

# Check backend_core_list and frontend_core has values
if [ -z "${backend_core_list}" ] || [ -z "${frontend_core}" ]; then
    echo "Error: please specify backend core lists and frontend core"
    usage
fi

# Check config file exists
if [ ! -f "${configFile}" ]; then
    echo "Error: cannot find config file"
    usage
fi

declare -a cores=($(echo $backend_core_list | tr "," " "))

cd /ppml
./init.sh
port=9000
cat $configFile | while read line
do
        while [[ $line =~ "minWorkers" ]]
        do
                line=${line#*\"minWorkers\": }
                num=${line%%,*}
                line=${line#*,}

                if [ ${#cores[@]} != $num ]; then
                    echo "Error: worker number does not equal to the length of core list"
                    exit 1
                fi

                for ((i=0;i<num;i++,port++))
                do
                (
                        bash /ppml/torchserve/start-backend-sgx.sh -p $port -c ${cores[$i]}
                )&
                done
        done
done
(
        bash /ppml/torchserve/start-frontend-sgx.sh -c $configFile -f $frontend_core
)&
wait

