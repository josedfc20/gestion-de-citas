package com.gestiondecitas;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import jakarta.persistence.Entity;

import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.RestController;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * Reglas de arquitectura del paso 3.2 (RNF-006) comprobadas sobre el código
 * compilado, sin arrancar Spring. Solo mira el código de la aplicación, no las pruebas.
 */
@AnalyzeClasses(packages = "com.gestiondecitas", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    /** Controller → Service → Repository: un controller nunca usa un repository. */
    @ArchTest
    static final ArchRule losControllersNoUsanRepositorios = noClasses()
            .that().areAnnotatedWith(RestController.class)
            .should().dependOnClassesThat().areAssignableTo(Repository.class)
            .because("las capas son Controller → Service → Repository (paso 3.2)");

    /** La API usa DTO: ningún endpoint devuelve una entidad, ni dentro de una lista o un ResponseEntity. */
    @ArchTest
    static final ArchRule losControllersNoDevuelvenEntidades = methods()
            .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
            .and().arePublic()
            .should(noDevolverEntidades())
            .because("la API usa DTO y nunca devuelve entidades (paso 3.2)");

    /** {@code usuario} y {@code beneficiario} no dependen de {@code cita}: definen interfaces que implementa {@code cita} (B6). */
    @ArchTest
    static final ArchRule usuarioYBeneficiarioNoDependenDeCita = noClasses()
            .that().resideInAnyPackage("com.gestiondecitas.usuario..", "com.gestiondecitas.beneficiario..")
            .should().dependOnClassesThat().resideInAPackage("com.gestiondecitas.cita..")
            .because("cita depende de ellos a través de AportaDatosPersonales y CitasDeBeneficiario (B6)");

    /** La hora siempre sale del {@code Clock} inyectado, para que las pruebas puedan fijarla. */
    @ArchTest
    static final ArchRule nadieLeeLaHoraSinElReloj = noClasses()
            .should().callMethod(LocalDate.class, "now")
            .orShould().callMethod(LocalDateTime.class, "now")
            .orShould().callMethod(LocalTime.class, "now")
            .orShould().callMethod(Instant.class, "now")
            .orShould().callMethod(ZonedDateTime.class, "now")
            .orShould().callMethod(OffsetDateTime.class, "now")
            .orShould().callMethod(System.class, "currentTimeMillis")
            .because("se usa el Clock inyectado, nunca now() directo (paso 3.2)");

    /** El cálculo de la disponibilidad es Java puro, sin Spring ni base de datos (B5). */
    @ArchTest
    static final ArchRule elCalculoDeDisponibilidadNoDependeDeSpring = classes()
            .that().haveSimpleNameStartingWith("CalculoDisponibilidad")
            .should().onlyDependOnClassesThat().resideOutsideOfPackages("org.springframework..", "jakarta.persistence..")
            .because("CalculoDisponibilidad se prueba sin Spring (B5)");

    private static ArchCondition<JavaMethod> noDevolverEntidades() {
        return new ArchCondition<>("no devolver entidades") {
            @Override
            public void check(JavaMethod metodo, ConditionEvents eventos) {
                for (JavaClass tipo : metodo.getReturnType().getAllInvolvedRawTypes()) {
                    if (tipo.isAnnotatedWith(Entity.class)) {
                        eventos.add(SimpleConditionEvent.violated(metodo,
                                metodo.getFullName() + " devuelve la entidad " + tipo.getSimpleName()));
                    }
                }
            }
        };
    }
}
