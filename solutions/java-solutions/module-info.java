/**
 * Course "Java Advanced", homeworks.
 *
 * @author Trofimov Nikita M32391
 */
module info.kgeorgiy.ja.trofimov {
    requires java.compiler;

    requires info.kgeorgiy.java.advanced.student;
    requires info.kgeorgiy.java.advanced.implementor;
    requires info.kgeorgiy.java.advanced.concurrent;
    requires info.kgeorgiy.java.advanced.mapper;

    exports info.kgeorgiy.ja.trofimov.walk;
    exports info.kgeorgiy.ja.trofimov.arrayset;
    exports info.kgeorgiy.ja.trofimov.implementor;
    exports info.kgeorgiy.ja.trofimov.student;
    exports info.kgeorgiy.ja.trofimov.concurrent;
}
