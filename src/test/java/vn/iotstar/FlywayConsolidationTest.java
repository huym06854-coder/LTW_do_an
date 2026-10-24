package vn.iotstar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlywayConsolidationTest {

    @TempDir
    Path migrations;

    @Test
    void newH2DatabaseRunsSchemaAndOperationsHistory() {
        String url = "jdbc:h2:mem:flyway-new-history;DB_CLOSE_DELAY=-1";
        Flyway flyway = Flyway.configure().dataSource(url, "sa", "")
                .table("flyway_schema_history_v2")
                .baselineOnMigrate(true).baselineVersion("1")
                .locations("classpath:db/migration/h2").load();
        assertEquals(2, flyway.migrate().migrationsExecuted);
        assertEquals("2", flyway.info().current().getVersion().toString());
    }

    @Test
    void existingVersionSevenAppliesLateAuditMigrationOnceWithoutLosingData() throws Exception {
        String url = "jdbc:h2:mem:flyway-late-audit;DB_CLOSE_DELAY=-1";
        Files.writeString(migrations.resolve("V1__schema.sql"),
                "CREATE TABLE sample (id INT PRIMARY KEY, name VARCHAR(50)); "
                + "INSERT INTO sample VALUES (1, 'preserved');");
        Files.writeString(migrations.resolve("V7__nationalize_product_text.sql"),
                "ALTER TABLE sample ADD note VARCHAR(50);");
        Flyway.configure().dataSource(url, "sa", "")
                .table("flyway_schema_history_v2")
                .locations("filesystem:" + migrations).load().migrate();
        // Production's V7 was applied previously but is no longer in the source tree.
        Files.delete(migrations.resolve("V7__nationalize_product_text.sql"));
        Files.writeString(migrations.resolve("V3__operations_audit_history.sql"),
                "CREATE TABLE audit_history (id INT PRIMARY KEY);");
        var properties = new java.util.Properties();
        try (var input = Files.newInputStream(Path.of("src/main/resources/application.properties"))) {
            properties.load(input);
        }
        Flyway upgraded = Flyway.configure().dataSource(url, "sa", "")
                .table("flyway_schema_history_v2")
                .outOfOrder(Boolean.parseBoolean(properties.getProperty("spring.flyway.out-of-order")))
                .locations("filesystem:" + migrations).load();
        assertEquals(1, upgraded.migrate().migrationsExecuted);
        assertEquals(0, upgraded.migrate().migrationsExecuted);
        assertEquals("7", upgraded.info().current().getVersion().toString());
        try (var connection = DriverManager.getConnection(url, "sa", "");
                var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT name FROM sample WHERE id = 1")) {
            result.next();
            assertEquals("preserved", result.getString(1));
        }
    }

    @Test
    void existingDatabaseKeepsItsOldHistoryAndRunsOnlyUpgrade() throws Exception {
        String url = "jdbc:h2:mem:flyway-rebased-history;DB_CLOSE_DELAY=-1";
        Path old = migrations.resolve("V6__old_schema.sql");
        Files.writeString(old, "CREATE TABLE sample (id INT PRIMARY KEY, name VARCHAR(50)); "
                + "INSERT INTO sample (id, name) VALUES (1, 'old');");
        Flyway legacy = Flyway.configure().dataSource(url, "sa", "")
                .locations("filesystem:" + migrations).load();
        assertEquals(1, legacy.migrate().migrationsExecuted);

        Files.delete(old);
        Files.writeString(migrations.resolve("V1__new_schema.sql"),
                "CREATE TABLE sample (id INT PRIMARY KEY, name VARCHAR(50), address VARCHAR(50));");
        Files.writeString(migrations.resolve("V2__upgrade.sql"),
                "ALTER TABLE sample ADD address VARCHAR(50); "
                + "UPDATE sample SET address = 'new' WHERE id = 1;");

        Flyway rebased = Flyway.configure().dataSource(url, "sa", "")
                .table("flyway_schema_history_v2")
                .baselineOnMigrate(true).baselineVersion("1")
                .locations("filesystem:" + migrations).load();
        assertEquals(1, rebased.migrate().migrationsExecuted);
        assertEquals("2", rebased.info().current().getVersion().toString());
        try (var connection = DriverManager.getConnection(url, "sa", "");
                var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT name, address FROM sample WHERE id = 1")) {
            result.next();
            assertEquals("old", result.getString(1));
            assertEquals("new", result.getString(2));
        }
        try (var connection = DriverManager.getConnection(url, "sa", "");
                var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT COUNT(*) FROM \"flyway_schema_history\" "
                        + "WHERE \"version\" = '6' AND \"success\" = TRUE")) {
            result.next();
            assertEquals(1, result.getInt(1));
        }
    }
}
