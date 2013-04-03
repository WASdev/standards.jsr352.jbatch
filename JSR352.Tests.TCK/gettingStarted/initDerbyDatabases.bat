@REM
@REM Copyright 2012 International Business Machines Corp.
@REM
@REM See the NOTICE file distributed with this work for additional information
@REM regarding copyright ownership. Licensed under the Apache License,
@REM Version 2.0 (the "License"); you may not use this file except in compliance
@REM with the License. You may obtain a copy of the License at
@REM
@REM   http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM

@ECHO OFF

REM This script can be used to create two tables, one for RI persistence and another for the TCK
REM transaction tests. After starting the database it will drop and recreate the necessary tables and populate them
REM using the DDL files provided with the RI/TCK. 

REM Example invokation of this script looks like this on windows
REM  initDerbyDatabases.bat

echo
echo "Inititalizing Derby Databases for JSR352 RI/TCK"

set USAGE="Usage: initDerbyDatabases.sh <DATABASE_ROOT_DIR>"

set SCRIPT_HOME=%~dp0
set WORKSPACE_HOME=%SCRIPT_HOME%..\..
set DERBY_HOME=%WORKSPACE_HOME%\JSR352.BinaryDependencies\shipped\derby


if NOT "%1" == ""  GOTO GoodInput
echo "Missing database root directory!!! This can be any directory where you want to create your derby databases. "
echo %USAGE%
exit 1

:GoodInput
set DATABASE_ROOT_DIR=%1
echo "Building databases in %DATABASE_ROOT_DIR% if they don't already exist"
set DERBY_OPTS="-Dderby.system.home=%DATABASE_ROOT_DIR%"

REM Run the DDL files to create the database (if it doesn't already exist) and drop and recreate the tables
echo %DERBY_HOME%\bin\ij.bat  %WORKSPACE_HOME%\JSR352.Runtime\resources\jsr352-derby.ddl
call %DERBY_HOME%\bin\ij.bat %WORKSPACE_HOME%\JSR352.Runtime\resources\jsr352-derby.ddl

echo
echo %DERBY_HOME%\bin\ij.bat  %WORKSPACE_HOME%\JSR352.Tests.TCK\resources\jsr352-order-database.ddl
call %DERBY_HOME%\bin\ij.bat %WORKSPACE_HOME%\JSR352.Tests.TCK\resources\jsr352-order-database.ddl

echo
echo %DERBY_HOME%\bin\ij.bat  %WORKSPACE_HOME%\JSR352.Tests.TCK\resources\jsr352-numbers-database.ddl
call %DERBY_HOME%\bin\ij.bat %WORKSPACE_HOME%\JSR352.Tests.TCK\resources\jsr352-numbers-database.ddl

echo "Derby database initialization complete"
  
