package vn.ticketscenter.transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import vn.ticketscenter.dto.common.ActorContext;

/** Session context is mutable so pooled connections can be safely reused. */
public final class ActorSessionContext {
    private ActorSessionContext() {}

    public static void set(Connection connection, ActorContext actor) throws SQLException {
        write(
                connection,
                actor.actorId() == null ? null : actor.actorId().toString(),
                actor.type().name());
    }

    public static void clear(Connection connection) throws SQLException {
        write(connection, null, null);
    }

    private static void write(Connection connection, String actorId, String actorType)
            throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement(
                        "EXEC sys.sp_set_session_context @key=N'actorId', @value=?; "
                                + "EXEC sys.sp_set_session_context @key=N'actorType', @value=?;")) {
            statement.setString(1, actorId);
            statement.setString(2, actorType);
            statement.setQueryTimeout(2);
            statement.execute();
        }
    }
}
