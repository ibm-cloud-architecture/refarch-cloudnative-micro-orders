create table if not exists orders (
    orderId bigint not null auto_increment primary key,
    itemId int not null,
    customerId varchar(64) not null,
    count int not null
);

