package com.xiaoju.uemc.tinyid.client;


import org.junit.jupiter.api.Test;
import org.macula.boot.starter.tinyid.base.factory.IdGeneratorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @Author du_imba
 */
@SpringBootTest
public class ClientTest {
    @Autowired
    private IdGeneratorFactory idGeneratorFactory;

    @Test
    public void testNextId() {
        for (int i = 0; i < 100; i++) {
            Long id = idGeneratorFactory.getIdGenerator("test").nextId();
            System.out.println("current id is: " + id);
        }
    }
}
