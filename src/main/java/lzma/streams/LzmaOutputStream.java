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

import lzma.sdk.lzma.Encoder;
import static lzma.sdk.lzma.Encoder.EMatchFinderTypeBT2;
import static lzma.sdk.lzma.Encoder.EMatchFinderTypeBT4;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An output stream filter that uses LZMA compression.
 *
 * @author Julien Ponge
 */
public class LzmaOutputStream extends FilterOutputStream
{
    private final Encoder encoder;

    private final boolean encoderConfigured;

    private IOException exception;

    private final LinkedBlockingQueue<byte[]> queue = new LinkedBlockingQueue<byte[]>(4);

    private byte[] currentBuffer = null;

    private int currentPosition;

    private final AtomicBoolean finishAtom = new AtomicBoolean(false);

    private final Thread encoderThread = new Thread(new Runnable()
    {
        private final InputStream fakeInputStream = new InputStream()
        {
            public int read() throws IOException
            {
                while (isNewBufferNeeded())
                {
                    if (isEOF())
                    {
                        return -1;
                    }
                    currentBuffer = queue.poll();
                    if (currentBuffer != null)
                    {
                        currentPosition = 0;
                    }
                }

                final int val = (int) currentBuffer[currentPosition] & 0xFF;
                currentPosition = currentPosition + 1;
                return val;

            }
        };

        public void run()
        {
            try
            {
                if (!encoderConfigured)
                {
                    encoder.setDictionarySize(28);
                    encoder.setEndMarkerMode(true);
                    encoder.writeCoderProperties(out);
                }
                encoder.code(fakeInputStream, out, -1, -1, null);
            }
            catch (IOException e)
            {
                exception = e;
            }
        }
    });

    private boolean isEOF()
    {
        return currentBuffer != null && currentPosition == currentBuffer.length && queue.isEmpty() && finishAtom.get();
    }

    private boolean isNewBufferNeeded()
    {
        return currentBuffer == null || currentPosition == currentBuffer.length;
    }

    public LzmaOutputStream(OutputStream out, Encoder encoder)
    {
        this(out, encoder, false);
    }

    public LzmaOutputStream(OutputStream out, Encoder encoder, boolean isEncoderConfigured)
    {
        super(out);

        this.encoder = encoder;
        this.encoderConfigured = isEncoderConfigured;
        encoderThread.start();
    }

    @Override
    public void close() throws IOException
    {
        checkForException();
        try
        {
            finishAtom.set(true);
            encoderThread.join();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        super.close();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        checkForException();

        final int bytesCount = len - off;
        final byte[] buffer = new byte[bytesCount];

        System.arraycopy(b, off, buffer, 0, bytesCount);
        try
        {
            queue.put(buffer);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private void checkForException() throws IOException
    {
        if (exception != null)
        {
            throw exception;
        }
    }

    @Override
    public void write(byte[] b) throws IOException
    {
        checkForException();
        write(b, 0, b.length);
    }

    @Override
    public void write(int b) throws IOException
    {
        checkForException();
        try
        {
            queue.put(new byte[]{(byte) b});
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * A convenient builder that makes it easier to configure the LZMA encoder.
     * Default values:
     * <ul>
     * <li>dictionnary size: max</li>
     * <li>end marker mode: true</li>
     * <li>match finder: BT4</li>
     * <li>number of fast bytes: 0x20</li>
     * </ul>
     */
    public static class Builder
    {
        private final OutputStream out;

        private int dictionnarySize = 28;

        private boolean endMarkerMode = true;

        private int matchFinder = EMatchFinderTypeBT4;

        private int numFastBytes = 0x20;

        public Builder(OutputStream out)
        {
            this.out = out;
        }

        public Builder useMaximalDictionarySize()
        {
            dictionnarySize = 28;
            return this;
        }

        public Builder useMediumDictionarySize()
        {
            dictionnarySize = 15;
            return this;
        }

        public Builder useMinimalDictionarySize()
        {
            dictionnarySize = 1;
            return this;
        }

        public Builder useEndMarkerMode(boolean endMarkerMode)
        {
            this.endMarkerMode = endMarkerMode;
            return this;
        }

        public Builder useBT4MatchFinder()
        {
            matchFinder = EMatchFinderTypeBT4;
            return this;
        }

        public Builder useBT2MatchFinder()
        {
            matchFinder = EMatchFinderTypeBT2;
            return this;
        }

        public Builder useMinimalFastBytes()
        {
            numFastBytes = 5;
            return this;
        }

        public Builder useMediumFastBytes()
        {
            numFastBytes = 0x20;
            return this;
        }

        public Builder useMaximalFastBytes()
        {
            numFastBytes = 273;
            return this;
        }

        public LzmaOutputStream build()
        {
            Encoder encoder = new Encoder();

            encoder.setDictionarySize(dictionnarySize);
            encoder.setEndMarkerMode(endMarkerMode);
            encoder.setMatchFinder(matchFinder);
            encoder.setNumFastBytes(numFastBytes);

            return new LzmaOutputStream(out, encoder, true);
        }
    }
}
