package lzma.streams;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import junit.framework.TestCase;
import lzma.sdk.lzma.Decoder;

public class DecodingTest extends TestCase {

	public void testDecodeFileWithoutEndOfStreamMarker() throws IOException {
		final InputStream compressedFileStream = FileUtils
				.openInputStream(new File("target/test-classes/hello.txt.lzma"));

		@SuppressWarnings("unchecked")
		final List<String> lines = IOUtils.readLines(new LzmaInputStream(
				compressedFileStream, new Decoder()));

		assertEquals(1, lines.size());
		assertEquals("Hello World !", lines.get(0));
	}
}
