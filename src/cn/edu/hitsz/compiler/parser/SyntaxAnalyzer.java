package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Action;
import cn.edu.hitsz.compiler.parser.table.LRTable;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

import javax.management.RuntimeErrorException;

//TODO: 实验二: 实现 LR 语法分析驱动程序

/**
 * LR 语法分析驱动程序
 * <br>
 * 该程序接受词法单元串与 LR 分析表 (action 和 goto 表), 按表对词法单元流进行分析, 执行对应动作, 并在执行动作时通知各注册的观察者.
 * <br>
 * 你应当按照被挖空的方法的文档实现对应方法, 你可以随意为该类添加你需要的私有成员对象, 但不应该再为此类添加公有接口, 也不应该改动未被挖空的方法,
 * 除非你已经同助教充分沟通, 并能证明你的修改的合理性, 且令助教确定可能被改动的评测方法. 随意修改该类的其它部分有可能导致自动评测出错而被扣分.
 */
public class SyntaxAnalyzer {
    private final SymbolTable symbolTable;
    private final List<ActionObserver> observers = new ArrayList<>();

    private LRTable lrTable;
    private final Stack<Token> tokenStack = new Stack<>(); // 符号栈
    private final Stack<Status> statusStack = new Stack<>(); // 状态栈

    public SyntaxAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    /**
     * 注册新的观察者
     *
     * @param observer 观察者
     */
    public void registerObserver(ActionObserver observer) {
        observers.add(observer);
        observer.setSymbolTable(symbolTable);
    }

    /**
     * 在执行 shift 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     * @param currentToken  当前词法单元
     */
    public void callWhenInShift(Status currentStatus, Token currentToken) {
        for (final var listener : observers) {
            listener.whenShift(currentStatus, currentToken);
        }
    }

    /**
     * 在执行 reduce 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     * @param production    待规约的产生式
     */
    public void callWhenInReduce(Status currentStatus, Production production) {
        for (final var listener : observers) {
            listener.whenReduce(currentStatus, production);
        }
    }

    /**
     * 在执行 accept 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     */
    public void callWhenInAccept(Status currentStatus) {
        for (final var listener : observers) {
            listener.whenAccept(currentStatus);
        }
    }

    public void loadTokens(Iterable<Token> tokens) {
        // 加载词法单元
        // 你可以自行选择要如何存储词法单元, 譬如使用迭代器, 或是栈, 或是干脆使用一个 list 全存起来
        // 需要注意的是, 在实现驱动程序的过程中, 你会需要面对只读取一个 token 而不能消耗它的情况,
        // 在自行设计的时候请加以考虑此种情况
        for (final var token : tokens) {
            tokenStack.add(token);
        }
    }

    public void loadLRTable(LRTable table) {
        // 加载 LR 分析表
        // 你可以自行选择要如何使用该表格:
        // 是直接对 LRTable 调用 getAction/getGoto, 抑或是直接将 initStatus 存起来使用
        this.lrTable = table;
    }

    public void run() {
        // 实现驱动程序
        // 你需要根据上面的输入来实现 LR 语法分析的驱动程序
        // 请分别在遇到 Shift, Reduce, Accept 的时候调用上面的 callWhenInShift, callWhenInReduce,
        // callWhenInAccept
        // 否则用于为实验二打分的产生式输出可能不会正常工作
        statusStack.push(lrTable.getInit()); // 起始状态
        while (!tokenStack.isEmpty()) {
            final Status currentStatus = statusStack.peek();
            final Token token = tokenStack.peek();
            final Action action = currentStatus.getAction(token);
            switch (action.getKind()) {
                case Shift -> {
                    final Status nextStatus = action.getStatus();
                    statusStack.push(nextStatus);
                    tokenStack.pop();

                    callWhenInShift(currentStatus, token);
                }
                case Reduce -> {
                    final Production production = action.getProduction(); // 产生式
                    for (int i = 0; i < production.body().size(); i++) {
                        statusStack.pop();
                    }
                    final Status newCurrentStatus = statusStack.peek();
                    final Status nextStatus = newCurrentStatus.getGoto(production.head());
                    statusStack.push(nextStatus);
                    callWhenInReduce(currentStatus, production);
                }
                case Accept -> {
                    // 此时 statusStack 有两个状态: 初始状态, acc 行的状态
                    System.err.println("=== Accept ===");
                    System.err.println("statusStack: " + statusStack);
                    System.err.println("tokenStack: " + tokenStack);
                    callWhenInAccept(currentStatus);
                    if (statusStack.size() != 2) {
                        throw new IllegalStateException("statusStack.size() != 2");
                    }
                    if (tokenStack.size() != 1) {
                        throw new IllegalStateException("tokenStack.size() != 1");
                    }
                    return;
                }
                case Error -> {
                    throw new RuntimeException("Syntax error");
                }
            }
        }
    }
}
