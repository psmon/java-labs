# 인프라

이 저장소인 SpringBoot+AKKA를 통해 다양한 OpenStack과 Reactive Stream으로 연결하기위해 활용되는 OpenStack입니다.
 
- Mysql 8 
- Kafka 5
- Kafka UI 
- ELK

로컬테스트에 따라 선택하여 구동할수 있습니다.  샘플에 의해 테스트되는 DB스키마의 DDL은 도커구동시 
자동생성되며 init/firstsql.txt 에 스크립트 작성되어 있습니다.

데이터를 시각화하는 Kibana및 Kafka UI툴이 포함되어 있습니다.

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
