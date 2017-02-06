#!/bin/bash

MYSQL_ORDERS_DATABASE=ordersdb
MYSQL_ORDERS_USER=orders_dbuser
MYSQL_ORDERS_PASSWORD=Pass4ordersus3r

_password_opt="-p${MYSQL_ROOT_PASSWORD}"
if [ -z "${MYSQL_ROOT_PASSWORD}" ]; then
    _password_opt=""
fi

echo "Creating database ${MYSQL_ORDERS_DATABASE} ..."
mysql -uroot ${_password_opt} <<EOF
create database if not exists ${MYSQL_ORDERS_DATABASE};
EOF

echo "Creating database user ${MYSQL_ORDERS_USER} ..."
mysql -uroot ${_password_opt} <<EOF
create user if not exists '${MYSQL_ORDERS_USER}'@'%' identified by '${MYSQL_ORDERS_PASSWORD}';
grant all on ${MYSQL_ORDERS_DATABASE}.* to '${MYSQL_ORDERS_USER}'@'%';
EOF

echo "Creating orders table ..."

mysql -uroot ${_password_opt} <<EOF
use ${MYSQL_ORDERS_DATABASE};
create table if not exists orders (
    orderId bigint not null auto_increment primary key,
    itemId int not null,
    customerId varchar(64) not null,
    count int not null
);
EOF
