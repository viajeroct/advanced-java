package info.kgeorgiy.ja.trofimov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.CodeSource;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

/**
 * Class that generates simple implementation for given class or interface,
 * specified by {@code token}.
 *
 * @see JarImpler#implement(Class, Path)
 * @see JarImpler#implementJar(Class, Path)
 */
public class Implementor implements JarImpler {
    /**
     * Empty constructor, creates class.
     */
    public Implementor() {
    }

    /**
     * Next line string for your system, which is returned by {@link System#lineSeparator()}.
     */
    private static final String EOLN = System.lineSeparator();

    /**
     * The main function for {@link Implementor}.
     * <p>
     * If the first argument is {@code "-jar"} calls {@link Implementor#implementJar(Class, Path)},
     * otherwise calls {@link Implementor#implement(Class, Path)}.
     * Last two arguments define token and path.
     * <p>
     * If any argument is null or number of arguments doesn't equal to 3 in case of {@code "-jar"}
     * and 2 in other case, then generates {@link ImplerException} error.
     *
     * @param args given arguments
     */
    public static void main(String[] args) {
        if (args == null || args.length == 0 || Arrays.stream(args).anyMatch(Objects::isNull) ||
                args.length != (args[0].equals("-jar") ? 3 : 2)) {
            System.err.println("Usage: java Implementor [-jar] <name_of_class> <path_to_save>");
            return;
        }

        String path = args[args.length - 1];
        String className = args[args.length - 2];

        try {
            Class<?> token = Class.forName(className);
            Path file = Path.of(path);

            Implementor impl = new Implementor();
            if (args[0].equals("-jar")) {
                impl.implementJar(token, file);
            } else {
                impl.implement(token, file);
            }
        } catch (ClassNotFoundException e) {
            System.err.println("No such class: " + e.getMessage());
        } catch (InvalidPathException e) {
            System.err.println("Path doesn't exist:  " + e.getMessage());
        } catch (ImplerException e) {
            System.err.println("(Jar)Implementor error: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (!canImplement(token)) {
            throw new ImplerException("Can't implement this class/interface.");
        }

        Path file = getPathToFile(token, root, "java");

        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(file)) {
                generateJavaFile(token, writer);
            }
        } catch (IOException e) {
            throw new ImplerException("Smth went wrong while writing result: " + e.getMessage());
        }
    }

    /**
     * Saves {@code data} to file through {@code writer}, preliminarily
     * converting it to Unicode, using {@link #convertToUnicode(String)}.
     *
     * @param writer where to write the result
     * @param data   string which to save
     * @throws IOException if error occurred during writing result through writer
     */
    private void save(BufferedWriter writer, String data) throws IOException {
        writer.write(convertToUnicode(data));
    }

    /**
     * Generates Java file through calling {@link #generateHeader(Class, BufferedWriter)} and
     * {@link #generateClass(Class, BufferedWriter)} functions, separated by {@link #EOLN}.
     *
     * @param token  specified class/interface
     * @param writer where to write the result
     * @throws IOException if error occurred during writing result through writer
     */
    private void generateJavaFile(Class<?> token, BufferedWriter writer) throws IOException {
        generateHeader(token, writer);
        save(writer, EOLN);
        generateClass(token, writer);
    }

    /**
     * Generates header of a file, this is {@code packageName}).
     *
     * @param token  specified class/interface
     * @param writer where to write the result
     * @throws IOException if error occurred during writing result through writer
     */
    private void generateHeader(Class<?> token, BufferedWriter writer) throws IOException {
        String packageName = token.getPackageName();
        save(writer, packageName.isEmpty() ? "" : "package " + packageName + ";" + EOLN);
    }

    /**
     * Generates header of the class, then calls method to generate
     * methods {@link #generateMethods(Class, BufferedWriter)}.
     *
     * @param token  specified class/interface
     * @param writer where to write the result
     * @throws IOException if error occurred during writing result through writer
     */
    private void generateClass(Class<?> token, BufferedWriter writer) throws IOException {
        save(writer, String.format("public class %s %s %s {",
                getClassName(token),
                token.isInterface() ? "implements" : "extends",
                token.getCanonicalName()) + EOLN);
        generateMethods(token, writer);
        save(writer, "}" + EOLN);
    }

    /**
     * Generates body of a class: constructors and abstract, not final methods.
     * To detect equal methods, uses {@link MethodWrapper}.
     *
     * @param token  specified class/interface
     * @param writer where to write the result
     * @throws IOException if error occurred during writing result through writer
     */
    private void generateMethods(Class<?> token, BufferedWriter writer) throws IOException {
        List<Constructor<?>> ctors = Arrays.stream(token.getDeclaredConstructors())
                .filter(ctor -> !Modifier.isPrivate(ctor.getModifiers())).toList();
        for (Constructor<?> ctor : ctors) {
            generateConstructor(ctor, writer);
        }

        List<MethodWrapper> methods = Stream.concat(Arrays.stream(token.getMethods()), Stream.<Class<?>>iterate(token,
                Objects::nonNull, Class::getSuperclass).flatMap(tok ->
                Arrays.stream(tok.getDeclaredMethods()))).map(MethodWrapper::new).toList();

        Map<MethodWrapper, Method> filtered = new HashMap<>();
        for (MethodWrapper methodWrapper : methods) {
            if (filtered.containsKey(methodWrapper)) {
                filtered.merge(methodWrapper, methodWrapper.method(), (old, nw) -> {
                   if (nw.getReturnType().isAssignableFrom(old.getReturnType())) {
                       return old;
                   } else {
                       return nw;
                   }
                });
            } else {
                filtered.put(methodWrapper, methodWrapper.method());
            }
        }

        for (Map.Entry<MethodWrapper, Method> wrapper : filtered.entrySet()) {
            Method m = wrapper.getValue();
            if (Modifier.isAbstract(m.getModifiers())) {
                save(writer, generateMethod(m));
            }
        }
    }

    /**
     * Returns implementation for some {@code exec}.
     * Calls {@link #getModifiers(Executable)}, {@link #getParameters(Executable)},
     * {@link #getExceptions(Executable)} on {@code exec}.
     * If {@code exec} is {@link Method} also returns its' return type,
     * using {@link Method#getReturnType()}.
     *
     * @param exec executable for which function generates implementation
     * @param name for executable
     * @return string representation of implemented {@code exec}
     */
    private String implSomeExecutable(Executable exec, String name) {
        String ret = "";
        if (exec instanceof Method method) {
            ret = method.getReturnType().getCanonicalName();
        }
        return String.format("%s %s %s%s%s {", getModifiers(exec), ret, name,
                getParameters(exec), getExceptions(exec));
    }

    /**
     * Generates constructor for {@code ctor}.
     *
     * @param ctor   constructor object for which to generate code
     * @param writer where to write the result
     * @throws IOException if error occurred during writing result through writer
     * @see #implSomeExecutable(Executable, String)
     */
    private void generateConstructor(Constructor<?> ctor, BufferedWriter writer) throws IOException {
        save(writer, tab(1) + implSomeExecutable(ctor, getClassName(ctor.getDeclaringClass())) +
                EOLN + tab(2) + Arrays.stream(ctor.getParameters()).map(Parameter::getName)
                .collect(Collectors.joining(",", "super(", ");")) +
                EOLN + tab(1) + "}" + EOLN);
    }

    /**
     * Generates return statement for some method. This is or empty return statement
     * or the result of {@link #getDefaultValue(Class)} function.
     *
     * @param method for which function creates a return statement
     * @return return statement
     */
    private String getReturnStatement(Method method) {
        return method.getReturnType() == void.class ? tab(2) + "return;" + EOLN :
                tab(2) + String.format("return %s;%n", getDefaultValue(method.getReturnType()));
    }

    /**
     * Generates body for some method. Uses {@link  #implSomeExecutable(Executable, String)}
     * and {@link #getReturnStatement(Method)}.
     *
     * @param method given method for which function creates a body
     * @return string representation of the method body
     */
    private String generateMethod(Method method) {
        return tab(1) + implSomeExecutable(method, method.getName()) +
                EOLN + getReturnStatement(method) + tab(1) + "}" + EOLN;
    }

    /**
     * Returns the name of generated class, it's {@link Class#getSimpleName()}, appended
     * by "Impl" string.
     *
     * @param token specified class/interface
     * @return name of class
     */
    private static String getClassName(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    /**
     * Returns {@link Path} for given {@code token}, resolved with {@code root},
     * in the end added {@code ext} of file.
     * {@code ext} is *.java or *.jar.
     *
     * @param token specified class/interface
     * @param root  root for path
     * @param ext   extension for file
     * @return path for file
     * @see Path#resolve(Path)
     */
    private static Path getPathToFile(Class<?> token, Path root, String ext) {
        Path tmp = Path.of(token.getPackageName().replace('.', File.separatorChar))
                .resolve(getClassName(token) + '.' + ext);
        return root == null ? tmp : root.resolve(tmp);
    }

    /**
     * Returns tab repeated {@code times}.
     *
     * @param tabs number of tabs to return
     * @return return the string of tabs
     */
    private String tab(int tabs) {
        return " ".repeat(4 * tabs);
    }

    /**
     * Returns default value for {@code token}. For reference types
     * returns {@code null}, {@code false} for {@code boolean} and
     * {@code 0} in other cases.
     *
     * @param token specified class/interface
     * @return string of default value
     */
    private String getDefaultValue(Class<?> token) {
        if (!token.isPrimitive()) {
            return "null";
        } else if (token == boolean.class) {
            return "false";
        }
        return "0";
    }

    /**
     * If there is no exceptions returns empty string, otherwise returns
     * string of exceptions, separated by ',', begins from ' throws ',
     * end with "".
     *
     * @param exec executable for with function generates string of exceptions
     * @return string representation of exceptions
     */
    private String getExceptions(Executable exec) {
        if (exec.getExceptionTypes().length != 0) {
            return Arrays.stream(exec.getExceptionTypes())
                    .map(Class::getCanonicalName)
                    .collect(Collectors.joining(", ", " throws ", ""));
        }
        return "";
    }

    /**
     * Returns modifiers of some {@code exec}, excluding {@link Modifier#ABSTRACT},
     * {@link Modifier#TRANSIENT} and {@link Modifier#NATIVE}.
     *
     * @param exec executable for which function returns modifiers
     * @return string of some {@code exec} exec modifiers
     */
    private String getModifiers(Executable exec) {
        return Modifier.toString(exec.getModifiers() & (Integer.MAX_VALUE - Modifier.ABSTRACT - Modifier.TRANSIENT
                - Modifier.NATIVE));
    }

    /**
     * Generates string of {@code exec} parameters separated by ',', begins from '('
     * and ends with ')'.
     *
     * @param exec executable for which function gets its parameters
     * @return string representation of parameters
     */
    private String getParameters(Executable exec) {
        return Arrays.stream(exec.getParameters())
                .map(p -> p.getType().getCanonicalName() + " " + p.getName())
                .collect(Collectors.joining(", ", "(", ")"));
    }

    /**
     * Checks if it's possible to create implementation of a class or
     * interface. It's not possible to generate implementation of primitives,
     * {@link Enum}, private or final tokens and of interfaces, which constructors
     * are all private.
     *
     * @param token specified class/interface
     * @return true if it's possible to create a class, which
     * implements {@code token}, false otherwise
     */
    private boolean canImplement(Class<?> token) {
        int mods = token.getModifiers();
        return !token.isPrimitive() && token != Enum.class && !Modifier.isPrivate(mods) && !Modifier.isFinal(mods) &&
                !(!token.isInterface() && Arrays.stream(token.getDeclaredConstructors()).map(Constructor::getModifiers)
                        .allMatch(Modifier::isPrivate));
    }

    /**
     * Returns input string, converted to Unicode format.
     *
     * @param s input string
     * @return converted string
     */
    private static String convertToUnicode(String s) {
        return s.codePoints().mapToObj(c -> c < 128 ? String.valueOf((char) c) :
                String.format("\\u%04x", c)).collect(Collectors.joining());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void implementJar(Class<?> token, Path path) throws ImplerException {
        Path dir;
        try {
            dir = Files.createTempDirectory(path.toAbsolutePath().getParent(), "build");
        } catch (IOException e) {
            throw new ImplerException("Can't create temp dir: " + e.getMessage());
        }

        implement(token, dir);
        compile(token, dir);
        createJar(dir, getPathToFile(token, null, "class"), path);

        try {
            clean(dir);
        } catch (IOException e) {
            throw new ImplerException("Can't clean temp dir: " + e.getMessage());
        }
    }

    /**
     * Compiles code generated by {@link Implementor#implement(Class, Path)}.
     *
     * @param token specified class/interface
     * @param root  path to directory with class sources
     * @throws ImplerException if Java compiler wasn't found
     * @throws ImplerException if error occurred during generating *.java file
     */
    private static void compile(Class<?> token, Path root) throws ImplerException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Java compiler wasn't detected.");
        }
        String classpath;
        try {
            CodeSource source = token.getProtectionDomain().getCodeSource();
            if (source == null) {
                classpath = ".";
            } else {
                classpath = Path.of(source.getLocation().toURI()).toString();
            }
        } catch (URISyntaxException e) {
            throw new ImplerException(e.getMessage());
        }
        String[] args = new String[]{"-encoding", StandardCharsets.UTF_8.name(), "-cp", classpath,
                getPathToFile(token, root, "java").toString()};
        if (compiler.run(null, null, null, args) != 0) {
            throw new ImplerException("Error while compiling generated class.");
        }
    }

    /**
     * Creates jar, including manifest file.
     *
     * @param root      directory, that contains compiled class
     * @param classFile full path of class, which is wrapping in jar
     * @param path      path to jar file
     * @throws ImplerException if error occurred during writing data to *.jar file
     */
    private static void createJar(Path root, Path classFile, Path path) throws ImplerException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        try (JarOutputStream stream = new JarOutputStream(Files.newOutputStream(path), manifest)) {
            stream.putNextEntry(new ZipEntry(classFile.toString().replace(File.separatorChar, '/')));
            Files.copy(root.resolve(classFile), stream);
        } catch (IOException e) {
            throw new ImplerException("Error while writing to jar: " + e.getMessage());
        }
    }

    /**
     * Visitor for deleting temp dirs after compiling.
     *
     * @see SimpleFileVisitor
     */
    private final SimpleFileVisitor<Path> DELETE_VISITOR = new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    };

    /**
     * Cleaning temp directory using {@link #DELETE_VISITOR}
     *
     * @param root path for deleting
     * @throws IOException if smth went wrong while deleting temp dir
     */
    private void clean(final Path root) throws IOException {
        if (Files.exists(root)) {
            Files.walkFileTree(root, DELETE_VISITOR);
        }
    }
}

