package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
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
    private final SymbolTable symbolTable; // 这个应该是: 拿到了单例的指针

    private String content;

    private List<Token> tokens;

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.tokens = new ArrayList<>();
    }

    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) throws IOException {
        this.content = new String(Files.readAllBytes(Paths.get(path)));
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        int last = 0;
        int length = content.length();
        SourceCodeType type = null;
        while (last < length) {
            char ch = content.charAt(last);
            if (Character.isWhitespace(ch)) {
                last++;
                continue;
            } else if (Character.isLetter(ch) || ch == '_') { // id
                int cur = last + 1;
                while (cur < length && (Character.isLetterOrDigit(content.charAt(cur)) || content.charAt(cur) == '_')) {
                    cur++;
                }
                String word = content.substring(last, cur);
                if (word.equals("int")) {
                    this.tokens.add(Token.simple("int"));
                    type = SourceCodeType.Int;
                } else if (word.equals("return")) {
                    this.tokens.add(Token.simple("return"));
                } else {
                    this.tokens.add(Token.normal("id", word));
                    if (!symbolTable.has(word)) {
                        symbolTable.add(word);
                        symbolTable.get(word).setType(type);
                    }
                }
                last = cur;
            } else if (Character.isDigit(ch)) {
                int cur = last + 1;
                while (cur < length && Character.isDigit(content.charAt(cur))) {
                    cur++;
                }
                String number = content.substring(last, cur);
                this.tokens.add(Token.normal("IntConst", number));
                last = cur;
            } else {
                switch (ch) {
                    case '=':
                        tokens.add(Token.simple("="));
                        last++;
                        break;
                    case ',':
                        tokens.add(Token.simple(","));
                        last++;
                        break;
                    case ';':
                        tokens.add(Token.simple("Semicolon"));
                        last++;
                        break;
                    case '+':
                        tokens.add(Token.simple("+"));
                        last++;
                        break;
                    case '-':
                        tokens.add(Token.simple("-"));
                        last++;
                        break;
                    case '*':
                        tokens.add(Token.simple("*"));
                        last++;
                        break;
                    case '/':
                        tokens.add(Token.simple("/"));
                        last++;
                        break;
                    case '(':
                        tokens.add(Token.simple("("));
                        last++;
                        break;
                    case ')':
                        tokens.add(Token.simple(")"));
                        last++;
                        break;

                    default:
                        throw new RuntimeException("Unknown character: " + ch);
                }
            }
        }

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
        // throw new NotImplementedException();
        return tokens;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
                path,
                StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList());
    }
}