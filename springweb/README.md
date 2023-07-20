# Getting Started

### Spring Boot Starter

이 프로젝트의 시작 템플릿으로 이미 작성된 프로젝트를 로드하는법을 아는것도 중요하지만

더 중요한것은 Empty상태에서 의존모듈을 추가하면서 직접 작동되고 빌드되는 프로젝트를 구성할줄 아는것입니다.


![텍스트](./doc/start.png)

- https://start.spring.io/

### Import

최초 로컬환경에서 프로젝트 로드하기

#### Step1 - Import Project 
![텍스트](./doc/step2.png)

#### Step2 - Gradle File 선택하기
![텍스트](./doc/step3.png)


#### Step3 - Gradle Build 환경 개인화
![텍스트](./doc/step4.png)

    //gradle-wapper.properties 
    //Spring 2.7.7을 호환하는 Gradle 버전으로 수정진행됨
    distributionUrl=https\://services.gradle.org/distributions/gradle-6.8.3-bin.zip


### OpenJDK 11
- https://www.openlogic.com/openjdk-downloads?field_java_parent_version_target_id=406&field_operating_system_target_id=436&field_architecture_target_id=391&field_java_package_target_id=396


### 빌드/실행하기

      .   ____          _            __ _ _
     /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
    ( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
     \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
      '  |____| .__|_| |_|_| |_\__, | / / / /
     =========|_|==============|___/=/_/_/_/
     :: Spring Boot ::       (v2.7.7-SNAPSHOT)

- IDE의 Build/RUN을 통해서실행
- Gradle Tab의 GRADLE TASK를 통해서도 수행가능

### Gradle 주요명령어

빌드툴로 Gradle또는 Maven및 SBT등 다양한 툴을 선택할수 있습니다.
빌드툴 채택은 CI/CD와도 연결되기 때문에 팀에 단일화 선택하는것이 좋으며
채택이 되었다고하면 자신의 코드를 빌드&실행&디버그를 하는것은 중요하기 때문에
빌드툴에서 지원하는 기능은 중요합니다.

조금더 숙련된 개발자라고 하면, 빌드타임시 유닛테스트를 포함 디펜던시 최적화 빌드시간및 빌드사이즈 최적화도 고민하게 됩니다.

#### 빌드
- grade build -x test : 단위테스트 건너띄고 빌드
- gradle -Dspring.profiles.active=dev build : 특정 프로필로 빌드하기

#### 실행
- gradle bootRun : 실행
- ./gradlew build && java -jar ./gate/build/libs/gate-spring-0.0.1-SNAPSHOT.jar : 실행2
- gradle -Dspring.profiles.active=dev bootRun : 프로필적용 실행

#### 테스트&유틸
- gradle test : 단위 테스트
- gradle :subproject:test : 특정 하위 프로젝트 단위 테스트만 수행
- gradle test --tests *VerificationRepositoryTest* : 특정케이스만 수행
- gradle :subproject:test --tests *VerificationRepositoryTest* : 특정하위 테스트 케이스만 돌리기
- gradle -q dependencies : 디펜던시 확인
- gradle -q dependencies subproject:dependencies : 특정프로젝트 하위 디펜던시 확인


### 연습과제

이 저장소를 빌드/실행 성공하였으면 다음을 수행

- https://start.spring.io 에서 자신의 Group ID로 (com.yourname) 어플리케이션 생성
- GreetingController 참고해서 , /greeting2 API 작성해보기 


###

- 실행 : http://localhost:8080/swagger-ui/index.html

### Reference Documentation
For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/2.7.7-SNAPSHOT/gradle-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/2.7.7-SNAPSHOT/gradle-plugin/reference/html/#build-image)
* [Spring Web](https://docs.spring.io/spring-boot/docs/2.7.7-SNAPSHOT/reference/htmlsingle/#web)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)

### Additional Links
These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)

