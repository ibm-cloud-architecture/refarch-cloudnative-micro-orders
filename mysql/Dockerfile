FROM mariadb:10.4.6

ADD scripts/create_orders_table.sql /docker-entrypoint-initdb.d/

CMD ["mysqld"]
