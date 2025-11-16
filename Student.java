import java.util.*;
import java.util.stream.Collectors;

public class Student {
    private String name;
    private Map<String, List<Integer>> subjectGrades;
    private Map<String, Double> subjectWeights;
    public Student(String name) {
        this.name = name.trim();
        this.subjectGrades = new HashMap<>();
        this.subjectWeights = new HashMap<>();
    }
    public String getName() { return name; }

    public void addGrade(String subject, int grade) {
        subject = subject.trim();
        if (grade < 0 || grade > 100) throw new IllegalArgumentException("Grade must be 0-100");
        subjectGrades.computeIfAbsent(subject, k -> new ArrayList<>()).add(grade);
        subjectWeights.putIfAbsent(subject, 1.0);
    }

    public void setSubjectWeight(String subject, double weight) {
        if (weight <= 0) throw new IllegalArgumentException("Weight must be positive");
        subjectWeights.put(subject.trim(), weight);
    }

    public double getSubjectWeight(String subject) {
        return subjectWeights.getOrDefault(subject.trim(), 1.0);
    }

    public List<Integer> getGrades(String subject) {
        return subjectGrades.getOrDefault(subject.trim(), Collections.emptyList());
    }

    public Set<String> getSubjects() { return Collections.unmodifiableSet(subjectGrades.keySet()); }

    public OptionalDouble getSubjectAverage(String subject) {
        List<Integer> g = getGrades(subject);
        if (g.isEmpty()) return OptionalDouble.empty();
        return OptionalDouble.of(g.stream().mapToInt(Integer::intValue).average().orElse(0.0));
    }

    public OptionalInt getSubjectHighest(String subject) {
        List<Integer> g = getGrades(subject);
        if (g.isEmpty()) return OptionalInt.empty();
        return OptionalInt.of(g.stream().mapToInt(Integer::intValue).max().getAsInt());
    }

    public OptionalInt getSubjectLowest(String subject) {
        List<Integer> g = getGrades(subject);
        if (g.isEmpty()) return OptionalInt.empty();
        return OptionalInt.of(g.stream().mapToInt(Integer::intValue).min().getAsInt());
    }
    public OptionalDouble getOverallAverage() {
        List<Integer> all = subjectGrades.values().stream().flatMap(List::stream).collect(Collectors.toList());
        if (all.isEmpty()) return OptionalDouble.empty();
        return OptionalDouble.of(all.stream().mapToInt(Integer::intValue).average().orElse(0.0));
    }
    public OptionalDouble getWeightedAverage() {
        double totalWeighted = 0.0;
        double totalWeight = 0.0;
        for (String subject : subjectGrades.keySet()) {
            OptionalDouble subjAvg = getSubjectAverage(subject);
            if (subjAvg.isPresent()) {
                double w = getSubjectWeight(subject);
                totalWeighted += subjAvg.getAsDouble() * w;
                totalWeight += w;
            }
        }
        if (totalWeight == 0.0) return OptionalDouble.empty();
        return OptionalDouble.of(totalWeighted / totalWeight);
    }
    public String subjectSummary(String subject) {
        List<Integer> g = getGrades(subject);
        if (g.isEmpty()) return "(no grades)";
        String gradesStr = g.toString();
        String avg = getSubjectAverage(subject).isPresent() ? String.format("%.2f", getSubjectAverage(subject).getAsDouble()) : "-";
        String high = getSubjectHighest(subject).isPresent() ? String.valueOf(getSubjectHighest(subject).getAsInt()) : "-";
        String low = getSubjectLowest(subject).isPresent() ? String.valueOf(getSubjectLowest(subject).getAsInt()) : "-";
        return String.format("Grades: %s | avg: %s | high: %s | low: %s | weight: %.2f", gradesStr, avg, high, low, getSubjectWeight(subject));
    }

        public List<String> toCsvLines() {
        List<String> lines = new ArrayList<>();
        for (String subject : subjectGrades.keySet()) {
            String gradesJoin = subjectGrades.get(subject).stream().map(Object::toString).collect(Collectors.joining(";"));
            double weight = getSubjectWeight(subject);
            lines.add(String.format("%s,%s,%s,%.2f", escapeCsv(name), escapeCsv(subject), gradesJoin, weight));
        }
        return lines;
    }

    private String escapeCsv(String s) {
        if (s.contains(",") || s.contains("\"")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
