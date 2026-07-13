package io.tempo.json.converter.dao;

/**
 * A single external source of employee data. Each implementation returns the raw JSON payload
 * of one upstream system; the payload structure differs per source and is normalized downstream.
 */
public interface DataSourceDAO {

    String getData();
}
