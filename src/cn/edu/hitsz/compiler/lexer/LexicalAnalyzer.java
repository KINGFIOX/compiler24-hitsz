package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.StreamSupport;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;

/**
 * TODO: 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private final SymbolTable symbolTable;

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    // @begin-del
    private String content;
    private final List<Token> tokens = new ArrayList<>();
    // @end

    /**
     * 从给予的路径中读取并加载文件内容
     * 
     * @param path 路径
     */
    public void loadFile(String path) {
        // TODO: 词法分析前的缓冲区实现
        // 可自由实现各类缓冲区
        // 或直接采用完整读入方法
        // @begin
        this.content = FileUtils.readFile(path);
        // @end
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        // TODO: 自动机实现的词法分析过程
        // @begin
        final var ptr = CharPointer.from(content);
        while (ptr.inBound()) {
            while (ptr.inBoundAnd(Character::isWhitespace)) {
                ptr.next();
            }

            if (!ptr.inBound()) {
                break;
            }

            if (ptr.satisfy(Character::isDigit)) {
                final var text = ptr.collectWhile(Character::isDigit);
                tokens.add(Token.normal("IntConst", text));

            } else if (ptr.satisfy(this::isIdentifierBegin)) {
                final var text = ptr.collectWhile(this::isIdentifierBody);
                if (isKeyword(text)) {
                    tokens.add(Token.simple(text));
                } else {
                    tokens.add(Token.normal("id", text));

                    if (!symbolTable.has(text)) {
                        symbolTable.add(text);
                    }
                }

            } else if (ptr.satisfy(this::isPunctuation)) {
                final var punc = String.valueOf(ptr.next());
                // 由于实验室那边提供的生成 LR 表的软件中分号是一个特殊字符,
                // 所以必须在这里对分号特殊处理, 换个词来表示分号
                tokens.add(Token.simple(punc.equals(";") ? "Semicolon" : punc));

            } else {
                throw new RuntimeException("Unknown character: " + ptr.curr());
            }
        }

        tokens.add(Token.eof());
        // @end
    }

    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     * 
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // TODO: 从词法分析过程中获取 Token 列表
        // 词法分析过程可以使用 Stream 或 Iterator 实现按需分析
        // 亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可
        // @begin
        return tokens;
        // @end
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
                path,
                StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList());
    }

    // @begin-del
    private boolean isIdentifierBegin(char ch) {
        return Character.isAlphabetic(ch) || ch == '_';
    }

    private boolean isIdentifierBody(char ch) {
        return isIdentifierBegin(ch) || Character.isDigit(ch);
    }

    private boolean isPunctuation(char c) {
        return c == '=' || c == ',' || c == ';' || c == '+' || c == ':'
                || c == '-' || c == '*' || c == '/' || c == '(' || c == ')';
    }

    private final Set<String> keywords = Set.of("int", "return", "input", "print", "ifeqz", "ifgtz", "goto");

    private boolean isKeyword(String text) {
        return keywords.contains(text);
    }

    interface CharPredicate {
        boolean test(char ch);
    }

    private static class CharPointer {
        public static CharPointer from(String string) {
            return new CharPointer(string);
        }

        public boolean inBound() {
            return currentIndex < length;
        }

        public boolean inBoundAnd(CharPredicate predicate) {
            return inBound() && predicate.test(chars[currentIndex]);
        }

        public char curr() {
            if (!inBound()) {
                throw new NoSuchElementException();
            }

            return chars[currentIndex];
        }

        public boolean satisfy(CharPredicate predicate) {
            return predicate.test(curr());
        }

        public boolean hasNext() {
            return currentIndex + 1 < length;
        }

        public char next() {
            if (!inBound()) {
                throw new NoSuchElementException();
            }

            return chars[currentIndex++];
        }

        public String collectWhile(CharPredicate predicate) {
            final var builder = new StringBuilder();
            while (inBoundAnd(predicate)) {
                builder.append(next());
            }

            return builder.toString();
        }

        private CharPointer(String string) {
            this.chars = string.toCharArray();
            this.currentIndex = 0;
            this.length = string.length();
        }

        private final char[] chars;
        private final int length;
        private int currentIndex;
    }
    // @end

    // @begin-del
    // 另一实现
    // 自动机
    LexStatus status = LexStatus.White;
    final StringBuilder buffer = new StringBuilder();

    public void run2() {
        // 多加一个 "\n" 确保非空白字符全部被 tokens 读走
        final var _content = content + "\n";
        for (final var c : _content.toCharArray()) {
            switch (status) {
                case White -> whiteShift(c);
                case Digit -> digitShift(c);
                case Id -> idShift(c);
                case Punc -> puncShift(c);
            }
        }
        tokens.add(Token.eof());
    }

    private enum LexStatus {
        White, Digit, Punc, Id
    }

    private boolean isWhiteSpace(char c) {
        return Character.isWhitespace(c);
    }

    private boolean isDigit(char c) {
        return Character.isDigit(c);
    }

    // 状态转移会伴随缓冲区操作
    // 规定 tokens 读走缓冲区会发生在转移到 white 或 punc 状态时
    private void whiteShift(char c) {
        if (isWhiteSpace(c)) {
            status = LexStatus.White;
        } else if (isPunctuation(c)) {
            buffer.append(c);
            status = LexStatus.Punc;
        } else if (isIdentifierBegin(c)) {
            buffer.append(c);
            status = LexStatus.Id;
        } else if (isDigit(c)) {
            buffer.append(c);
            status = LexStatus.Digit;
        } else {
            throw new RuntimeException("Unknown character: " + c);
        }
    }

    private void digitShift(char c) {
        if (isWhiteSpace(c)) {
            tokens.add(Token.normal("IntConst", buffer.toString()));
            buffer.delete(0, buffer.length());
            status = LexStatus.White;
        } else if (isPunctuation(c)) {
            tokens.add(Token.normal("IntConst", buffer.toString()));
            buffer.delete(0, buffer.length());
            buffer.append(c);
            status = LexStatus.Punc;
        } else if (isIdentifierBegin(c)) {
            throw new RuntimeException("Illegal token: " + buffer.append(c));
        } else if (isDigit(c)) {
            buffer.append(c);
            status = LexStatus.Digit;
        } else {
            throw new RuntimeException("Unknown character: " + c);
        }
    }

    private void puncShift(char c) {
        if (isWhiteSpace(c)) {
            final var punc = buffer.toString();
            tokens.add(Token.simple(punc.equals(";") ? "Semicolon" : punc));
            buffer.delete(0, buffer.length());
            status = LexStatus.White;
        } else if (isPunctuation(c)) {
            final var punc = buffer.toString();
            tokens.add(Token.simple(punc.equals(";") ? "Semicolon" : punc));
            buffer.delete(0, buffer.length());
            buffer.append(c);
            status = LexStatus.Punc;
        } else if (isIdentifierBegin(c)) {
            final var punc = buffer.toString();
            tokens.add(Token.simple(punc.equals(";") ? "Semicolon" : punc));
            buffer.delete(0, buffer.length());
            buffer.append(c);
            status = LexStatus.Id;
        } else if (isDigit(c)) {
            final var punc = buffer.toString();
            tokens.add(Token.simple(punc.equals(";") ? "Semicolon" : punc));
            buffer.delete(0, buffer.length());
            buffer.append(c);
            status = LexStatus.Digit;
        } else {
            throw new RuntimeException("Unknown character: " + c);
        }
    }

    private void idShift(char c) {
        if (isWhiteSpace(c)) {
            final var text = buffer.toString();
            tokens.add(isKeyword(text) ? Token.simple(text) : Token.normal("id", text));
            buffer.delete(0, buffer.length());
            status = LexStatus.White;
        } else if (isPunctuation(c)) {
            final var text = buffer.toString();
            tokens.add(isKeyword(text) ? Token.simple(text) : Token.normal("id", text));
            buffer.delete(0, buffer.length());
            buffer.append(c);
            status = LexStatus.Punc;
        } else if (isIdentifierBegin(c) || isDigit(c)) {
            buffer.append(c);
            status = LexStatus.Id;
        } else {
            throw new RuntimeException("Unknown character: " + c);
        }
    }
    // @end
}