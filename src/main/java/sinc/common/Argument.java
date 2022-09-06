package sinc.common;

import java.util.Objects;

public abstract class Argument {
    public final int id;
    public final String name;
    public final boolean isVar;

    public Argument(int id, String name, boolean isVar) {
        this.id = id;
        this.name = name;
        this.isVar = isVar;
    }

    public Argument(Argument another) {
        this.id = another.id;
        this.name = another.name;
        this.isVar = another.isVar;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Argument argument = (Argument) o;
        return id == argument.id && isVar == argument.isVar && Objects.equals(name, argument.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, isVar);
    }
}
