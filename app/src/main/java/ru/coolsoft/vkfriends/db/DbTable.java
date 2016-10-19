package ru.coolsoft.vkfriends.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Bobby© on 12.04.2015.
 * Annotation class that defines the value of annotated field as Database Table name
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface DbTable {
    //String upgrade_rename() default ""; //format: oldVer=oldName
    //String upgrade_create() default ""; //version number where introduced
}
