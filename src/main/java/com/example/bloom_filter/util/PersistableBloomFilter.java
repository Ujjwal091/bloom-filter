package com.example.bloom_filter.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class PersistableBloomFilter<T> extends BloomFilter<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public PersistableBloomFilter(int size, int numHashFunctions) {
        super(size, numHashFunctions);
    }

    public void saveToFile(Path path) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(path))) {
            out.writeObject(this);
        }
    }

    public static <T> PersistableBloomFilter<T> loadFromFile(Path path) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(path))) {
            return (PersistableBloomFilter<T>) in.readObject();
        }
    }
}
