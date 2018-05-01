FROM mysql

# For mysql and mysqld
ENV PATH /usr/bin:$PATH
ENV PATH /usr/sbin:$PATH

ADD scripts/load-data.sql scripts/create_orders_table.sql /docker-entrypoint-initdb.d/

CMD ["mysqld"]
