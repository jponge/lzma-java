package lzma.streams;

import static lzma.sdk.lzma.Encoder.EMatchFinderTypeBT2;
import static lzma.sdk.lzma.Encoder.EMatchFinderTypeBT4;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lzma.sdk.lzma.Encoder;

import org.cservenak.streams.Coder;

public class LzmaEncoderWrapper
    implements Coder
{
    private final Encoder encoder;

    public LzmaEncoderWrapper( final Encoder encoder )
    {
        this.encoder = encoder;
    }

    @Override
    public void code( final InputStream in, final OutputStream out )
        throws IOException
    {
        encoder.writeCoderProperties( out );

        // write -1 as "unknown" for file size
        long fileSize = -1;
        for ( int i = 0; i < 8; i++ )
        {
            out.write( (int) ( fileSize >>> ( 8 * i ) ) & 0xFF );
        }

        encoder.code( in, out, -1, -1, null );
    }

    /**
     * A convenient builder that makes it easier to configure the LZMA encoder. Default values:
     * <ul>
     * <li>dictionnary size: 23 (almost max, so is memory hungry)</li>
     * <li>end marker mode: true</li>
     * <li>match finder: BT4</li>
     * <li>number of fast bytes: 0x20</li>
     * </ul>
     */
    public static class Builder
    {
        private int dictionnarySize = 1 << 23;

        private boolean endMarkerMode = true;

        private int matchFinder = EMatchFinderTypeBT4;

        private int numFastBytes = 0x20;

        public Builder useMaximalDictionarySize()
        {
            dictionnarySize = 1 << 28;
            return this;
        }

        public Builder useMediumDictionarySize()
        {
            dictionnarySize = 1 << 15;
            return this;
        }

        public Builder useMinimalDictionarySize()
        {
            dictionnarySize = 1;
            return this;
        }

        public Builder useEndMarkerMode( boolean endMarkerMode )
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

        public LzmaEncoderWrapper build()
        {
            Encoder encoder = new Encoder();

            encoder.setDictionarySize( dictionnarySize );
            encoder.setEndMarkerMode( endMarkerMode );
            encoder.setMatchFinder( matchFinder );
            encoder.setNumFastBytes( numFastBytes );

            return new LzmaEncoderWrapper( encoder );
        }
    }
}
