#!/bin/bash
#
# Copyright 2012 International Business Machines Corp.
#
# See the NOTICE file distributed with this work for additional information
# regarding copyright ownership. Licensed under the Apache License,
# Version 2.0 (the "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# This script can be used to start two Derby network servers; one for JDBC persistence and another for the TCK
# transaction tests. After starting the databases it will drop and recreate the necessary tables and populate them
# using the DDL files provided with the RI/TCK. 
# 

#Example invokation of this script looks like this on windows using cygwin:
#>> initDerbyDatabases.sh /cygdrive/c/ibm/tools/db-derby-10.9.1.0-bin /cygdrive/c/ibm/sandbox/jsr352db/ 1621 c:/ibm/RAD_workspaces/JSR352.Workspace5/

echo
date
echo "Inititalizing Derby Databases for JSR352 RI/TCK"

USAGE="Usage: initDerbyDatabases.sh <DERBY_HOME> <DATABASE_ROOT_DIR> <PORT_NUMBER> <JSR352_ECLIPSE_WORKSPACE>"

DERBY_HOME=$1                    #The derby installation directory
DATABASE_ROOT_DIR=$2             #Location where databases will be created
PORT_NUMBER=$3                   #Port number for JDBC persistence database. The TCK Transaction test database will use PORT_NUMBER+1. So make sure both ports are available.
JSR352_ECLIPSE_WORKSPACE=$4      #Location of your JSR352 Eclipse workspace


if [ -z "$DERBY_HOME" ]; then
    echo "Missing Derby Home directory!!! It usually looks something like c:/tools/derby-10.9.1.0 or /home/kmukher/derby-10.9.1.0"
    echo $USAGE
    exit 1
fi

if [ -z "$DATABASE_ROOT_DIR" ]; then
    echo "Missing database root directory!!! This can be any directory where you want to create your derby databases. "
    echo $USAGE
    exit 2
fi

if [ -z "$PORT_NUMBER" ]; then
    echo "Missing PORT_NUMBER root directory!!! This can be any directory where you want to create your derby databases. "
    echo $USAGE
    exit 3
fi

if [ -z "$JSR352_ECLIPSE_WORKSPACE" ]; then
    echo "Missing JSR352_ECLIPSE_WORKSPACE root directory!!! This is the workspace that contains the JSR352 RI/TCK "
    echo $USAGE
    exit 4
fi

echo "PWD is $PWD"
CUR_TEMP_DIR=$PWD

echo
#Start the two databases
cd $DATABASE_ROOT_DIR
echo "PWD is $PWD"

$DERBY_HOME/bin/startNetworkServer -h localhost -p $PORT_NUMBER &

PORT_TWO=`expr $PORT_NUMBER + 1`
$DERBY_HOME/bin/startNetworkServer -h localhost -p $PORT_TWO &

PORT_THREE=`expr $PORT_NUMBER + 2`
$DERBY_HOME/bin/startNetworkServer -h localhost -p $PORT_THREE &

echo
echo "*********************************************************"
echo "If you see socket errors like this it means you derby servers are already started and you can ignore the warning. Or" 
echo "the port you assigned to the derby server is already being used by another process, in which case you need to choose"
echo "a new listener port. Don't forget to update the DDL file connect statements and datasource connection ports if this happens."
echo
echo "example error: Tue Oct 16 18:29:13 EDT 2012 : Could not listen on port 1622 on host localhost:"
echo "example error: java.net.SocketException: Unrecognized Windows Sockets error: 0: JVM_Bind"
echo "*********************************************************"
echo

#Wait a few seconds for the databases to start up
sleep 4

echo "Building databases in $DATABASE_ROOT_DIR if they don't already exist"

echo "PWD is $PWD"
#Run the DDL files to create the database (if it doesn't already exist) and drop and recreate the tables
echo "Running $DERBY_HOME/bin/ij $JSR352_ECLIPSE_WORKSPACE/JSR352.Runtime/resources/jsr352-derby.ddl"
$DERBY_HOME/bin/ij "$JSR352_ECLIPSE_WORKSPACE/JSR352.Runtime/resources/jsr352-derby.ddl"

echo
echo "Running $DERBY_HOME/bin/ij $JSR352_ECLIPSE_WORKSPACE/JSR352.Tests.TCK/resources/jsr352-order-database.ddl"
$DERBY_HOME/bin/ij "$JSR352_ECLIPSE_WORKSPACE/JSR352.Tests.TCK/resources/jsr352-order-database.ddl"

echo
echo "Running $DERBY_HOME/bin/ij $JSR352_ECLIPSE_WORKSPACE/JSR352.Tests.TCK/resources/jsr352-numbers-database.ddl"
$DERBY_HOME/bin/ij "$JSR352_ECLIPSE_WORKSPACE/JSR352.Tests.TCK/resources/jsr352-numbers-database.ddl"

cd $CUR_TEMP_DIR

echo ""
date
#echo "Derby database initialization complete"
  
