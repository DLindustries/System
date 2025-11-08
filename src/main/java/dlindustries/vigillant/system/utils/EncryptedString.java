package dlindustries.vigillant.system.utils;

import java.nio.CharBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public class EncryptedString implements AutoCloseable, CharSequence {
	public static final String PART = "giggatronon";
	private final char[] key;
	private final char[] value;
	private final int length;
	private static final SecureRandom random = new SecureRandom();
	private boolean closed = false;

	public EncryptedString(String string) {
		if (string == null) {
			throw new IllegalArgumentException("Input string cannot be null");
		} else {
			this.length = string.length();
			this.key = generateRandomKey(Math.min(this.length, 128));
			this.value = new char[this.length];
			string.getChars(0, this.length, this.value, 0);
			applyXorEncryption(this.value, this.key, 0, this.length);
		}
	}

	public EncryptedString(char[] original, char[] original2) {
		if (original == null || original2 == null) {
			throw new IllegalArgumentException("Neither encrypted value nor key can be null");
		} else if (original2.length == 0) {
			throw new IllegalArgumentException("Encryption key cannot be empty");
		} else {
			this.length = original.length;
			this.value = Arrays.copyOf(original, original.length);
			this.key = Arrays.copyOf(original2, original2.length);
		}
	}

	public static EncryptedString of(String s) {
		return new EncryptedString(s);
	}

	public static EncryptedString of(String s, String s2) {
		if (s != null && s2 != null) {
			return new EncryptedString(s.toCharArray(), s2.toCharArray());
		} else {
			throw new IllegalArgumentException("Neither encrypted data nor key can be null");
		}
	}

	private static char[] generateRandomKey(int n) {
		char[] array = new char[n];

		for (int i = 0; i < n; i++) {
			array[i] = (char)random.nextInt(65536);
		}

		return array;
	}

	private static void applyXorEncryption(char[] array, char[] array2, int n, int n2) {
		for (int i = 0; i < n2; i++) {
			int n3 = n + i;
			array[n3] ^= array2[i % array2.length];
		}
	}

	@Override
	public int length() {
		this.setClosed();
		return this.length;
	}

	@Override
	public char charAt(int n) {
		this.setClosed();
		if (n >= 0 && n < this.length) {
			return (char)(this.value[n] ^ this.key[n % this.key.length]);
		} else {
			throw new IndexOutOfBoundsException("Index: " + n + ", Length: " + this.length);
		}
	}

	@NotNull
	@Override
	public CharSequence subSequence(int n, int n2) {
		this.setClosed();
		if (n >= 0 && n2 <= this.length && n <= n2) {
			int n3 = n2 - n;
			char[] array = new char[n3];
			System.arraycopy(this.value, n, array, 0, n3);
			char[] array2 = new char[n3];

			for (int i = 0; i < n3; i++) {
				array2[i] = this.key[(n + i) % this.key.length];
			}

			applyXorEncryption(array, this.key, 0, n3);
			applyXorEncryption(array, array2, 0, n3);
			return new EncryptedString(array, array2);
		} else {
			throw new IndexOutOfBoundsException("Invalid subsequence range: " + n + " to " + n2 + " (length: " + this.length);
		}
	}

	@NotNull
	@Override
	public String toString() {
		this.setClosed();
		char[] array = new char[this.length];

		for (int i = 0; i < this.length; i++) {
			array[i] = this.charAt(i);
		}

		String s = new String(array);
		Arrays.fill(array, '\u0000');
		return s;
	}

	@NotNull
	public String keyCodec() {
		this.setClosed();
		return this.toString();
	}

	@NotNull
	public Text toText() {
		this.setClosed();
		return Text.literal(this.toString());
	}

	public CharBuffer elementCodec() {
		this.setClosed();
		CharBuffer allocate = CharBuffer.allocate(this.length);

		for (int i = 0; i < this.length; i++) {
			allocate.put(i, this.charAt(i));
		}

		allocate.flip();
		return allocate.asReadOnlyBuffer();
	}

	@Override
	public void close() {
		if (!this.closed) {
			Arrays.fill(this.value, '\u0000');
			Arrays.fill(this.key, '\u0000');
			this.closed = true;
		}
	}

	private void setClosed() {
		if (this.closed) {
			throw new IllegalStateException("This EncryptedString has been closed and cannot be used");
		}
	}

	@Override
	public boolean equals(Object o) {
		this.setClosed();
		if (this == o) {
			return true;
		} else if (!(o instanceof CharSequence)) {
			return false;
		} else if (this.length != ((CharSequence)o).length()) {
			return false;
		} else {
			for (int i = 0; i < this.length; i++) {
				if (this.charAt(i) != ((CharSequence)o).charAt(i)) {
					return false;
				}
			}

			return true;
		}
	}

	@Override
	public int hashCode() {
		this.setClosed();
		int n = 0;

		for (int i = 0; i < this.length; i++) {
			n = 31 * n + this.charAt(i);
		}

		return n;
	}
}
