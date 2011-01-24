package lzma.streams;

import java.io.IOException;
import java.io.InputStream;

import lzma.sdk.lzma.Decoder;

import org.cservenak.streams.CoderInputStream;

public class LzmaDecompressingInputStream
    extends CoderInputStream
{
    public LzmaDecompressingInputStream( final InputStream in, final Decoder lzmaDecoder )
        throws IOException
    {
        super( in, new LzmaDecoderWrapper( lzmaDecoder ) );
    }
}
