env /bin/bash
#echo "docker container start" > runpgcmd.sh
c=`docker ps -a | grep devdb|awk '{printf $1}'`
echo $c
#docker ps -a | grep devdb|awk '{cmd="docker container start" <<< print $1}'
echo "docker container start $c"
