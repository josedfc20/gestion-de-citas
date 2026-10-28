package com.gestiondecitas;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * PostgreSQL 16 real para las pruebas de integración. Testcontainers lo crea en
 * Docker al empezar y lo destruye al terminar; no toca la base de datos local.
 * Los correos van a un buzón en memoria y las tareas {@code @Async} se ejecutan
 * en el mismo hilo, para que el correo ya esté enviado al terminar la petición.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:16-alpine");
    }

    @Bean
    @Primary
    CorreosDePrueba correosDePrueba() {
        return new CorreosDePrueba();
    }

    @Bean(name = "taskExecutor")
    TaskExecutor taskExecutor() {
        return new SyncTaskExecutor();
    }
}
