FROM mysql

ADD scripts/load-data.sh load-data.sh
ADD scripts/load-data.sql load-data.sql

RUN chmod u+x load-data.sh

ADD scripts/create_orders_db.sh create_orders_db.sh
ADD scripts/create_orders_table.sql create_orders_table.sql

RUN chmod u+x create_orders_db.sh

CMD ["mysqld"]
