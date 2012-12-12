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


# This script can be used to create two tables, one for RI persistence and another for the TCK
# transaction tests. After starting the database it will drop and recreate the necessary tables and populate them
# using the DDL files provided with the RI/TCK. 

# Example invokation of this script looks like this on windows
#  initDerbyDatabases.bat

echo
echo "Inititalizing Derby Databases for JSR352 RI/TCK"

USAGE="Usage: initDerbyDatabases.sh <DATABASE_ROOT_DIR>"

cd `dirname $0`
SCRIPT_HOME=`pwd`
cd -
WORKSPACE_HOME=$SCRIPT_HOME/../..
DERBY_HOME=$WORKSPACE_HOME/JSR352.BinaryDependencies/shipped/derby


if [ -z "$DERBY_HOME" ]; then
	echo "Missing DERBY_HOME directory!!! This is the directory where derby is installed. "
	echo $USAGE
	exit 1
fi

DATABASE_ROOT_DIR=$1
if [ -z "$DATABASE_ROOT_DIR" ]; then
	echo "Missing database root directory!!! This can be any directory where you want to create your derby databases. "
	echo $USAGE
	exit 1
fi
echo "Building databases in $DATABASE_ROOT_DIR if they don't already exist"
export DERBY_OPTS="-Dderby.system.home=$DATABASE_ROOT_DIR"

# Run the DDL files to create the database (if it doesn't already exist) and drop and recreate the tables
echo $DERBY_HOME/bin/ij $WORKSPACE_HOME/JSR352.Runtime/resources/jsr352-derby.ddl
`$DERBY_HOME/bin/ij $WORKSPACE_HOME/JSR352.Runtime/resources/jsr352-derby.ddl`

echo
echo $DERBY_HOME/bin/ij $WORKSPACE_HOME/JSR352.Tests.TCK/resources/jsr352-order-database.ddl
`$DERBY_HOME/bin/ij $WORKSPACE_HOME/JSR352.Tests.TCK/resources/jsr352-order-database.ddl`

echo
echo $DERBY_HOME/bin/ij $WORKSPACE_HOME/JSR352.Tests.TCK/resources/jsr352-numbers-database.ddl
`$DERBY_HOME/bin/ij $WORKSPACE_HOME/JSR352.Tests.TCK/resources/jsr352-numbers-database.ddl`

echo "Derby database initialization complete"
  
