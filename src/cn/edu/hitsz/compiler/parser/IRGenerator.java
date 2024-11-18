package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.lexer.TokenKind;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;

// TODO: 实验三: 实现 IR 生成

/**
 *
 */
public class IRGenerator implements ActionObserver {

    private final List<Instruction> irList = new ArrayList<>();
    private final List<IRValue> valueStack = new ArrayList<>();

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        if (currentToken.getKind().equals(TokenKind.fromString("id"))) {
            valueStack.add(IRVariable.named(currentToken.getText()));
        } else if (currentToken.getKind().equals(TokenKind.fromString("IntConst"))) {
            valueStack.add(IRImmediate.of(Integer.parseInt(currentToken.getText())));
        } else {
            valueStack.add(null);
        }
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        switch (production.index()) {
            case 2 -> { // S_list -> S ; S_list
                for (int i = 0; i < 2; i++) {
                    valueStack.removeLast();
                }
            }
            case 3, 4 -> {
                // S_list -> S ;
                // S -> D id
                valueStack.removeLast();
            }
            case 6 -> { // S -> id = E
                irList.add(
                        Instruction.createMov(
                                (IRVariable) valueStack.get(valueStack.size() - 3),
                                valueStack.getLast()));
                for (int i = 0; i < 3; i++) {
                    valueStack.removeLast();
                }
                valueStack.add(null);
            }
            case 7 -> { // S -> return E
                irList.add(
                        Instruction.createRet(
                                valueStack.getLast()));
                for (int i = 0; i < 2; i++) {
                    valueStack.removeLast();
                }
                valueStack.add(null);
            }
            case 8 -> { // E -> E + A
                IRVariable res;
                if (valueStack.get(valueStack.size() - 3).isImmediate()) {
                    res = IRVariable.temp();
                } else if (!((IRVariable) valueStack.get(valueStack.size() - 3)).getName().startsWith("$")) {
                    res = IRVariable.temp();
                } else {
                    res = (IRVariable) valueStack.get(valueStack.size() - 3);
                }
                irList.add(
                        Instruction.createAdd(
                                res,
                                valueStack.get(valueStack.size() - 3),
                                valueStack.getLast()));
                for (int i = 0; i < 3; i++) {
                    valueStack.removeLast();
                }
                valueStack.add(res);
            }
            case 9 -> { // E -> E - A
                IRVariable res;
                if (valueStack.get(valueStack.size() - 3).isImmediate()) {
                    res = IRVariable.temp();
                } else if (!((IRVariable) valueStack.get(valueStack.size() - 3)).getName().startsWith("$")) {
                    res = IRVariable.temp();
                } else {
                    res = (IRVariable) valueStack.get(valueStack.size() - 3);
                }
                irList.add(
                        Instruction.createSub(
                                res,
                                valueStack.get(valueStack.size() - 3),
                                valueStack.getLast()));
                for (int i = 0; i < 3; i++) {
                    valueStack.removeLast();
                }
                valueStack.add(res);
            }
            case 11 -> { // A -> A * B
                IRVariable res;
                if (valueStack.get(valueStack.size() - 3).isImmediate()) {
                    res = IRVariable.temp();
                } else if (!((IRVariable) valueStack.get(valueStack.size() - 3)).getName().startsWith("$")) {
                    res = IRVariable.temp();
                } else {
                    res = (IRVariable) valueStack.get(valueStack.size() - 3);
                }
                irList.add(Instruction.createMul(
                        res,
                        valueStack.get(valueStack.size() - 3),
                        valueStack.getLast()));
                for (int i = 0; i < 3; i++)
                    valueStack.removeLast();
                valueStack.add(res);
            }
            case 13 -> { // B -> ( E )
                valueStack.removeLast();
                IRValue tmp = valueStack.getLast();
                valueStack.removeLast();
                valueStack.removeLast();
                valueStack.add(tmp);
            }
            default -> {
                // P -> S_list
                // D -> int
                // E -> A
                // A -> B
                // B -> id
                // B -> IntConst
            }
        }
    }

    @Override
    public void whenAccept(Status currentStatus) {
        // do nothing
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // do nothing
    }

    public List<Instruction> getIR() {
        return irList;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}
