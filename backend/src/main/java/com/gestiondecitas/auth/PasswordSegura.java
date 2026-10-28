package com.gestiondecitas.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/** Valida un campo de contraseña con la {@link PoliticaPassword} (→ 400 con el motivo). */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordSegura.Validador.class)
public @interface PasswordSegura {

    String message() default "La contraseña no cumple la política.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<PasswordSegura, String> {

        @Override
        public boolean isValid(String password, ConstraintValidatorContext contexto) {
            return PoliticaPassword.incumplimiento(password)
                    .map(motivo -> {
                        contexto.disableDefaultConstraintViolation();
                        contexto.buildConstraintViolationWithTemplate(motivo).addConstraintViolation();
                        return false;
                    })
                    .orElse(true);
        }
    }
}
