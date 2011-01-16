/*
 *  Copyright (c) 2009 Julien Ponge. All rights reserved.
 *
 *  <julien.ponge@gmail.com>
 *  http://julien.ponge.info/
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  This work is based on the LZMA SDK by Igor Pavlov.
 *  The LZMA SDK is placed under the public domain, and can be obtained from
 *
 *      http://www.7-zip.org/sdk.html
 *
 *  The LzmaInputStream and LzmaOutputStream classes were inspired by the
 *  work of Christopher League, although they are not derivative works.
 *
 *      http://contrapunctus.net/league/haques/lzmajio/
 */

package lzma.streams;

import lzma.sdk.lzma.Decoder;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An input stream filter that deflates LZMA-compressed data.
 *
 * @author Julien Ponge
 */
public class LzmaInputStream extends FilterInputStream
{
    private final Decoder decoder;

    private final boolean decoderConfigured;

    private IOException exception;

    private final LinkedBlockingQueue<byte[]> queue = new LinkedBlockingQueue<byte[]>(8);

    private byte[] currentBuffer = null;

    private int currentPosition = 0;

    private final AtomicBoolean finishAtom = new AtomicBoolean(false);

    private final Thread decoderThread = new Thread(new Runnable()
    {
        private final byte[] internalBuffer = new byte[1024];

        private int internalPosition = 0;

        private final OutputStream fakeOutputStream = new OutputStream()
        {
            @Override
            public void write(int b) throws IOException
            {
                internalBuffer[internalPosition] = (byte) b;
                internalPosition = internalPosition + 1;

                if (internalPosition == internalBuffer.length)
                {
                    try
                    {
                        final byte[] buffer = new byte[internalBuffer.length];
                        System.arraycopy(internalBuffer, 0, buffer, 0, internalBuffer.length);
                        queue.put(buffer);
                        internalPosition = 0;
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        };

        private void sendRemainingBytes() throws InterruptedException
        {
            if (internalPosition != internalBuffer.length)
            {
                final byte[] buffer = new byte[internalPosition];
                System.arraycopy(internalBuffer, 0, buffer, 0, internalPosition);
                queue.put(buffer);
            }
        }

        public void run()
        {
            try
            {
                if (!decoderConfigured)
                {
                    byte[] properties = new byte[5];
                    in.read(properties);
                    decoder.setDecoderProperties(properties);
                    
                    // skip 8 bytes for file size
                    in.skip( 8 );
                }

                decoder.code(in, fakeOutputStream, -1);

                sendRemainingBytes();
                finishAtom.set(true);
            }
            catch (IOException e)
            {
                exception = e;
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

    });

    public LzmaInputStream(InputStream in, Decoder decoder)
    {
        this(in, decoder, false);
    }

    public LzmaInputStream(InputStream in, Decoder decoder, boolean isDecoderConfigured)
    {
        super(in);

        this.decoder = decoder;
        this.decoderConfigured = isDecoderConfigured;
        decoderThread.start();
    }

    private void provision()
    {
        if (isNewBufferNeeded())
        {
            takeFreshBuffer();
        }
    }

    private void takeFreshBuffer()
    {
        currentBuffer = null;
        while (currentBuffer == null)
        {
            currentBuffer = queue.poll();
            if (currentBuffer == null && finishAtom.get())
            {
                return;
            }
        }
        currentPosition = 0;
    }

    private boolean isNewBufferNeeded()
    {
        return currentBuffer == null || currentPosition == currentBuffer.length;
    }

    private void checkForException() throws IOException
    {
        if (exception != null)
        {
            throw exception;
        }
    }

    @Override
    public int read() throws IOException
    {
        checkForException();
        provision();
        if (currentBuffer == null)
        {
            return -1;
        }

        final int val = (currentBuffer[currentPosition] & 0xFF);
        currentPosition = currentPosition + 1;
        return val;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        checkForException();
        provision();
        if (currentBuffer == null)
        {
            return -1;
        }

        final int wanted = len - off;
        final int available = currentBuffer.length - currentPosition;
        final int transfered = (wanted <= available) ? wanted : (available - currentPosition);

        System.arraycopy(currentBuffer, currentPosition, b, off, transfered);
        currentPosition = currentPosition + transfered;

        return transfered;
    }

    @Override
    public int read(byte[] b) throws IOException
    {
        checkForException();
        return read(b, 0, b.length);
    }

}
