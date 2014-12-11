
package com.github.java2uml;


import japa.parser.JavaParser;
import japa.parser.ast.CompilationUnit;

import japa.parser.ast.body.*;

import japa.parser.ast.type.ClassOrInterfaceType;

import japa.parser.ast.visitor.VoidVisitorAdapter;

import java.io.*;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nadcukandrej on 09.12.14.
 */
public class CreateUmlCode {

    private static List<File> files;
    private static StringBuilder source;
    private static String fileUML;

    public CreateUmlCode(String folder) throws Exception {
        // Генерирование названия файла UML
        fileUML = "umlTemplates/UML" + getClass().getSimpleName() + ".pumpl.ft";

        init(folder);
    }

    public static void init(String path) throws Exception {
        String absolutePath = path;
        File folder = new File(absolutePath);
        createArrayFiles(folder);
        source = new StringBuilder();

        // текст в формате plantuml - начало сборки
        source.append("@startuml\n");
        for (File fileName : files) {
            getCU(fileName);
        }
        source.append("@enduml\n");

        // Запись в файл UML в папку umlTemplates
        write(source.toString());
    }

    public static void getCU(File path) throws Exception {
        // creates an input stream for the file to be parsed
        FileInputStream in = new FileInputStream(path);

        CompilationUnit cu;
        try {
            // parse the file
            cu = JavaParser.parse(in);
        } finally {
            in.close();
        }
        /**
         *  Вызов визитора для классов и интерфейсов
         */
        new GetClassOrInterfaceDeclaration().visit(cu, null);
    }

    /**
     * Visitor implementation for visiting ClassOrInterfaceDeclaration nodes.
     */
    private static class GetClassOrInterfaceDeclaration extends VoidVisitorAdapter {

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Object arg) {


            if (n.getImplements() != null || n.getExtends() != null) {
                List<ClassOrInterfaceType> list = (n.getImplements() == null ? n.getExtends() : n.getImplements());
                for (ClassOrInterfaceType type : list)
                    source.append("\n" + type.getName());
                source.append(" <|-- ");
                source.append(n.getName() + "\n\n");
            }

            source.append(Modifier.toString(n.getModifiers() - 1));
            source.append(n.getModifiers() - 1 > 0 ? " " : "");
            if (n.isInterface())
                source.append(Modifier.toString(Modifier.INTERFACE) + " ");
            else
                source.append("class ");

            source.append(n.getName());

            if (n.getMembers().size() > 0) {
                source.append("{\n");

                // Вызов визитора для полей
                new GetFields().visit(n, arg);

                // Вызов визитора для методов
                new GetMethods().visit(n, arg);
                source.append("}\n");
            }
        }

    }

    /**
     * Visitor implementation for visiting FieldDeclaration nodes.
     */
    private static class GetFields extends VoidVisitorAdapter {

        @Override
        public void visit(FieldDeclaration n, Object arg) {

            setModifier(n.getModifiers());

            source.append(n.getType());
            for (VariableDeclarator var : n.getVariables()) {
                source.append(" " + var.getId() + "\n");
            }
        }

    }

    /**
     * Visitor implementation for visiting MethodDeclaration nodes.
     */
    private static class GetMethods extends VoidVisitorAdapter {

        @Override
        public void visit(MethodDeclaration n, Object arg) {

            setModifier(n.getModifiers());

            source.append(n.getName() + "(");
            if (n.getParameters() != null) {
                setParameters(n.getParameters());
            }
            source.append(")\n");
        }

        private void setParameters(List<Parameter> parameters) {
            for (Parameter parameter : parameters) {
                source.append(parameter.getType() + " ");
                source.append(parameter.getId());
            }
        }

    }

    private static void setModifier(int mod) {
        switch (mod) {
            case Modifier.PRIVATE:
                source.append(" -");
                break;
            case Modifier.PROTECTED:
                source.append(" ~");
                break;
            default:
                source.append(" +");
        }
    }

    private static void createArrayFiles(File path) {
        files = new ArrayList<File>();
        File[] folder = path.listFiles();

        for (int i = 0; i < folder.length; i++) {
            if (folder[i].isDirectory())
                createArrayFiles(folder[i]);
            else if (folder[i].toString().toLowerCase().endsWith(".java"))
                files.add(folder[i]);
        }
    }

    public static void write(String text) {
        //Определяем файл
        File file = new File(fileUML).getAbsoluteFile();
        try {
            //проверяем, что если файл не существует то создаем его
            if (!file.exists()) {
                file.createNewFile();
            }

            //PrintWriter обеспечит возможности записи в файл
            PrintWriter out = new PrintWriter(file.getAbsoluteFile());

            try {
                //Записываем текст у файл
                out.print(text);
            } finally {
                out.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
