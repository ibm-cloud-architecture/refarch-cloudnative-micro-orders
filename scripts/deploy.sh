#!/bin/bash
set -x

BUILD_NUMBER=$1
REGISTRY_NAME=$2
REGISTRY_NAMESPACE=$3

# Init helm
helm init

# Edit chart values using yaml (NEED TO INSTALL YAML) - Call image chart deployer
cd ../chart/orders

# Replace tag
string_to_replace=$(yaml read values.yaml image.tag)
sed -i.bak s%${string_to_replace}%${BUILD_NUMBER}%g values.yaml

# Replace registry name
string_to_replace=$(yaml read values.yaml registry.name)
sed -i.bak s%${string_to_replace}%${REGISTRY_NAME}%g values.yaml

# Replace registry namespace
string_to_replace=$(yaml read values.yaml registry.namespace)
sed -i.bak s%${string_to_replace}%${REGISTRY_NAMESPACE}%g values.yaml

# Install/Upgrade Chart
cd ..

release=$(helm list | grep orders | awk '{print $1}' | head -1)

if [[ -z "${release// }" ]]; then
    echo "Installing orders chart for the first time"
    helm install orders
else
    echo "Upgrading orders chart release"
    helm upgrade ${release} orders
fi