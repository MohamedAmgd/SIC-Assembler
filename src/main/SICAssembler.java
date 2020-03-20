/*
 Copyright 2019 Mohamed Amgd

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */
package main;

import java.io.File;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.Scanner;

/**
 *
 * @author Mohamed Amgd
 */
public class SICAssembler {

    public static void main(String[] args) {
        String line, startingAddress = "0", locctr = "0", progarmLength = "0";
        String[][] optab = new String[60][2]; // 60 the number of lines in optab file
        
        File fileIn = new File("src/testFiles/test.txt");
        File intermediateFile = new File("src/testFiles/intermediate.txt");
        File optabFile = new File("src/testFiles/OPTAB.txt");
            
        LinkedList<symtabElement> symtab = new LinkedList<>();
        LinkedList<Instruction> lines = new LinkedList<>();
            
        try {
           
            //opening each file
            Scanner readFile = new Scanner(fileIn);
            Scanner readOptab = new Scanner(optabFile);
            Formatter formatIntermediate = new Formatter(intermediateFile);
            
            //reading the input file (fileIn)
            while (readFile.hasNext()) {
                line = readFile.nextLine();
                if (!line.equals("") && instructionMaker(line) != null) {
                    lines.add(instructionMaker(line));
                    line = "";
                }
            }
            
            //reading the optab file
            for (int i = 0; i < 60; i++) {
                if (readOptab.hasNext()) {
                    optab[i][0] = readOptab.next();
                    optab[i][1] = readOptab.next();
                }
            }
            
            //runing the program form the input file (fileIn)
            for (int i = 0; i < lines.size(); i++) {
                
                //parsing each instruction
                Instruction instruction = lines.get(i);
                
                //check if the operation code of this instruction is the start instrucion
                if ("START".equals(instruction.getOpCode())) {
                    startingAddress = instruction.getOperand();
                    locctr = hexToDec(startingAddress);
                }
                
                //check if the operation code of this instruction isn't the start instrucion or the end 
                if (!"END".equals(instruction.getOpCode())) {
                    
                    //writing the last processed instruction before continuing
                    String x =  decToHex(locctr) + "\t" + instruction.getLabel() + "\t" + instruction.getOpCode() + "\t" + instruction.getOperand() + "\r\n";
                    formatIntermediate.format("%s",x);

                    //check if the label of the current instruction isn't empty 
                    if (!"".equals(instruction.getLabel())) {
                        //check if the label of the current instruction exists in symbol table (symtab)
                        boolean notFoundInSymtab = true;
                        if (!symtab.isEmpty()) {
                            for (int j = 0; j < symtab.size(); j++) {
                                if (instruction.getLabel().equals(symtab.get(j).getSymbol())) {
                                    notFoundInSymtab = false;
                                }
                            }
                        }
                        //if not exists or symtab is empty add it
                        if (notFoundInSymtab) {
                            symtab.add(new symtabElement(instruction.getLabel(), locctr));
                        }
                    }
                    
                    //check if the opration code of the current instruction exists in optab
                    boolean notFoundInOptab = true;
                    for (int j = 0; j < 60; j++) {
                        if (instruction.getOpCode().equals(optab[j][0])) {
                            notFoundInOptab = false;
                        }
                    }
                    
                    //check for a specific opration codes
                    if ("WORD".equals(instruction.getOpCode())) {
                        locctr = (Integer.parseInt(locctr) + 3) + "";
                    } else if ("RESW".equals(instruction.getOpCode())) {
                        locctr = Integer.parseInt(locctr) + (3 * Integer.parseInt(instruction.getOperand())) + "";
                    } else if ("RESB".equals(instruction.getOpCode())) {
                        locctr = Integer.parseInt(locctr) + Integer.parseInt(instruction.getOperand()) + "";
                    } else if ("BYTE".equals(instruction.getOpCode())) {
                        switch (instruction.getOperand().charAt(0)) {
                            case 'X':
                                Double temp = Math.ceil(((double)instruction.getOperand().length() - 3) / 2);
                                locctr = Integer.parseInt(locctr) + temp.intValue() + "";
                                break;
                            case 'C':
                                locctr = Integer.parseInt(locctr) + (instruction.getOperand().length() - 3) + "";
                                break;
                        }
                    } else if (!notFoundInOptab) {
                        locctr = Integer.parseInt(locctr) + 3 + "";
                    }

                } else {
                    // case: opratation code is end
                    // write the last line
                    formatIntermediate.format("%s", decToHex(locctr) + "\t" + instruction.getOpCode() + "\t" + instruction.getOperand() + "\r\n");
                }
            }
            
            //calculation of program length and print it in intermediate
            progarmLength = Integer.parseInt(locctr) - Integer.parseInt(hexToDec(startingAddress)) + "";
            formatIntermediate.format("%s", "\n\n\n" + "program length = " + " " + decToHex(progarmLength) + " ");
            
            // Symtab print in intermediate
            formatIntermediate.format("%s", "\n\n\t" + "-----------------");
            formatIntermediate.format("%S", "\n\t" + "| Symbol" + " | " + " Loc |" + "\r\n");
            for (int i = 0; i < symtab.size(); i++) {
                formatIntermediate.format("%S", "\t" + "| " + symtab.get(i).getSymbol() + " | " + decToHex(symtab.get(i).getLoc()) + " |" + "\r\n");
            }
            
            //close all files
            formatIntermediate.close();
            readFile.close();
            readOptab.close();
            System.out.println("Done \nfinal result saved in the intermediate file");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static String decToHex(String address) {
        int num = Integer.parseInt(address);
        return Integer.toHexString(num);
    }

    public static String hexToDec(String hex) {
        return Integer.parseInt(hex, 16) + "";
    }

    public static Instruction instructionMaker(String line) {
        String label = "", opCode = "", operand = "";
        Scanner read = new Scanner(line);
        LinkedList<String> words = new LinkedList<>();
        while (read.hasNext()) {
            words.add(read.next());
        }
        if (words.size() == 1 && !".".equals(words.get(0))) {
            opCode = words.get(0);
        } else if (words.size() == 2) {
            opCode = words.get(0);
            operand = words.get(1);
        } else if (words.size() == 3) {
            label = words.get(0);
            opCode = words.get(1);
            operand = words.get(2);
        } else {
            return null;
        }
        if (".".equals(label) || ".".equals(opCode) || ".".equals(operand)) {
            return null;
        }
        return new Instruction(label, opCode, operand);
    }

}
