# JAVA-LABS

Spring Boot을 통해 JAVA를 연구하고 다양한 오픈스택들을 실험하고 학습하는 프로젝트입니다.

활용되는 OpenStack들은 모두 도커로 구동하며, 로컬인프라를 통한 LocalFirst 테스트가능한 코드작성을 지원합니다.

## 사용툴

- https://www.jetbrains.com/ko-kr/idea/download

## Spring BootWEB 기초

- [프로젝트셋팅하기](./springweb/README.md)

## AKKA

- [AKKA 이용샘플](./springweb/src/test/java/com/webnori/springweb/akka/README.md)

## Docker Build And Cluster

```
cd springweb

docker build -f Dockerfile --force-rm -t java-labs-webnori:dev  .

docker run -e SPRING_PROFILES_ACTIVE=dev --publish 8081:8080 --name cluster-lab-app1 java-labs-webnori:dev

# Cluster 테스트를 위해 N개 구동

docker run -e SPRING_PROFILES_ACTIVE=dev --publish 8082:8080 --name cluster-lab-app2 java-labs-webnori:dev

docker run -e SPRING_PROFILES_ACTIVE=dev --publish 8083:8080 --name cluster-lab-app3 java-labs-webnori:dev

```


## 유용한 단축키

- 그레이들 디펜던시 업데이트 : Ctrl + Shift + O
- 코드 Style : Ctrl + Shift + Alt + L

## 활용 예정 Stacks
 
- Persitence With (Mysql8,Nosql,ElasticSearch...)
- Docker with Spring BOOT
- Swagger API DOC with SpringBoot
- AKKA with Spring BOOT
- 
