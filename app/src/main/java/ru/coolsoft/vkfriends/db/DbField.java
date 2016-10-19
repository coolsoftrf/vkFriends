package ru.coolsoft.vkfriends.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Bobby© on 14.04.2015.
 * Annotation class that defines Database Field parameters of the annotated java class field
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface DbField {
    boolean primary() default false;
    //TODO: boolean primary_autoincrement() default false;
    String type() default "INTEGER";
    boolean notnull() default false;
    String default_value() default "";
    //String upgrade_rename() default ""; //format: oldVer=oldName
    //String upgrade_add() default "";
}
