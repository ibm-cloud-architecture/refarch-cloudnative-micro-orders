#!/bin/bash
keytool -genkeypair -dname "cn=bc.ibm.com, o=Hema, ou=IBM, c=US" -alias bckey -keyalg RSA -keysize 2048 -keypass password -storetype JKS -keystore ./BCKeyStoreFile.jks -storepass password -validity 3650
keytool -list -keystore ./BCKeyStoreFile.jks -storepass password
keytool -export -alias bckey -file client.cer -keystore ./BCKeyStoreFile.jks -storepass password -noprompt
keytool -import -v -trustcacerts -alias bckey -file client.cer -keystore ./truststore.jks -storepass password -noprompt
mkdir keystorevol && cp *.jks $_

echo done!
