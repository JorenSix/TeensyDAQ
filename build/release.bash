#!/bin/bash

#Remove old releases
rm -R TeensyDAQ-*

#Build the new release
ant 

#create the javadoc
ant javadoc

#Find the current version
filename=$(basename TeensyDAQ-*.jar)
version=${filename:10:3}

#move the javadoc documentation to another foler:
mv doc TeensyDAQ-$version-Documentation

deploy_dir="/var/www/be.0110/current/public/releases/TeensyDAQ"
deploy_location=$deploy_dir/TeensyDAQ-$version

#Build the readme file
textile2html ../README.textile TeensyDAQ-$version-Readme.html

#Remove old version from the server:
ssh joren@0110.be rm -R $deploy_location
ssh joren@0110.be mkdir $deploy_location

#Deploy to the server 
scp -r TeensyDAQ-* joren@0110.be:$deploy_location

ssh joren@0110.be rm -R $deploy_dir/TeensyDAQ-latest
ssh joren@0110.be mkdir $deploy_dir/TeensyDAQ-latest
ssh joren@0110.be ln -s -f $deploy_location/TeensyDAQ-$version.jar $deploy_dir/TeensyDAQ-latest/TeensyDAQ-latest.jar
ssh joren@0110.be ln -s -f $deploy_location/TeensyDAQ-$version-Documentation $deploy_dir/TeensyDAQ-latest/TeensyDAQ-latest-Documentation
ssh joren@0110.be ln -s -f $deploy_location/TeensyDAQ-$version-Readme.html $deploy_dir/TeensyDAQ-latest/TeensyDAQ-latest-Readme.html
