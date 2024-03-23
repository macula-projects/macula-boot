package dev.macula.example.consumer;

import lombok.Getter;
import org.junit.jupiter.api.Test;

import java.util.Optional;

/**
 * <p>
 * <b>OptionalTest</b> Optional使用测试
 * </p>
 *
 * @author Rain
 * @since 2024/3/22
 */
public class OptionalTest {
    Person person = new Person();

    @Test
    public void testNull() {
        System.out.println(
                Optional.ofNullable(person)
                        .map(Person::getUser)
                        .map(User::getAddress)
                        .map(Address::getStreet).orElse("default")
        );
    }
}

@Getter
class Person {
    private User user = new User();
}

@Getter
class User {
    private Address address = new Address();
}

@Getter
class Address {
    private String street = "abc";
}


