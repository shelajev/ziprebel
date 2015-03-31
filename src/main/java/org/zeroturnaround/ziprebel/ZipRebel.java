package org.zeroturnaround.ziprebel;

import java.io.IOException;
import java.nio.file.*;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.lang.Integer.bitCount;
import static java.nio.file.Files.write;

/*
 * So how does ZipRebel work? Let’s take a look at this single byte example.
 *
 * 0101 1001
 *
 * Here we have 4 bits of data and 4 spacers. Yeh, 4 bits of nothing,
 * packing half of our byte with useless nothingness. Well what happens
 * if we remove the empty information:
 *
 * 1111
 *
 * Check that out! We just halved our storage size from a byte to a nibble! CHOMP!
 * Also we’re left with nothing but pure data. No waste, no empty bits.
 * But get this, there’s more - our 4 bits of pure data is currently being
 * viewed as a raw string of bits. We now know every bit is data, so why not
 * count the number of bits and represent it as a binary number? We have 4 bits
 * of data so we can say it’s equivalent to 100. 100 is binary for 4, so it
 * equates to 1111 in pure data form.
 *
 *
 * @author shelajev, @date 3/30/15 12:10 PM
 */
public class ZipRebel {

  public static final String ZIPREBEL_FILE_EXTENSION = ".zr";

  /**
   * Compresses the file on the given path using the revolutionary ZipRebel algorithm. Writes the results to a file near the original.
   *
   * @param path
   *   - path of the file to compress
   *
   * @return - path of the compressed file, same parent dir, same file name, '@{value #ZIPREBEL_FILE_EXTENSION}' extension
   */
  public Path compressAndDump(Path path) {
    try {
      Path target = Paths.get(path.getParent().toString(), path.getFileName() + ZIPREBEL_FILE_EXTENSION);
      long result = compress(path);
      write(target, Long.toHexString(result).getBytes());
      return target;
    }
    catch (IOException e) {
      throw new RuntimeException("Cannot compress file: " + path, e);
    }
  }

  /**
   * This method takes byte array and recursively compresses it using the revolutionary ZipRebel algorithm
   *
   * @param path
   *   -  path of the file to compress
   *
   * @return the result of the compression
   */
  public long compress(Path path) {
    try {
      byte[] data = Files.readAllBytes(path);
      return compress(data);
    }
    catch (IOException e) {
      throw new RuntimeException("Cannot compress file: " + path, e);
    }
  }

  /**
   * This method takes byte array and recursively compresses it using the revolutionary ZipRebel algorithm
   *
   * @param bytes
   *   - data to compress
   *
   * @return the result of the compression.
   */
  public long compress(byte[] bytes) {
    long result, nextResult = 0;
    do {
      result = nextResult;
      nextResult = compress0(bytes);
      bytes = long2bytes(nextResult);
    }
    while (result != nextResult);
    return result;
  }

  /**
   * Internal method that we recursively call to compress bytes
   */
  private long compress0(byte[] bytes) {
    return StreamSupport.stream(Spliterators.spliterator(new Bytes(bytes),
        bytes.length,
        Spliterator.ORDERED),
      false)
      .collect(Collectors.summingLong(b -> bitCount(b)));
  }

  /**
   * convert a long to byte[] representation. Used mainly for the compression, when we need to iterate further.
   */
  private byte[] long2bytes(long l) {
    return new byte[] {
      (byte) ((l >> 56) & 0xFF),
      (byte) ((l >> 48) & 0xFF),
      (byte) ((l >> 40) & 0xFF),
      (byte) ((l >> 32) & 0xFF),

      (byte) ((l >> 24) & 0xFF),
      (byte) ((l >> 16) & 0xFF),
      (byte) ((l >> 8) & 0xFF),
      (byte) ((l >> 0) & 0xFF)
    };
  }

  /**
   * Represent byte[] as iterator of ints. Be generous with synchronization.
   */
  public static class Bytes implements PrimitiveIterator.OfInt {

    private final byte[] bytes;
    private volatile int idx;

    public Bytes(byte[] bytes) {
      this.bytes = bytes;
    }

    @Override public synchronized int nextInt() {
      return bytes[idx++];
    }

    @Override public synchronized boolean hasNext() {
      return idx < bytes.length;
    }
  }
}
