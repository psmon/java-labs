# test env

```
akka.hostname=127.0.0.1
akka.role=aa
akka.port=2209
akka.seed-nodes=2209

```


# spring application properties

```
################################################################################
# Service DB Config
################################################################################
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://localhost:13306/db_test?useSSL=false&useUnicode=true&serverTimezone=Asia/Seoul
spring.datasource.username=root
spring.datasource.password=root
# JPA
spring.service.hibernate.hbm2ddl.auto = none
spring.service.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.service.hibernate.show_sql  = false
spring.service.hibernate.format_sql  = false

spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=none
spring.jpa.properties.hibernate.format_sql=true
```

```
################################################################################
# Test For Config
################################################################################
server.port = 8080
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://localhost:13306/db_test?useSSL=false&useUnicode=true&serverTimezone=Asia/Seoul
spring.datasource.username=root
spring.datasource.password=root
spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.database-platform = org.hibernate.dialect.MySQL8Dialect
```