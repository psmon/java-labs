# 인프라

이 저장소인 SpringBoot+AKKA를 통해 다양한 OpenStack과 Reactive Stream으로 연결하기위해 활용되는 OpenStack입니다.
 
- Mysql 5.6
- Postgres 9 
- Kafka 5
- Kafka UI 
- ELK


OpenStack을 다룰수있는 UI툴도 포함되어 있습니다.

## Kibana

<img src="https://velog.velcdn.com/images/jskim/post/b97de0fe-50d7-4377-9cca-db17b089a922/image.png" />

## KafkaUI

<img src="https://github.com/schooldevops/kafka-tutorials-with-kido/raw/main/imgs/kafka-ui-02.png" />

```
    # 구동하기
    
    ## 기본(DB)
    docker-compose up -d
    
    ## Kafka
    docker-compose -f docker-compose-kafka.yml up -d
    
    ## Elk
    docker-compose -f docker-compose-elk.yml up -d        

    #내리기
    docker-compose -f docker-compose-elk.yml down    
    docker-compose -f docker-compose-kafka.yml down    
    docker-compose down
