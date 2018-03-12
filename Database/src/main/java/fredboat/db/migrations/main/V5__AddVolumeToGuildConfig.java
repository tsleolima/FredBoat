package fredboat.db.migrations.main;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.Statement;

public class V5__AddVolumeToGuildConfig implements JdbcMigration {

    private static final String ALTER
            = "ALTER TABLE public.guild_config "
            + "ADD COLUMN volume int NOT NULL DEFAULT 100;";

    @Override
    public void migrate(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute(ALTER);
        }
    }
}
