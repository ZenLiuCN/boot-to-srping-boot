package cn.zenliu.java.boot.advise.util;

import cn.zenliu.java.boot.advise.properties.ResponseAdviserProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * @author Zen.Liu
 * @apiNote
 * @since 2020-11-17
 */
public interface Util {
    static <T> T ifNull(T raw, T replace) {
        return raw == null ? replace : raw;
    }
    static Predicate<String> predicateBuilder(ResponseAdviserProperty property) {
        List<Predicate<String>> collect = new ArrayList<>();
        for (Pattern i : property.getIgnore()) {
            Predicate<String> stringPredicate = i.asPredicate();
            collect.add(stringPredicate);
        }
        return x -> {
            for (Predicate<String> i : collect) {
                if (i.test(x)) {
                    return true;
                }
            }
            return false;
        };
    }
}
