#!/bin/bash
set -x

#------------------------------------------------------------------------------
# Running Jakarta Batch TCK Version 2.1.0 against com.ibm.jbatch 2.1.0-M2
#
# This is a documented script that can be used to execute the Jakarta Batch TCK
# against the com.ibm.jbatch implementation.
#
# Think of this as a template to produce runs that could be use to certify
# com.ibm.jbatch as a compatible implementation.   It's not quite a record of
# what was actually run to certify (the latter should be available linked
# from an issue found here:
#   https://github.com/eclipse-ee4j/batch-api/labels/certification
#
# By using "set -x" we allow the 'script' command to be used to record output,
# documenting the environment.
#
# Note also this is not fully parameterized such that it will be automatically
# runnable with future versions of the related software, but is rather tied to
# the specific version under test.
#------------------------------------------------------------------------------

# We assume this will match the $JAVA_HOME level but don't enforce it within this script.
# We do at least echo enough output that someone verifying the run could manually check.
    
echo "Executing with JDK: $JDK_BASE_QUAL"

################
# OK TO MODIFY
################
echo
echo ------------------
echo Begin common setup
echo ------------------
echo

# 1. Root location of TCK execution - Also useful for holding this script itself, and its output logs
TCK_HOME_DIR=~/jkbatch
mkdir $TCK_HOME_DIR

#--------------------------------------------------
# Show some basic info about the OS, JDK/JRE, etc.
# This could be tweaked without ruining the
# validity of the test.
#--------------------------------------------------
uname -a
cat /etc/os-release
echo $JAVA_HOME
$JAVA_HOME/bin/java -version

#############################################
# DON'T CHANGE remainder of Java 8 portion
# (except comment out official URL override
#  when testing a staged copy of TCK)
#############################################

TCK_ARTIFACT_ID=jakarta.batch.official.tck-2.1.0

#
# STAGED
#

TCK_DOWNLOAD_URL=https://download.eclipse.org/jakartabatch/tck/eftl/$TCK_ARTIFACT_ID.zip

#
# OFFICIAL PROJECT LOCATION
#
#TCK_DOWNLOAD_URL=https://download.eclipse.org/jakartaee/batch/2.1/jakarta.batch.official.tck-2.1.0.zip

################
# DON'T CHANGE
################
cd $TCK_HOME_DIR

#
# get TCK zip into an empty directory
#
rm -rf tckdir; mkdir tckdir; cd tckdir
ls -la .
curl -fsSL -o $TCK_ARTIFACT_ID.zip $TCK_DOWNLOAD_URL

#
# Show SHA 256 of everything so far.  Validation should be done manually, since this script
# is part of the TCK zip
#
openssl version
openssl dgst -sha256 *.zip

# extract TCK 
$JAVA_HOME/bin/jar xvf $TCK_ARTIFACT_ID.zip

#------------------------------------------------
# Run JUnit runtime tests 
#------------------------------------------------

cd $TCK_ARTIFACT_ID/runners/se-classpath
pwd

echo
echo ------------------
echo Begin runtime tests
echo ------------------
echo

#mvn clean verify
mvn clean verify -Pstill-staging

echo
echo ------------------
echo End runtime tests
echo ------------------
echo

cd ../../..
pwd
cd $TCK_ARTIFACT_ID/runners/sigtest
pwd

echo
echo --------------------------
echo Begin signature tests
echo --------------------------
echo

#mvn clean verify 
mvn clean verify -Pstill-staging

echo --------------------
echo Begin SigTest tests 
echo --------------------


echo --------------------
echo Capture SHAs
echo --------------------
openssl dgst -sha256 ~/.m2/repository/com/ibm/jbatch/com.ibm.jbatch.container/2.1.0-M2/com.ibm.jbatch.container-2.1.0-M2.jar
openssl dgst -sha256 ~/.m2/repository/com/ibm/jbatch/com.ibm.jbatch.spi/2.1.0-M2/com.ibm.jbatch.spi-2.1.0-M2.jar
openssl dgst -sha256 ~/.m2/repository/jakarta/batch/jakarta.batch-api/2.1.0/jakarta.batch-api-2.1.0.jar
