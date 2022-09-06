package sinc2.common;

import java.util.Objects;

/**
 * This class denotes the arguments parsed from plain-text strings.
 *
 * @since 2.0
 */
public class ParsedArg {
    /** If the argument is a constant, the name denotes the constant symbol. Otherwise, it is NULL */
    public final String name;
    /** If the argument is a variable, the id denotes the variable id. */
    public final int id;

    /**
     * Create an instance for a constant symbol.
     */
    public static ParsedArg constant(String constSymbol) {
        return new ParsedArg(constSymbol, 0);
    }

    /**
     * Create an instance for a variable.
     */
    public static ParsedArg variable(int id) {
        return new ParsedArg(null, id);
    }

    private ParsedArg(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public boolean isVariable() {
        return null == name;
    }

    public boolean isConstant() {
        return null != name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParsedArg parsedArg = (ParsedArg) o;
        return id == parsedArg.id && Objects.equals(name, parsedArg.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id);
    }
}
