FROM open-liberty:microProfile1

MAINTAINER IBM Java engineering at IBM Cloud

RUN ln -s /opt/ol/wlp/usr/servers /servers

# copy over the server
COPY /target/liberty/wlp/usr/servers/defaultServer /config/

# copy over the opentracing extension
COPY /target/liberty/wlp/usr/extension /opt/ol/wlp/usr/extension

# Install required features if not present
# RUN installUtility install --acceptLicense defaultServer

CMD ["/opt/ol/wlp/bin/server", "run", "defaultServer"]

# Accept license if URL to JAR provided
ARG LICENSE_JAR_URL
RUN \
  if [ $LICENSE_JAR_URL ]; then \
    wget $LICENSE_JAR_URL -O /tmp/license.jar \
    && java -jar /tmp/license.jar -acceptLicense /opt/ol \
   && rm /tmp/license.jar; \
  fi
