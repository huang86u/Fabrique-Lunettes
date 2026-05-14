package bernard_flou;

import java.util.UUID;

public class Fabricateur {
    private TypeLunette[] configuration;

    public enum TypeLunette {
        BANANA,
        CHATGPT,
        LE_CHAT,
        CLAUDE
    }

    public static final class Lunette {
        public final String serial;
        public final TypeLunette type;

        public Lunette(TypeLunette type, String serial) {
            this.type = type;
            this.serial = serial;
        }
    }

    public Fabricateur() {
    }

    public int getCapacity() {
        return 5;
    }

    public void configurer(TypeLunette[] configuration) {
        this.configuration = configuration;
    }

    public Lunette fabriquer(TypeLunette type) throws Exception {
        if (type == null) {
            throw new IllegalArgumentException("Type de lunette requis");
        }
        String serial = type.name() + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return new Lunette(type, serial);
    }
}
