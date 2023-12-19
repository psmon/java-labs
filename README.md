# JAVA-LABS

![텍스트](./springweb//doc/akka-intro.png)


Spring Boot을 통해 JAVA를 연구하고 다양한 오픈스택들을 실험하고 학습하는 프로젝트로

Akka를 중심으로 Reactive Streams을 다루고 있습니다.


## 사용툴

- https://www.jetbrains.com/ko-kr/idea/download

## Spring BootWEB 기초

- [프로젝트셋팅하기](./springweb/README.md)

## AKKA

- [AKKA 이용샘플](./springweb/src/test/java/com/webnori/springweb/akka/README.md)
- [AkkaLabs](https://wiki.webnori.com/display/AKKA/AKKA+Labs)

## Docker Build

```
cd springweb

docker build -f Dockerfile --force-rm -t java-labs-webnori:dev  .

docker run -e SPRING_PROFILES_ACTIVE=dev --publish 8081:8080 --name cluster-lab-app1 java-labs-webnori:dev

# Cluster Test

docker run -e SPRING_PROFILES_ACTIVE=dev --publish 8082:8080 --name cluster-lab-app2 java-labs-webnori:dev

docker run -e SPRING_PROFILES_ACTIVE=dev --publish 8083:8080 --name cluster-lab-app3 java-labs-webnori:dev

```


