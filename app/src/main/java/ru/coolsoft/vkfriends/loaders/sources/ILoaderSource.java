package ru.coolsoft.vkfriends.loaders.sources;

/**
 * Interface for all loader source types
 */
public interface ILoaderSource {
    String value(int... index);
}