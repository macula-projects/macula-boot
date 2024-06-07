# Macula Boot Starter Async

## @Aysnc注解支持TTL

```java
public class AsyncAutoConfiguration {
    @Bean
    public TaskDecorator ttlTaskDecorator() {
        return TtlRunnable::get;
    }
}
```

## CompletableFuture支持TTL

```java
CompletableFuture;CompletableFuture.runAsync(TtlRunnable.get(() -> {
    //... do your business
}));

CompletableFuture.supplyAsync(TtlWrappers.wrapSupplier(() -> {
    //... do your business
}));

// 建议采用下述方式与@Async共享Executor
CompletableFuture.runAsync(()-> {
        //... do your business
}), taskExecutor);
``` 