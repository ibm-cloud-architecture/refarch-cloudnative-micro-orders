create table if not exists orders (
   orderId varchar(64) not null primary key,
   itemId int not null,
   customerId varchar(64) not null,
   count int not null,
   date timestamp not null
);
