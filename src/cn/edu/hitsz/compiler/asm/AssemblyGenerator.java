package cn.edu.hitsz.compiler.asm;

// import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.Instruction;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {

    /* ---------- fields ---------- */

    private List<Instruction> instructions;
    private final Map<Integer, String> reg = new HashMap<>();
    private final Map<String, Integer> var = new HashMap<>();
    private final List<String> assembly = new ArrayList<>(List.of(".text"));

    /* ---------- func ---------- */

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        // TODO: 读入前端提供的中间代码并生成所需要的信息
        instructions = originInstructions;
    }

    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        // TODO: 执行寄存器分配与代码生成
        for (Instruction ins : instructions) {
            boolean hasReturn = false;
            switch (ins.getKind()) {
                case ADD -> {
                    int resultReg = getReg(ins.getResult().toString());
                    List<IRValue> operands = ins.getOperands();
                    String ope2 = operands.getLast().toString();
                    int reg1, reg2;
                    if (operands.getFirst().isImmediate()) {
                        reg1 = getReg("temp1");
                        assembly.add("    li x%d, %s".formatted(reg1, operands.getFirst().toString()));
                    } else {
                        reg1 = getReg(ins.getOperands().getFirst().toString());
                    }
                    if (operands.getLast().isImmediate()) {
                        assembly.add("    addi x%d, x%d, %s\t\t#  %s".formatted(resultReg, reg1, ope2, ins.toString()));
                    } else {
                        reg2 = getReg(ope2);
                        assembly.add("    add x%d, x%d, x%d\t\t#  %s".formatted(resultReg, reg1, reg2, ins.toString()));
                    }
                }
                case SUB -> {
                    int resultReg = getReg(ins.getResult().toString());
                    List<IRValue> operands = ins.getOperands();
                    String ope2 = operands.getLast().toString();
                    int reg1, reg2;
                    if (operands.getFirst().isImmediate()) {
                        reg1 = getReg("temp1");
                        assembly.add("    li x%d, %s".formatted(reg1, operands.getFirst().toString()));
                    } else {
                        reg1 = getReg(ins.getOperands().getFirst().toString());
                    }
                    if (operands.getLast().isImmediate()) {
                        assembly.add("    subi x%d, x%d, %s\t\t#  %s".formatted(resultReg, reg1, ope2, ins.toString()));
                    } else {
                        reg2 = getReg(ope2);
                        assembly.add("    sub x%d, x%d, x%d\t\t#  %s".formatted(resultReg, reg1, reg2, ins.toString()));
                    }
                }
                case MUL -> {
                    int resultReg = getReg(ins.getResult().toString());
                    List<IRValue> operands = ins.getOperands();
                    String ope2 = operands.getLast().toString();
                    int reg1, reg2;
                    if (operands.getFirst().isImmediate()) {
                        reg1 = getReg("temp1");
                        assembly.add("    li x%d, %s".formatted(reg1, operands.getFirst().toString()));
                    } else {
                        reg1 = getReg(ins.getOperands().getFirst().toString());
                    }
                    if (operands.getLast().isImmediate()) {
                        reg2 = getReg("temp2");
                        assembly.add("    li x%d, %s".formatted(reg1, ope2));
                    } else {
                        reg2 = getReg(ope2);
                    }
                    assembly.add("    mul x%d, x%d, x%d\t\t#  %s".formatted(resultReg, reg1, reg2, ins.toString()));
                }
                case MOV -> {
                    int resultReg = getReg(ins.getResult().toString());
                    List<IRValue> operands = ins.getOperands();
                    String ope = operands.getFirst().toString();
                    if (operands.getFirst().isImmediate()) {
                        assembly.add("    li x%d, %s\t\t#  %s".formatted(resultReg, ope, ins.toString()));
                    } else {
                        assembly.add("    mv x%d, x%d\t\t#  %s".formatted(resultReg, getReg(ope), ins.toString()));
                    }
                }
                case RET -> {
                    List<IRValue> operands = ins.getOperands();
                    String ope = operands.getFirst().toString();
                    if (operands.getFirst().isImmediate()) {
                        assembly.add("    li x10, %s\t\t#  %s".formatted(ope, ins.toString()));
                    } else {
                        assembly.add("    mv x10, x%d\t\t#  %s".formatted(getReg(ope), ins.toString()));
                    }
                    hasReturn = true;
                }
            }
            if (hasReturn)
                break;
        }
    }

    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        // TODO: 输出汇编代码到文件
        try (FileWriter writer = new FileWriter(path)) {
            for (String asm : assembly) {
                writer.write(asm + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 寄存器分配很简单, 应该这么说: 不会考虑寄存器溢出的情况 😋
     * 
     * @param varName
     * @return
     */
    private int getReg(String varName) {
        if (var.get(varName) == null) {
            for (int i = 1; i < 32; i++) {
                if (reg.get(i) == null) {
                    var.put(varName, i);
                    reg.put(i, varName);
                    return i;
                }
            }
            throw new RuntimeException("No available register");
        } else {
            return var.get(varName);
        }
    }
}
