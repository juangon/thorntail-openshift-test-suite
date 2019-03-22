package io.thorntail.openshift.ts.sql.db.arquillian;

public interface SqlDatabaseAndConfigMapStrategy {

    default void setup() throws Exception {}

    default void deploy() throws Exception {}

    default void undeploy() throws Exception{}

    SqlDatabaseAndConfigMapStrategy NOOP = new SqlDatabaseAndConfigMapStrategy() {
    };
}
