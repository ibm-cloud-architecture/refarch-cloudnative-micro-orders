#!/bin/bash
####################################################################################################################
#####################################################################################################################
##  detect-and-install-new-relic.sh
##  ©Copyright IBM Corporation 2016
##  Written by Hans Kristian Moen September 2016
##
##  Script does three things:
##  1. Looks for New Relic license key in bound services and copy them to NEW_RELIC_LICENSE_KEY if exists
##  2. If New Relic license key is present and agent is not installed, run nmp install
##  3. If New Relic license key is present and no agent config file is installed, sets NEW_RELIC_NO_CONFIG_FILE
##
##  NOTE: After Cloud Foundry v238, this functionality should be moved from .profile.d/ into .profile 
##
##  LICENSE: MIT (http://opensource.org/licenses/MIT) 
##
#####################################################################################################################
###################################################################################################################

agent_file="/agents/newrelic/newrelic.jar"
agent_config="/agents/newrelic/newrelic.yaml"

# Only check for license key in VCAP_SERVICES if they have not been passed in directly
if [[ -z $NEW_RELIC_LICENSE_KEY ]] 
then
  echo "Checking for New Relic license key in bound services"
  ## Check if we have bound to a brokered New Relic service
  LICENSE_KEY=$(echo  "${VCAP_SERVICES}" | jq --raw-output ".newrelic[0].credentials.licenseKey")

  ## Allow user-provided-services to overwrite brokered services, if they exist
  UP_LICENSE_KEY=$(echo "${VCAP_SERVICES}" | jq --raw-output  '.["user-provided"] | .[] | select(.name == "newrelic") | .credentials.licenseKey' 2>/dev/null )
  if [[ "$UP_LICENSE_KEY" != "null" ]] && [[ ! -z $UP_LICENSE_KEY ]]
  then
    echo "License Key found in User Provided Service: ${UP_LICENSE_KEY}"
    LICENSE_KEY=$UP_LICENSE_KEY
  fi
  
  if [[ ! -z $LICENSE_KEY ]] && [[ "${LICENSE_KEY}" != "null" ]]
  then
    echo "Found bound New Relic service instance"
    export NEW_RELIC_LICENSE_KEY=$LICENSE_KEY
  fi
fi

# If we have a New Relic License Key, make sure newrelic agent is loaded on Java start
if [[ ! -z $NEW_RELIC_LICENSE_KEY ]]
then
  echo "Found New Relic license Key"
  ## Check if module is supplied
  if [[ ! -f ${agent_file} ]]
  then
    # TODO: Figure out what to do if we can't fint the agent
    echo "Couldn't find newrelic agent installed in ${agent_file}"
  else
    # Enable the newrelic agent
    export JAVA_OPTS="${JAVA_OPTS} -javaagent:${agent_file}"
    APP_NAME=${CG_NAME}
    #APP_NAME=$(echo $VCAP_APPLICATION | jq --raw-output '.name')
    export NEW_RELIC_APP_NAME=$APP_NAME
    echo "Setting New Relic appname to ${APP_NAME}"
  fi
  if [[ -f ${agent_config} ]]
  then
    echo "Found New Relic config in ${agent_config}"
    export JAVA_OPTS="${JAVA_OPTS} -Dnewrelic.config.file=${agent_config} -Dnewrelic.bootstrap_classpath=true"
  else
    echo "Problems finding New Relic config file"
    echo "Agent may have problems loading"
  fi
else
  echo "No New Relic license key found"
fi


