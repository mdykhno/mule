/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static java.lang.Math.min;
import static java.lang.Math.toIntExact;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.DataUnit.BYTE;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.streaming.exception.StreamingBufferSizeExceededException;
import org.mule.runtime.api.util.DataSize;
import org.mule.runtime.core.streaming.bytes.InMemoryCursorStreamConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * An implementation of {@link AbstractInputStreamBuffer} which holds the buffered
 * information in memory.
 * <p>
 * If the buffer does not have enough capacity to hold all the data, then it will
 * expanded up to a certain threshold configured in the constructor. Once that threshold
 * is reached, a {@link StreamingBufferSizeExceededException} will be thrown. If no threshold
 * is provided, then the buffer will be allowed to grow indefinitely.
 *
 * @since 4.0
 */
public class InMemoryStreamBuffer extends AbstractInputStreamBuffer {

  private final DataSize bufferSizeIncrement;
  private final DataSize maxBufferSize;
  private long bufferTip = 0;

  /**
   * Creates a new instance
   *
   * @param stream the stream to be buffered
   * @param config this buffer's configuration.
   */
  public InMemoryStreamBuffer(InputStream stream, InMemoryCursorStreamConfig config, ByteBufferManager bufferManager) {
    super(stream, bufferManager, config.getInitialBufferSize().toBytes());

    this.bufferSizeIncrement = config.getBufferSizeIncrement() != null
        ? config.getBufferSizeIncrement()
        : new DataSize(0, BYTE);

    this.maxBufferSize = config.getMaxBufferSize();
  }

  @Override
  protected int doGet(ByteBuffer destination, long position, int length) {
    return doGet(destination, position, length, true);
  }

  private int doGet(ByteBuffer dest, long position, int length, boolean consumeStreamIfNecessary) {
    return withReadLock(() -> {

      Optional<Integer> presentRead = getFromCurrentData(dest, position, length);
      if (presentRead.isPresent()) {
        return presentRead.get();
      }

      if (consumeStreamIfNecessary) {
        releaseReadLock();
        return withWriteLock(() -> {
          final long requiredUpperBound = position + length;

          Optional<Integer> refetch;
          refetch = getFromCurrentData(dest, position, length);
          if (refetch.isPresent()) {
            return refetch.get();
          }

          while (!isStreamFullyConsumed() && bufferTip < requiredUpperBound) {
            try {
              final int read = consumeForwardData();
              if (read > 0) {
                refetch = getFromCurrentData(dest, position, length);
                if (refetch.isPresent()) {
                  return refetch.get();
                }
              } else {
                streamFullyConsumed();
                buffer.limit(buffer.position());
              }
            } catch (IOException e) {
              throw new MuleRuntimeException(createStaticMessage("Could not read stream"), e);
            }
          }

          return doGet(dest, position, length, false);
        });
      } else {
        return getFromCurrentData(dest, position, length).orElse(-1);
      }
    });
  }

  private Optional<Integer> getFromCurrentData(ByteBuffer dest, long position, int length) {
    if (isStreamFullyConsumed() && position > bufferTip) {
      return of(-1);
    }

    if (position < bufferTip) {
      length = min(length, toIntExact(bufferTip - position));
      return of(copy(dest, position, length));
    }

    return empty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void doClose() {
    // no - op
  }

  /**
   * {@inheritDoc}
   * If {@code buffer} doesn't have any remaining capacity, then {@link #expandBuffer(int)}
   * is invoked before attempting to consume new information.
   *
   * @throws StreamingBufferSizeExceededException if the buffer is not big enough and cannot be expanded
   */
  @Override
  public int consumeForwardData() throws IOException {
    if (!buffer.hasRemaining()) {
      buffer = expandBuffer(buffer.capacity());
    }

    int read = consumeStream();

    if (read > 0) {
      bufferTip += read;
    }

    return read;
  }

  /**
   * Expands the size of the buffer by {@code bytesIncrement}
   *
   * @param bytesIncrement how many bytes to gain
   * @return a new, expanded {@link ByteBuffer}
   */
  private ByteBuffer expandBuffer(int bytesIncrement) {
    int newSize = buffer.capacity() + bytesIncrement;
    if (!canBeExpandedTo(newSize)) {
      throw new StreamingBufferSizeExceededException(maxBufferSize.toBytes());
    }

    ByteBuffer newBuffer = bufferManager.allocate(newSize);
    buffer.position(0);
    newBuffer.put(buffer);
    ByteBuffer oldBuffer = buffer;
    buffer = newBuffer;
    deallocate(oldBuffer);

    return buffer;
  }

  private boolean canBeExpandedTo(int newSize) {
    if (bufferSizeIncrement.getSize() <= 0) {
      return false;
    } else if (maxBufferSize == null) {
      return true;
    }

    return newSize <= maxBufferSize.toBytes();
  }
}
