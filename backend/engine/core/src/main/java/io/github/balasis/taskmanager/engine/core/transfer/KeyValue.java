package io.github.balasis.taskmanager.engine.core.transfer;

// generic pair used for returning simple key-value results from service methods
public record KeyValue<K, V>(K key, V value) {
}
