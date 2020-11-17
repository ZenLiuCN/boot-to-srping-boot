package cn.zenliu.java.boot.advise.properties;

import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Zen.Liu
 * @apiNote
 * @since 2020-11-17
 */
@ConfigurationProperties(prefix = ResponseAdviserProperty.Prefix)
@ConstructorBinding
@Getter
@ToString
public class ResponseAdviserProperty implements Serializable {
    static final String Prefix = "boot.advise";
    private static final long serialVersionUID = 4518604574735888395L;
    /**
     * enable response advise
     */
    private final boolean enable;
    /**
     * debug response adviser
     */
    private final boolean debug;
    /**
     * Regex for decide which methods should be ignored.<br>
     * Full test string may like: cn.zenliu.java.boot.advise.properties.ResponseAdviser#method <br>
     */
    private final List<Pattern> ignore;

    public ResponseAdviserProperty(@Nullable Boolean enable, @Nullable Boolean debug, List<Pattern> ignore) {
        this.enable = enable != null ? enable : true;
        this.debug = debug != null && debug;
        this.ignore = new ArrayList<>(ignore);
        ignore.add(Pattern.compile("swagger\\."));
        ignore.add(Pattern.compile("swagger\\."));
    }
}
