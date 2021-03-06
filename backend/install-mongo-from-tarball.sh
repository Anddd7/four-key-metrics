#!/usr/bin/env bash

sudo apt-get install libcurl4 openssl liblzma5
wget https://fastdl.mongodb.org/linux/mongodb-linux-x86_64-debian10-4.4.4.tgz
tar -zxvf mongodb-linux-*-4.4.4.tgz
sudo ln -s ${PWD}/mongodb-linux-x86_64-debian10-4.4.4/bin/* /usr/local/bin/
mongo --eval 'db.serverStatus()'
echo "success!"