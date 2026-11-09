# ADR-003 · Java 21 y Spring Boot 4.1

- **Estado:** aceptada el 25/10/2026 (stack, paso 3.1); Spring Boot 4.1 desde
  el 28/10/2026
- **Origen:** paso 3.1 y registro de decisiones del 28/10

## Contexto

El backend tiene que ofrecer una API REST con seguridad, acceso a PostgreSQL
y validación, y el proyecto quiere practicar el stack más pedido en las
ofertas de backend en Java. Al empezar el desarrollo, Spring Boot 3.5 estaba
al final de su soporte gratuito.

## Decisión

**Java 21 (LTS) + Spring Boot 4.1** con Web MVC, Data JPA, Validation y
Security, construido con **Maven**. El JWT se firma y se valida con el
`oauth2-resource-server` de Spring, sin librerías de terceros.

## Alternativas descartadas

- **Spring Boot 3.5:** más ejemplos en internet, pero sin soporte gratuito
  durante la vida del proyecto.
- **Gradle:** igual de válido; Maven es el más extendido en los proyectos
  Spring y su `pom.xml` se lee sin saber un lenguaje de scripts.

## Consecuencias

- Algunas guías y respuestas de internet son de la 3.x y hay que adaptarlas.
- El mismo backend servirá a la web y a la futura app Android.