/*
Test test46_covariantReturns failed: Error implementing class info.kgeorgiy.java.advanced.implementor.full.classes.CovariantReturns$StringChild
java.lang.AssertionError: Error implementing class info.kgeorgiy.java.advanced.implementor.full.classes.CovariantReturns$StringChild
        at info.kgeorgiy.java.advanced.implementor/info.kgeorgiy.java.advanced.implementor.BaseImplementorTest.implement(BaseImplementorTest.java:110)
        at info.kgeorgiy.java.advanced.implementor/info.kgeorgiy.java.advanced.implementor.BaseImplementorTest.test(BaseImplementorTest.java:170)
        at info.kgeorgiy.java.advanced.implementor/info.kgeorgiy.java.advanced.implementor.BaseImplementorTest.test(BaseImplementorTest.java:192)
        at info.kgeorgiy.java.advanced.implementor/info.kgeorgiy.java.advanced.implementor.AdvancedImplementorTest.test46_covariantReturns(AdvancedImplementorTest.java:51)
        at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
        at java.base/java.lang.reflect.Method.invoke(Method.java:578)
        at junit@4.11/org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:47)
        at junit@4.11/org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
        at junit@4.11/org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:44)
        at junit@4.11/org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
        at junit@4.11/org.junit.rules.TestWatcher$1.evaluate(TestWatcher.java:55)
        at junit@4.11/org.junit.rules.RunRules.evaluate(RunRules.java:20)
        at junit@4.11/org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:271)
        at junit@4.11/org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:70)
        at junit@4.11/org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:50)
        at junit@4.11/org.junit.runners.ParentRunner$3.run(ParentRunner.java:238)
        at junit@4.11/org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:63)
        at junit@4.11/org.junit.runners.ParentRunner.runChildren(ParentRunner.java:236)
        at junit@4.11/org.junit.runners.ParentRunner.access$000(ParentRunner.java:53)
        at junit@4.11/org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:229)
        at junit@4.11/org.junit.runners.ParentRunner.run(ParentRunner.java:309)
        at junit@4.11/org.junit.runners.Suite.runChild(Suite.java:127)
        at junit@4.11/org.junit.runners.Suite.runChild(Suite.java:26)
        at junit@4.11/org.junit.runners.ParentRunner$3.run(ParentRunner.java:238)
        at junit@4.11/org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:63)
        at junit@4.11/org.junit.runners.ParentRunner.runChildren(ParentRunner.java:236)
        at junit@4.11/org.junit.runners.ParentRunner.access$000(ParentRunner.java:53)
        at junit@4.11/org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:229)
        at junit@4.11/org.junit.runners.ParentRunner.run(ParentRunner.java:309)
        at junit@4.11/org.junit.runner.JUnitCore.run(JUnitCore.java:160)
        at junit@4.11/org.junit.runner.JUnitCore.run(JUnitCore.java:138)
        at junit@4.11/org.junit.runner.JUnitCore.run(JUnitCore.java:117)
        at info.kgeorgiy.java.advanced.base/info.kgeorgiy.java.advanced.base.BaseTester.test(BaseTester.java:55)
        at info.kgeorgiy.java.advanced.base/info.kgeorgiy.java.advanced.base.BaseTester.lambda$add$0(BaseTester.java:95)
        at info.kgeorgiy.java.advanced.base/info.kgeorgiy.java.advanced.base.BaseTester.test(BaseTester.java:48)
        at info.kgeorgiy.java.advanced.base/info.kgeorgiy.java.advanced.base.BaseTester.run(BaseTester.java:39)
        at info.kgeorgiy.java.advanced.implementor/info.kgeorgiy.java.advanced.implementor.Tester.main(Tester.java:22)
Caused by: info.kgeorgiy.java.advanced.implementor.ImplerException: Error while compiling generated class.
        at info.kgeorgiy.ja.trofimov.implementor.Implementor.compile(Implementor.java:420)
        at info.kgeorgiy.ja.trofimov.implementor.Implementor.implementJar(Implementor.java:383)
        at info.kgeorgiy.java.advanced.implementor/info.kgeorgiy.java.advanced.implementor.InterfaceJarImplementorTest.implementJar(InterfaceJarImplementorTest.java:42)
        at info.kgeorgiy.java.advanced.implementor/info.kgeorgiy.java.advanced.implementor.AdvancedJarImplementorTest.implement(AdvancedJarImplementorTest.java:37)
        at info.kgeorgiy.java.advanced.implementor/info.kgeorgiy.java.advanced.implementor.BaseImplementorTest.implement(BaseImplementorTest.java:103)
        ... 36 more
ERROR: Tests: failed

 */