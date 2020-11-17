package cn.zenliu.java.boot.advise.annotations;

import java.lang.annotation.*;

/**
 * @author Zen.Liu
 * @apiNote
 * @since 2020-11-17
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface IgnoreBodyAdvice {
}
