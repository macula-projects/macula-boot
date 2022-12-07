# Leader Election

借助redis选择master

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Component
    public class Service {
        public Service(LeaderElection leaderElection) {
            leaderElection.addElectionListener(() -> System.out.println("master selected"));
        }
    }
}
```
