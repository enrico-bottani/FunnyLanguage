package tokenizer;

import org.junit.jupiter.api.Test;
import tokenizer.exceptions.CommentNotClosedException;
import tokenizer.exceptions.StringNotClosedException;
import tokenizer.exceptions.TokenizerException;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TokenizerSymbolsTest {
    @Test
    void checkCurlyBracketOpen() throws TokenizerException,StringNotClosedException, IOException, CommentNotClosedException {
        InputStream is = new ByteArrayInputStream("{".getBytes(Charset.defaultCharset()));
        Tokenizer tokenizer = new Tokenizer(new BufferedReader(new InputStreamReader(is)));
        Token next = tokenizer.nextToken();
        assertEquals(Token.TYPE.CURLY_BRACKET_OPEN, next.getType());
    }

    @Test
    void checkCurlyBracketClosed() throws TokenizerException,StringNotClosedException, IOException, CommentNotClosedException {
        InputStream is = new ByteArrayInputStream("}".getBytes(Charset.defaultCharset()));
        Tokenizer tokenizer = new Tokenizer(new BufferedReader(new InputStreamReader(is)));
        Token next = tokenizer.nextToken();
        assertEquals(Token.TYPE.CURLY_BRACKET_CLOSE, next.getType());
    }

    @Test
    void testTrue() throws TokenizerException,StringNotClosedException, IOException, CommentNotClosedException {
        InputStream is = new ByteArrayInputStream("true".getBytes(Charset.defaultCharset()));
        Tokenizer tokenizer = new Tokenizer(new BufferedReader(new InputStreamReader(is)));
        Token next = tokenizer.nextToken();
        assertEquals(Token.TYPE.TRUE, next.getType());
    }

    @Test
    void testFalse() throws TokenizerException,StringNotClosedException, IOException, CommentNotClosedException {
        InputStream is = new ByteArrayInputStream("false".getBytes(Charset.defaultCharset()));
        Tokenizer tokenizer = new Tokenizer(new BufferedReader(new InputStreamReader(is)));
        Token next = tokenizer.nextToken();
        assertEquals(Token.TYPE.FALSE, next.getType());
    }

    @Test
    void testLambda() throws TokenizerException,StringNotClosedException, IOException, CommentNotClosedException {
        InputStream is = new ByteArrayInputStream(" ->".getBytes(Charset.defaultCharset()));
        Tokenizer tokenizer = new Tokenizer(new BufferedReader(new InputStreamReader(is)));
        Token next = tokenizer.nextToken();
        assertEquals(Token.TYPE.LAMBDA, next.getType());
    }

    @Test
    void testMinusEqual() throws TokenizerException,StringNotClosedException, IOException, CommentNotClosedException {
        InputStream is = new ByteArrayInputStream(" -=".getBytes(Charset.defaultCharset()));
        Tokenizer tokenizer = new Tokenizer(new BufferedReader(new InputStreamReader(is)));
        Token next = tokenizer.nextToken();
        assertEquals(Token.TYPE.MINUS_EQUAL, next.getType());
    }

    @Test
    void testAsteriskEqual() throws TokenizerException,StringNotClosedException, IOException, CommentNotClosedException {
        InputStream is = new ByteArrayInputStream(" *=".getBytes(Charset.defaultCharset()));
        Tokenizer tokenizer = new Tokenizer(new BufferedReader(new InputStreamReader(is)));
        Token next = tokenizer.nextToken();
        assertEquals(Token.TYPE.ABSTERISC_EQUAL, next.getType());
    }

    @Test
    void divideEqual() throws TokenizerException,StringNotClosedException, IOException, CommentNotClosedException {
        InputStream is = new ByteArrayInputStream(" /=".getBytes(Charset.defaultCharset()));
        Tokenizer tokenizer = new Tokenizer(new BufferedReader(new InputStreamReader(is)));
        Token next = tokenizer.nextToken();
        assertEquals(Token.TYPE.DIVIDE_EQUAL, next.getType());
    }

    @Test
    void minorEqual() throws TokenizerException,StringNotClosedException, IOException, CommentNotClosedException {
        InputStream is = new ByteArrayInputStream(" <=".getBytes(Charset.defaultCharset()));
        Tokenizer tokenizer = new Tokenizer(new BufferedReader(new InputStreamReader(is)));
        Token next = tokenizer.nextToken();
        assertEquals(Token.TYPE.MINOR_EQUAL, next.getType());
    }

    @Test
    void testScientificNotation() throws TokenizerException,StringNotClosedException, IOException, CommentNotClosedException {
        InputStream is = new ByteArrayInputStream("1e-3".getBytes(Charset.defaultCharset()));
        Tokenizer tokenizer = new Tokenizer(new BufferedReader(new InputStreamReader(is)));
        Token next = tokenizer.nextToken();
        assertEquals(Token.TYPE.NUMBER, next.getType());
        assertEquals(new BigDecimal("1e-3"), next.getBigDecimalVal());
    }
    @Test
    void testNegativeNumber() throws TokenizerException,StringNotClosedException, IOException, CommentNotClosedException {
        InputStream is = new ByteArrayInputStream("-10".getBytes(Charset.defaultCharset()));
        Tokenizer tokenizer = new Tokenizer(new BufferedReader(new InputStreamReader(is)));
        Token next = tokenizer.nextToken();
        assertEquals(Token.TYPE.NUMBER, next.getType());
        assertEquals(new BigDecimal("-10"), next.getBigDecimalVal());
    }
    @Test
    void testIdentifier() throws TokenizerException,StringNotClosedException, IOException, CommentNotClosedException {
        InputStream is = new ByteArrayInputStream("print".getBytes(Charset.defaultCharset()));
        Tokenizer tokenizer = new Tokenizer(new BufferedReader(new InputStreamReader(is)));
        Token next = tokenizer.nextToken();
        assertEquals(Token.TYPE.PRINT, next.getType());
    }
}
