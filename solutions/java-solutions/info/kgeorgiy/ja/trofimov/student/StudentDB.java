package info.kgeorgiy.ja.trofimov.student;

import info.kgeorgiy.java.advanced.student.*;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements AdvancedQuery {
    // Компаратор по name.
    private static final Comparator<Student> StudentComparator =
            Comparator.comparing(Student::getLastName, Comparator.reverseOrder())
                    .thenComparing(Student::getFirstName, Comparator.reverseOrder())
                    .thenComparingInt(Student::getId);

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return sortedGroupsOfSortedLists(students, StudentComparator);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return sortedGroupsOfSortedLists(students, Comparator.comparingInt(Student::getId));
    }

    @Override
    public GroupName getLargestGroup(Collection<Student> students) {
        return getBiggestGroupBy(studentsToGroupStream(students),
                Comparator.comparingInt(List::size),
                Comparator.comparing(GroupName::name));
    }

    @Override
    public GroupName getLargestGroupFirstName(Collection<Student> students) {
        return getBiggestGroupBy(studentsToGroupStream(students),
                Comparator.comparingInt(list -> getDistinctFirstNames(list).size()),
                Comparator.comparing(GroupName::name).reversed());
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return mapStudents(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return mapStudents(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return mapStudents(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return mapStudents(students, StudentDB::getFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return mapStudents(students, Student::getFirstName, Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream().max(Comparator.comparing(Student::getId)).map(Student::getFirstName).orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return getSortedStudents(students, Comparator.naturalOrder());
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return getSortedStudents(students, StudentComparator);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return filterSortedByNameList(students, Student::getFirstName, name);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return filterSortedByNameList(students, Student::getLastName, name);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return filterSortedByNameList(students, Student::getGroup, group);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return filterSortedByNameList(students, Student::getGroup, group).stream().collect(Collectors.toMap(
                Student::getLastName, Student::getFirstName, BinaryOperator.minBy(Comparator.naturalOrder())));
    }

    @Override
    public List<Map.Entry<String, String>> findStudentNamesByGroupList(List<Student> students, GroupName group) {
        return AdvancedQuery.super.findStudentNamesByGroupList(students, group);
    }

    @Override
    public String getMostPopularName(Collection<Student> students) {
        return students.stream().collect(Collectors.groupingBy(Student::getFirstName, Collectors.mapping(
                        Student::getGroup, Collectors.toSet()))).entrySet().stream().map(e -> new AbstractMap
                        .SimpleEntry<>(e.getKey(), e.getValue().size()))
                .max(Map.Entry.<String, Integer>comparingByValue().thenComparing(Map.Entry.comparingByKey(
                        Comparator.reverseOrder()))).map(Map.Entry::getKey).orElse("");
    }

    @Override
    public List<String> getFirstNames(Collection<Student> students, int[] indices) {
        return getStudentsByIndices(new ArrayList<>(students), indices, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(Collection<Student> students, int[] indices) {
        return getStudentsByIndices(new ArrayList<>(students), indices, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(Collection<Student> students, int[] indices) {
        return getStudentsByIndices(new ArrayList<>(students), indices, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(Collection<Student> students, int[] indices) {
        return getStudentsByIndices(new ArrayList<>(students), indices, StudentDB::getFullName);
    }

    // Преобразует массив студентов.
    private static <T, A, R> R mapStudents(List<Student> list, Function<? super Student, ? extends T> f,
                                           Collector<T, A, R> collector) {
        return list.stream().map(f).collect(collector);
    }

    // Вызывает функцию выше, использует Collectors.toList().
    private static <T> List<T> mapStudents(List<Student> list, Function<? super Student, ? extends T> f) {
        return mapStudents(list, f, Collectors.toList());
    }

    // Возвращает полное имя студента в формате <Имя><пробел><Фамилия>.
    private static String getFullName(Student student) {
        return student.getFirstName().concat(" ").concat(student.getLastName());
    }

    // Сортирует коллекцию по компаратору.
    private static List<Student> getSortedStudents(Collection<Student> collection,
                                                   Comparator<? super Student> comparator) {
        return collection.stream().sorted(comparator).collect(Collectors.toList());
    }

    // Фильтрует по предикату коллекцию.
    private static <T> List<Student> filterSortedByNameList(Collection<Student> collection,
                                                            Function<? super Student, ? extends T> f, T to) {
        return collection.stream().filter(s -> f.apply(s).equals(to)).sorted(StudentComparator)
                .collect(Collectors.toList());
    }

    // Набор групп, отсортированных по Group::getName,
    // в каждой группе студенты расположены по studentComparator.
    private static List<Group> sortedGroupsOfSortedLists(
            Collection<Student> collection, Comparator<Student> studentComparator) {
        return sortedGroupsOfLists(collection.stream().sorted(studentComparator).toList(),
                Comparator.comparing(Group::getName));
    }

    // Набор групп, сортированных по groupComparator.
    private static List<Group> sortedGroupsOfLists(Collection<Student> students, Comparator<Group> groupComparator) {
        return studentsToGroupStream(students).sorted(groupComparator).collect(Collectors.toList());
    }

    // Группирует студентов по группам.
    private static Stream<Group> studentsToGroupStream(Collection<Student> students) {
        return students.stream().collect(Collectors.groupingBy(Student::getGroup, HashMap::new, Collectors.toList()))
                .entrySet().stream().map(e -> new Group(e.getKey(), e.getValue()));
    }

    // Возвращает самую крупную группу по listComparator,
    // в случае равенства групп, возвращается по groupNameComparator.
    private static GroupName getBiggestGroupBy(Stream<Group> groupStream,
                                               Comparator<? super List<Student>> listComparator,
                                               Comparator<? super GroupName> groupNameComparator) {
        return groupStream.max(Comparator.comparing(Group::getStudents, listComparator).thenComparing(Group::getName,
                groupNameComparator)).map(Group::getName).orElse(null);
    }

    private static <T> List<T> getStudentsByIndices(List<Student> students, int[] indices,
                                                    Function<? super Student, ? extends T> f) {
        return Arrays.stream(indices).mapToObj(index -> students.stream().collect(Collectors.toMap(
                Student::getId, Function.identity())).get(index)).map(f).collect(Collectors.toList());
    }
}
