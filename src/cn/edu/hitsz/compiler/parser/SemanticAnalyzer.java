package cn.edu.hitsz.compiler.parser;

import java.util.ArrayList;

import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

// TODO: 实验三: 实现语义分析
public class SemanticAnalyzer implements ActionObserver {

    private SymbolTable symbolTable;
    private final ArrayList<Token> tokenStack = new ArrayList<>();
    private final ArrayList<SourceCodeType> dataStack = new ArrayList<>();

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO: 该过程在遇到 reduce production 时要采取的代码动作
        switch (production.index()) {
            case 4 -> { // S -> D id
                symbolTable.get(tokenStack.getLast().getText()).setType(dataStack.get(dataStack.size() - 2));
            }
            case 5 -> { // D -> int
                tokenStack.removeLast();
                dataStack.removeLast();
                tokenStack.add(null);
                dataStack.add(SourceCodeType.Int);
            }
            default -> {
                for (int i = 0; i < production.body().size(); i++) {
                    tokenStack.removeLast();
                    dataStack.removeLast();
                }
                tokenStack.add(null);
                dataStack.add(null);
            }
        }
    }

    @Override
    public void whenAccept(Status currentStatus) {
        // // TODO: 该过程在遇到 Accept 时要采取的代码动作
        // throw new NotImplementedException();
        // do nothing
    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO: 该过程在遇到 shift 时要采取的代码动作
        tokenStack.add(currentToken);
        if (currentToken.getKind().getTermName().equals("int")) {
            dataStack.add(SourceCodeType.Int);
        } else {
            dataStack.add(null);
        }
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO: 设计你可能需要的符号表存储结构
        // 如果需要使用符号表的话, 可以将它或者它的一部分信息存起来, 比如使用一个成员变量存储
        symbolTable = table;
    }
}
