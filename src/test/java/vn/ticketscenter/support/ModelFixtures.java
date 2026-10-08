package vn.ticketscenter.support;

/** Identity-only relationship fixtures; never evidence of another owner's behavior. */
public final class ModelFixtures {
    private ModelFixtures() {}

    public static <T> T emptyJpaEntity(Class<T> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(
                    "JPA entity must provide its no-arg constructor: " + type.getName(), error);
        }
    }
}
