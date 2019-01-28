FROM websphere-liberty:18.0.0.3-webProfile7

MAINTAINER IBM Java engineering at IBM Cloud

# copy over the server
COPY /target/liberty/wlp/usr/servers/defaultServer /config/

# copy over the opentracing extension
COPY /target/liberty/wlp/usr/extension /opt/ibm/wlp/usr/extension

# Install required features if not present

RUN installUtility install --acceptLicense defaultServer

CMD ["/opt/ibm/wlp/bin/server", "run", "defaultServer"]

# Upgrade to production license if URL to JAR provided
ARG LICENSE_JAR_URL
RUN \
  if [ $LICENSE_JAR_URL ]; then \
    wget $LICENSE_JAR_URL -O /tmp/license.jar \
    && java -jar /tmp/license.jar -acceptLicense /opt/ibm \
    && rm /tmp/license.jar; \
  fi
