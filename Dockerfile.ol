FROM open-liberty:microProfile1

MAINTAINER IBM Java engineering at IBM Cloud

RUN ln -s /opt/ol/wlp/usr/servers /servers

# copy over the server
COPY /target/liberty/wlp/usr/servers/defaultServer /config/

# copy over the opentracing extension
COPY /target/liberty/wlp/usr/extension /opt/ol/wlp/usr/extension

CMD ["/opt/ol/wlp/bin/server", "run", "defaultServer"]
