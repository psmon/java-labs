# Infra as CODE

오늘날의 웹은 다양한 오픈스택과 함께 작동이되며~

쿠버네티스는 서비스를 안정적으로 배포하고 운영해야하는 데브옵스 조직에서 이용해야하는 표준스택이 되었습니다.

그 중심에 도커가 있으며 로컬환경에서 다양한 오픈스택을 쉽게 구축하고 이용할수 있으며

이제 이러한 기술은 SE가 구축하고 제공해줘 개발자가 관심없어야 하는 스택이 아니라

자신이 만든 서비스의 의존스택을 스스로 띄울수 있는것은 선택적 요소가 아니라 기본적 소양이 되었습니다.

Kafka/Elastic Search 스택을 단순하게 구동하는것을 넘어 이것을 잘 활용하는 수준까지 이 저장소의 연구목표입니다.


## 이 연구 프로젝트가 활용하는 스택

 
- Mysql 8 
- Kafka 5
- Kafka UI 
- ELK
- Spark

로컬테스트에 따라 선택하여 구동할수 있습니다.  샘플에 의해 테스트되는 DB스키마의 DDL은 도커구동시 
자동생성되며 init/firstsql.txt 에 스크립트 작성되어 있습니다.

데이터를 시각화하는 Kibana및 Kafka UI툴이 포함되어 있습니다.

## Kibana

<img src="https://velog.velcdn.com/images/jskim/post/b97de0fe-50d7-4377-9cca-db17b089a922/image.png" />

## KafkaUI

<img src="https://github.com/schooldevops/kafka-tutorials-with-kido/raw/main/imgs/kafka-ui-02.png" />

```
# 각 OpenStack별로 구동하기

로컬컴퓨터의 메모리 요구사항이 너무 높아지는것을 방지하기위해 
구성요소를 구분하였습니다.


## Mysql

docker-compose -f docker-compose-mysql.yml up -d

## Kafka

docker-compose -f docker-compose-kafka.yml up -d

## Elk

docker-compose -f docker-compose-elk.yml up -d


## Spark

docker-compose -f docker-compose-spark.yml up -d

## AWS simulator by LocalStack(s3)

docker-compose -f docker-compose-localstack.yml up -d

# Down

docker-compose -f docker-compose-elk.yml down    

docker-compose -f docker-compose-kafka.yml down

docker-compose -f docker-compose-mysql.yml down

docker-compose -f docker-compose-localstack.yml down


```

## AWS CLI Tool

```
// AWS TestKey 설정
aws configure
      - AWS_ACCESS_KEY_ID=test
      - AWS_SECRET_ACCESS_KEY=test
      - AWS_DEFAULT_REGION=us-east-1

// S3(LocalStack) Bucket 생성

aws --endpoint-url=http://localhost:4567 s3api create-bucket --bucket mybucket --region us-east-1

aws --endpoint-url=http://localhost:4567 s3api create-bucket --bucket my-bucket2 --region us-east-1

aws --endpoint-url=http://localhost:4567 s3api list-buckets

aws --endpoint-url=http://localhost:4567 s3api list-object

aws --endpoint-url=http://localhost:4567 s3 ls s3://my-bucket//

```

## Docker Build

```
cd springweb

docker build -f Dockerfile --force-rm -t java-labs-webnori:dev  .

docker run -e SPRING_PROFILES_ACTIVE=dev --publish 8081:8080 --name cluster-lab-app1 java-labs-webnori:dev

# Cluster Test

docker run -e SPRING_PROFILES_ACTIVE=dev --publish 8082:8080 --name cluster-lab-app2 java-labs-webnori:dev

docker run -e SPRING_PROFILES_ACTIVE=dev --publish 8083:8080 --name cluster-lab-app3 java-labs-webnori:dev

```